package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppLanguage
import com.example.data.model.FileItem
import com.example.data.model.OperationType
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DestinationPickerDialog
import com.example.ui.components.FileDetailsDialog
import com.example.ui.components.OperationBottomBar
import com.example.ui.components.PasteBottomBar
import com.example.ui.components.SettingsDialog
import com.example.ui.components.SortDialog
import com.example.ui.components.TextInputDialog
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.TrashScreen
import com.example.ui.theme.MyFilesTheme
import com.example.ui.viewmodel.FileViewModel
import com.example.ui.viewers.AudioPlayerDialog
import com.example.ui.viewers.ImageViewerDialog
import com.example.ui.viewers.PdfViewerDialog
import com.example.ui.viewers.TextEditorDialog
import com.example.ui.viewers.VideoPlayerDialog
import com.example.util.ActiveViewer
import com.example.util.FileUtils
import java.io.File
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: FileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val languageMode by viewModel.languageMode.collectAsStateWithLifecycle()
            val baseContext = LocalContext.current

            val localizedContext = remember(baseContext, languageMode) {
                val locale = when (languageMode) {
                    AppLanguage.ENGLISH -> Locale("en")
                    AppLanguage.PERSIAN -> Locale("fa")
                    AppLanguage.SYSTEM -> Locale.getDefault()
                }
                val configuration = Configuration(baseContext.resources.configuration)
                configuration.setLocale(locale)
                baseContext.createConfigurationContext(configuration)
            }

            val isRtl = when (languageMode) {
                AppLanguage.PERSIAN -> true
                AppLanguage.ENGLISH -> false
                AppLanguage.SYSTEM -> (Locale.getDefault().language == "fa")
            }
            val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalLayoutDirection provides layoutDirection
            ) {
                MyFilesTheme(themeMode = themeMode) {
                    MainContent(viewModel = viewModel)
                }
            }
        }
    }

    private val storageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.refreshStorageVolumes()
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            val mediaFilter = IntentFilter().apply {
                addAction(Intent.ACTION_MEDIA_MOUNTED)
                addAction(Intent.ACTION_MEDIA_UNMOUNTED)
                addAction(Intent.ACTION_MEDIA_REMOVED)
                addAction(Intent.ACTION_MEDIA_BAD_REMOVAL)
                addAction(Intent.ACTION_MEDIA_EJECT)
                addDataScheme("file")
            }
            val usbFilter = IntentFilter().apply {
                addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED")
                addAction("android.hardware.usb.action.USB_DEVICE_DETACHED")
            }
            ContextCompat.registerReceiver(this, storageReceiver, mediaFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
            ContextCompat.registerReceiver(this, storageReceiver, usbFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } catch (_: Exception) {}
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(storageReceiver)
        } catch (_: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermission()
        viewModel.refreshStorageVolumes()
        viewModel.refreshCurrentDirectory()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(viewModel: FileViewModel) {
    val context = LocalContext.current
    val storages by viewModel.storageVolumes.collectAsStateWithLifecycle()
    val currentDir by viewModel.currentDirectory.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedItems by viewModel.selectedItems.collectAsStateWithLifecycle()
    val clipboard by viewModel.clipboard.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val hasPermission by viewModel.hasStoragePermission.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val languageMode by viewModel.languageMode.collectAsStateWithLifecycle()
    val trashEnabled by viewModel.trashEnabled.collectAsStateWithLifecycle()
    val trashItems by viewModel.trashItems.collectAsStateWithLifecycle()

    var isSearchActive by remember { mutableStateOf(false) }
    var isTrashOpen by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var destinationPickerOperation by remember { mutableStateOf<OperationType?>(null) }
    var activeViewer by remember { mutableStateOf<ActiveViewer?>(null) }
    var detailsTargetItem by remember { mutableStateOf<FileItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler(enabled = true) {
        if (activeViewer != null) {
            activeViewer = null
        } else if (isTrashOpen) {
            isTrashOpen = false
        } else if (isSearchActive || searchQuery.isNotEmpty()) {
            isSearchActive = false
            viewModel.clearSearch()
        } else if (selectedItems.isNotEmpty()) {
            viewModel.clearSelection()
        } else if (currentDir != null) {
            viewModel.navigateUp()
        } else {
            (context as? ComponentActivity)?.finish()
        }
    }

    Scaffold(
        topBar = {
            if (!isTrashOpen) {
                CenterAlignedTopAppBar(
                    title = {
                        if (isSearchActive) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("topbar_search_input")
                            )
                        } else {
                            Text(
                                text = if (currentDir == null) stringResource(R.string.app_name) else (currentDir?.name ?: ""),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    navigationIcon = {
                        if (isSearchActive) {
                            IconButton(
                                onClick = {
                                    isSearchActive = false
                                    viewModel.clearSearch()
                                },
                                modifier = Modifier.testTag("button_close_search")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                            }
                        } else if (currentDir != null) {
                            IconButton(
                                onClick = { viewModel.navigateUp() },
                                modifier = Modifier.testTag("button_back")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        if (!isSearchActive) {
                            IconButton(
                                onClick = { isSearchActive = true },
                                modifier = Modifier.testTag("button_search")
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            IconButton(
                                onClick = { viewModel.refreshCurrentDirectory() },
                                modifier = Modifier.testTag("button_refresh")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.retry))
                            }
                            IconButton(
                                onClick = { showSettingsDialog = true },
                                modifier = Modifier.testTag("button_settings")
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                            }
                        } else if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearSearch() }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            if (!isTrashOpen) {
                AnimatedVisibility(
                    visible = selectedItems.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    OperationBottomBar(
                        selectedCount = selectedItems.size,
                        onCopy = { destinationPickerOperation = OperationType.COPY },
                        onMove = { destinationPickerOperation = OperationType.MOVE },
                        onDelete = { showDeleteConfirmDialog = true },
                        onRename = { showRenameDialog = true },
                        onShare = {
                            FileUtils.shareFiles(context, selectedItems.map { it.file })
                            viewModel.clearSelection()
                        },
                        onDetails = {
                            detailsTargetItem = selectedItems.firstOrNull()
                        }
                    )
                }

                AnimatedVisibility(
                    visible = selectedItems.isEmpty() && clipboard != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    clipboard?.let { clip ->
                        PasteBottomBar(
                            clipboard = clip,
                            onPaste = { viewModel.executePaste() },
                            onCancel = { viewModel.cancelClipboard() }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isTrashOpen) {
                TrashScreen(
                    trashItems = trashItems,
                    onRestoreItems = { viewModel.restoreTrashItems(it) },
                    onDeletePermanently = { viewModel.deleteTrashPermanently(it) },
                    onEmptyTrash = { viewModel.emptyTrash() },
                    onNavigateBack = { isTrashOpen = false }
                )
            } else if (currentDir == null) {
                HomeScreen(
                    storages = storages,
                    hasStoragePermission = hasPermission,
                    trashEnabled = trashEnabled,
                    trashCount = trashItems.size,
                    onGrantPermission = { FileUtils.openPermissionSettings(context) },
                    onSelectStorage = { viewModel.navigateTo(it) },
                    onCategoryClick = { viewModel.openCategory(it) },
                    onOpenTrash = { isTrashOpen = true }
                )
            } else {
                BrowserScreen(
                    currentDirectory = currentDir!!,
                    rootDirectory = storages.firstOrNull()?.rootFile,
                    uiState = uiState,
                    selectedItems = selectedItems,
                    searchQuery = searchQuery,
                    searchResults = searchResults,
                    isSearching = isSearching,
                    onItemClick = { item ->
                        if (selectedItems.isNotEmpty()) {
                            viewModel.toggleSelection(item)
                        } else if (item.isDirectory) {
                            viewModel.navigateTo(item.file)
                        } else {
                            val viewer = FileUtils.getViewerForFile(item.file)
                            if (viewer != null) {
                                activeViewer = viewer
                            } else {
                                FileUtils.openFile(context, item.file)
                            }
                        }
                    },
                    onItemLongClick = { item -> viewModel.toggleSelection(item) },
                    onNavigate = { viewModel.navigateTo(it) },
                    onNavigateHome = { viewModel.navigateToHome() },
                    onCreateFolder = { showCreateFolderDialog = true },
                    onSelectAll = { viewModel.selectAll(it) },
                    onOpenSort = { showSortDialog = true },
                    onRefresh = { viewModel.refreshCurrentDirectory() }
                )
            }
        }

        // Dialogs
        if (showCreateFolderDialog) {
            TextInputDialog(
                title = stringResource(R.string.new_folder_title),
                initialValue = "",
                confirmButtonText = stringResource(R.string.create),
                onDismiss = { showCreateFolderDialog = false },
                onConfirm = { name ->
                    viewModel.createFolder(name)
                    showCreateFolderDialog = false
                }
            )
        }

        if (showRenameDialog && selectedItems.size == 1) {
            val target = selectedItems.first()
            TextInputDialog(
                title = stringResource(R.string.rename),
                initialValue = target.name,
                confirmButtonText = stringResource(R.string.rename),
                onDismiss = { showRenameDialog = false },
                onConfirm = { newName ->
                    viewModel.renameItem(target, newName)
                    showRenameDialog = false
                }
            )
        }

        if (showDeleteConfirmDialog) {
            ConfirmDeleteDialog(
                count = selectedItems.size,
                isTrashEnabled = trashEnabled,
                onDismiss = { showDeleteConfirmDialog = false },
                onConfirm = {
                    viewModel.deleteSelected()
                    showDeleteConfirmDialog = false
                }
            )
        }

        if (showSortDialog) {
            SortDialog(
                currentSort = sortOption,
                onSortSelected = { viewModel.setSortOption(it) },
                onDismiss = { showSortDialog = false }
            )
        }

        if (showSettingsDialog) {
            SettingsDialog(
                currentTheme = themeMode,
                onThemeSelected = { viewModel.setThemeMode(it) },
                currentLanguage = languageMode,
                onLanguageSelected = { viewModel.setLanguageMode(it) },
                trashEnabled = trashEnabled,
                onTrashToggled = { viewModel.setTrashEnabled(it) },
                onDismiss = { showSettingsDialog = false }
            )
        }

        detailsTargetItem?.let { item ->
            FileDetailsDialog(
                item = item,
                onDismiss = { detailsTargetItem = null }
            )
        }

        destinationPickerOperation?.let { op ->
            val rootDir = storages.firstOrNull()?.rootFile
            val initialDir = currentDir ?: rootDir ?: File("/storage/emulated/0")
            DestinationPickerDialog(
                operationType = op,
                selectedItemsCount = selectedItems.size,
                initialDirectory = initialDir,
                rootDirectory = rootDir,
                storages = storages,
                onGetSubdirectories = { dir -> viewModel.getSubdirectories(dir) },
                onCreateFolder = { parent, name -> viewModel.createFolderIn(parent, name) },
                onConfirm = { dest ->
                    if (op == OperationType.COPY) {
                        viewModel.copySelectedTo(dest)
                    } else {
                        viewModel.moveSelectedTo(dest)
                    }
                    destinationPickerOperation = null
                },
                onDismiss = { destinationPickerOperation = null }
            )
        }

        when (val viewer = activeViewer) {
            is ActiveViewer.Image -> ImageViewerDialog(file = viewer.file, onDismiss = { activeViewer = null })
            is ActiveViewer.Video -> VideoPlayerDialog(file = viewer.file, onDismiss = { activeViewer = null })
            is ActiveViewer.Audio -> AudioPlayerDialog(file = viewer.file, onDismiss = { activeViewer = null })
            is ActiveViewer.Pdf -> PdfViewerDialog(file = viewer.file, onDismiss = { activeViewer = null })
            is ActiveViewer.Text -> TextEditorDialog(
                file = viewer.file,
                onSaveFile = { f, content -> viewModel.saveTextFile(f, content) },
                onDismiss = { activeViewer = null }
            )
            null -> Unit
        }
    }
}
