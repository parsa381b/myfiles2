package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppLanguage
import com.example.data.model.ClipboardState
import com.example.data.model.FileCategory
import com.example.data.model.FileItem
import com.example.data.model.OperationType
import com.example.data.model.SortOption
import com.example.data.model.StorageInfo
import com.example.data.model.ThemeMode
import com.example.data.repository.FileRepository
import com.example.util.FileUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface UiState {
    object Idle : UiState
    object Loading : UiState
    data class Success(val items: List<FileItem>) : UiState
    data class Error(val message: String) : UiState
}

class FileViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = FileRepository()
    private val trashRepository = com.example.data.repository.TrashRepository(application)
    private val prefs = application.getSharedPreferences("my_files_settings", Context.MODE_PRIVATE)

    private val _trashEnabled = MutableStateFlow(prefs.getBoolean("pref_trash_enabled", true))
    val trashEnabled: StateFlow<Boolean> = _trashEnabled.asStateFlow()

    private val _showHiddenFiles = MutableStateFlow(prefs.getBoolean("pref_show_hidden_files", false))
    val showHiddenFiles: StateFlow<Boolean> = _showHiddenFiles.asStateFlow()

    private val _trashItems = MutableStateFlow<List<com.example.data.model.TrashItem>>(emptyList())
    val trashItems: StateFlow<List<com.example.data.model.TrashItem>> = _trashItems.asStateFlow()

    private val _themeMode = MutableStateFlow(
        when (prefs.getString("pref_theme", "SYSTEM")) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _languageMode = MutableStateFlow(
        when (prefs.getString("pref_lang", "SYSTEM")) {
            "ENGLISH" -> AppLanguage.ENGLISH
            "PERSIAN" -> AppLanguage.PERSIAN
            else -> AppLanguage.SYSTEM
        }
    )
    val languageMode: StateFlow<AppLanguage> = _languageMode.asStateFlow()

    private val _storageVolumes = MutableStateFlow<List<StorageInfo>>(emptyList())
    val storageVolumes: StateFlow<List<StorageInfo>> = _storageVolumes.asStateFlow()

    private val _currentDirectory = MutableStateFlow<File?>(null)
    val currentDirectory: StateFlow<File?> = _currentDirectory.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _selectedItems = MutableStateFlow<Set<FileItem>>(emptySet())
    val selectedItems: StateFlow<Set<FileItem>> = _selectedItems.asStateFlow()

    private val _clipboard = MutableStateFlow<ClipboardState?>(null)
    val clipboard: StateFlow<ClipboardState?> = _clipboard.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.NAME_ASC)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _activeCategory = MutableStateFlow<FileCategory?>(null)
    val activeCategory: StateFlow<FileCategory?> = _activeCategory.asStateFlow()

    private val _categoryFiles = MutableStateFlow<List<FileItem>?>(null)
    val categoryFiles: StateFlow<List<FileItem>?> = _categoryFiles.asStateFlow()

    private val _isCategoryLoading = MutableStateFlow(false)
    val isCategoryLoading: StateFlow<Boolean> = _isCategoryLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<FileItem>?>(null)
    val searchResults: StateFlow<List<FileItem>?> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _hasStoragePermission = MutableStateFlow(false)
    val hasStoragePermission: StateFlow<Boolean> = _hasStoragePermission.asStateFlow()

    private val storageAnalysisRepository = com.example.data.repository.StorageAnalysisRepository()
    private val duplicateFinderRepository = com.example.data.repository.DuplicateFinderRepository()

    private val _storageAnalysis = MutableStateFlow<com.example.data.model.StorageAnalysisResult?>(null)
    val storageAnalysis: StateFlow<com.example.data.model.StorageAnalysisResult?> = _storageAnalysis.asStateFlow()

    private val _isAnalyzingStorage = MutableStateFlow(false)
    val isAnalyzingStorage: StateFlow<Boolean> = _isAnalyzingStorage.asStateFlow()

    private val _duplicateGroups = MutableStateFlow<List<com.example.data.model.DuplicateGroup>?>(null)
    val duplicateGroups: StateFlow<List<com.example.data.model.DuplicateGroup>?> = _duplicateGroups.asStateFlow()

    private val _isScanningDuplicates = MutableStateFlow(false)
    val isScanningDuplicates: StateFlow<Boolean> = _isScanningDuplicates.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private var searchJob: Job? = null

    init {
        checkPermission()
        refreshStorageVolumes()
        loadTrashItems()
    }

    fun setTrashEnabled(enabled: Boolean) {
        _trashEnabled.value = enabled
        prefs.edit().putBoolean("pref_trash_enabled", enabled).apply()
    }

    fun setShowHiddenFiles(show: Boolean) {
        _showHiddenFiles.value = show
        prefs.edit().putBoolean("pref_show_hidden_files", show).apply()
        refreshCurrentDirectory()
    }

    fun loadTrashItems() {
        viewModelScope.launch {
            trashRepository.getTrashItems().onSuccess {
                _trashItems.value = it
            }
        }
    }

    fun checkPermission() {
        _hasStoragePermission.value = FileUtils.hasStoragePermission(getApplication())
    }

    fun refreshStorageVolumes() {
        viewModelScope.launch {
            _storageVolumes.value = repository.getStorageVolumes(getApplication())
        }
    }

    fun navigateTo(directory: File) {
        _currentDirectory.value = directory
        clearSelection()
        clearSearch()
        loadDirectory(directory)
    }

    fun openCategory(category: FileCategory) {
        _activeCategory.value = category
        _currentDirectory.value = null
        clearSelection()
        clearSearch()
        loadCategoryFiles(category)
    }

    fun closeCategory() {
        _activeCategory.value = null
        _categoryFiles.value = null
        clearSelection()
        clearSearch()
    }

    fun refreshCategory() {
        _activeCategory.value?.let { loadCategoryFiles(it) }
    }

    private fun loadCategoryFiles(category: FileCategory) {
        viewModelScope.launch {
            _isCategoryLoading.value = true
            val items = repository.getCategoryFiles(
                context = getApplication(),
                category = category,
                sortOption = _sortOption.value,
                showHiddenFiles = _showHiddenFiles.value
            )
            _categoryFiles.value = items
            _isCategoryLoading.value = false
        }
    }

    fun navigateUp(): Boolean {
        if (_searchQuery.value.isNotEmpty()) {
            clearSearch()
            return true
        }
        if (_activeCategory.value != null) {
            closeCategory()
            return true
        }
        val curr = _currentDirectory.value ?: return false
        val parent = curr.parentFile
        val root = _storageVolumes.value.firstOrNull()?.rootFile
        return if (curr == root || parent == null || !parent.canRead()) {
            _currentDirectory.value = null
            clearSelection()
            refreshStorageVolumes()
            true
        } else {
            navigateTo(parent)
            true
        }
    }

    fun navigateToHome() {
        _currentDirectory.value = null
        _activeCategory.value = null
        _categoryFiles.value = null
        clearSelection()
        clearSearch()
        refreshStorageVolumes()
    }

    fun refreshCurrentDirectory() {
        if (_activeCategory.value != null) {
            refreshCategory()
        } else if (_currentDirectory.value != null) {
            loadDirectory(_currentDirectory.value!!)
        } else {
            refreshStorageVolumes()
        }
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
        _currentDirectory.value?.let { loadDirectory(it) }
        _activeCategory.value?.let { loadCategoryFiles(it) }
    }

    private fun loadDirectory(directory: File) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.getFilesInDirectory(directory, _sortOption.value, _showHiddenFiles.value)
                .onSuccess { _uiState.value = UiState.Success(it) }
                .onFailure { _uiState.value = UiState.Error(it.localizedMessage ?: "Cannot access folder") }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = null
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            // Fast debounce for snappy responsiveness
            delay(80)
            val root = _currentDirectory.value
            val results = repository.searchFiles(
                context = getApplication(),
                rootDir = root,
                query = query,
                showHiddenFiles = _showHiddenFiles.value
            )
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _searchQuery.value = ""
        _searchResults.value = null
        _isSearching.value = false
    }

    fun toggleSelection(item: FileItem) {
        val current = _selectedItems.value.toMutableSet()
        if (current.contains(item)) current.remove(item) else current.add(item)
        _selectedItems.value = current
    }

    fun selectAll(items: List<FileItem>) {
        val current = _selectedItems.value
        val itemsSet = items.toSet()
        if (itemsSet.isNotEmpty() && current.containsAll(itemsSet)) {
            _selectedItems.value = emptySet()
        } else {
            _selectedItems.value = itemsSet
        }
    }

    fun clearSelection() {
        _selectedItems.value = emptySet()
    }

    fun createFolder(name: String) {
        val curr = _currentDirectory.value ?: return
        viewModelScope.launch {
            repository.createDirectory(curr, name)
                .onSuccess {
                    _userMessage.emit("Folder \"$name\" created")
                    refreshCurrentDirectory()
                }
                .onFailure { _userMessage.emit(it.localizedMessage ?: "Failed to create folder") }
        }
    }

    fun renameItem(item: FileItem, newName: String) {
        viewModelScope.launch {
            repository.renameFile(item.file, newName)
                .onSuccess {
                    clearSelection()
                    _userMessage.emit("Renamed to \"$newName\"")
                    refreshCurrentDirectory()
                }
                .onFailure { _userMessage.emit(it.localizedMessage ?: "Rename failed") }
        }
    }

    fun deleteSelected() {
        val files = _selectedItems.value.map { it.file }
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            if (_trashEnabled.value) {
                trashRepository.moveToTrash(files)
                    .onSuccess { count ->
                        _userMessage.emit("$count item(s) moved to Trash")
                        clearSelection()
                        refreshCurrentDirectory()
                        loadTrashItems()
                    }
                    .onFailure { _userMessage.emit("Failed to move to Trash: ${it.localizedMessage}") }
            } else {
                repository.deleteFiles(files)
                    .onSuccess { count ->
                        _userMessage.emit("$count item(s) deleted permanently")
                        clearSelection()
                        refreshCurrentDirectory()
                    }
                    .onFailure { _userMessage.emit("Failed to delete items: ${it.localizedMessage}") }
            }
        }
    }

    fun restoreTrashItems(items: List<com.example.data.model.TrashItem>) {
        viewModelScope.launch {
            trashRepository.restoreItems(items)
                .onSuccess { count ->
                    _userMessage.emit("$count item(s) restored")
                    loadTrashItems()
                    refreshCurrentDirectory()
                }
                .onFailure { _userMessage.emit("Failed to restore: ${it.localizedMessage}") }
        }
    }

    fun deleteTrashPermanently(items: List<com.example.data.model.TrashItem>) {
        viewModelScope.launch {
            trashRepository.deletePermanently(items)
                .onSuccess { count ->
                    _userMessage.emit("$count item(s) deleted permanently")
                    loadTrashItems()
                }
                .onFailure { _userMessage.emit("Failed to delete: ${it.localizedMessage}") }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            trashRepository.emptyTrash()
                .onSuccess { count ->
                    _userMessage.emit("Trash emptied")
                    loadTrashItems()
                }
                .onFailure { _userMessage.emit("Failed to empty Trash: ${it.localizedMessage}") }
        }
    }

    fun setClipboard(type: OperationType) {
        val files = _selectedItems.value.map { it.file }
        if (files.isNotEmpty()) {
            _clipboard.value = ClipboardState(files, type)
            clearSelection()
            val text = if (type == OperationType.COPY) "Copied ${files.size} items" else "Moved ${files.size} items"
            viewModelScope.launch { _userMessage.emit(text) }
        }
    }

    fun cancelClipboard() {
        _clipboard.value = null
    }

    fun executePaste() {
        val clip = _clipboard.value ?: return
        val dest = _currentDirectory.value ?: return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            var allSuccessful = true
            for (src in clip.sourceFiles) {
                val ok = if (clip.type == OperationType.COPY) {
                    repository.copyFileOrDirectory(src, dest)
                } else {
                    repository.moveFileOrDirectory(src, dest)
                }
                if (!ok) allSuccessful = false
            }
            _clipboard.value = null
            _userMessage.emit(if (allSuccessful) "Operation completed" else "Some items could not be processed")
            refreshCurrentDirectory()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("pref_theme", mode.name).apply()
    }

    fun setLanguageMode(lang: AppLanguage) {
        _languageMode.value = lang
        prefs.edit().putString("pref_lang", lang.name).apply()
    }

    fun copySelectedTo(destDir: File) {
        val files = _selectedItems.value.map { it.file }
        if (files.isEmpty()) return
        for (f in files) {
            if (f.isDirectory && (destDir.absolutePath == f.absolutePath || destDir.absolutePath.startsWith(f.absolutePath + File.separator))) {
                viewModelScope.launch {
                    _userMessage.emit("Cannot copy a folder into itself")
                }
                return
            }
        }
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            var allSuccessful = true
            for (src in files) {
                val ok = repository.copyFileOrDirectory(src, destDir)
                if (!ok) allSuccessful = false
            }
            clearSelection()
            _userMessage.emit(if (allSuccessful) "Operation completed" else "Some items could not be processed")
            refreshCurrentDirectory()
        }
    }

    fun moveSelectedTo(destDir: File) {
        val files = _selectedItems.value.map { it.file }
        if (files.isEmpty()) return
        for (f in files) {
            if (f.isDirectory && (destDir.absolutePath == f.absolutePath || destDir.absolutePath.startsWith(f.absolutePath + File.separator))) {
                viewModelScope.launch {
                    _userMessage.emit("Cannot move a folder into itself")
                }
                return
            }
        }
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            var allSuccessful = true
            for (src in files) {
                val ok = repository.moveFileOrDirectory(src, destDir)
                if (!ok) allSuccessful = false
            }
            clearSelection()
            _userMessage.emit(if (allSuccessful) "Operation completed" else "Some items could not be processed")
            refreshCurrentDirectory()
        }
    }

    suspend fun getSubdirectories(dir: File): Result<List<FileItem>> {
        return repository.getSubdirectoriesIn(dir)
    }

    suspend fun createFolderIn(parent: File, name: String): Result<File> {
        return repository.createDirectory(parent, name)
    }

    suspend fun saveTextFile(file: File, content: String): Result<Unit> {
        val result = repository.writeTextFile(file, content)
        if (result.isSuccess) {
            refreshCurrentDirectory()
        }
        return result
    }

    fun analyzeStorage() {
        viewModelScope.launch {
            _isAnalyzingStorage.value = true
            try {
                _storageAnalysis.value = storageAnalysisRepository.analyzeStorage(getApplication())
            } catch (e: Exception) {
                _userMessage.emit("Storage analysis failed: ${e.localizedMessage}")
            } finally {
                _isAnalyzingStorage.value = false
            }
        }
    }

    fun cleanEmptyFolders(folders: List<File>) {
        viewModelScope.launch {
            val count = storageAnalysisRepository.cleanEmptyFolders(folders)
            _userMessage.emit("Removed $count empty folder(s)")
            analyzeStorage()
        }
    }

    fun scanDuplicates() {
        viewModelScope.launch {
            _isScanningDuplicates.value = true
            try {
                _duplicateGroups.value = duplicateFinderRepository.findDuplicates(getApplication())
            } catch (e: Exception) {
                _userMessage.emit("Duplicate scan failed: ${e.localizedMessage}")
            } finally {
                _isScanningDuplicates.value = false
            }
        }
    }

    fun deleteDuplicateFiles(files: List<FileItem>) {
        viewModelScope.launch {
            val fileList = files.map { it.file }
            if (_trashEnabled.value) {
                trashRepository.moveToTrash(fileList)
                    .onSuccess {
                        _userMessage.emit("Moved ${files.size} duplicate(s) to Trash")
                        loadTrashItems()
                    }
                    .onFailure {
                        _userMessage.emit("Failed to move to Trash: ${it.localizedMessage}")
                    }
            } else {
                var deleted = 0
                for (f in fileList) {
                    if (f.delete()) deleted++
                }
                _userMessage.emit("Permanently deleted $deleted duplicate(s)")
            }
            scanDuplicates()
            refreshCurrentDirectory()
        }
    }
}
