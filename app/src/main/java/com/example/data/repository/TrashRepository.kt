package com.example.data.repository

import android.content.Context
import com.example.data.model.TrashItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

class TrashRepository(private val context: Context) {

    private fun getTrashRootDir(): File {
        val externalRoot = File("/storage/emulated/0")
        val baseDir = if (externalRoot.exists() && externalRoot.canWrite()) {
            File(externalRoot, ".myfiles_trash")
        } else {
            File(context.filesDir, "trash")
        }
        if (!baseDir.exists()) baseDir.mkdirs()
        return baseDir
    }

    private fun getTrashFilesDir(): File {
        val dir = File(getTrashRootDir(), "files")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getMetaFile(): File {
        return File(getTrashRootDir(), "trash_meta.json")
    }

    private fun readMetadata(): MutableList<TrashItem> {
        val metaFile = getMetaFile()
        if (!metaFile.exists() || !metaFile.canRead()) return mutableListOf()

        val items = mutableListOf<TrashItem>()
        try {
            val content = metaFile.readText()
            if (content.isBlank()) return mutableListOf()

            val jsonArray = JSONArray(content)
            val filesDir = getTrashFilesDir()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                val trashedFile = File(filesDir, id)
                if (trashedFile.exists()) {
                    items.add(
                        TrashItem(
                            id = id,
                            originalPath = obj.getString("originalPath"),
                            trashedFile = trashedFile,
                            name = obj.getString("name"),
                            trashedTimestamp = obj.getLong("trashedTimestamp"),
                            size = obj.getLong("size"),
                            isDirectory = obj.getBoolean("isDirectory")
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return items
    }

    private fun writeMetadata(items: List<TrashItem>) {
        try {
            val jsonArray = JSONArray()
            for (item in items) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("originalPath", item.originalPath)
                    put("name", item.name)
                    put("trashedTimestamp", item.trashedTimestamp)
                    put("size", item.size)
                    put("isDirectory", item.isDirectory)
                }
                jsonArray.put(obj)
            }
            getMetaFile().writeText(jsonArray.toString(2))
        } catch (_: Exception) {}
    }

    suspend fun getTrashItems(): Result<List<TrashItem>> = withContext(Dispatchers.IO) {
        try {
            val items = readMetadata()
            val now = System.currentTimeMillis()
            val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000

            val validItems = mutableListOf<TrashItem>()
            var hadExpired = false

            for (item in items) {
                if (now - item.trashedTimestamp > thirtyDaysMillis) {
                    // Expired after 30 days: delete permanently
                    hadExpired = true
                    if (item.isDirectory) {
                        item.trashedFile.deleteRecursively()
                    } else {
                        item.trashedFile.delete()
                    }
                } else {
                    validItems.add(item)
                }
            }

            if (hadExpired) {
                writeMetadata(validItems)
            }

            validItems.sortByDescending { it.trashedTimestamp }
            Result.success(validItems)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun moveToTrash(files: List<File>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val filesDir = getTrashFilesDir()
            val currentItems = readMetadata()
            var movedCount = 0

            for (file in files) {
                if (!file.exists()) continue
                val id = "${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
                val targetFile = File(filesDir, id)

                val success = file.renameTo(targetFile) || copyAndDelete(file, targetFile)
                if (success) {
                    currentItems.add(
                        TrashItem(
                            id = id,
                            originalPath = file.absolutePath,
                            trashedFile = targetFile,
                            name = file.name,
                            trashedTimestamp = System.currentTimeMillis(),
                            size = if (file.isDirectory) 0L else file.length(),
                            isDirectory = file.isDirectory
                        )
                    )
                    movedCount++
                }
            }

            writeMetadata(currentItems)
            Result.success(movedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreItems(items: List<TrashItem>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val allItems = readMetadata()
            var restoredCount = 0
            val itemIds = items.map { it.id }.toSet()

            for (item in items) {
                val originalFile = File(item.originalPath)
                val parent = originalFile.parentFile
                if (parent != null && !parent.exists()) {
                    parent.mkdirs()
                }

                // If a file already exists at original path, find a non-conflicting name
                var destFile = originalFile
                if (destFile.exists()) {
                    val nameWithoutExt = originalFile.nameWithoutExtension
                    val ext = originalFile.extension
                    var counter = 1
                    while (destFile.exists()) {
                        val newName = if (ext.isNotEmpty()) "$nameWithoutExt ($counter).$ext" else "$nameWithoutExt ($counter)"
                        destFile = File(parent, newName)
                        counter++
                    }
                }

                val ok = item.trashedFile.renameTo(destFile) || copyAndDelete(item.trashedFile, destFile)
                if (ok) {
                    restoredCount++
                }
            }

            val remainingItems = allItems.filterNot { itemIds.contains(it.id) }
            writeMetadata(remainingItems)
            Result.success(restoredCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePermanently(items: List<TrashItem>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val allItems = readMetadata()
            val itemIds = items.map { it.id }.toSet()
            var deletedCount = 0

            for (item in items) {
                val ok = if (item.isDirectory) {
                    item.trashedFile.deleteRecursively()
                } else {
                    item.trashedFile.delete()
                }
                if (ok) deletedCount++
            }

            val remainingItems = allItems.filterNot { itemIds.contains(it.id) }
            writeMetadata(remainingItems)
            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun emptyTrash(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val allItems = readMetadata()
            val count = allItems.size
            for (item in allItems) {
                if (item.isDirectory) item.trashedFile.deleteRecursively() else item.trashedFile.delete()
            }
            writeMetadata(emptyList())
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun copyAndDelete(source: File, target: File): Boolean {
        return try {
            if (source.isDirectory) {
                if (!target.exists()) target.mkdirs()
                val children = source.listFiles() ?: emptyArray()
                for (child in children) {
                    copyAndDelete(child, File(target, child.name))
                }
                source.deleteRecursively()
            } else {
                FileInputStream(source).channel.use { inCh ->
                    FileOutputStream(target).channel.use { outCh ->
                        inCh.transferTo(0, inCh.size(), outCh)
                    }
                }
                source.delete()
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
