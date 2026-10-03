package com.example.ui.viewers

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
                // Compute high-resolution DPI scaling based on device display density
                val displayMetrics = context.resources.displayMetrics
                val densityScale = (displayMetrics.density * 1.5f).coerceIn(2.5f, 3.5f)
                val width = (page.width * densityScale).toInt().coerceAtLeast(1)
                val height = (page.height * densityScale).toInt().coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                // CRITICAL: Pre-fill bitmap with solid white background so PDF text renders clearly
                bitmap.eraseColor(android.graphics.Color.WHITE)
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
                // Top App Bar with safe status bars insets
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("pdf_button_close")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.close)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp)
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

                        // Reset zoom button when zoomed in
                        if (scale != 1f || offsetX != 0f || offsetY != 0f) {
                            IconButton(
                                onClick = {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                },
                                modifier = Modifier.testTag("pdf_button_reset_zoom")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitScreen,
                                    contentDescription = "Fit to Screen",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        IconButton(
                            onClick = { scale = (scale + 0.35f).coerceAtMost(5f) },
                            modifier = Modifier.testTag("pdf_button_zoom_in")
                        ) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In")
                        }

                        IconButton(
                            onClick = {
                                val newScale = (scale - 0.35f).coerceAtLeast(1f)
                                scale = newScale
                                if (newScale == 1f) {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            },
                            modifier = Modifier.testTag("pdf_button_zoom_out")
                        ) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out")
                        }

                        IconButton(
                            onClick = { FileUtils.shareFiles(context, listOf(file)) },
                            modifier = Modifier.testTag("pdf_button_share")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.share)
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Content View (Neutral Slate Backdrop with Pristine White Paper Sheet)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF262930))
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
                                scale = newScale
                                if (newScale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1.2f) {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    } else {
                                        scale = 2.4f
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Rendering page ${currentPageIndex + 1}…",
                                    color = Color.White.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        errorMessage != null -> {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.Black.copy(alpha = 0.75f),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Text(
                                    text = errorMessage ?: "",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(18.dp)
                                )
                            }
                        }

                        currentBitmap != null -> {
                            // High-quality paper sheet with drop shadow and pure white surface
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offsetX,
                                        translationY = offsetY
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.White,
                                    shadowElevation = 8.dp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .shadow(8.dp, RoundedCornerShape(4.dp))
                                ) {
                                    Image(
                                        bitmap = currentBitmap!!.asImageBitmap(),
                                        contentDescription = "PDF Page ${currentPageIndex + 1}",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Bottom Page Navigation Bar with safe navigation bar insets
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Optional page scrub slider if document has multiple pages
                        if (pageCount > 1) {
                            Slider(
                                value = currentPageIndex.toFloat(),
                                onValueChange = { newIdx ->
                                    val idx = newIdx.toInt().coerceIn(0, pageCount - 1)
                                    if (idx != currentPageIndex) {
                                        currentPageIndex = idx
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                },
                                valueRange = 0f..(pageCount - 1).toFloat(),
                                steps = (pageCount - 2).coerceAtLeast(0),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .testTag("pdf_page_slider")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                enabled = currentPageIndex > 0,
                                modifier = Modifier.testTag("pdf_button_prev_page")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = stringResource(R.string.previous_page),
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = if (pageCount > 0) "${currentPageIndex + 1} / $pageCount" else "—",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (currentPageIndex < pageCount - 1) {
                                        currentPageIndex++
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                },
                                enabled = currentPageIndex < pageCount - 1,
                                modifier = Modifier.testTag("pdf_button_next_page")
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
}
