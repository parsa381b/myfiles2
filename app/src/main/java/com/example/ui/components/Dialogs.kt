package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.FileItem
import com.example.data.model.SortOption
import com.example.data.model.ThemeMode
import com.example.util.FileUtils

@Composable
fun TextInputDialog(
    title: String,
    initialValue: String = "",
    confirmButtonText: String = stringResource(R.string.ok),
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var textFieldValue by remember {
        val lastDot = initialValue.lastIndexOf('.')
        val endSelection = if (lastDot > 0) lastDot else initialValue.length
        mutableStateOf(TextFieldValue(initialValue, selection = TextRange(0, endSelection)))
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("dialog_text_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (textFieldValue.text.isNotBlank()) {
                        onConfirm(textFieldValue.text.trim())
                    }
                },
                modifier = Modifier.testTag("dialog_confirm_button")
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_dismiss_button")
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun ConfirmDeleteDialog(
    count: Int,
    isTrashEnabled: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isTrashEnabled) {
                    stringResource(R.string.move_to_trash)
                } else {
                    stringResource(R.string.delete_confirm_title, count)
                },
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = if (isTrashEnabled) {
                    stringResource(R.string.confirm_move_to_trash, count)
                } else {
                    stringResource(R.string.delete_confirm_desc)
                },
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTrashEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.testTag("dialog_delete_confirm")
            ) {
                Text(
                    text = if (isTrashEnabled) stringResource(R.string.move_to_trash) else stringResource(R.string.delete)
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_delete_dismiss")
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun FileDetailsDialog(
    item: FileItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.details), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                DetailRow(label = stringResource(R.string.item_name), value = item.name)
                DetailRow(
                    label = stringResource(R.string.item_type),
                    value = if (item.isDirectory) stringResource(R.string.folder) else FileUtils.getMimeType(item.file)
                )
                if (!item.isDirectory) {
                    val formatted = FileUtils.formatFileSize(item.size)
                    val bytesLabel = stringResource(R.string.bytes)
                    DetailRow(label = stringResource(R.string.item_size), value = "$formatted (${item.size} $bytesLabel)")
                } else {
                    val itemsSuffix = stringResource(R.string.items_suffix)
                    DetailRow(label = stringResource(R.string.item_contents), value = "${item.subItemCount} $itemsSuffix")
                }
                DetailRow(label = stringResource(R.string.item_modified), value = FileUtils.formatDate(item.lastModified))
                DetailRow(label = stringResource(R.string.item_location), value = item.file.parent ?: "")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SortDialog(
    currentSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sort_by), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                SortRadioItem(stringResource(R.string.sort_name_asc), SortOption.NAME_ASC, currentSort) { onSortSelected(it); onDismiss() }
                SortRadioItem(stringResource(R.string.sort_name_desc), SortOption.NAME_DESC, currentSort) { onSortSelected(it); onDismiss() }
                SortRadioItem(stringResource(R.string.sort_date_desc), SortOption.DATE_DESC, currentSort) { onSortSelected(it); onDismiss() }
                SortRadioItem(stringResource(R.string.sort_date_asc), SortOption.DATE_ASC, currentSort) { onSortSelected(it); onDismiss() }
                SortRadioItem(stringResource(R.string.sort_size_desc), SortOption.SIZE_DESC, currentSort) { onSortSelected(it); onDismiss() }
                SortRadioItem(stringResource(R.string.sort_size_asc), SortOption.SIZE_ASC, currentSort) { onSortSelected(it); onDismiss() }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun SortRadioItem(
    label: String,
    option: SortOption,
    selectedOption: SortOption,
    onSelect: (SortOption) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(option) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = (option == selectedOption),
            onClick = { onSelect(option) }
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun SettingsDialog(
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    trashEnabled: Boolean,
    onTrashToggled: (Boolean) -> Unit,
    showHiddenFiles: Boolean,
    onShowHiddenFilesToggled: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.theme),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                SettingRadioRow(stringResource(R.string.theme_system), currentTheme == ThemeMode.SYSTEM) {
                    onThemeSelected(ThemeMode.SYSTEM)
                }
                SettingRadioRow(stringResource(R.string.theme_light), currentTheme == ThemeMode.LIGHT) {
                    onThemeSelected(ThemeMode.LIGHT)
                }
                SettingRadioRow(stringResource(R.string.theme_dark), currentTheme == ThemeMode.DARK) {
                    onThemeSelected(ThemeMode.DARK)
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                SettingRadioRow(stringResource(R.string.language_system), currentLanguage == AppLanguage.SYSTEM) {
                    onLanguageSelected(AppLanguage.SYSTEM)
                }
                SettingRadioRow(stringResource(R.string.language_en), currentLanguage == AppLanguage.ENGLISH) {
                    onLanguageSelected(AppLanguage.ENGLISH)
                }
                SettingRadioRow(stringResource(R.string.language_fa), currentLanguage == AppLanguage.PERSIAN) {
                    onLanguageSelected(AppLanguage.PERSIAN)
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.trash),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTrashToggled(!trashEnabled) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.trash_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.trash_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = trashEnabled,
                        onCheckedChange = onTrashToggled,
                        modifier = Modifier.testTag("trash_toggle_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.file_display),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onShowHiddenFilesToggled(!showHiddenFiles) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.show_hidden_files),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.show_hidden_files_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = showHiddenFiles,
                        onCheckedChange = onShowHiddenFilesToggled,
                        modifier = Modifier.testTag("show_hidden_files_switch")
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun SettingRadioRow(
    label: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
