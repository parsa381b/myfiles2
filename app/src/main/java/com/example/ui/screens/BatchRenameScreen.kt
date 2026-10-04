package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import java.io.File
import java.util.Locale

enum class RenameMode {
    FIND_REPLACE,
    PREFIX_SUFFIX,
    SEQUENCE,
    CHANGE_CASE
}

enum class CaseType {
    LOWERCASE,
    UPPERCASE,
    TITLE_CASE
}

@Composable
fun BatchRenameScreen(
    files: List<File>,
    onApplyRename: (List<Pair<File, String>>) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    var selectedMode by rememberSaveable { mutableStateOf(RenameMode.FIND_REPLACE) }

    // Mode 1: Find & Replace states
    var findQuery by rememberSaveable { mutableStateOf("") }
    var replaceWith by rememberSaveable { mutableStateOf("") }

    // Mode 2: Prefix & Suffix states
    var prefixText by rememberSaveable { mutableStateOf("") }
    var suffixText by rememberSaveable { mutableStateOf("") }

    // Mode 3: Sequence states
    var baseName by rememberSaveable { mutableStateOf("Item_") }
    var startNumber by rememberSaveable { mutableIntStateOf(1) }
    var paddingDigits by rememberSaveable { mutableIntStateOf(2) }

    // Mode 4: Case states
    var selectedCase by rememberSaveable { mutableStateOf(CaseType.TITLE_CASE) }

    // Compute preview of renamed items
    val renamePairs by remember(
        files, selectedMode, findQuery, replaceWith,
        prefixText, suffixText, baseName, startNumber, paddingDigits, selectedCase
    ) {
        derivedStateOf {
            files.mapIndexed { index, file ->
                val nameWithoutExt = file.nameWithoutExtension
                val ext = if (file.extension.isNotEmpty()) ".${file.extension}" else ""

                val newName = when (selectedMode) {
                    RenameMode.FIND_REPLACE -> {
                        if (findQuery.isNotEmpty()) {
                            file.name.replace(findQuery, replaceWith)
                        } else {
                            file.name
                        }
                    }

                    RenameMode.PREFIX_SUFFIX -> {
                        val modified = "$prefixText$nameWithoutExt$suffixText"
                        "$modified$ext"
                    }

                    RenameMode.SEQUENCE -> {
                        val num = startNumber + index
                        val numStr = num.toString().padStart(paddingDigits, '0')
                        "$baseName$numStr$ext"
                    }

                    RenameMode.CHANGE_CASE -> {
                        val modifiedBase = when (selectedCase) {
                            CaseType.LOWERCASE -> nameWithoutExt.lowercase(Locale.getDefault())
                            CaseType.UPPERCASE -> nameWithoutExt.uppercase(Locale.getDefault())
                            CaseType.TITLE_CASE -> nameWithoutExt.split(" ", "_", "-")
                                .joinToString(" ") { part ->
                                    part.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                                }
                        }
                        "$modifiedBase$ext"
                    }
                }

                Pair(file, newName)
            }
        }
    }

    // Check for collisions or invalid names
    val hasCollisions by remember(renamePairs) {
        derivedStateOf {
            val names = renamePairs.map { it.second }
            names.size != names.distinct().size || names.any { it.isBlank() }
        }
    }

    val changeCount by remember(renamePairs) {
        derivedStateOf {
            renamePairs.count { it.first.name != it.second }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("batch_rename_screen")
    ) {
        // Top App Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("button_back_batch_rename")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.close)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.batch_renamer),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${files.size} items selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        HorizontalDivider()

        // Mode Selector Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedMode.ordinal,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedMode == RenameMode.FIND_REPLACE,
                onClick = { selectedMode = RenameMode.FIND_REPLACE },
                text = { Text(stringResource(R.string.find_and_replace)) },
                icon = { Icon(Icons.Default.FindReplace, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedMode == RenameMode.PREFIX_SUFFIX,
                onClick = { selectedMode = RenameMode.PREFIX_SUFFIX },
                text = { Text(stringResource(R.string.prefix_suffix)) },
                icon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedMode == RenameMode.SEQUENCE,
                onClick = { selectedMode = RenameMode.SEQUENCE },
                text = { Text(stringResource(R.string.sequence_numbering)) },
                icon = { Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedMode == RenameMode.CHANGE_CASE,
                onClick = { selectedMode = RenameMode.CHANGE_CASE },
                text = { Text(stringResource(R.string.case_change)) },
                icon = { Icon(Icons.Default.FormatSize, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        // Configuration Panel based on active mode
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                when (selectedMode) {
                    RenameMode.FIND_REPLACE -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = findQuery,
                                onValueChange = { findQuery = it },
                                label = { Text(stringResource(R.string.find_text)) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_find_query")
                            )
                            OutlinedTextField(
                                value = replaceWith,
                                onValueChange = { replaceWith = it },
                                label = { Text(stringResource(R.string.replace_with)) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_replace_with")
                            )
                        }
                    }

                    RenameMode.PREFIX_SUFFIX -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = prefixText,
                                onValueChange = { prefixText = it },
                                label = { Text(stringResource(R.string.prefix_text)) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_prefix")
                            )
                            OutlinedTextField(
                                value = suffixText,
                                onValueChange = { suffixText = it },
                                label = { Text(stringResource(R.string.suffix_text)) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_suffix")
                            )
                        }
                    }

                    RenameMode.SEQUENCE -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = baseName,
                                onValueChange = { baseName = it },
                                label = { Text(stringResource(R.string.base_name)) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1.4f)
                                    .testTag("input_base_name")
                            )
                            OutlinedTextField(
                                value = startNumber.toString(),
                                onValueChange = { it.toIntOrNull()?.let { num -> startNumber = num } },
                                label = { Text(stringResource(R.string.start_number)) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(0.8f)
                                    .testTag("input_start_number")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Digits:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            listOf(1 to "1", 2 to "01", 3 to "001", 4 to "0001").forEach { (digits, label) ->
                                val isSelected = paddingDigits == digits
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    tonalElevation = 2.dp,
                                    onClick = { paddingDigits = digits },
                                    modifier = Modifier.clip(CircleShape)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    RenameMode.CHANGE_CASE -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                CaseType.TITLE_CASE to "Title Case",
                                CaseType.LOWERCASE to "lowercase",
                                CaseType.UPPERCASE to "UPPERCASE"
                            ).forEach { (type, label) ->
                                val isSelected = selectedCase == type
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    tonalElevation = 2.dp,
                                    onClick = { selectedCase = type },
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Preview Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Preview (${renamePairs.size} items)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (hasCollisions) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Name collision detected",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "$changeCount files changed",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (changeCount > 0) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // File List Preview
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(renamePairs) { _, (file, newName) ->
                val isChanged = file.name != newName
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            // Original Name
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            // New Name
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = if (isChanged) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = newName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isChanged) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isChanged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (isChanged) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Changed",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Action Bar
        Surface(
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.cancel))
                }

                Button(
                    onClick = { onApplyRename(renamePairs) },
                    enabled = !hasCollisions && changeCount > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("button_apply_batch_rename")
                ) {
                    Icon(
                        imageVector = Icons.Default.DriveFileRenameOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.rename_files_count, changeCount))
                }
            }
        }
    }
}
