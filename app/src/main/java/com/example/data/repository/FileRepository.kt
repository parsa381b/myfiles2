package com.example.data.repository

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import androidx.core.content.ContextCompat
import com.example.data.model.FileCategory
import com.example.data.model.FileItem
import com.example.data.model.SortOption
import com.example.data.model.StorageInfo
import com.example.data.model.StorageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class FileRepository {

    suspend fun getStorageVolumes(context: Context): List<StorageInfo> = withContext(Dispatchers.IO) {
        val storages = mutableListOf<StorageInfo>()
        val seenPaths = HashSet<String>()

        fun addStorage(
            name: String,
            root: File,
            isPrimary: Boolean,
            type: StorageType
        ) {
            val canonical = try { root.canonicalPath } catch (_: Exception) { root.absolutePath }
            if (seenPaths.contains(canonical)) return
            if (!root.exists() || !root.canRead()) return

            val (total, free) = try {
                val stat = StatFs(root.path)
                Pair(stat.totalBytes, stat.availableBytes)
            } catch (_: Exception) {
                Pair(0L, 0L)
            }

            seenPaths.add(canonical)
            storages.add(
                StorageInfo(
                    name = name,
                    rootFile = root,
                    totalBytes = total,
                    freeBytes = free,
                    isPrimary = isPrimary,
                    type = type
                )
            )
        }

        // 1. Primary Internal Storage
        val primaryDir = Environment.getExternalStorageDirectory()
        if (primaryDir != null && primaryDir.exists()) {
            addStorage(
                name = "Internal Storage",
                root = primaryDir,
                isPrimary = true,
                type = StorageType.INTERNAL
            )
        }

        // 2. StorageManager volumes (API 24+)
        try {
            val sm = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
            if (sm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                for (volume in sm.storageVolumes) {
                    val desc = volume.getDescription(context) ?: ""
                    val isPrimary = volume.isPrimary

                    var dir: File? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try { volume.directory } catch (_: Exception) { null }
                    } else null

                    if (dir == null) {
                        try {
                            val getPathMethod = volume.javaClass.getMethod("getPath")
                            val path = getPathMethod.invoke(volume) as? String
                            if (path != null) dir = File(path)
                        } catch (_: Exception) {}
                    }

                    if (dir != null && dir.exists() && dir.canRead()) {
                        val type = when {
                            isPrimary -> StorageType.INTERNAL
                            isUsbDevice(desc, dir.path) -> StorageType.USB_DRIVE
                            else -> StorageType.SD_CARD
                        }
                        val displayName = when {
                            isPrimary -> "Internal Storage"
                            desc.isNotBlank() -> desc
                            type == StorageType.USB_DRIVE -> "USB Storage"
                            else -> "SD Card"
                        }
                        addStorage(displayName, dir, isPrimary, type)
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Scan ContextCompat.getExternalFilesDirs
        try {
            val externalDirs = ContextCompat.getExternalFilesDirs(context, null)
            for (dir in externalDirs) {
                if (dir != null) {
                    val root = getRootOfExternalStorage(dir)
                    if (root != null && root.exists() && root.canRead()) {
                        val isRemovable = try { Environment.isExternalStorageRemovable(root) } catch (_: Exception) { true }
                        if (isRemovable) {
                            val type = if (isUsbDevice("", root.path)) StorageType.USB_DRIVE else StorageType.SD_CARD
                            val name = if (type == StorageType.USB_DRIVE) "USB Storage" else "SD Card"
                            addStorage(name, root, isPrimary = false, type = type)
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 4. Scan /storage directory for attached removable drives (including USB OTG and SD cards)
        try {
            val storageBase = File("/storage")
            if (storageBase.exists() && storageBase.canRead()) {
                val list = storageBase.listFiles()
                for (f in list ?: emptyArray()) {
                    val n = f.name.lowercase()
                    if (f.isDirectory && f.canRead() && n != "emulated" && n != "self" && n != "knox-emulated" && n != "enc_user") {
                        val type = if (isUsbDevice("", f.path)) StorageType.USB_DRIVE else StorageType.SD_CARD
                        val name = if (type == StorageType.USB_DRIVE) "USB Storage" else "SD Card"
                        addStorage(name, f, isPrimary = false, type = type)
                    }
                }
            }
        } catch (_: Exception) {}

        // 5. Scan common mount points for USB OTG mounts
        try {
            for (mntPath in listOf("/mnt/media_rw", "/mnt/usb", "/mnt/usb_storage", "/storage/usbotg")) {
                val mntDir = File(mntPath)
                if (mntDir.exists() && mntDir.canRead() && mntDir.isDirectory) {
                    val subFiles = mntDir.listFiles()
                    if (subFiles != null && subFiles.isNotEmpty()) {
                        for (sub in subFiles) {
                            if (sub.isDirectory && sub.canRead()) {
                                addStorage("USB Storage", sub, isPrimary = false, type = StorageType.USB_DRIVE)
                            }
                        }
                    } else {
                        addStorage("USB Storage", mntDir, isPrimary = false, type = StorageType.USB_DRIVE)
                    }
                }
            }
        } catch (_: Exception) {}

        // Fallback if empty: app files dir
        if (storages.isEmpty()) {
            val fallback = context.getExternalFilesDir(null) ?: context.filesDir
            addStorage("Internal Storage", fallback, isPrimary = true, type = StorageType.INTERNAL)
        }

        // Sort: Primary first, then SD cards, then USB drives
        storages.sortedBy { storage ->
            when (storage.type) {
                StorageType.INTERNAL -> 0
                StorageType.SD_CARD -> 1
                StorageType.USB_DRIVE -> 2
            }
        }
    }

    private fun isUsbDevice(description: String, path: String): Boolean {
        val lowerDesc = description.lowercase()
        val lowerPath = path.lowercase()
        return lowerDesc.contains("usb") || lowerDesc.contains("otg") || lowerDesc.contains("drive") ||
               lowerDesc.contains("flash") || lowerDesc.contains("pendrive") ||
               lowerPath.contains("usb") || lowerPath.contains("otg") || lowerPath.contains("media_rw")
    }

    private fun getRootOfExternalStorage(appDir: File): File? {
        var current: File? = appDir
        while (current != null) {
            val parent = current.parentFile
            if (parent != null && parent.name == "Android") {
                return parent.parentFile
            }
            current = parent
        }
        return null
    }

    suspend fun getFilesInDirectory(
        directory: File,
        sortOption: SortOption = SortOption.NAME_ASC,
        showHiddenFiles: Boolean = false
    ): Result<List<FileItem>> = withContext(Dispatchers.IO) {
        try {
            if (!directory.exists()) {
                return@withContext Result.failure(IOException("Folder does not exist"))
            }
            if (!directory.canRead()) {
                return@withContext Result.failure(IOException("Folder cannot be read. Grant permission in Settings."))
            }

            val rawFiles = directory.listFiles()?.toList() ?: emptyList()
            val filteredFiles = if (showHiddenFiles) {
                rawFiles
            } else {
                rawFiles.filter { !it.name.startsWith(".") && !it.isHidden }
            }

            val mapped = filteredFiles.map { file ->
                val isDir = file.isDirectory
                val count = if (isDir) {
                    val subFiles = file.listFiles()
                    if (showHiddenFiles) {
                        subFiles?.size ?: 0
                    } else {
                        subFiles?.count { !it.name.startsWith(".") && !it.isHidden } ?: 0
                    }
                } else 0
                val isHidden = file.name.startsWith(".") || file.isHidden
                FileItem(
                    file = file,
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = isDir,
                    size = if (isDir) 0L else file.length(),
                    lastModified = file.lastModified(),
                    extension = if (isDir) "" else file.extension.lowercase(),
                    subItemCount = count,
                    isHidden = isHidden
                )
            }

            val comparator = when (sortOption) {
                SortOption.NAME_ASC -> compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name.lowercase() }
                SortOption.NAME_DESC -> compareByDescending<FileItem> { it.isDirectory }.thenByDescending { it.name.lowercase() }
                SortOption.DATE_DESC -> compareByDescending<FileItem> { it.isDirectory }.thenByDescending { it.lastModified }
                SortOption.DATE_ASC -> compareByDescending<FileItem> { it.isDirectory }.thenBy { it.lastModified }
                SortOption.SIZE_DESC -> compareByDescending<FileItem> { it.isDirectory }.thenByDescending { it.size }
                SortOption.SIZE_ASC -> compareByDescending<FileItem> { it.isDirectory }.thenBy { it.size }
            }

            Result.success(mapped.sortedWith(comparator))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchFiles(
        rootDir: File,
        query: String,
        showHiddenFiles: Boolean = false,
        maxResults: Int = 100
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<FileItem>()
        val lowercaseQuery = query.lowercase().trim()
        if (lowercaseQuery.isEmpty() || !rootDir.canRead()) return@withContext emptyList()

        fun searchRecursive(dir: File, currentDepth: Int) {
            if (currentDepth > 4 || results.size >= maxResults) return
            val list = dir.listFiles() ?: return
            for (f in list) {
                if (results.size >= maxResults) break
                val isHidden = f.name.startsWith(".") || f.isHidden
                if (!showHiddenFiles && isHidden) continue

                if (f.name.lowercase().contains(lowercaseQuery)) {
                    val isDir = f.isDirectory
                    val count = if (isDir) {
                        val subFiles = f.listFiles()
                        if (showHiddenFiles) subFiles?.size ?: 0 else subFiles?.count { !it.name.startsWith(".") && !it.isHidden } ?: 0
                    } else 0
                    results.add(
                        FileItem(
                            file = f,
                            name = f.name,
                            path = f.absolutePath,
                            isDirectory = isDir,
                            size = if (isDir) 0L else f.length(),
                            lastModified = f.lastModified(),
                            extension = if (isDir) "" else f.extension.lowercase(),
                            subItemCount = count,
                            isHidden = isHidden
                        )
                    )
                }
                if (f.isDirectory) {
                    if (showHiddenFiles || !isHidden) {
                        searchRecursive(f, currentDepth + 1)
                    }
                }
            }
        }

        searchRecursive(rootDir, 0)
        results
    }

    suspend fun getCategoryDirectory(category: FileCategory): File = withContext(Dispatchers.IO) {
        when (category) {
            FileCategory.IMAGES -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            FileCategory.VIDEOS -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            FileCategory.AUDIO -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            FileCategory.DOCUMENTS -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            FileCategory.DOWNLOADS -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            FileCategory.INSTALLATION_FILES -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }
    }

    suspend fun renameFile(targetFile: File, newName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val cleanName = newName.trim()
            if (cleanName.isEmpty() || cleanName.contains("/") || cleanName.contains("\\")) {
                return@withContext Result.failure(IllegalArgumentException("Invalid file name"))
            }
            val destination = File(targetFile.parentFile, cleanName)
            if (destination.exists()) {
                return@withContext Result.failure(IOException("A file or folder with this name already exists"))
            }
            if (targetFile.renameTo(destination)) {
                Result.success(destination)
            } else {
                Result.failure(IOException("Failed to rename file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFiles(files: List<File>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var count = 0
            for (file in files) {
                val ok = if (file.isDirectory) file.deleteRecursively() else file.delete()
                if (ok) count++
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createDirectory(parent: File, name: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val cleanName = name.trim()
            if (cleanName.isEmpty() || cleanName.contains("/") || cleanName.contains("\\")) {
                return@withContext Result.failure(IllegalArgumentException("Invalid folder name"))
            }
            val newDir = File(parent, cleanName)
            if (newDir.exists()) {
                return@withContext Result.failure(IOException("Folder already exists"))
            }
            if (newDir.mkdirs() || newDir.exists()) {
                Result.success(newDir)
            } else {
                Result.failure(IOException("Could not create folder"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSubdirectoriesIn(directory: File): Result<List<FileItem>> = withContext(Dispatchers.IO) {
        try {
            if (!directory.exists()) {
                return@withContext Result.failure(IOException("Folder does not exist"))
            }
            if (!directory.canRead()) {
                return@withContext Result.failure(IOException("Folder cannot be read"))
            }
            val dirs = directory.listFiles { file -> file.isDirectory } ?: emptyArray()
            val mapped = dirs.map { f ->
                FileItem(
                    file = f,
                    name = f.name,
                    path = f.absolutePath,
                    isDirectory = true,
                    size = 0L,
                    lastModified = f.lastModified(),
                    extension = "",
                    subItemCount = f.list { _, name -> !name.startsWith(".") }?.size ?: 0
                )
            }.sortedBy { it.name.lowercase() }
            Result.success(mapped)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun copyFileOrDirectory(source: File, destDir: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!destDir.exists()) destDir.mkdirs()
            val target = File(destDir, source.name)
            if (source.isDirectory) {
                if (!target.exists()) target.mkdirs()
                val children = source.listFiles() ?: emptyArray()
                for (child in children) {
                    if (!copyFileOrDirectory(child, target)) return@withContext false
                }
                true
            } else {
                FileInputStream(source).channel.use { inChannel ->
                    FileOutputStream(target).channel.use { outChannel ->
                        inChannel.transferTo(0, inChannel.size(), outChannel)
                    }
                }
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun moveFileOrDirectory(source: File, destDir: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!destDir.exists()) destDir.mkdirs()
            val target = File(destDir, source.name)
            if (source.renameTo(target)) {
                true
            } else {
                val copied = copyFileOrDirectory(source, destDir)
                if (copied) {
                    if (source.isDirectory) source.deleteRecursively() else source.delete()
                    true
                } else {
                    false
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun readTextFile(file: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || !file.canRead()) {
                return@withContext Result.failure(IOException("Cannot read file"))
            }
            Result.success(file.readText())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun writeTextFile(file: File, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            file.writeText(content)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
