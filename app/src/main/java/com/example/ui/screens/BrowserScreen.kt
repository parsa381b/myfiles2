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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.FileItem
import com.example.ui.components.BreadcrumbBar
import com.example.ui.components.FileItemRow
import com.example.ui.viewmodel.UiState
import java.io.File

@Composable
fun BrowserScreen(
    currentDirectory: File,
    rootDirectory: File?,
    uiState: UiState,
    selectedItems: Set<FileItem>,
    searchQuery: String,
    searchResults: List<FileItem>?,
    isSearching: Boolean,
    onItemClick: (FileItem) -> Unit,
    onItemLongClick: (FileItem) -> Unit,
    onNavigate: (File) -> Unit,
    onNavigateHome: () -> Unit,
    onCreateFolder: () -> Unit,
    onCreateFile: () -> Unit,
    onSelectAll: (List<FileItem>) -> Unit,
    onOpenSort: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Breadcrumb path navigation bar
        BreadcrumbBar(
            rootDirectory = rootDirectory,
            currentDirectory = currentDirectory,
            onNavigate = onNavigate,
            onNavigateHome = onNavigateHome
        )

        // If active search results exist
        if (searchQuery.isNotBlank()) {
            SearchHeader(
                isSearching = isSearching,
                count = searchResults?.size ?: 0
            )

            if (isSearching) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                }
            } else if (searchResults.isNullOrEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Search,
                    message = stringResource(R.string.no_results_found, searchQuery)
                )
            } else {
                FileList(
                    files = searchResults,
                    selectedItems = selectedItems,
                    showPath = true,
                    onItemClick = onItemClick,
                    onItemLongClick = onItemLongClick
                )
            }
            return
        }

        // Standard directory view
        Box(modifier = Modifier.fillMaxSize()) {
            when (uiState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            IconButton(onClick = onRefresh) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.retry)
                                )
                            }
                        }
                    }
                }

                is UiState.Success -> {
                    val files = uiState.items
                    if (files.isEmpty()) {
                        EmptyFolderView(
                            onCreateFolder = onCreateFolder,
                            onCreateFile = onCreateFile
                        )
                    } else {
                        val isAllSelected = files.isNotEmpty() && files.all { it in selectedItems }

                        Column(modifier = Modifier.fillMaxSize()) {
                            BrowserToolbar(
                                count = files.size,
                                selectedCount = selectedItems.size,
                                isAllSelected = isAllSelected,
                                onCreateFolder = onCreateFolder,
                                onCreateFile = onCreateFile,
                                onSelectAll = { onSelectAll(files) },
                                onOpenSort = onOpenSort
                            )

                            FileList(
                                files = files,
                                selectedItems = selectedItems,
                                onItemClick = onItemClick,
                                onItemLongClick = onItemLongClick
                            )
                        }
                    }
                }

                is UiState.Idle -> Unit
            }

            // Expandable FAB Menu for Creating Folder & File
            if (selectedItems.isEmpty() && uiState is UiState.Success && uiState.items.isNotEmpty()) {
                var fabExpanded by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (fabExpanded) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                fabExpanded = false
                                onCreateFolder()
                            },
                            icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                            text = { Text(stringResource(R.string.create_folder)) },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.testTag("fab_create_folder")
                        )
                        ExtendedFloatingActionButton(
                            onClick = {
                                fabExpanded = false
                                onCreateFile()
                            },
                            icon = { Icon(Icons.Default.NoteAdd, contentDescription = null) },
                            text = { Text(stringResource(R.string.create_file)) },
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.testTag("fab_create_file")
                        )
                    }

                    FloatingActionButton(
                        onClick = { fabExpanded = !fabExpanded },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_add_menu")
                    ) {
                        Icon(
                            imageVector = if (fabExpanded) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = if (fabExpanded) "Close" else "Create Item"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowserToolbar(
    count: Int,
    selectedCount: Int,
    isAllSelected: Boolean,
    onCreateFolder: () -> Unit,
    onCreateFile: () -> Unit,
    onSelectAll: () -> Unit,
    onOpenSort: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val label = if (selectedCount > 0) {
            stringResource(R.string.items_selected, selectedCount)
        } else {
            stringResource(R.string.items_count, count)
        }

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (selectedCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
                onClick = onOpenSort,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("button_sort")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = stringResource(R.string.sort_by),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onCreateFolder,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("button_create_folder")
            ) {
                Icon(
                    imageVector = Icons.Default.CreateNewFolder,
                    contentDescription = stringResource(R.string.create_folder),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onCreateFile,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("button_create_file")
            ) {
                Icon(
                    imageVector = Icons.Default.NoteAdd,
                    contentDescription = stringResource(R.string.create_file),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onSelectAll,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("button_select_all")
            ) {
                Icon(
                    imageVector = if (isAllSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                    contentDescription = if (isAllSelected) "Deselect All" else stringResource(R.string.select_all),
                    tint = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchHeader(
    isSearching: Boolean,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val text = if (isSearching) {
            stringResource(R.string.searching)
        } else {
            stringResource(R.string.results_found, count)
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FileList(
    files: List<FileItem>,
    selectedItems: Set<FileItem>,
    showPath: Boolean = false,
    onItemClick: (FileItem) -> Unit,
    onItemLongClick: (FileItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("file_list_column"),
        contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)
    ) {
        items(
            items = files,
            key = { it.path },
            contentType = { it.isDirectory }
        ) { item ->
            val isSelected = selectedItems.contains(item)
            FileItemRow(
                item = item,
                isSelected = isSelected,
                isInSelectionMode = selectedItems.isNotEmpty(),
                showPath = showPath,
                onClick = { onItemClick(item) },
                onLongClick = { onItemLongClick(item) }
            )
        }
    }
}

@Composable
private fun EmptyFolderView(
    onCreateFolder: () -> Unit,
    onCreateFile: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.empty_folder),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onCreateFolder,
                    modifier = Modifier.testTag("empty_state_button_create_folder")
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.create_folder))
                }
                OutlinedButton(
                    onClick = onCreateFile,
                    modifier = Modifier.testTag("empty_state_button_create_file")
                ) {
                    Icon(
                        imageVector = Icons.Default.NoteAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.create_file))
                }
            }
        }
    }
}

@Composable
private fun EmptyStateView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
