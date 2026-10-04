package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.FileItem
import com.example.ui.theme.CategoryApks
import com.example.ui.theme.CategoryArchives
import com.example.ui.theme.CategoryAudio
import com.example.ui.theme.CategoryDocs
import com.example.ui.theme.CategoryImages
import com.example.ui.theme.CategoryVideos
import com.example.ui.theme.FolderYellow
import com.example.util.MediaThumbnailLoader

@Composable
fun FileThumbnailView(
    item: FileItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val extension = item.extension

    if (item.isDirectory) {
        Box(
            modifier = modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(FolderYellow.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = "Folder",
                tint = FolderYellow,
                modifier = Modifier.size(30.dp)
            )
        }
        return
    }

    // Audio Cover Art Thumbnail
    if (extension in setOf("mp3", "flac", "m4a", "ogg", "wav", "aac", "wma", "opus")) {
        var coverArt by remember(item.path) {
            mutableStateOf(MediaThumbnailLoader.getCachedThumbnail(item.path))
        }

        LaunchedEffect(item.path) {
            if (coverArt == null) {
                coverArt = MediaThumbnailLoader.loadAudioCoverArt(item.file)
            }
        }

        if (coverArt != null) {
            Image(
                bitmap = coverArt!!.asImageBitmap(),
                contentDescription = "Cover art",
                contentScale = ContentScale.Crop,
                modifier = modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        } else {
            DefaultIconBox(
                icon = Icons.Default.AudioFile,
                tint = CategoryAudio,
                modifier = modifier
            )
        }
        return
    }

    // APK App Icon Thumbnail
    if (extension in setOf("apk", "xapk", "apks")) {
        var apkIcon by remember(item.path) {
            mutableStateOf(MediaThumbnailLoader.getCachedThumbnail(item.path))
        }

        LaunchedEffect(item.path) {
            if (apkIcon == null) {
                apkIcon = MediaThumbnailLoader.loadApkIcon(context, item.file)
            }
        }

        if (apkIcon != null) {
            Box(
                modifier = modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = apkIcon!!.asImageBitmap(),
                    contentDescription = "APK Icon",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else {
            DefaultIconBox(
                icon = Icons.Default.Android,
                tint = CategoryApks,
                modifier = modifier
            )
        }
        return
    }

    // Video Thumbnail
    if (extension in setOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "flv", "ts")) {
        var videoThumbnail by remember(item.path) {
            mutableStateOf(MediaThumbnailLoader.getCachedThumbnail(item.path))
        }

        LaunchedEffect(item.path) {
            if (videoThumbnail == null) {
                videoThumbnail = MediaThumbnailLoader.loadVideoThumbnail(item.file)
            }
        }

        if (videoThumbnail != null) {
            Box(
                modifier = modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = videoThumbnail!!.asImageBitmap(),
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Small video play badge overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Video",
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        } else {
            DefaultIconBox(
                icon = Icons.Default.VideoFile,
                tint = CategoryVideos,
                modifier = modifier
            )
        }
        return
    }

    // Image thumbnail (via Coil AsyncImage)
    if (extension in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic")) {
        SubcomposeAsyncImage(
            model = item.file,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp)),
            error = {
                DefaultIconBox(
                    icon = Icons.Default.Image,
                    tint = CategoryImages,
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
        return
    }

    // Fallbacks for other file categories
    val (icon, tint) = when (extension) {
        "mp4", "mkv", "avi", "mov", "webm", "3gp", "flv" -> Pair(Icons.Default.VideoFile, CategoryVideos)
        "pdf", "doc", "docx", "txt", "rtf", "xlsx", "xls", "pptx", "ppt", "csv" -> Pair(Icons.Default.Description, CategoryDocs)
        "zip", "rar", "7z", "tar", "gz", "bz2" -> Pair(Icons.Default.FolderZip, CategoryArchives)
        else -> Pair(Icons.Default.InsertDriveFile, Color(0xFF90A4AE))
    }

    DefaultIconBox(icon = icon, tint = tint, modifier = modifier)
}

@Composable
private fun DefaultIconBox(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(28.dp)
        )
    }
}
