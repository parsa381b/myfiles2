package com.example.ui.viewers

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.util.FileUtils
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.github.barteksc.pdfviewer.util.FitPolicy
import java.io.File

@Composable
fun PdfViewerDialog(
    file: File,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()

    var pageCount by remember { mutableIntStateOf(0) }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Display configuration
    var isHorizontalSwipe by remember { mutableStateOf(false) }
    var isNightMode by remember { mutableStateOf(false) }
    var pdfViewInstance by remember { mutableStateOf<PDFView?>(null) }
    var reloadTrigger by remember { mutableIntStateOf(0) }
    var showPageSlider by remember { mutableStateOf(false) }

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
            color = if (isNightMode) Color(0xFF121212) else Color(0xFF262930)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
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
                                    text = "Page ${currentPageIndex + 1} of $pageCount • ${if (isHorizontalSwipe) "Page Flip" else "Continuous Scroll"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Toggle Continuous Scroll vs Horizontal Page Flip
                        IconButton(
                            onClick = {
                                isHorizontalSwipe = !isHorizontalSwipe
                                reloadTrigger++
                            },
                            modifier = Modifier.testTag("pdf_toggle_view_mode")
                        ) {
                            Icon(
                                imageVector = if (isHorizontalSwipe) Icons.Default.ViewAgenda else Icons.Default.Layers,
                                contentDescription = if (isHorizontalSwipe) "Switch to Continuous Vertical" else "Switch to Horizontal Page Flip",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Toggle Night Mode (Invert Colors)
                        IconButton(
                            onClick = {
                                isNightMode = !isNightMode
                                reloadTrigger++
                            },
                            modifier = Modifier.testTag("pdf_toggle_night_mode")
                        ) {
                            Icon(
                                imageVector = if (isNightMode) Icons.Default.WbSunny else Icons.Default.Nightlight,
                                contentDescription = "Toggle Night Mode"
                            )
                        }

                        // Reset Zoom / Fit
                        IconButton(
                            onClick = {
                                pdfViewInstance?.resetZoomWithAnimation()
                            },
                            modifier = Modifier.testTag("pdf_button_reset_zoom")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitScreen,
                                contentDescription = "Fit to Screen"
                            )
                        }

                        // Zoom In
                        IconButton(
                            onClick = {
                                pdfViewInstance?.let { view ->
                                    val newZoom = (view.zoom * 1.35f).coerceAtMost(view.maxZoom)
                                    view.zoomWithAnimation(newZoom)
                                }
                            },
                            modifier = Modifier.testTag("pdf_button_zoom_in")
                        ) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In")
                        }

                        // Zoom Out
                        IconButton(
                            onClick = {
                                pdfViewInstance?.let { view ->
                                    val newZoom = (view.zoom / 1.35f).coerceAtLeast(view.minZoom)
                                    view.zoomWithAnimation(newZoom)
                                }
                            },
                            modifier = Modifier.testTag("pdf_button_zoom_out")
                        ) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out")
                        }

                        // Share PDF
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

                // Main PDF Rendering Canvas via AndroidPdfViewer
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(if (isNightMode) Color(0xFF141414) else Color(0xFF262930))
                ) {
                    key(reloadTrigger) {
                        AndroidView(
                            factory = { ctx ->
                                PDFView(ctx, null).apply {
                                    pdfViewInstance = this
                                    setBackgroundColor(if (isNightMode) 0xFF141414.toInt() else 0xFF262930.toInt())
                                    fromFile(file)
                                        .enableSwipe(true)
                                        .swipeHorizontal(isHorizontalSwipe)
                                        .enableDoubletap(true)
                                        .defaultPage(currentPageIndex)
                                        .enableAnnotationRendering(true)
                                        .scrollHandle(DefaultScrollHandle(ctx))
                                        .enableAntialiasing(true)
                                        .spacing(12)
                                        .autoSpacing(false)
                                        .pageFitPolicy(FitPolicy.WIDTH)
                                        .nightMode(isNightMode)
                                        .onLoad { total ->
                                            pageCount = total
                                            isLoading = false
                                        }
                                        .onPageChange { page, total ->
                                            currentPageIndex = page
                                            pageCount = total
                                        }
                                        .onError { t ->
                                            errorMessage = t.localizedMessage ?: "Failed to render PDF"
                                            isLoading = false
                                        }
                                        .onPageError { page, t ->
                                            // Handle individual corrupted page
                                        }
                                        .load()
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("android_pdf_view")
                        )
                    }

                    // Loading State Spinner
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Rendering all pages…",
                                    color = Color.White.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    // Error State Box
                    if (errorMessage != null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.Black.copy(alpha = 0.85f),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Text(
                                    text = errorMessage ?: "",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    }

                    // Floating Bottom Page Pill and Jumper
                    if (pageCount > 1) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            tonalElevation = 6.dp,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 20.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                if (showPageSlider) {
                                    Slider(
                                        value = currentPageIndex.toFloat(),
                                        onValueChange = { newIdx ->
                                            val target = newIdx.toInt().coerceIn(0, pageCount - 1)
                                            if (target != currentPageIndex) {
                                                currentPageIndex = target
                                                pdfViewInstance?.jumpTo(target, false)
                                            }
                                        },
                                        valueRange = 0f..(pageCount - 1).toFloat(),
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth(0.75f)
                                            .height(30.dp)
                                            .padding(top = 4.dp)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (currentPageIndex > 0) {
                                                val prev = currentPageIndex - 1
                                                pdfViewInstance?.jumpTo(prev, true)
                                            }
                                        },
                                        enabled = currentPageIndex > 0,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = "Previous page"
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        onClick = { showPageSlider = !showPageSlider },
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    ) {
                                        Text(
                                            text = "${currentPageIndex + 1} / $pageCount",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (currentPageIndex < pageCount - 1) {
                                                val next = currentPageIndex + 1
                                                pdfViewInstance?.jumpTo(next, true)
                                            }
                                        },
                                        enabled = currentPageIndex < pageCount - 1,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Next page"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
