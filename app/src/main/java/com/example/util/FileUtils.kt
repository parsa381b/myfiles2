package com.example.util

import android.content.Context
import android.content.Intent
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
        try {
            val authority = "${context.packageName}.provider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            val mime = getMimeType(file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareFiles(context: Context, files: List<File>) {
        if (files.isEmpty()) return
        try {
            val authority = "${context.packageName}.provider"
            val uris = ArrayList(files.map { FileProvider.getUriForFile(context, authority, it) })
            val intent = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_STREAM, uris.first())
                    type = getMimeType(files.first())
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    type = "*/*"
                }
            }.apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share via"))
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
