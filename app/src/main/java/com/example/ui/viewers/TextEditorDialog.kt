package com.example.ui.viewers

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.util.FileUtils
import com.example.util.SyntaxHighlighter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun TextEditorDialog(
    file: File,
    onSaveFile: suspend (File, String) -> Result<Unit>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    var textContent by remember { mutableStateOf("") }
    var originalContent by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isEditMode by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val extension = remember(file) { file.extension.lowercase() }

    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            val content = try {
                file.readText()
            } catch (e: Exception) {
                "Error reading file: ${e.localizedMessage}"
            }
            textContent = content
            originalContent = content
            isLoading = false
        }
    }

    val isModified = textContent != originalContent
    val lineCount = remember(textContent) { textContent.count { it == '\n' } + 1 }
    val charCount = textContent.length
    val wordCount = remember(textContent) {
        if (textContent.isBlank()) 0 else textContent.trim().split("\\s+".toRegex()).size
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("text_editor_dialog"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Action Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.close)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isModified) "● Modified" else extension.uppercase(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isModified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { isEditMode = !isEditMode },
                            modifier = Modifier.testTag("text_editor_mode_toggle")
                        ) {
                            Icon(
                                imageVector = if (isEditMode) Icons.Default.Visibility else Icons.Default.Edit,
                                contentDescription = if (isEditMode) stringResource(R.string.view_mode) else stringResource(R.string.edit),
                                tint = if (isEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                scope.launch {
                                    isSaving = true
                                    onSaveFile(file, textContent)
                                        .onSuccess {
                                            originalContent = textContent
                                            Toast.makeText(context, "Saved successfully", Toast.LENGTH_SHORT).show()
                                        }
                                        .onFailure {
                                            Toast.makeText(context, "Failed to save: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    isSaving = false
                                }
                            },
                            enabled = isModified && !isSaving,
                            modifier = Modifier.testTag("text_editor_save")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = stringResource(R.string.save),
                                tint = if (isModified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            )
                        }

                        IconButton(onClick = { FileUtils.shareFiles(context, listOf(file)) }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.share)
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Editor / Viewer Body
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val verticalScroll = rememberScrollState()
                    val horizontalScroll = rememberScrollState()

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        // Line Numbers Column
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(46.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .verticalScroll(verticalScroll)
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            for (i in 1..lineCount) {
                                Text(
                                    text = "$i",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        lineHeight = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                )
                            }
                        }

                        VerticalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // Text Content Area with Syntax Highlighting
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(verticalScroll)
                                .horizontalScroll(horizontalScroll)
                                .padding(12.dp)
                        ) {
                            val codeStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            val syntaxTransformation = remember(extension, isDark) {
                                VisualTransformation { annotated ->
                                    val highlighted = SyntaxHighlighter.highlight(annotated.text, extension, isDark)
                                    TransformedText(highlighted, OffsetMapping.Identity)
                                }
                            }

                            BasicTextField(
                                value = textContent,
                                onValueChange = {
                                    if (isEditMode) {
                                        textContent = it
                                    }
                                },
                                readOnly = !isEditMode,
                                textStyle = codeStyle,
                                visualTransformation = syntaxTransformation,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("text_editor_input")
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Status Bar at Bottom
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$lineCount lines | $wordCount words | $charCount chars",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "UTF-8",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
