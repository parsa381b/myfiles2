package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import java.io.File

@Composable
fun BreadcrumbBar(
    rootDirectory: File?,
    currentDirectory: File?,
    onNavigate: (File) -> Unit,
    onNavigateHome: () -> Unit
) {
    val scrollState = rememberScrollState()

    val pathNodes = mutableListOf<File>()
    var temp = currentDirectory
    while (temp != null) {
        pathNodes.add(0, temp)
        if (rootDirectory != null && temp.absolutePath == rootDirectory.absolutePath) break
        temp = temp.parentFile
    }

    LaunchedEffect(pathNodes.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    val homeLabel = stringResource(R.string.home)
    val internalStorageLabel = stringResource(R.string.internal_storage)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onNavigateHome,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = homeLabel,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        pathNodes.forEachIndexed { index, file ->
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )

            val isLast = index == pathNodes.size - 1
            val displayName = if (index == 0 && file == rootDirectory) internalStorageLabel else file.name

            TextButton(
                onClick = { onNavigate(file) },
                enabled = !isLast
            ) {
                Text(
                    text = displayName,
                    color = if (isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
