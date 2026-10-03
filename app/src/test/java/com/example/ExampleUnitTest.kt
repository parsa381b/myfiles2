package com.example

import com.example.data.repository.FileRepository
import com.example.util.ActiveViewer
import com.example.util.FileUtils
import com.example.util.SyntaxHighlighter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ExampleUnitTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testFormatFileSize() {
        assertEquals("0 B", FileUtils.formatFileSize(0))
        assertEquals("500 B", FileUtils.formatFileSize(500))
        assertEquals("1.0 KB", FileUtils.formatFileSize(1024))
        assertEquals("1.5 MB", FileUtils.formatFileSize((1.5 * 1024 * 1024).toLong()))
        assertEquals("2.0 GB", FileUtils.formatFileSize((2L * 1024 * 1024 * 1024)))
    }

    @Test
    fun testGetViewerForFile() {
        assertTrue(FileUtils.getViewerForFile(File("photo.jpg")) is ActiveViewer.Image)
        assertTrue(FileUtils.getViewerForFile(File("movie.mp4")) is ActiveViewer.Video)
        assertTrue(FileUtils.getViewerForFile(File("song.mp3")) is ActiveViewer.Audio)
        assertTrue(FileUtils.getViewerForFile(File("doc.pdf")) is ActiveViewer.Pdf)
        assertTrue(FileUtils.getViewerForFile(File("script.kt")) is ActiveViewer.Text)
        assertTrue(FileUtils.getViewerForFile(File("data.json")) is ActiveViewer.Text)
        assertEquals(null, FileUtils.getViewerForFile(File("archive.zip")))
    }

    @Test
    fun testSyntaxHighlighter() {
        val code = "val name = \"My Files\" // A comment"
        val highlighted = SyntaxHighlighter.highlight(code, "kt", isDark = true)
        assertEquals(code, highlighted.text)
        assertTrue(highlighted.spanStyles.isNotEmpty())
    }

    @Test
    fun testMediaThumbnailLoader() {
        val cached = com.example.util.MediaThumbnailLoader.getCachedThumbnail("/non/existent/path.mp3")
        assertEquals(null, cached)
    }

    @Test
    fun testFileRepositoryOperations() = runBlocking {
        val repo = FileRepository()
        val root = tempFolder.root

        // Create folder
        val createResult = repo.createDirectory(root, "TestFolder")
        assertTrue(createResult.isSuccess)
        val testFolder = createResult.getOrThrow()
        assertTrue(testFolder.exists() && testFolder.isDirectory)

        // Create file inside
        val sampleFile = File(testFolder, "notes.txt")
        sampleFile.writeText("Hello My Files")

        // List files
        val listResult = repo.getFilesInDirectory(testFolder)
        assertTrue(listResult.isSuccess)
        val files = listResult.getOrThrow()
        assertEquals(1, files.size)
        assertEquals("notes.txt", files[0].name)
        assertEquals("txt", files[0].extension)

        // Rename
        val renameResult = repo.renameFile(sampleFile, "renamed_notes.txt")
        assertTrue(renameResult.isSuccess)
        val renamed = renameResult.getOrThrow()
        assertTrue(renamed.exists())
        assertEquals("renamed_notes.txt", renamed.name)

        // Delete
        val deleteResult = repo.deleteFiles(listOf(testFolder))
        assertTrue(deleteResult.isSuccess)
        assertTrue(!testFolder.exists())
    }

    @Test
    fun testTrashItemExtension() {
        val trashFile = File(tempFolder.root, "item_123")
        val item = com.example.data.model.TrashItem(
            id = "123",
            originalPath = "/storage/emulated/0/Download/document.pdf",
            trashedFile = trashFile,
            name = "document.pdf",
            trashedTimestamp = System.currentTimeMillis(),
            size = 1024L,
            isDirectory = false
        )
        assertEquals("pdf", item.extension)
        assertEquals(false, item.isDirectory)
    }

    @Test
    fun testStorageInfoTypes() {
        val internal = com.example.data.model.StorageInfo(
            name = "Internal Storage",
            rootFile = File("/storage/emulated/0"),
            totalBytes = 64L * 1024 * 1024 * 1024,
            freeBytes = 32L * 1024 * 1024 * 1024,
            isPrimary = true,
            type = com.example.data.model.StorageType.INTERNAL
        )
        assertEquals(com.example.data.model.StorageType.INTERNAL, internal.type)
        assertEquals(0.5f, internal.progress, 0.01f)

        val usb = com.example.data.model.StorageInfo(
            name = "USB Storage",
            rootFile = File("/storage/1234-5678"),
            totalBytes = 32L * 1024 * 1024 * 1024,
            freeBytes = 16L * 1024 * 1024 * 1024,
            isPrimary = false,
            type = com.example.data.model.StorageType.USB_DRIVE
        )
        assertEquals(com.example.data.model.StorageType.USB_DRIVE, usb.type)
        assertEquals(false, usb.isPrimary)
    }

    @Test
    fun testPackageFormat() {
        assertEquals("APK", com.example.util.PackageFormat.APK.label)
        assertEquals("XAPK", com.example.util.PackageFormat.XAPK.label)
        assertEquals("APKS", com.example.util.PackageFormat.APKS.label)

        val pkg = com.example.util.PackageDetails(
            file = File("/test.xapk"),
            appName = "Test App",
            packageName = "com.test.app",
            versionName = "1.0.0",
            versionCode = 10,
            icon = null,
            format = com.example.util.PackageFormat.XAPK,
            totalSizeBytes = 1024L,
            splitApkNames = listOf("base.apk", "config.arm64_v8a.apk"),
            hasObb = true
        )
        assertEquals(true, pkg.hasObb)
        assertEquals(2, pkg.splitApkNames.size)
    }

    @Test
    fun testHiddenFileFlag() {
        val normalItem = com.example.data.model.FileItem(file = File("/storage/emulated/0/Documents/report.pdf"))
        assertEquals(false, normalItem.isHidden)

        val hiddenItem = com.example.data.model.FileItem(file = File("/storage/emulated/0/Documents/.nomedia"))
        assertEquals(true, hiddenItem.isHidden)

        val hiddenFolder = com.example.data.model.FileItem(file = File("/storage/emulated/0/.trash"))
        assertEquals(true, hiddenFolder.isHidden)
    }

    @Test
    fun testArchiveFormats() {
        val zipFile = File("/storage/emulated/0/Download/bundle.zip")
        assertEquals(com.example.util.ArchiveFormat.ZIP, com.example.util.ArchiveFormat.fromFile(zipFile))

        val rarFile = File("/storage/emulated/0/Download/archive.rar")
        assertEquals(com.example.util.ArchiveFormat.RAR, com.example.util.ArchiveFormat.fromFile(rarFile))

        val sevenZFile = File("/storage/emulated/0/Download/backup.7z")
        assertEquals(com.example.util.ArchiveFormat.SEVEN_Z, com.example.util.ArchiveFormat.fromFile(sevenZFile))

        val pdfFile = File("/storage/emulated/0/Download/document.pdf")
        assertEquals(null, com.example.util.ArchiveFormat.fromFile(pdfFile))
    }

    @Test
    fun testSearchDisplayFormatting() {
        val testFile = File("/storage/emulated/0/Download/invoice.pdf")
        val item = com.example.data.model.FileItem(
            file = testFile,
            size = 2048576L
        )
        assertEquals("/storage/emulated/0/Download", item.file.parent)
        val formattedSize = com.example.util.FileUtils.formatFileSize(item.size)
        assertEquals("2.0 MB", formattedSize)
    }

    @Test
    fun testCategoryExtensions() {
        val audioExts = setOf("mp3", "wav", "flac", "ogg", "m4a", "aac", "wma", "opus")
        val musicFile = File("/storage/emulated/0/Download/song.mp3")
        assertEquals(true, audioExts.contains(musicFile.extension.lowercase()))

        val videoFile = File("/storage/emulated/0/DCIM/Camera/vid.mp4")
        assertEquals(false, audioExts.contains(videoFile.extension.lowercase()))
    }

    @Test
    fun testPdfExtension() {
        val pdfFile = File("/storage/emulated/0/Download/document.pdf")
        assertEquals("pdf", pdfFile.extension.lowercase())
    }
}
