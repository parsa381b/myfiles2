package com.example.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface ActiveViewer {
    data class Image(val file: File) : ActiveViewer
    data class Video(val file: File) : ActiveViewer
    data class Audio(val file: File) : ActiveViewer
    data class Pdf(val file: File) : ActiveViewer
    data class Text(val file: File) : ActiveViewer
    data class PackageInstaller(val file: File) : ActiveViewer
    data class ArchiveExtractor(val file: File) : ActiveViewer
}

object FileUtils {

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        return String.format(Locale.US, if (index == 0) "%.0f %s" else "%.1f %s", value, units[index])
    }

    fun formatDate(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val formatter = SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    fun getMimeType(file: File): String {
        val ext = file.extension.lowercase(Locale.ROOT)
        if (ext.isEmpty()) return "*/*"
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        if (mime != null) return mime
        return when (ext) {
            "apk", "xapk", "apks" -> "application/vnd.android.package-archive"
            "pdf" -> "application/pdf"
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            "7z" -> "application/x-7z-compressed"
            "tar", "gz" -> "application/x-tar"
            "json" -> "application/json"
            "md", "txt", "log", "kt", "java", "xml", "gradle" -> "text/plain"
            else -> "*/*"
        }
    }

    fun getViewerForFile(file: File): ActiveViewer? {
        if (file.isDirectory) return null
        val ext = file.extension.lowercase(Locale.ROOT)
        return when (ext) {
            "apk", "xapk", "apks" -> ActiveViewer.PackageInstaller(file)
            "zip", "rar", "7z" -> ActiveViewer.ArchiveExtractor(file)
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "svg" -> ActiveViewer.Image(file)
            "mp4", "mkv", "avi", "mov", "webm", "3gp", "flv", "ts" -> ActiveViewer.Video(file)
            "mp3", "wav", "flac", "ogg", "m4a", "aac", "wma", "opus" -> ActiveViewer.Audio(file)
            "pdf" -> ActiveViewer.Pdf(file)
            "txt", "md", "kt", "java", "py", "js", "ts", "json", "html", "xml",
            "gradle", "properties", "sh", "c", "cpp", "h", "css", "sql", "yaml", "yml", "log", "csv", "env" -> ActiveViewer.Text(file)
            else -> null
        }
    }

    fun openFile(context: Context, file: File) {
        if (!file.exists()) {
            Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
            return
        }
        if (file.isDirectory) {
            Toast.makeText(context, "Cannot open folder as file", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val authority = "${context.packageName}.provider"
            val uri = try {
                FileProvider.getUriForFile(context, authority, file)
            } catch (_: Exception) {
                // Fallback for special mount points or SD cards
                val shareDir = File(context.cacheDir, "shared_cache").apply { mkdirs() }
                val tempFile = File(shareDir, file.name)
                file.inputStream().use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                FileProvider.getUriForFile(context, authority, tempFile)
            }

            val mime = getMimeType(file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                clipData = ClipData.newRawUri(file.name, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "Open with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareFiles(context: Context, files: List<File>) {
        val eligibleFiles = files.filter { it.isFile && it.exists() }
        if (eligibleFiles.isEmpty()) {
            Toast.makeText(context, "Folders cannot be shared. Please select files.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val authority = "${context.packageName}.provider"
            val uris = ArrayList<Uri>()

            for (f in eligibleFiles) {
                val uri = try {
                    FileProvider.getUriForFile(context, authority, f)
                } catch (e: Exception) {
                    // Fallback: Copy to app cache dir for FileProvider compatibility
                    try {
                        val shareDir = File(context.cacheDir, "shared_cache").apply { mkdirs() }
                        val tempFile = File(shareDir, f.name)
                        f.inputStream().use { input ->
                            tempFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        FileProvider.getUriForFile(context, authority, tempFile)
                    } catch (_: Exception) {
                        null
                    }
                }
                if (uri != null) {
                    uris.add(uri)
                }
            }

            if (uris.isEmpty()) {
                Toast.makeText(context, "Failed to prepare files for sharing", Toast.LENGTH_SHORT).show()
                return
            }

            val intent = if (uris.size == 1) {
                val singleUri = uris.first()
                val mime = getMimeType(eligibleFiles.first())
                Intent(Intent.ACTION_SEND).apply {
                    type = mime
                    putExtra(Intent.EXTRA_STREAM, singleUri)
                    clipData = ClipData.newRawUri(eligibleFiles.first().name, singleUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                val firstMime = getMimeType(eligibleFiles.first())
                val allSameType = eligibleFiles.all { getMimeType(it) == firstMime }
                val resolvedMime = if (allSameType) firstMime else "*/*"

                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = resolvedMime
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    val clip = ClipData.newRawUri("Shared files", uris.first())
                    for (i in 1 until uris.size) {
                        clip.addItem(ClipData.Item(uris[i]))
                    }
                    clipData = clip
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            val chooser = Intent.createChooser(intent, "Share via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Explicitly grant read URI permissions to target candidates
            val resInfoList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.queryIntentActivities(
                    chooser,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY)
            }

            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                for (u in uris) {
                    context.grantUriPermission(
                        packageName,
                        u,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }

            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share files: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val read = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            read
        }
    }

    fun openPermissionSettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }
}
