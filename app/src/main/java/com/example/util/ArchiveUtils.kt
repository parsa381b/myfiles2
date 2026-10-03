package com.example.util

import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

enum class ArchiveFormat(val label: String, val extension: String) {
    ZIP("ZIP", "zip"),
    RAR("RAR", "rar"),
    SEVEN_Z("7Z", "7z");

    companion object {
        fun fromFile(file: File): ArchiveFormat? {
            val ext = file.extension.lowercase(Locale.ROOT)
            return when (ext) {
                "zip", "jar" -> ZIP
                "rar" -> RAR
                "7z" -> SEVEN_Z
                else -> null
            }
        }
    }
}

data class ArchiveEntryItem(
    val name: String,
    val size: Long,
    val isDirectory: Boolean
)

data class ArchiveDetails(
    val file: File,
    val format: ArchiveFormat,
    val totalEntries: Int,
    val totalUncompressedBytes: Long,
    val sampleEntries: List<ArchiveEntryItem>
)

object ArchiveUtils {

    suspend fun inspectArchive(archiveFile: File): Result<ArchiveDetails> = withContext(Dispatchers.IO) {
        try {
            val format = ArchiveFormat.fromFile(archiveFile)
                ?: return@withContext Result.failure(IllegalArgumentException("Unsupported archive format"))

            when (format) {
                ArchiveFormat.ZIP -> inspectZip(archiveFile)
                ArchiveFormat.RAR -> inspectRar(archiveFile)
                ArchiveFormat.SEVEN_Z -> inspectSevenZ(archiveFile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun inspectZip(archiveFile: File): Result<ArchiveDetails> {
        ZipFile(archiveFile).use { zip ->
            val entriesEnum = zip.entries()
            var count = 0
            var totalBytes = 0L
            val samples = mutableListOf<ArchiveEntryItem>()

            while (entriesEnum.hasMoreElements()) {
                val entry = entriesEnum.nextElement()
                count++
                val size = if (entry.size > 0) entry.size else 0L
                totalBytes += size
                if (samples.size < 50) {
                    samples.add(ArchiveEntryItem(entry.name, size, entry.isDirectory))
                }
            }

            return Result.success(
                ArchiveDetails(
                    file = archiveFile,
                    format = ArchiveFormat.ZIP,
                    totalEntries = count,
                    totalUncompressedBytes = totalBytes,
                    sampleEntries = samples
                )
            )
        }
    }

    private fun inspectRar(archiveFile: File): Result<ArchiveDetails> {
        val archive = Archive(archiveFile)
        try {
            val headers = archive.fileHeaders ?: emptyList<FileHeader>()
            var count = 0
            var totalBytes = 0L
            val samples = mutableListOf<ArchiveEntryItem>()

            for (header in headers) {
                count++
                val size = header.fullUnpackSize.coerceAtLeast(0L)
                totalBytes += size
                if (samples.size < 50) {
                    val name = header.fileName.replace('\\', '/')
                    samples.add(ArchiveEntryItem(name, size, header.isDirectory))
                }
            }

            return Result.success(
                ArchiveDetails(
                    file = archiveFile,
                    format = ArchiveFormat.RAR,
                    totalEntries = count,
                    totalUncompressedBytes = totalBytes,
                    sampleEntries = samples
                )
            )
        } finally {
            archive.close()
        }
    }

    private fun inspectSevenZ(archiveFile: File): Result<ArchiveDetails> {
        val sevenZFile = SevenZFile(archiveFile)
        try {
            var count = 0
            var totalBytes = 0L
            val samples = mutableListOf<ArchiveEntryItem>()

            var entry: SevenZArchiveEntry? = sevenZFile.nextEntry
            while (entry != null) {
                count++
                val size = entry.size.coerceAtLeast(0L)
                totalBytes += size
                if (samples.size < 50) {
                    samples.add(ArchiveEntryItem(entry.name, size, entry.isDirectory))
                }
                entry = sevenZFile.nextEntry
            }

            return Result.success(
                ArchiveDetails(
                    file = archiveFile,
                    format = ArchiveFormat.SEVEN_Z,
                    totalEntries = count,
                    totalUncompressedBytes = totalBytes,
                    sampleEntries = samples
                )
            )
        } finally {
            sevenZFile.close()
        }
    }

    suspend fun extractArchive(
        archiveFile: File,
        targetDir: File,
        onProgress: (extracted: Int, total: Int, currentName: String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            val format = ArchiveFormat.fromFile(archiveFile)
                ?: return@withContext Result.failure(IllegalArgumentException("Unsupported archive format"))

            when (format) {
                ArchiveFormat.ZIP -> extractZip(archiveFile, targetDir, onProgress)
                ArchiveFormat.RAR -> extractRar(archiveFile, targetDir, onProgress)
                ArchiveFormat.SEVEN_Z -> extractSevenZ(archiveFile, targetDir, onProgress)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractZip(
        archiveFile: File,
        targetDir: File,
        onProgress: (extracted: Int, total: Int, currentName: String) -> Unit
    ): Result<File> {
        ZipFile(archiveFile).use { zip ->
            val total = zip.size()
            var extracted = 0
            val entries = zip.entries()

            val buffer = ByteArray(16 * 1024)
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val targetFile = validateZipSlip(targetDir, entry.name)

                onProgress(extracted, total, entry.name)

                if (entry.isDirectory) {
                    targetFile.mkdirs()
                } else {
                    targetFile.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        BufferedOutputStream(FileOutputStream(targetFile)).use { output ->
                            var len: Int
                            while (input.read(buffer).also { len = it } > 0) {
                                output.write(buffer, 0, len)
                            }
                        }
                    }
                }
                extracted++
                onProgress(extracted, total, entry.name)
            }
        }
        return Result.success(targetDir)
    }

    private fun extractRar(
        archiveFile: File,
        targetDir: File,
        onProgress: (extracted: Int, total: Int, currentName: String) -> Unit
    ): Result<File> {
        val archive = Archive(archiveFile)
        try {
            val headers = archive.fileHeaders ?: emptyList()
            val total = headers.size
            var extracted = 0

            for (header in headers) {
                val normalizedName = header.fileName.replace('\\', '/')
                val targetFile = validateZipSlip(targetDir, normalizedName)

                onProgress(extracted, total, normalizedName)

                if (header.isDirectory) {
                    targetFile.mkdirs()
                } else {
                    targetFile.parentFile?.mkdirs()
                    BufferedOutputStream(FileOutputStream(targetFile)).use { output ->
                        archive.extractFile(header, output)
                    }
                }
                extracted++
                onProgress(extracted, total, normalizedName)
            }
            return Result.success(targetDir)
        } finally {
            archive.close()
        }
    }

    private fun extractSevenZ(
        archiveFile: File,
        targetDir: File,
        onProgress: (extracted: Int, total: Int, currentName: String) -> Unit
    ): Result<File> {
        // First pass: count entries
        var total = 0
        SevenZFile(archiveFile).use { countFile ->
            while (countFile.nextEntry != null) {
                total++
            }
        }

        val sevenZFile = SevenZFile(archiveFile)
        try {
            var extracted = 0
            val buffer = ByteArray(16 * 1024)

            var entry: SevenZArchiveEntry? = sevenZFile.nextEntry
            while (entry != null) {
                val targetFile = validateZipSlip(targetDir, entry.name)
                onProgress(extracted, total, entry.name)

                if (entry.isDirectory) {
                    targetFile.mkdirs()
                } else {
                    targetFile.parentFile?.mkdirs()
                    BufferedOutputStream(FileOutputStream(targetFile)).use { output ->
                        var len: Int
                        while (sevenZFile.read(buffer).also { len = it } > 0) {
                            output.write(buffer, 0, len)
                        }
                    }
                }

                extracted++
                onProgress(extracted, total, entry.name)
                entry = sevenZFile.nextEntry
            }

            return Result.success(targetDir)
        } finally {
            sevenZFile.close()
        }
    }

    /**
     * Prevents Zip Slip vulnerability by ensuring the entry path does not escape the destination directory.
     */
    private fun validateZipSlip(destDir: File, entryName: String): File {
        val destCanonical = destDir.canonicalPath
        val target = File(destDir, entryName)
        val targetCanonical = target.canonicalPath

        if (!targetCanonical.startsWith(destCanonical + File.separator) && targetCanonical != destCanonical) {
            throw SecurityException("Arbitrary file write attempt detected: $entryName")
        }
        return target
    }
}
