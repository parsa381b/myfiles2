package com.example.ui.viewers

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfViewerDialog(
    file: File,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var pageCount by remember { mutableIntStateOf(0) }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    var rendererRef by remember { mutableStateOf<PdfRenderer?>(null) }
    var pfdRef by remember { mutableStateOf<ParcelFileDescriptor?>(null) }

    DisposableEffect(file) {
        try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            pfdRef = pfd
            rendererRef = renderer
            pageCount = renderer.pageCount
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Cannot open PDF"
            isLoading = false
        }

        onDispose {
            try {
                rendererRef?.close()
                pfdRef?.close()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(currentPageIndex, rendererRef) {
        val renderer = rendererRef ?: return@LaunchedEffect
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                val page = renderer.openPage(currentPageIndex)
                val width = page.width * 2
                val height = page.height * 2
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                currentBitmap = bitmap
                isLoading = false
            } catch (e: Exception) {
                errorMessage = e.localizedMessage
                isLoading = false
            }
        }
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
                .testTag("pdf_viewer_dialog"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
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
                            if (pageCount > 0) {
                                Text(
                                    text = stringResource(R.string.page_indicator, currentPageIndex + 1, pageCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = { scale = (scale + 0.3f).coerceAtMost(4f) }) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In")
                        }

                        IconButton(onClick = { scale = (scale - 0.3f).coerceAtLeast(0.7f) }) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out")
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

                // Content View (Rendered Page Bitmap)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF50535A))
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.7f, 5f)
                                if (scale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(color = Color.White)
                        }

                        errorMessage != null -> {
                            Text(
                                text = errorMessage ?: "",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        currentBitmap != null -> {
                            Image(
                                bitmap = currentBitmap!!.asImageBitmap(),
                                contentDescription = "Page ${currentPageIndex + 1}",
                                modifier = Modifier
                                    .padding(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offsetX,
                                        translationY = offsetY
                                    )
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Bottom Page Navigation Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (currentPageIndex > 0) {
                                    currentPageIndex--
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            },
                            enabled = currentPageIndex > 0
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = stringResource(R.string.previous_page),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = if (pageCount > 0) "${currentPageIndex + 1} / $pageCount" else "—",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = {
                                if (currentPageIndex < pageCount - 1) {
                                    currentPageIndex++
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            },
                            enabled = currentPageIndex < pageCount - 1
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.next_page),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
