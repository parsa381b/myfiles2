package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Environment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LocalHttpServer {

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _serverUrl = MutableStateFlow<String?>(null)
    val serverUrl: StateFlow<String?> = _serverUrl.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _rootDirectory = MutableStateFlow<File>(
        Environment.getExternalStorageDirectory() ?: File("/storage/emulated/0")
    )
    val rootDirectory: StateFlow<File> = _rootDirectory.asStateFlow()

    fun setRootDirectory(dir: File) {
        _rootDirectory.value = dir
        addLog("Shared folder changed to: ${dir.name.ifEmpty { dir.absolutePath }}")
    }

    fun startServer(context: Context, port: Int = 8080): Boolean {
        if (_isRunning.value) return true

        val ip = getLocalIpAddress()
        if (ip == null) {
            addLog("Error: Could not determine local Wi-Fi IP address. Please connect to Wi-Fi.")
            return false
        }

        return try {
            val socket = ServerSocket(port)
            serverSocket = socket
            _isRunning.value = true
            val url = "http://$ip:$port"
            _serverUrl.value = url
            addLog("Server started on $url")

            serverJob = scope.launch {
                while (isActive && !socket.isClosed) {
                    try {
                        val client = socket.accept()
                        launch {
                            handleClient(client)
                        }
                    } catch (_: Exception) {
                        break
                    }
                }
            }
            true
        } catch (e: Exception) {
            addLog("Failed to start server on port $port: ${e.message}")
            stopServer()
            false
        }
    }

    fun stopServer() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverJob?.cancel()
        serverSocket = null
        serverJob = null
        _isRunning.value = false
        _serverUrl.value = null
        addLog("Server stopped")
    }

    private fun addLog(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = "[$time] $message"
        val current = _logs.value.toMutableList()
        if (current.size > 80) current.removeAt(0)
        current.add(entry)
        _logs.value = current
    }

    private fun handleClient(client: Socket) {
        val clientIp = client.inetAddress?.hostAddress ?: "Unknown"
        try {
            client.soTimeout = 10000
            val input = client.getInputStream()
            val output = BufferedOutputStream(client.getOutputStream())

            val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0]
            val rawPath = parts[1]
            val path = URLDecoder.decode(rawPath.split("?")[0], "UTF-8")

            // Read headers
            val headers = mutableMapOf<String, String>()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrBlank()) break
                val headerParts = line!!.split(":", limit = 2)
                if (headerParts.size == 2) {
                    headers[headerParts[0].trim().lowercase()] = headerParts[1].trim()
                }
            }

            if (method.equals("GET", ignoreCase = true)) {
                handleGet(clientIp, path, output)
            } else if (method.equals("POST", ignoreCase = true) && path.startsWith("/upload")) {
                handleUpload(clientIp, rawPath, headers, input, output)
            } else {
                sendResponse(output, 405, "Method Not Allowed", "text/plain", "Method not supported".toByteArray())
            }
        } catch (_: Exception) {
        } finally {
            try { client.close() } catch (_: Exception) {}
        }
    }

    private fun handleGet(clientIp: String, reqPath: String, output: OutputStream) {
        val root = _rootDirectory.value
        val relative = reqPath.removePrefix("/")
        val targetFile = if (relative.isEmpty()) root else File(root, relative)

        if (!targetFile.exists() || !targetFile.canRead()) {
            sendResponse(output, 404, "Not Found", "text/plain", "File or folder not found".toByteArray())
            return
        }

        if (targetFile.isDirectory) {
            addLog("$clientIp viewed folder: ${targetFile.name.ifEmpty { "Root" }}")
            val html = generateDirectoryHtml(targetFile)
            sendResponse(output, 200, "OK", "text/html; charset=UTF-8", html.toByteArray(Charsets.UTF_8))
        } else {
            addLog("$clientIp downloading: ${targetFile.name} (${FileUtils.formatFileSize(targetFile.length())})")
            val mime = FileUtils.getMimeType(targetFile)
            val header = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: $mime\r\n" +
                    "Content-Length: ${targetFile.length()}\r\n" +
                    "Content-Disposition: attachment; filename=\"${targetFile.name}\"\r\n" +
                    "Connection: close\r\n\r\n"
            output.write(header.toByteArray())

            FileInputStream(targetFile).use { fis ->
                val buffer = ByteArray(32768)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
            }
            output.flush()
        }
    }

    private fun handleUpload(
        clientIp: String,
        rawPath: String,
        headers: Map<String, String>,
        input: java.io.InputStream,
        output: OutputStream
    ) {
        val root = _rootDirectory.value
        val dirParam = rawPath.substringAfter("dir=", "").let {
            if (it.isNotEmpty()) URLDecoder.decode(it, "UTF-8") else ""
        }
        val targetDir = if (dirParam.isNotEmpty()) File(root, dirParam) else root

        val contentType = headers["content-type"] ?: ""
        if (!contentType.contains("multipart/form-data")) {
            sendResponse(output, 400, "Bad Request", "text/plain", "Expected multipart/form-data".toByteArray())
            return
        }

        val boundary = contentType.substringAfter("boundary=", "").trim()
        if (boundary.isEmpty()) {
            sendResponse(output, 400, "Bad Request", "text/plain", "Missing boundary".toByteArray())
            return
        }

        try {
            val contentLength = headers["content-length"]?.toLongOrNull() ?: 0L
            var uploadedFileName = "uploaded_${System.currentTimeMillis()}"

            // Read upload stream and extract file data
            val boundaryBytes = "--$boundary".toByteArray(Charsets.US_ASCII)
            val buffer = ByteArray(16384)
            var bytesRead: Int

            // Simple direct multipart parser
            val lineReader = BufferedReader(InputStreamReader(input, Charsets.ISO_8859_1))
            var currentLine: String?
            var insideFile = false
            var fileOut: java.io.FileOutputStream? = null
            var destFile: File? = null

            // Read lines until filename is found
            while (lineReader.readLine().also { currentLine = it } != null) {
                val l = currentLine ?: break
                if (l.contains("Content-Disposition:") && l.contains("filename=")) {
                    val fn = l.substringAfter("filename=\"").substringBefore("\"")
                    if (fn.isNotBlank()) {
                        uploadedFileName = File(fn).name
                        destFile = File(targetDir, uploadedFileName)
                        // Read until empty line (end of headers)
                        while (lineReader.readLine().also { currentLine = it } != null) {
                            if (currentLine.isNullOrBlank()) break
                        }
                        insideFile = true
                        break
                    }
                }
            }

            if (destFile != null) {
                fileOut = java.io.FileOutputStream(destFile)
                // Stream remaining content up to boundary
                val charBuf = CharArray(8192)
                var charsRead: Int
                val marker = "--$boundary"
                val sb = StringBuilder()

                while (lineReader.read(charBuf).also { charsRead = it } != -1) {
                    val chunk = String(charBuf, 0, charsRead)
                    val markerIdx = chunk.indexOf(marker)
                    if (markerIdx != -1) {
                        fileOut.write(chunk.substring(0, markerIdx).toByteArray(Charsets.ISO_8859_1))
                        break
                    } else {
                        fileOut.write(chunk.toByteArray(Charsets.ISO_8859_1))
                    }
                }
                fileOut.flush()
                fileOut.close()

                addLog("$clientIp uploaded file: ${destFile.name}")
                val redirectPath = if (dirParam.isNotEmpty()) "/$dirParam" else "/"
                val response = "HTTP/1.1 303 See Other\r\nLocation: $redirectPath\r\nConnection: close\r\n\r\n"
                output.write(response.toByteArray())
                output.flush()
            } else {
                sendResponse(output, 400, "Bad Request", "text/plain", "No file found in upload".toByteArray())
            }
        } catch (e: Exception) {
            addLog("Upload error from $clientIp: ${e.message}")
            sendResponse(output, 500, "Server Error", "text/plain", "Upload failed: ${e.message}".toByteArray())
        }
    }

    private fun generateDirectoryHtml(dir: File): String {
        val root = _rootDirectory.value
        val relPath = dir.relativeToOrNull(root)?.path?.replace("\\", "/") ?: ""
        val items = dir.listFiles()?.sortedWith(
            compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() }
        ) ?: emptyList()

        val sb = StringBuilder()
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'>")
        sb.append("<title>My Files - Wi-Fi Share</title>")
        sb.append("<style>")
        sb.append("body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,sans-serif;margin:0;padding:20px;background:#f8fafc;color:#1e293b}")
        sb.append(".container{max-width:920px;margin:0 auto;background:#fff;border-radius:18px;box-shadow:0 6px 24px rgba(0,0,0,0.06);overflow:hidden}")
        sb.append(".header{background:linear-gradient(135deg,#2563eb,#1d4ed8);color:#fff;padding:26px;border-bottom:1px solid #e2e8f0}")
        sb.append(".header h1{margin:0;font-size:22px;display:flex;align-items:center;gap:10px}.header p{margin:6px 0 0 0;font-size:14px;opacity:0.92}")
        sb.append(".nav{padding:14px 26px;background:#f1f5f9;font-size:14px;display:flex;align-items:center;gap:8px;flex-wrap:wrap}")
        sb.append(".nav a{color:#2563eb;text-decoration:none;font-weight:600}.nav a:hover{text-decoration:underline}")
        sb.append(".toolbar{padding:14px 26px;background:#fff;display:flex;align-items:center;justify-content:space-between;gap:12px;border-bottom:1px solid #f1f5f9;flex-wrap:wrap}")
        sb.append(".search-input{padding:8px 14px;border:1px solid #cbd5e1;border-radius:10px;font-size:14px;width:240px;outline:none}.search-input:focus{border-color:#2563eb;box-shadow:0 0 0 3px rgba(37,99,235,0.15)}")
        sb.append(".upload-card{padding:18px 26px;background:#eff6ff;border-bottom:1px solid #bfdbfe;border-radius:12px;margin:16px 26px;display:flex;align-items:center;gap:14px;flex-wrap:wrap}")
        sb.append(".upload-card input[type=file]{font-size:14px}")
        sb.append(".btn{background:#2563eb;color:#fff;border:none;padding:9px 18px;border-radius:10px;font-weight:600;cursor:pointer;font-size:14px;transition:background 0.2s}")
        sb.append(".btn:hover{background:#1d4ed8}")
        sb.append(".file-list{list-style:none;margin:0;padding:0}")
        sb.append(".file-item{display:flex;align-items:center;padding:13px 26px;border-bottom:1px solid #f1f5f9;text-decoration:none;color:inherit;transition:background 0.15s}")
        sb.append(".file-item:hover{background:#f8fafc}")
        sb.append(".icon{width:40px;font-size:24px;display:flex;align-items:center}")
        sb.append(".info{flex:1;min-width:0}.name{font-weight:600;font-size:15px;word-break:break-all}.meta{font-size:12px;color:#64748b;margin-top:2px}")
        sb.append(".download{color:#2563eb;font-weight:600;font-size:13px;padding:6px 14px;background:#eff6ff;border-radius:8px;text-decoration:none}")
        sb.append(".download:hover{background:#dbeafe}")
        sb.append("</style></head><body><div class='container'>")

        // Header
        sb.append("<div class='header'>")
        sb.append("<h1>📱 My Files - Wireless Transfer</h1>")
        sb.append("<p>Browse, download, and send files to your phone over local Wi-Fi without cables</p>")
        sb.append("</div>")

        // Breadcrumbs
        sb.append("<div class='nav'>")
        sb.append("<a href='/'>🏠 Internal Storage</a>")
        if (relPath.isNotEmpty()) {
            val parts = relPath.split("/")
            var accumulated = ""
            for (p in parts) {
                accumulated = if (accumulated.isEmpty()) p else "$accumulated/$p"
                sb.append(" <span>/</span> <a href='/$accumulated'>$p</a>")
            }
        }
        sb.append("</div>")

        // Toolbar with Quick Search & File Count
        sb.append("<div class='toolbar'>")
        sb.append("<div><input type='text' id='searchInput' class='search-input' placeholder='🔍 Search in this folder…' onkeyup='filterFiles()'></div>")
        sb.append("<div style='font-size:13px;color:#64748b;font-weight:500;'>").append(items.size).append(" items</div>")
        sb.append("</div>")

        // Upload Form
        sb.append("<div class='upload-card'>")
        sb.append("<form method='POST' action='/upload?dir=").append(relPath).append("' enctype='multipart/form-data' style='display:flex;align-items:center;gap:12px;flex-wrap:wrap;width:100%;'>")
        sb.append("<span style='font-size:14px;'><strong>📤 Upload to Phone:</strong></span>")
        sb.append("<input type='file' name='file' required style='flex:1;'>")
        sb.append("<button type='submit' class='btn'>Send File</button>")
        sb.append("</form>")
        sb.append("</div>")

        // File List
        sb.append("<ul class='file-list' id='fileList'>")
        if (dir != root) {
            val parentRel = dir.parentFile?.relativeToOrNull(root)?.path?.replace("\\", "/") ?: ""
            sb.append("<li><a class='file-item' href='/").append(parentRel).append("'><div class='icon'>📁</div><div class='info'><div class='name'>.. (Parent Folder)</div></div></a></li>")
        }

        val dateFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())

        for (f in items) {
            val isDir = f.isDirectory
            val icon = if (isDir) "📁" else when (f.extension.lowercase()) {
                "jpg", "jpeg", "png", "webp", "gif" -> "🖼️"
                "mp4", "mkv", "avi", "mov" -> "🎬"
                "mp3", "wav", "flac", "m4a" -> "🎵"
                "pdf" -> "📄"
                "zip", "rar", "7z", "tar", "gz" -> "📦"
                "apk" -> "🤖"
                else -> "📝"
            }

            val itemRel = f.relativeToOrNull(root)?.path?.replace("\\", "/") ?: f.name
            val sizeStr = if (isDir) "${f.listFiles()?.size ?: 0} items" else FileUtils.formatFileSize(f.length())
            val dateStr = dateFormat.format(Date(f.lastModified()))

            sb.append("<li class='item-entry'>")
            sb.append("<a class='file-item' href='/").append(itemRel).append("'>")
            sb.append("<div class='icon'>").append(icon).append("</div>")
            sb.append("<div class='info'>")
            sb.append("<div class='name'>").append(f.name).append("</div>")
            sb.append("<div class='meta'>").append(sizeStr).append(" • ").append(dateStr).append("</div>")
            sb.append("</div>")
            if (!isDir) {
                sb.append("<span class='download'>Download</span>")
            }
            sb.append("</a>")
            sb.append("</li>")
        }
        sb.append("</ul>")

        // Client-side quick filter script
        sb.append("<script>")
        sb.append("function filterFiles(){")
        sb.append("var q=document.getElementById('searchInput').value.toLowerCase();")
        sb.append("var items=document.querySelectorAll('.item-entry');")
        sb.append("items.forEach(function(el){")
        sb.append("var nameEl=el.querySelector('.name');")
        sb.append("if(nameEl){")
        sb.append("var text=nameEl.innerText.toLowerCase();")
        sb.append("el.style.display=text.indexOf(q)!==-1?'':'none';")
        sb.append("}")
        sb.append("});")
        sb.append("}")
        sb.append("</script>")

        sb.append("</div></body></html>")
        return sb.toString()
    }

    private fun sendResponse(output: OutputStream, status: Int, statusText: String, contentType: String, body: ByteArray) {
        val header = "HTTP/1.1 $status $statusText\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${body.size}\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray())
        output.write(body)
        output.flush()
    }

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: return null
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = intf.inetAddresses.toList()
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress
                        if (host != null && (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172."))) {
                            return host
                        }
                    }
                }
            }
            // Fallback
            for (intf in interfaces) {
                val addrs = intf.inetAddresses.toList()
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    fun getWifiName(context: Context): String {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork
            val capabilities = cm?.getNetworkCapabilities(network)

            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val info = wm?.connectionInfo
                val ssid = info?.ssid?.replace("\"", "") ?: ""
                if (ssid.isNotEmpty() && ssid != "<unknown ssid>") ssid else "Wi-Fi Network"
            } else if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) {
                "Mobile Hotspot"
            } else {
                "Local Network"
            }
        } catch (_: Exception) {
            "Wi-Fi Network"
        }
    }
}
