package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DuplicateGroup
import com.example.data.model.FileItem
import com.example.data.model.StorageAnalysisResult
import com.example.util.FileUtils
import java.io.File

enum class AdvancedSubScreen {
    HUB,
    STORAGE_USAGE,
    DUPLICATE_FINDER,
    PHONE_INFO,
    BATCH_RENAMER,
    WIFI_SHARING
}

@Composable
fun AdvancedFeaturesScreen(
    storageAnalysis: StorageAnalysisResult?,
    isAnalyzingStorage: Boolean,
    onAnalyzeStorage: () -> Unit,
    onCleanEmptyFolders: (List<File>) -> Unit,
    duplicateGroups: List<DuplicateGroup>?,
    isScanningDuplicates: Boolean,
    onScanDuplicates: () -> Unit,
    onDeleteDuplicates: (List<FileItem>) -> Unit,
    onOpenFile: (File) -> Unit,
    onDeleteFile: (FileItem) -> Unit,
    candidateFiles: List<File> = emptyList(),
    onApplyBatchRename: (List<Pair<File, String>>) -> Unit = {},
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var subScreen by rememberSaveable { mutableStateOf(AdvancedSubScreen.HUB) }

    BackHandler {
        if (subScreen != AdvancedSubScreen.HUB) {
            subScreen = AdvancedSubScreen.HUB
        } else {
            onNavigateBack()
        }
    }

    when (subScreen) {
        AdvancedSubScreen.STORAGE_USAGE -> {
            StorageUsageScreen(
                analysisResult = storageAnalysis,
                isLoading = isAnalyzingStorage,
                onRefresh = onAnalyzeStorage,
                onCleanEmptyFolders = onCleanEmptyFolders,
                onOpenFile = onOpenFile,
                onDeleteFile = onDeleteFile,
                onNavigateBack = { subScreen = AdvancedSubScreen.HUB },
                modifier = modifier
            )
        }

        AdvancedSubScreen.DUPLICATE_FINDER -> {
            DuplicateFinderScreen(
                duplicateGroups = duplicateGroups,
                isScanning = isScanningDuplicates,
                onRescan = onScanDuplicates,
                onDeleteDuplicates = onDeleteDuplicates,
                onOpenFile = onOpenFile,
                onNavigateBack = { subScreen = AdvancedSubScreen.HUB },
                modifier = modifier
            )
        }

        AdvancedSubScreen.PHONE_INFO -> {
            PhoneInfoScreen(
                onNavigateBack = { subScreen = AdvancedSubScreen.HUB },
                modifier = modifier
            )
        }

        AdvancedSubScreen.BATCH_RENAMER -> {
            // If candidateFiles is empty, use sample/recent files from internal storage
            val effectiveFiles = remember(candidateFiles) {
                if (candidateFiles.isNotEmpty()) {
                    candidateFiles
                } else {
                    val root = android.os.Environment.getExternalStorageDirectory()
                    val downloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    val candidates = (downloads?.listFiles()?.filter { it.isFile } ?: emptyList())
                        .ifEmpty { root?.listFiles()?.filter { it.isFile } ?: emptyList() }
                    candidates.take(15)
                }
            }

            BatchRenameScreen(
                files = effectiveFiles,
                onApplyRename = { renames ->
                    onApplyBatchRename(renames)
                    subScreen = AdvancedSubScreen.HUB
                },
                onNavigateBack = { subScreen = AdvancedSubScreen.HUB },
                modifier = modifier
            )
        }

        AdvancedSubScreen.WIFI_SHARING -> {
            WifiSharingScreen(
                onNavigateBack = { subScreen = AdvancedSubScreen.HUB },
                modifier = modifier
            )
        }

        AdvancedSubScreen.HUB -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .testTag("advanced_features_hub")
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
                            modifier = Modifier.testTag("advanced_features_button_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.close)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = stringResource(R.string.advanced_features),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                HorizontalDivider()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        // Welcome Banner Card
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 3.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = Color(0xFF8B5CF6),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.advanced_features),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Dedicated tools for deep storage analytics, duplicate cleanup, and power user workflows.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Storage & Cleanup Utilities",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }

                    // 1. Storage Usage Card
                    item {
                        val storageSubtitle = if (storageAnalysis != null) {
                            val used = FileUtils.formatFileSize(storageAnalysis.usedBytes)
                            val total = FileUtils.formatFileSize(storageAnalysis.totalBytes)
                            "$used used of $total • ${storageAnalysis.largeFiles.size} large files"
                        } else {
                            "Analyze breakdown by file type, detect large files (>50MB), and clean empty folders"
                        }

                        AdvancedFeaturePrimaryCard(
                            title = "Storage Usage",
                            description = storageSubtitle,
                            icon = Icons.Default.PieChart,
                            accentColor = Color(0xFF3B82F6),
                            badgeText = if (storageAnalysis != null) "View Details" else "Analyze",
                            onClick = {
                                subScreen = AdvancedSubScreen.STORAGE_USAGE
                                if (storageAnalysis == null) {
                                    onAnalyzeStorage()
                                }
                            },
                            testTag = "card_storage_usage"
                        )
                    }

                    // 2. Duplicate File Finder Card
                    item {
                        val dupSubtitle = if (duplicateGroups != null) {
                            val count = duplicateGroups.sumOf { it.items.size - 1 }
                            val bytes = duplicateGroups.sumOf { it.fileSize * (it.items.size - 1) }
                            "$count duplicate copies found • ${FileUtils.formatFileSize(bytes)} recoverable"
                        } else {
                            "Find exact identical copies wasting space across internal storage and SD card"
                        }

                        AdvancedFeaturePrimaryCard(
                            title = "Duplicate File Finder",
                            description = dupSubtitle,
                            icon = Icons.Default.FindInPage,
                            accentColor = Color(0xFF10B981),
                            badgeText = if (duplicateGroups != null) "${duplicateGroups.size} Groups" else "Scan Now",
                            onClick = {
                                subScreen = AdvancedSubScreen.DUPLICATE_FINDER
                                if (duplicateGroups == null) {
                                    onScanDuplicates()
                                }
                            },
                            testTag = "card_duplicate_finder"
                        )
                    }

                    item {
                        Text(
                            text = "System & Hardware",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                        )
                    }

                    // Phone Info Card
                    item {
                        AdvancedFeaturePrimaryCard(
                            title = stringResource(R.string.phone_info),
                            description = stringResource(R.string.phone_info_subtitle),
                            icon = Icons.Default.PhoneAndroid,
                            accentColor = Color(0xFF6366F1),
                            badgeText = "System Specs",
                            onClick = {
                                subScreen = AdvancedSubScreen.PHONE_INFO
                            },
                            testTag = "card_phone_info"
                        )
                    }

                    item {
                        Text(
                            text = "More Utilities",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                        )
                    }

                    // 3. Batch File Renamer
                    item {
                        AdvancedFeaturePrimaryCard(
                            title = stringResource(R.string.batch_renamer),
                            description = stringResource(R.string.batch_renamer_subtitle),
                            icon = Icons.Default.DriveFileRenameOutline,
                            accentColor = Color(0xFFF59E0B),
                            badgeText = if (candidateFiles.isNotEmpty()) "${candidateFiles.size} Files" else "Open Tool",
                            onClick = {
                                subScreen = AdvancedSubScreen.BATCH_RENAMER
                            },
                            testTag = "card_batch_renamer"
                        )
                    }

                    // 4. Local Network & Wi-Fi Sharing
                    item {
                        AdvancedFeaturePrimaryCard(
                            title = stringResource(R.string.wifi_sharing),
                            description = stringResource(R.string.wifi_sharing_subtitle),
                            icon = Icons.Default.Wifi,
                            accentColor = Color(0xFF10B981),
                            badgeText = "Wireless Transfer",
                            onClick = {
                                subScreen = AdvancedSubScreen.WIFI_SHARING
                            },
                            testTag = "card_wifi_sharing"
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvancedFeaturePrimaryCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    badgeText: String,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
