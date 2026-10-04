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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.data.model.ViewMode
import com.example.ui.components.FileItemDetailedRow
import com.example.ui.components.FileItemGridCard
import com.example.ui.components.FileItemRow

@Composable
fun SearchResultsScreen(
    searchQuery: String,
    searchResults: List<FileItem>?,
    isSearching: Boolean,
    selectedItems: Set<FileItem>,
    viewMode: ViewMode = ViewMode.LIST,
    onViewModeChange: (ViewMode) -> Unit = {},
    onItemClick: (FileItem) -> Unit,
    onItemLongClick: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search Status Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val text = when {
                isSearching -> stringResource(R.string.searching)
                searchResults != null -> stringResource(R.string.results_found, searchResults.size)
                else -> stringResource(R.string.search_placeholder)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!isSearching && searchResults != null && searchResults.isNotEmpty()) {
                    Text(
                        text = "Tap folder to open • Tap file to preview",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!searchResults.isNullOrEmpty()) {
                var showViewModeMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { showViewModeMenu = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("search_button_view_mode")
                    ) {
                        val viewIcon = when (viewMode) {
                            ViewMode.GRID -> Icons.Default.GridView
                            ViewMode.DETAILED_LIST -> Icons.Default.ViewAgenda
                            ViewMode.LIST -> Icons.AutoMirrored.Filled.ViewList
                        }
                        Icon(
                            imageVector = viewIcon,
                            contentDescription = stringResource(R.string.view_layout_mode),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showViewModeMenu,
                        onDismissRequest = { showViewModeMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.view_list)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ViewList, contentDescription = null) },
                            trailingIcon = {
                                if (viewMode == ViewMode.LIST) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            onClick = {
                                onViewModeChange(ViewMode.LIST)
                                showViewModeMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.view_grid)) },
                            leadingIcon = { Icon(Icons.Default.GridView, contentDescription = null) },
                            trailingIcon = {
                                if (viewMode == ViewMode.GRID) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            onClick = {
                                onViewModeChange(ViewMode.GRID)
                                showViewModeMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.view_detailed_list)) },
                            leadingIcon = { Icon(Icons.Default.ViewAgenda, contentDescription = null) },
                            trailingIcon = {
                                if (viewMode == ViewMode.DETAILED_LIST) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            onClick = {
                                onViewModeChange(ViewMode.DETAILED_LIST)
                                showViewModeMenu = false
                            }
                        )
                    }
                }
            }
        }

        when {
            isSearching && searchResults.isNullOrEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = stringResource(R.string.searching),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            searchResults.isNullOrEmpty() && searchQuery.isNotBlank() && !isSearching -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.no_results_found, searchQuery),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            !searchResults.isNullOrEmpty() -> {
                when (viewMode) {
                    ViewMode.GRID -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 105.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("search_results_grid"),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 96.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = searchResults,
                                key = { it.path }
                            ) { item ->
                                val isSelected = selectedItems.contains(item)
                                FileItemGridCard(
                                    item = item,
                                    isSelected = isSelected,
                                    isInSelectionMode = selectedItems.isNotEmpty(),
                                    onClick = { onItemClick(item) },
                                    onLongClick = { onItemLongClick(item) }
                                )
                            }
                        }
                    }

                    ViewMode.DETAILED_LIST -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("search_results_detailed_list"),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)
                        ) {
                            items(
                                items = searchResults,
                                key = { it.path },
                                contentType = { it.isDirectory }
                            ) { item ->
                                val isSelected = selectedItems.contains(item)
                                FileItemDetailedRow(
                                    item = item,
                                    isSelected = isSelected,
                                    isInSelectionMode = selectedItems.isNotEmpty(),
                                    onClick = { onItemClick(item) },
                                    onLongClick = { onItemLongClick(item) }
                                )
                            }
                        }
                    }

                    ViewMode.LIST -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("search_results_column"),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)
                        ) {
                            items(
                                items = searchResults,
                                key = { it.path },
                                contentType = { it.isDirectory }
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

            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.search_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
