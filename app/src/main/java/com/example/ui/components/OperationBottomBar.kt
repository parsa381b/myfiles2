package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ClipboardState
import com.example.data.model.OperationType

@Composable
fun OperationBottomBar(
    selectedCount: Int,
    canExtract: Boolean = false,
    onExtract: () -> Unit = {},
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (canExtract) {
                BottomBarActionItem(
                    icon = Icons.Default.Unarchive,
                    label = stringResource(R.string.extract),
                    onClick = onExtract,
                    testTag = "action_extract"
                )
            }
            BottomBarActionItem(
                icon = Icons.Default.ContentCopy,
                label = stringResource(R.string.copy),
                onClick = onCopy,
                testTag = "action_copy"
            )
            BottomBarActionItem(
                icon = Icons.Default.DriveFileMove,
                label = stringResource(R.string.move),
                onClick = onMove,
                testTag = "action_move"
            )
            BottomBarActionItem(
                icon = Icons.Default.Share,
                label = stringResource(R.string.share),
                onClick = onShare,
                testTag = "action_share"
            )
            BottomBarActionItem(
                icon = Icons.Default.Delete,
                label = stringResource(R.string.delete),
                onClick = onDelete,
                isDestructive = true,
                testTag = "action_delete"
            )
            if (selectedCount == 1) {
                BottomBarActionItem(
                    icon = Icons.Default.DriveFileRenameOutline,
                    label = stringResource(R.string.rename),
                    onClick = onRename,
                    testTag = "action_rename"
                )
                BottomBarActionItem(
                    icon = Icons.Default.Info,
                    label = stringResource(R.string.details),
                    onClick = onDetails,
                    testTag = "action_details"
                )
            }
        }
    }
}

@Composable
fun PasteBottomBar(
    clipboard: ClipboardState,
    onPaste: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val count = clipboard.sourceFiles.size
            val statusText = if (clipboard.type == OperationType.COPY) {
                stringResource(R.string.items_copied, count)
            } else {
                stringResource(R.string.items_ready_move, count)
            }
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.navigate_destination),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.testTag("action_cancel_paste")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cancel),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(stringResource(R.string.cancel), modifier = Modifier.padding(start = 4.dp))
                }
                Button(
                    onClick = onPaste,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("action_paste_here")
                ) {
                    Text(stringResource(R.string.paste_here))
                }
            }
        }
    }
}

@Composable
private fun BottomBarActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    testTag: String = ""
) {
    val tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag(testTag)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(42.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = tint
        )
    }
}
