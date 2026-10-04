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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.delay
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.data.model.FileCategory
import com.example.data.model.FileItem
import com.example.data.model.OperationType
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DestinationPickerDialog
import com.example.ui.components.FileDetailsDialog
import com.example.ui.components.OperationBottomBar
import com.example.ui.components.PasteBottomBar
import com.example.ui.components.SettingsDialog
import com.example.ui.components.SortDialog
import com.example.ui.components.StorageAccessRationaleDialog
import com.example.ui.components.TextInputDialog
import com.example.ui.screens.AdvancedFeaturesScreen
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.CategoryFilesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SearchResultsScreen
import com.example.ui.screens.TrashScreen
import com.example.ui.theme.MyFilesTheme
import com.example.ui.viewmodel.FileViewModel
import com.example.ui.viewers.ArchiveExtractDialog
import com.example.ui.viewers.AudioPlayerDialog
import com.example.ui.viewers.ImageViewerDialog
import com.example.ui.viewers.PackageInstallerDialog
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
    val showHiddenFiles by viewModel.showHiddenFiles.collectAsStateWithLifecycle()
    val rememberFolderSort by viewModel.rememberFolderSort.collectAsStateWithLifecycle()
    val rememberFolderView by viewModel.rememberFolderView.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val activeCategory by viewModel.activeCategory.collectAsStateWithLifecycle()
    val categoryFiles by viewModel.categoryFiles.collectAsStateWithLifecycle()
    val isCategoryLoading by viewModel.isCategoryLoading.collectAsStateWithLifecycle()
    val storageAnalysis by viewModel.storageAnalysis.collectAsStateWithLifecycle()
    val isAnalyzingStorage by viewModel.isAnalyzingStorage.collectAsStateWithLifecycle()
    val duplicateGroups by viewModel.duplicateGroups.collectAsStateWithLifecycle()
    val isScanningDuplicates by viewModel.isScanningDuplicates.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()

    var isSearchActive by remember { mutableStateOf(false) }
    var isTrashOpen by remember { mutableStateOf(false) }
    var isAdvancedFeaturesOpen by remember { mutableStateOf(false) }
    var isBatchRenameOpen by remember { mutableStateOf(false) }
    var showRationaleDialog by rememberSaveable { mutableStateOf(!hasPermission) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var destinationPickerOperation by remember { mutableStateOf<OperationType?>(null) }
    var activeViewer by remember { mutableStateOf<ActiveViewer?>(null) }
    var detailsTargetItem by remember { mutableStateOf<FileItem?>(null) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            delay(100)
            try {
                searchFocusRequester.requestFocus()
                keyboardController?.show()
            } catch (_: Exception) {}
        } else {
            keyboardController?.hide()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            showRationaleDialog = false
        }
    }

    BackHandler(enabled = true) {
        if (activeViewer != null) {
            activeViewer = null
        } else if (isBatchRenameOpen) {
            isBatchRenameOpen = false
        } else if (isTrashOpen) {
            isTrashOpen = false
        } else if (isAdvancedFeaturesOpen) {
            isAdvancedFeaturesOpen = false
        } else if (activeCategory != null) {
            viewModel.closeCategory()
        } else if (isSearchActive || searchQuery.isNotEmpty()) {
            isSearchActive = false
            keyboardController?.hide()
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
            if (!isTrashOpen && !isAdvancedFeaturesOpen) {
                CenterAlignedTopAppBar(
                    title = {
                        if (isSearchActive) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = { keyboardController?.hide() }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchFocusRequester)
                                    .testTag("topbar_search_input")
                            )
                        } else if (activeCategory != null) {
                            val catTitle = when (activeCategory) {
                                FileCategory.AUDIO -> stringResource(R.string.audio)
                                FileCategory.IMAGES -> stringResource(R.string.images)
                                FileCategory.VIDEOS -> stringResource(R.string.videos)
                                FileCategory.DOCUMENTS -> stringResource(R.string.documents)
                                FileCategory.DOWNLOADS -> stringResource(R.string.downloads)
                                FileCategory.INSTALLATION_FILES -> stringResource(R.string.installation_files)
                                null -> ""
                            }
                            Text(
                                text = catTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
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
                                    keyboardController?.hide()
                                    viewModel.clearSearch()
                                },
                                modifier = Modifier.testTag("button_close_search")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                            }
                        } else if (activeCategory != null) {
                            IconButton(
                                onClick = { viewModel.closeCategory() },
                                modifier = Modifier.testTag("button_back_category")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                        val isHomePage = currentDir == null && activeCategory == null && !isTrashOpen && !isAdvancedFeaturesOpen && !isBatchRenameOpen && searchQuery.isEmpty()
                        if (!isSearchActive && isHomePage) {
                            IconButton(
                                onClick = { isSearchActive = true },
                                modifier = Modifier.testTag("button_search")
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            IconButton(
                                onClick = { showSettingsDialog = true },
                                modifier = Modifier.testTag("button_settings")
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                            }
                        } else if (!isSearchActive) {
                            IconButton(
                                onClick = { isSearchActive = true },
                                modifier = Modifier.testTag("button_search")
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
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
                    val singleItem = if (selectedItems.size == 1) selectedItems.first() else null
                    val canExtract = singleItem != null && com.example.util.ArchiveFormat.fromFile(singleItem.file) != null
                    val canOpenWith = singleItem != null && !singleItem.isDirectory
                    OperationBottomBar(
                        selectedCount = selectedItems.size,
                        canExtract = canExtract,
                        canOpenWith = canOpenWith,
                        onOpenWith = {
                            singleItem?.let {
                                FileUtils.openWithAnotherApp(context, it.file)
                                viewModel.clearSelection()
                            }
                        },
                        onExtract = {
                            singleItem?.let {
                                viewModel.clearSelection()
                                activeViewer = ActiveViewer.ArchiveExtractor(it.file)
                            }
                        },
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
                        },
                        onBatchRename = {
                            isBatchRenameOpen = true
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
            if (isBatchRenameOpen) {
                com.example.ui.screens.BatchRenameScreen(
                    files = selectedItems.map { it.file },
                    onApplyRename = { renames ->
                        viewModel.batchRenameFiles(renames)
                        isBatchRenameOpen = false
                    },
                    onNavigateBack = { isBatchRenameOpen = false }
                )
            } else if (isTrashOpen) {
                TrashScreen(
                    trashItems = trashItems,
                    onRestoreItems = { viewModel.restoreTrashItems(it) },
                    onDeletePermanently = { viewModel.deleteTrashPermanently(it) },
                    onEmptyTrash = { viewModel.emptyTrash() },
                    onNavigateBack = { isTrashOpen = false }
                )
            } else if (isAdvancedFeaturesOpen) {
                AdvancedFeaturesScreen(
                    storageAnalysis = storageAnalysis,
                    isAnalyzingStorage = isAnalyzingStorage,
                    onAnalyzeStorage = { viewModel.analyzeStorage() },
                    onCleanEmptyFolders = { viewModel.cleanEmptyFolders(it) },
                    duplicateGroups = duplicateGroups,
                    isScanningDuplicates = isScanningDuplicates,
                    onScanDuplicates = { viewModel.scanDuplicates() },
                    onDeleteDuplicates = { viewModel.deleteDuplicateFiles(it) },
                    onOpenFile = { file ->
                        val viewer = FileUtils.getViewerForFile(file)
                        if (viewer != null) {
                            activeViewer = viewer
                        } else {
                            FileUtils.openFile(context, file)
                        }
                    },
                    onDeleteFile = { item ->
                        viewModel.deleteDuplicateFiles(listOf(item))
                    },
                    candidateFiles = selectedItems.map { it.file },
                    onApplyBatchRename = { renames ->
                        viewModel.batchRenameFiles(renames)
                    },
                    onNavigateBack = { isAdvancedFeaturesOpen = false }
                )
            } else if (activeCategory != null) {
                CategoryFilesScreen(
                    category = activeCategory!!,
                    files = categoryFiles,
                    isLoading = isCategoryLoading,
                    selectedItems = selectedItems,
                    viewMode = viewMode,
                    onViewModeChange = { viewModel.setViewMode(it) },
                    searchQuery = searchQuery,
                    onItemClick = { item ->
                        keyboardController?.hide()
                        if (selectedItems.isNotEmpty()) {
                            viewModel.toggleSelection(item)
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
                    onSelectAll = { viewModel.selectAll(it) },
                    onOpenSort = { showSortDialog = true }
                )
            } else if (currentDir == null) {
                if (isSearchActive || searchQuery.isNotBlank()) {
                    SearchResultsScreen(
                        searchQuery = searchQuery,
                        searchResults = searchResults,
                        isSearching = isSearching,
                        selectedItems = selectedItems,
                        viewMode = viewMode,
                        onViewModeChange = { viewModel.setViewMode(it) },
                        onItemClick = { item ->
                            keyboardController?.hide()
                            if (selectedItems.isNotEmpty()) {
                                viewModel.toggleSelection(item)
                            } else if (item.isDirectory) {
                                isSearchActive = false
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
                        onItemLongClick = { item -> viewModel.toggleSelection(item) }
                    )
                } else {
                    HomeScreen(
                        storages = storages,
                        hasStoragePermission = hasPermission,
                        trashEnabled = trashEnabled,
                        trashCount = trashItems.size,
                        onGrantPermission = { showRationaleDialog = true },
                        onSelectStorage = { viewModel.navigateTo(it) },
                        onCategoryClick = { viewModel.openCategory(it) },
                        onOpenTrash = { isTrashOpen = true },
                        onOpenAdvancedFeatures = { isAdvancedFeaturesOpen = true }
                    )
                }
            } else {
                BrowserScreen(
                    currentDirectory = currentDir!!,
                    rootDirectory = storages.firstOrNull()?.rootFile,
                    uiState = uiState,
                    selectedItems = selectedItems,
                    viewMode = viewMode,
                    onViewModeChange = { viewModel.setViewMode(it) },
                    searchQuery = searchQuery,
                    searchResults = searchResults,
                    isSearching = isSearching,
                    onItemClick = { item ->
                        keyboardController?.hide()
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
                    onCreateFile = { showCreateFileDialog = true },
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

        if (showCreateFileDialog) {
            TextInputDialog(
                title = stringResource(R.string.new_file_title),
                initialValue = "",
                confirmButtonText = stringResource(R.string.create),
                onDismiss = { showCreateFileDialog = false },
                onConfirm = { name ->
                    viewModel.createFile(name)
                    showCreateFileDialog = false
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
                showHiddenFiles = showHiddenFiles,
                onShowHiddenFilesToggled = { viewModel.setShowHiddenFiles(it) },
                rememberFolderSort = rememberFolderSort,
                onRememberFolderSortToggled = { viewModel.setRememberFolderSort(it) },
                rememberFolderView = rememberFolderView,
                onRememberFolderViewToggled = { viewModel.setRememberFolderView(it) },
                onDismiss = { showSettingsDialog = false }
            )
        }

        if (showRationaleDialog && !hasPermission) {
            StorageAccessRationaleDialog(
                onGrantAccess = {
                    FileUtils.openPermissionSettings(context)
                },
                onDismiss = {
                    showRationaleDialog = false
                }
            )
        }

        detailsTargetItem?.let { item ->
            FileDetailsDialog(
                item = item,
                onOpenWith = {
                    FileUtils.openWithAnotherApp(context, item.file)
                    viewModel.clearSelection()
                },
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
            is ActiveViewer.PackageInstaller -> PackageInstallerDialog(
                file = viewer.file,
                onDismiss = { activeViewer = null }
            )
            is ActiveViewer.ArchiveExtractor -> ArchiveExtractDialog(
                file = viewer.file,
                onDismiss = { activeViewer = null },
                onExtracted = { _ ->
                    viewModel.refreshCurrentDirectory()
                }
            )
            null -> Unit
        }
    }
}
