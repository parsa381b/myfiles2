package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.FileCategory
import com.example.data.model.FileItem
import com.example.ui.components.FileItemRow
import com.example.ui.theme.CategoryAudio
import com.example.ui.theme.CategoryDocs
import com.example.ui.theme.CategoryDownloads
import com.example.ui.theme.CategoryImages
import com.example.ui.theme.CategoryApks
import com.example.ui.theme.CategoryVideos
import com.example.util.FileUtils

@Composable
fun CategoryFilesScreen(
    category: FileCategory,
    files: List<FileItem>?,
    isLoading: Boolean,
    selectedItems: Set<FileItem>,
    searchQuery: String,
    onItemClick: (FileItem) -> Unit,
    onItemLongClick: (FileItem) -> Unit,
    onSelectAll: (List<FileItem>) -> Unit,
    onOpenSort: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryIcon: ImageVector = remember(category) {
        when (category) {
            FileCategory.AUDIO -> Icons.Default.AudioFile
            FileCategory.IMAGES -> Icons.Default.Image
            FileCategory.VIDEOS -> Icons.Default.VideoFile
            FileCategory.DOCUMENTS -> Icons.Default.Description
            FileCategory.DOWNLOADS -> Icons.Default.Download
            FileCategory.INSTALLATION_FILES -> Icons.Default.InstallMobile
        }
    }

    val categoryColor: Color = remember(category) {
        when (category) {
            FileCategory.AUDIO -> CategoryAudio
            FileCategory.IMAGES -> CategoryImages
            FileCategory.VIDEOS -> CategoryVideos
            FileCategory.DOCUMENTS -> CategoryDocs
            FileCategory.DOWNLOADS -> CategoryDownloads
            FileCategory.INSTALLATION_FILES -> CategoryApks
        }
    }

    val categoryTitle: String = when (category) {
        FileCategory.AUDIO -> stringResource(R.string.audio)
        FileCategory.IMAGES -> stringResource(R.string.images)
        FileCategory.VIDEOS -> stringResource(R.string.videos)
        FileCategory.DOCUMENTS -> stringResource(R.string.documents)
        FileCategory.DOWNLOADS -> stringResource(R.string.downloads)
        FileCategory.INSTALLATION_FILES -> stringResource(R.string.installation_files)
    }

    val displayFiles = remember(files, searchQuery) {
        if (files == null) return@remember null
        if (searchQuery.isBlank()) files
        else files.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }

    val totalBytes = remember(displayFiles) {
        displayFiles?.sumOf { it.size } ?: 0L
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Category Header Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(categoryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = categoryTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val count = displayFiles?.size ?: 0
                    val summary = if (count > 0) {
                        "$count items • ${FileUtils.formatFileSize(totalBytes)}"
                    } else if (isLoading) {
                        "Scanning storage…"
                    } else {
                        "0 items"
                    }
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!displayFiles.isNullOrEmpty()) {
                    IconButton(
                        onClick = onOpenSort,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = stringResource(R.string.sort_by),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { onSelectAll(displayFiles) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelectAll,
                            contentDescription = stringResource(R.string.select_all),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        when {
            isLoading && displayFiles.isNullOrEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = categoryColor,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Scanning device for $categoryTitle…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            displayFiles.isNullOrEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                stringResource(R.string.no_results_found, searchQuery)
                            } else {
                                "No $categoryTitle found on storage"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("category_files_list"),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)
                ) {
                    items(
                        items = displayFiles,
                        key = { it.path },
                        contentType = { it.extension }
                    ) { item ->
                        val isSelected = selectedItems.contains(item)
                        FileItemRow(
                            item = item,
                            isSelected = isSelected,
                            isInSelectionMode = selectedItems.isNotEmpty(),
                            showPath = true,
                            onClick = { onItemClick(item) },
                            onLongClick = { onItemLongClick(item) }
                        )
                    }
                }
            }
        }
    }
}
