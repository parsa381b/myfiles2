package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.FileItem
import com.example.data.model.OperationType
import com.example.data.model.StorageInfo
import com.example.data.model.StorageType
import com.example.ui.theme.FolderYellow
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun DestinationPickerDialog(
    operationType: OperationType,
    selectedItemsCount: Int,
    initialDirectory: File,
    rootDirectory: File?,
    storages: List<StorageInfo>,
    onGetSubdirectories: suspend (File) -> Result<List<FileItem>>,
    onCreateFolder: suspend (File, String) -> Result<File>,
    onConfirm: (File) -> Unit,
    onDismiss: () -> Unit
) {
    var currentDir by remember { mutableStateOf(initialDirectory) }
    var subdirectories by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun loadCurrentSubdirectories(dir: File) {
        scope.launch {
            isLoading = true
            errorMessage = null
            onGetSubdirectories(dir)
                .onSuccess {
                    subdirectories = it
                    isLoading = false
                }
                .onFailure {
                    errorMessage = it.localizedMessage ?: "Could not read folders"
                    isLoading = false
                }
        }
    }

    LaunchedEffect(currentDir) {
        loadCurrentSubdirectories(currentDir)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.75f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("destination_picker_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Dialog Top Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (operationType == OperationType.COPY) Icons.Default.ContentCopy else Icons.Default.DriveFileMove,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        val titleText = if (operationType == OperationType.COPY) {
                            stringResource(R.string.copy_items_title, selectedItemsCount)
                        } else {
                            stringResource(R.string.move_items_title, selectedItemsCount)
                        }
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.select_destination),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Storage Switcher (when multiple storages: Internal, SD card, USB)
                if (storages.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        storages.forEach { storage ->
                            val isCurrent = currentDir.absolutePath.startsWith(storage.rootFile.absolutePath)
                            FilterChip(
                                selected = isCurrent,
                                onClick = { currentDir = storage.rootFile },
                                label = {
                                    val label = when (storage.type) {
                                        StorageType.INTERNAL -> stringResource(R.string.internal_storage)
                                        StorageType.USB_DRIVE -> if (storage.name != "USB Storage") storage.name else stringResource(R.string.usb_storage)
                                        StorageType.SD_CARD -> if (storage.name != "SD Card") storage.name else stringResource(R.string.sd_card)
                                    }
                                    Text(label)
                                },
                                leadingIcon = {
                                    val icon = when (storage.type) {
                                        StorageType.INTERNAL -> Icons.Default.Smartphone
                                        StorageType.USB_DRIVE -> Icons.Default.Usb
                                        StorageType.SD_CARD -> Icons.Default.SdCard
                                    }
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }

                // Directory Path Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isAtStorageRoot = storages.any { it.rootFile.absolutePath == currentDir.absolutePath }
                    val canGoUp = !isAtStorageRoot && currentDir != rootDirectory && currentDir.parentFile != null
                    IconButton(
                        onClick = {
                            val parent = currentDir.parentFile
                            if (parent != null) {
                                currentDir = parent
                            }
                        },
                        enabled = canGoUp,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Up",
                            tint = if (canGoUp) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    val matchingStorage = storages.firstOrNull { it.rootFile.absolutePath == currentDir.absolutePath }
                    val folderDisplayName = when {
                        matchingStorage?.type == StorageType.INTERNAL -> stringResource(R.string.internal_storage)
                        matchingStorage?.type == StorageType.USB_DRIVE -> if (matchingStorage.name != "USB Storage") matchingStorage.name else stringResource(R.string.usb_storage)
                        matchingStorage?.type == StorageType.SD_CARD -> if (matchingStorage.name != "SD Card") matchingStorage.name else stringResource(R.string.sd_card)
                        currentDir == rootDirectory -> stringResource(R.string.internal_storage)
                        else -> currentDir.name
                    }

                    Text(
                        text = folderDisplayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    IconButton(
                        onClick = { showCreateFolderDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("picker_create_folder")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = stringResource(R.string.create_folder),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Subfolders list or status
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when {
                        isLoading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(36.dp))
                            }
                        }

                        errorMessage != null -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = errorMessage ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        subdirectories.isEmpty() -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = stringResource(R.string.no_subfolders),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = subdirectories,
                                    key = { it.path }
                                ) { folder ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { currentDir = folder.file }
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = FolderYellow,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = folder.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val itemsSuffix = stringResource(R.string.items_suffix)
                                            Text(
                                                text = "${folder.subItemCount} $itemsSuffix",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                            contentDescription = "Enter",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("picker_cancel")
                    ) {
                        Text(stringResource(R.string.cancel))
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = { onConfirm(currentDir) },
                        modifier = Modifier.testTag("picker_confirm")
                    ) {
                        Icon(
                            imageVector = if (operationType == OperationType.COPY) Icons.Default.ContentCopy else Icons.Default.DriveFileMove,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val confirmText = if (operationType == OperationType.COPY) {
                            stringResource(R.string.copy_here)
                        } else {
                            stringResource(R.string.move_here)
                        }
                        Text(confirmText)
                    }
                }
            }
        }

        if (showCreateFolderDialog) {
            TextInputDialog(
                title = stringResource(R.string.new_folder_title),
                initialValue = "",
                confirmButtonText = stringResource(R.string.create),
                onDismiss = { showCreateFolderDialog = false },
                onConfirm = { folderName ->
                    scope.launch {
                        onCreateFolder(currentDir, folderName)
                            .onSuccess {
                                loadCurrentSubdirectories(currentDir)
                            }
                        showCreateFolderDialog = false
                    }
                }
            )
        }
    }
}
