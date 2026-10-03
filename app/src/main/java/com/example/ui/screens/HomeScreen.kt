package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.FileCategory
import com.example.data.model.StorageInfo
import com.example.data.model.StorageType
import com.example.ui.components.PermissionBanner
import com.example.ui.theme.*
import com.example.util.FileUtils
import java.io.File

@Composable
fun HomeScreen(
    storages: List<StorageInfo>,
    hasStoragePermission: Boolean,
    trashEnabled: Boolean = true,
    trashCount: Int = 0,
    onGrantPermission: () -> Unit,
    onSelectStorage: (File) -> Unit,
    onCategoryClick: (FileCategory) -> Unit,
    onOpenTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp)
    ) {
        if (!hasStoragePermission) {
            item {
                PermissionBanner(onGrantClick = onGrantPermission)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        item {
            Text(
                text = stringResource(R.string.categories),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp, top = 4.dp)
            )
        }

        item {
            CategoriesSection(onCategoryClick = onCategoryClick)
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text(
                text = stringResource(R.string.storage),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )
        }

        items(storages.size) { index ->
            val storage = storages[index]
            StorageCard(
                storage = storage,
                onClick = { onSelectStorage(storage.rootFile) },
                modifier = Modifier.testTag("storage_card_${index}")
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (trashEnabled) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.utilities),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                )
            }

            item {
                TrashShortcutCard(
                    trashCount = trashCount,
                    onClick = onOpenTrash,
                    modifier = Modifier.testTag("trash_shortcut_card")
                )
            }
        }
    }
}

@Composable
private fun TrashShortcutCard(
    trashCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.trash),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                val subtitle = if (trashCount == 0) {
                    stringResource(R.string.trash_empty)
                } else {
                    val itemsSuffix = stringResource(R.string.items_suffix)
                    "$trashCount $itemsSuffix"
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun CategoriesSection(onCategoryClick: (FileCategory) -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("categories_section")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                CategoryGridItem(
                    name = stringResource(R.string.images),
                    icon = Icons.Default.Image,
                    color = CategoryImages,
                    testTag = "category_images"
                ) {
                    onCategoryClick(FileCategory.IMAGES)
                }
                CategoryGridItem(
                    name = stringResource(R.string.videos),
                    icon = Icons.Default.VideoFile,
                    color = CategoryVideos,
                    testTag = "category_videos"
                ) {
                    onCategoryClick(FileCategory.VIDEOS)
                }
                CategoryGridItem(
                    name = stringResource(R.string.audio),
                    icon = Icons.Default.AudioFile,
                    color = CategoryAudio,
                    testTag = "category_audio"
                ) {
                    onCategoryClick(FileCategory.AUDIO)
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                CategoryGridItem(
                    name = stringResource(R.string.documents),
                    icon = Icons.Default.Description,
                    color = CategoryDocs,
                    testTag = "category_docs"
                ) {
                    onCategoryClick(FileCategory.DOCUMENTS)
                }
                CategoryGridItem(
                    name = stringResource(R.string.downloads),
                    icon = Icons.Default.Download,
                    color = CategoryDownloads,
                    testTag = "category_downloads"
                ) {
                    onCategoryClick(FileCategory.DOWNLOADS)
                }
                CategoryGridItem(
                    name = stringResource(R.string.installation_files),
                    icon = Icons.Default.Android,
                    color = CategoryApks,
                    testTag = "category_apks"
                ) {
                    onCategoryClick(FileCategory.INSTALLATION_FILES)
                }
            }
        }
    }
}

@Composable
private fun CategoryGridItem(
    name: String,
    icon: ImageVector,
    color: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StorageCard(
    storage: StorageInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = when (storage.type) {
        StorageType.INTERNAL -> stringResource(R.string.internal_storage)
        StorageType.USB_DRIVE -> if (storage.name != "USB Storage") storage.name else stringResource(R.string.usb_storage)
        StorageType.SD_CARD -> if (storage.name != "SD Card") storage.name else stringResource(R.string.sd_card)
    }

    val icon = when (storage.type) {
        StorageType.INTERNAL -> Icons.Default.Smartphone
        StorageType.USB_DRIVE -> Icons.Default.Usb
        StorageType.SD_CARD -> Icons.Default.SdCard
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${(storage.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { storage.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(6.dp))

                val used = FileUtils.formatFileSize(storage.usedBytes)
                val total = FileUtils.formatFileSize(storage.totalBytes)
                val free = FileUtils.formatFileSize(storage.freeBytes)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$used / $total",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.free_space, free),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
