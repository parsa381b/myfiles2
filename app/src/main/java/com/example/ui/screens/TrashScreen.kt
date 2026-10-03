package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.FileItem
import com.example.data.model.TrashItem
import com.example.ui.components.FileThumbnailView
import com.example.util.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    trashItems: List<TrashItem>,
    onRestoreItems: (List<TrashItem>) -> Unit,
    onDeletePermanently: (List<TrashItem>) -> Unit,
    onEmptyTrash: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedItems by remember { mutableStateOf<Set<TrashItem>>(emptySet()) }
    var showEmptyConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.trash),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("trash_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.close)
                        )
                    }
                },
                actions = {
                    if (trashItems.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                if (selectedItems.size == trashItems.size) {
                                    selectedItems = emptySet()
                                } else {
                                    selectedItems = trashItems.toSet()
                                }
                            }
                        ) {
                            Text(
                                if (selectedItems.size == trashItems.size) {
                                    stringResource(R.string.cancel)
                                } else {
                                    stringResource(R.string.select_all)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (trashItems.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedItems.isNotEmpty()) {
                            // Restore Selected
                            Button(
                                onClick = {
                                    onRestoreItems(selectedItems.toList())
                                    selectedItems = emptySet()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("trash_restore_button")
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.restore))
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Delete Selected Permanently
                            Button(
                                onClick = { showDeleteConfirmDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("trash_delete_button")
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.delete_permanently))
                            }
                        } else {
                            // Restore All
                            Button(
                                onClick = { onRestoreItems(trashItems) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("trash_restore_all_button")
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.restore_all))
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Empty Trash
                            Button(
                                onClick = { showEmptyConfirmDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("trash_empty_button")
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.empty_trash))
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (trashItems.isEmpty()) {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = stringResource(R.string.trash_empty),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.trash_notice),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.trash_notice),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(
                        items = trashItems,
                        key = { it.id }
                    ) { item ->
                        val isSelected = selectedItems.contains(item)
                        val dummyFileItem = remember(item) {
                            FileItem(
                                file = item.trashedFile,
                                name = item.name,
                                path = item.originalPath,
                                isDirectory = item.isDirectory,
                                size = item.size,
                                lastModified = item.trashedTimestamp,
                                extension = item.extension,
                                subItemCount = 0
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    val current = selectedItems.toMutableSet()
                                    if (isSelected) current.remove(item) else current.add(item)
                                    selectedItems = current
                                },
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = if (isSelected) "Selected" else "Not selected",
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(end = 4.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                FileThumbnailView(item = dummyFileItem)

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    val subtitle = if (item.isDirectory) {
                                        FileUtils.formatDate(item.trashedTimestamp)
                                    } else {
                                        "${FileUtils.formatFileSize(item.size)} | ${FileUtils.formatDate(item.trashedTimestamp)}"
                                    }

                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }

        // Empty Confirm Dialog
        if (showEmptyConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showEmptyConfirmDialog = false },
                title = { Text(stringResource(R.string.empty_trash)) },
                text = { Text(stringResource(R.string.empty_trash_confirm)) },
                confirmButton = {
                    Button(
                        onClick = {
                            onEmptyTrash()
                            selectedItems = emptySet()
                            showEmptyConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.delete_permanently))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmptyConfirmDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }

        // Delete Confirm Dialog
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text(stringResource(R.string.delete_permanently)) },
                text = { Text(stringResource(R.string.confirm_delete_permanent, selectedItems.size)) },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeletePermanently(selectedItems.toList())
                            selectedItems = emptySet()
                            showDeleteConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.delete_permanently))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}
