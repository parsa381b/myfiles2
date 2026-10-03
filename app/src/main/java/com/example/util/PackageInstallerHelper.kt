package com.example.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipFile

enum class PackageFormat(val label: String) {
    APK("APK"),
    XAPK("XAPK"),
    APKS("APKS")
}

data class PackageDetails(
    val file: File,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val icon: Bitmap?,
    val format: PackageFormat,
    val totalSizeBytes: Long,
    val splitApkNames: List<String> = emptyList(),
    val hasObb: Boolean = false,
    val minSdkVersion: Int = 0
)

object PackageInstallerHelper {

    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                val fallbackIntent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            }
        }
    }

    suspend fun parsePackageDetails(context: Context, file: File): PackageDetails? = withContext(Dispatchers.IO) {
        if (!file.exists() || !file.canRead()) return@withContext null
        val ext = file.extension.lowercase()

        when (ext) {
            "apk" -> parseSingleApk(context, file)
            "xapk", "apks", "zip" -> parseArchivePackage(context, file, if (ext == "xapk") PackageFormat.XAPK else PackageFormat.APKS)
            else -> null
        }
    }

    private fun parseSingleApk(context: Context, file: File): PackageDetails? {
        return try {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) PackageManager.MATCH_UNINSTALLED_PACKAGES else 0
            val pi = pm.getPackageArchiveInfo(file.absolutePath, flags) ?: pm.getPackageArchiveInfo(file.absolutePath, 0)
            val ai = pi?.applicationInfo ?: return null
            ai.sourceDir = file.absolutePath
            ai.publicSourceDir = file.absolutePath

            val appName = try {
                ai.loadLabel(pm).toString().takeIf { it.isNotBlank() } ?: file.nameWithoutExtension
            } catch (_: Exception) {
                file.nameWithoutExtension
            }

            val icon = try {
                MediaThumbnailLoader.drawableToBitmap(ai.loadIcon(pm))
            } catch (_: Exception) {
                null
            }

            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pi.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pi.versionCode.toLong()
            }

            PackageDetails(
                file = file,
                appName = appName,
                packageName = pi.packageName ?: "",
                versionName = pi.versionName ?: "1.0",
                versionCode = versionCode,
                icon = icon,
                format = PackageFormat.APK,
                totalSizeBytes = file.length(),
                splitApkNames = listOf(file.name),
                hasObb = false,
                minSdkVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) ai.minSdkVersion else 0
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseArchivePackage(context: Context, file: File, format: PackageFormat): PackageDetails? {
        var zip: ZipFile? = null
        var tempApk: File? = null
        try {
            zip = ZipFile(file)
            val entries = zip.entries().toList()

            // Find all .apk entries
            val apkEntries = entries.filter { !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true) }
            if (apkEntries.isEmpty()) return null

            val hasObb = entries.any { !it.isDirectory && (it.name.contains(".obb", ignoreCase = true) || it.name.startsWith("Android/obb", ignoreCase = true)) }

            // Find base apk (prefer "base.apk" or entry with "base" or the largest APK)
            val baseEntry = apkEntries.firstOrNull { it.name.equals("base.apk", ignoreCase = true) }
                ?: apkEntries.firstOrNull { it.name.contains("base", ignoreCase = true) }
                ?: apkEntries.maxByOrNull { it.size }
                ?: apkEntries.first()

            // Extract base entry temporarily to cache to inspect package info
            val tempDir = File(context.cacheDir, "pkg_inspect").apply { mkdirs() }
            tempApk = File(tempDir, "temp_${System.currentTimeMillis()}.apk")
            zip.getInputStream(baseEntry).use { input ->
                FileOutputStream(tempApk).use { output ->
                    input.copyTo(output)
                }
            }

            val parsedBase = parseSingleApk(context, tempApk)

            // Look for standalone icon.png in zip root if not extracted
            var customIcon = parsedBase?.icon
            if (customIcon == null) {
                val iconEntry = entries.firstOrNull { !it.isDirectory && it.name.equals("icon.png", ignoreCase = true) }
                if (iconEntry != null) {
                    zip.getInputStream(iconEntry).use { inStream ->
                        customIcon = BitmapFactory.decodeStream(inStream)
                    }
                }
            }

            val appName = parsedBase?.appName ?: file.nameWithoutExtension
            val packageName = parsedBase?.packageName ?: ""
            val versionName = parsedBase?.versionName ?: "1.0"
            val versionCode = parsedBase?.versionCode ?: 1L
            val splitNames = apkEntries.map { File(it.name).name }

            return PackageDetails(
                file = file,
                appName = appName,
                packageName = packageName,
                versionName = versionName,
                versionCode = versionCode,
                icon = customIcon,
                format = format,
                totalSizeBytes = file.length(),
                splitApkNames = splitNames,
                hasObb = hasObb,
                minSdkVersion = parsedBase?.minSdkVersion ?: 0
            )
        } catch (_: Exception) {
            return null
        } finally {
            try { zip?.close() } catch (_: Exception) {}
            try { tempApk?.delete() } catch (_: Exception) {}
        }
    }

    suspend fun installPackage(
        context: Context,
        details: PackageDetails,
        onProgress: (step: String, progress: Float) -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val file = details.file
        if (!file.exists()) {
            withContext(Dispatchers.Main) { onError("Package file does not exist") }
            return@withContext
        }

        // Single APK installation
        if (details.format == PackageFormat.APK) {
            withContext(Dispatchers.Main) {
                try {
                    val authority = "${context.packageName}.provider"
                    val uri = FileProvider.getUriForFile(context, authority, file)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    onSuccess()
                } catch (e: Exception) {
                    onError("Failed to launch package installer: ${e.message}")
                }
            }
            return@withContext
        }

        // Multi-APK / XAPK / APKS Installation via PackageInstaller
        var zip: ZipFile? = null
        val tempExtractDir = File(context.cacheDir, "install_session_${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            withContext(Dispatchers.Main) { onProgress("Scanning package contents...", 0.1f) }
            zip = ZipFile(file)
            val entries = zip.entries().toList()

            // Extract OBB expansion files if present
            val obbEntries = entries.filter { !it.isDirectory && (it.name.contains(".obb", ignoreCase = true) || it.name.startsWith("Android/obb", ignoreCase = true)) }
            if (obbEntries.isNotEmpty() && details.packageName.isNotEmpty()) {
                withContext(Dispatchers.Main) { onProgress("Extracting OBB game data...", 0.25f) }
                val targetObbDir = File("/storage/emulated/0/Android/obb/${details.packageName}").apply { mkdirs() }
                for (obb in obbEntries) {
                    val obbFileName = File(obb.name).name
                    val destObbFile = File(targetObbDir, obbFileName)
                    zip.getInputStream(obb).use { input ->
                        FileOutputStream(destObbFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }

            // Extract split APKs
            val apkEntries = entries.filter { !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true) }
            if (apkEntries.isEmpty()) {
                withContext(Dispatchers.Main) { onError("No APK files found inside archive") }
                return@withContext
            }

            withContext(Dispatchers.Main) { onProgress("Extracting split packages...", 0.4f) }
            val extractedFiles = mutableListOf<File>()
            for ((index, entry) in apkEntries.withIndex()) {
                val apkName = File(entry.name).name
                val extractedFile = File(tempExtractDir, apkName)
                zip.getInputStream(entry).use { input ->
                    FileOutputStream(extractedFile).use { output ->
                        input.copyTo(output)
                    }
                }
                extractedFiles.add(extractedFile)
                val prog = 0.4f + (0.3f * (index + 1) / apkEntries.size)
                withContext(Dispatchers.Main) { onProgress("Extracted $apkName...", prog) }
            }

            // If only one APK in the archive and no OBB, we can install directly via FileProvider
            if (extractedFiles.size == 1 && obbEntries.isEmpty()) {
                withContext(Dispatchers.Main) {
                    try {
                        val singleApk = extractedFiles.first()
                        val authority = "${context.packageName}.provider"
                        val uri = FileProvider.getUriForFile(context, authority, singleApk)
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/vnd.android.package-archive")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                        onSuccess()
                    } catch (e: Exception) {
                        onError("Failed to launch package installer: ${e.message}")
                    }
                }
                return@withContext
            }

            // Multi-part PackageInstaller session
            withContext(Dispatchers.Main) { onProgress("Preparing installation session...", 0.75f) }
            val packageInstaller = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            if (details.packageName.isNotEmpty()) {
                params.setAppPackageName(details.packageName)
            }

            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)

            try {
                for ((index, apk) in extractedFiles.withIndex()) {
                    withContext(Dispatchers.Main) {
                        onProgress("Writing ${apk.name}...", 0.8f + (0.15f * (index + 1) / extractedFiles.size))
                    }
                    session.openWrite(apk.name, 0, apk.length()).use { out ->
                        apk.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                }

                withContext(Dispatchers.Main) { onProgress("Waiting for confirmation...", 0.98f) }

                val receiverIntent = Intent(context, PackageInstallReceiver::class.java).apply {
                    action = PackageInstallReceiver.ACTION_INSTALL_STATUS
                    setPackage(context.packageName)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    receiverIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )

                session.commit(pendingIntent.intentSender)
                withContext(Dispatchers.Main) { onSuccess() }
            } finally {
                session.close()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("Installation error: ${e.message ?: "Unknown error"}")
            }
        } finally {
            try { zip?.close() } catch (_: Exception) {}
        }
    }
}
