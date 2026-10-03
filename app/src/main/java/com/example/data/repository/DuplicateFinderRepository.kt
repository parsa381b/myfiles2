package com.example.data.repository

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.example.data.model.DuplicateGroup
import com.example.data.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.security.MessageDigest

class DuplicateFinderRepository {

    suspend fun findDuplicates(context: Context): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        val root = Environment.getExternalStorageDirectory()
        val candidateFiles = ArrayList<File>()

        // 1. Gather all files via MediaStore (ultra-fast)
        try {
            val uri = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.SIZE
            )

            context.contentResolver.query(
                uri,
                projection,
                "${MediaStore.Files.FileColumns.SIZE} > 1024", // Minimum 1 KB
                null,
                null
            )?.use { cursor ->
                val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                val sizeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)

                while (cursor.moveToNext()) {
                    val path = if (dataCol != -1) cursor.getString(dataCol) else null
                    val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L

                    if (path != null && size > 1024) {
                        val file = File(path)
                        if (file.exists() && file.isFile && !file.name.startsWith(".")) {
                            candidateFiles.add(file)
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // Fallback / supplement from file system if MediaStore returned few items
        if (candidateFiles.size < 50 && root != null && root.exists()) {
            val queue = ArrayDeque<File>()
            queue.add(root)
            var count = 0
            while (queue.isNotEmpty() && count < 3000) {
                val dir = queue.removeFirst()
                val list = dir.listFiles() ?: continue
                for (f in list) {
                    count++
                    if (f.isDirectory) {
                        if (!f.name.startsWith(".") && f.name != "Android") {
                            queue.add(f)
                        }
                    } else if (f.isFile && f.length() > 1024 && !f.name.startsWith(".")) {
                        candidateFiles.add(f)
                    }
                }
            }
        }

        // 2. Group candidate files by size
        val sizeMap = candidateFiles.groupBy { it.length() }
            .filter { it.value.size >= 2 }

        val duplicateGroups = ArrayList<DuplicateGroup>()

        // 3. For size collisions, verify content with fast partial & full hashing
        for ((size, files) in sizeMap) {
            val hashMap = HashMap<String, MutableList<File>>()

            for (file in files) {
                val hash = computeFastHash(file) ?: continue
                hashMap.getOrPut(hash) { mutableListOf() }.add(file)
            }

            for ((hash, matchingFiles) in hashMap) {
                if (matchingFiles.size >= 2) {
                    // Double check with full or deeper verification if necessary
                    val verified = verifyExactDuplicates(matchingFiles)
                    for ((subHash, verifiedFiles) in verified) {
                        if (verifiedFiles.size >= 2) {
                            val items = verifiedFiles
                                .map { f ->
                                    FileItem(
                                        file = f,
                                        name = f.name,
                                        path = f.absolutePath,
                                        isDirectory = false,
                                        size = f.length(),
                                        lastModified = f.lastModified()
                                    )
                                }
                                .sortedBy { it.lastModified } // Oldest original first

                            duplicateGroups.add(
                                DuplicateGroup(
                                    id = subHash,
                                    fileSize = size,
                                    items = items
                                )
                            )
                        }
                    }
                }
            }
        }

        // Sort by total recoverable space descending (wasted bytes = fileSize * (count - 1))
        duplicateGroups.sortByDescending { it.fileSize * (it.items.size - 1) }
        duplicateGroups
    }

    private fun computeFastHash(file: File): String? {
        return try {
            val length = file.length()
            val md = MessageDigest.getInstance("MD5")
            md.update(length.toString().toByteArray())

            val sampleSize = 4096
            if (length <= sampleSize * 2) {
                // Small file: hash entirety
                FileInputStream(file).use { input ->
                    val buffer = ByteArray(sampleSize)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        md.update(buffer, 0, read)
                    }
                }
            } else {
                // Large file: hash header and footer samples
                RandomAccessFile(file, "r").use { raf ->
                    val head = ByteArray(sampleSize)
                    raf.readFully(head)
                    md.update(head)

                    raf.seek(length - sampleSize)
                    val tail = ByteArray(sampleSize)
                    raf.readFully(tail)
                    md.update(tail)
                }
            }

            md.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            null
        }
    }

    private fun verifyExactDuplicates(files: List<File>): Map<String, List<File>> {
        val verifiedMap = HashMap<String, MutableList<File>>()
        for (f in files) {
            val fullHash = computeFullHash(f) ?: continue
            verifiedMap.getOrPut(fullHash) { mutableListOf() }.add(f)
        }
        return verifiedMap
    }

    private fun computeFullHash(file: File): String? {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            FileInputStream(file).use { input ->
                var read: Int
                var totalRead = 0L
                val maxHashBytes = 20 * 1024 * 1024L // Max 20MB scan for performance
                while (input.read(buffer).also { read = it } != -1) {
                    md.update(buffer, 0, read)
                    totalRead += read
                    if (totalRead >= maxHashBytes) break
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            null
        }
    }
}
