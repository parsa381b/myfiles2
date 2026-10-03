# PROJECT_CONTEXT.md

## 1. Project Overview
**My Files** is a fast, lightweight, and responsive Android file manager inspired by the visual language of **Samsung One UI's "My Files"**. It is built with **100% Kotlin**, **Jetpack Compose (Material 3)**, and **Kotlin Coroutines**.

## 2. Key Requirements & Architectural Decisions

### Performance & Low Memory
- **Low Footprint I/O**: Direct file queries via `java.io.File` and NIO executed strictly on `Dispatchers.IO` to keep the UI thread completely unblocked.
- **Allocation-Free Sorting**: Sorting algorithms use in-place/sequence comparators (`compareByDescending<FileItem> { it.isDirectory }.thenBy { ... }`), placing folders at the top and eliminating unnecessary allocations.
- **Compose Recycling (`LazyColumn`)**:
  - `key = { it.path }` provides stable item identity.
  - `contentType = { it.isDirectory }` separates folder view recycling from regular file recycling, preserving 120 FPS scrolling even with directories containing thousands of items.
- **Fast MIME & Icon Mapping**: Lightweight extension lookup table using Android's `MimeTypeMap` without heavy asynchronous disk decodes or memory-hogging bitmap caches.
- **Minimal APK Size**: Kept dependencies lean (no heavy unused third-party runtimes, no local SQL/Room DB overhead). Unused libraries commented out.

### Samsung One UI Design Aesthetic
- **Header & Breadcrumbs**: Large title header that smoothly adapts to folder browsing with interactive horizontal breadcrumbs (`Home > Internal storage > Download > ...`). Tapping any breadcrumb segment jumps directly to that directory.
- **Category Grid**: Rounded 24dp card with pastel-tinted squircle shortcuts for Images, Videos, Audio, Documents, Downloads, and Installation files.
- **Storage Progress Card**: Clean card displaying used/free capacity with a rounded linear progress indicator and percentage for Internal Storage, SD Cards, and attached USB OTG flash drives.
- **USB & External Storage Support**:
  - **Dynamic Multi-Device Detection**: Scans `StorageManager.storageVolumes`, `ContextCompat.getExternalFilesDirs`, `/storage`, and `/mnt/media_rw` for secondary SD cards and USB OTG thumb drives.
  - **Live Plug & Play Hotplugging**: Integrated with Android's `BroadcastReceiver` listening to `ACTION_MEDIA_MOUNTED`, `ACTION_MEDIA_UNMOUNTED`, `ACTION_MEDIA_REMOVED`, `USB_DEVICE_ATTACHED`, and `USB_DEVICE_DETACHED` to instantly auto-refresh volumes without restarting.
  - **Dedicated Samsung One UI Styling**: Custom icon (`Icons.Default.Usb` for USB flash drives, `Icons.Default.SdCard` for SD cards, and `Icons.Default.Smartphone` for internal storage).
  - **Cross-Storage File Operations**: The modal `DestinationPickerDialog` provides horizontal storage filter chips allowing instant cross-device copying and moving between Internal storage, SD cards, and USB drives.
- **Floating Bottom Action Bar**: Contextual bottom dock for selected files (Copy, Move, Share, Delete, Rename, Details).
- **Modal Destination Picker Dialog**: Copy and Move open a centered modal box (`DestinationPickerDialog`) allowing in-dialog folder hierarchy browsing, new folder creation, and direct "Copy here" / "Move here" execution without having to navigate away from the current page.
- **Trash & Recycle Bin System (`TrashRepository` & `TrashScreen`)**:
  - **Settings Toggle On/Off**: Configurable setting with `Switch` control ("Keep deleted files and folders for 30 days before they are deleted permanently"). Persisted via `SharedPreferences` (`pref_trash_enabled`).
  - **Safe Deletion**: When Trash is enabled, deleted files/folders are preserved in `.myfiles_trash/files/` with metadata (`trash_meta.json`). The delete confirmation dialog automatically switches to "Move to Trash".
  - **Dedicated Trash Screen**: View all trashed items with deletion timestamps and thumbnails, multi-select items to **Restore** back to original paths or **Delete Permanently**, or **Empty Trash** with one tap.
  - **30-Day Auto Cleanup**: Automatically cleans up and purges items older than 30 days.
  - **Home Screen Shortcut**: Utilities card displays real-time trash item count and opens the Trash screen.
- **Rich Dynamic Thumbnails (`FileThumbnailView` & `MediaThumbnailLoader`)**:
  - **Audio Cover Art**: Automatically parses embedded ID3/metadata album art from MP3, FLAC, M4A, OGG, WAV, AAC, and Opus files using `MediaMetadataRetriever`. Both the file list and the built-in audio player display the genuine album art with squircle rounded corners.
  - **APK App Icons**: Dynamically resolves and extracts the authentic application icon from uninstalled APK packages via `PackageManager.getPackageArchiveInfo`.
  - **Image Thumbnails**: High-performance asynchronous image thumbnails powered by Coil.
  - **Memory Cache (LRU)**: Thread-safe in-memory cache to maintain instant recycling and 120 FPS scrolling without duplicate disk parses.
- **Built-in Media Viewers**:
  - **Image Viewer**: Fullscreen viewer with pinch-to-zoom (0.7x-6x), double-tap zoom toggle, pan, 90° rotation, and share for JPEG, PNG, GIF, WebP, BMP, HEIC, SVG.
  - **Video Player**: Video player with custom overlay controls (Play/Pause, Seekbar, Rewind 10s, Forward 10s, auto-hide) for MP4, MKV, AVI, MOV, WebM, 3GP.
  - **Audio Player**: One UI styled player card with album art, seek slider, 10s rewind/forward, repeat/loop mode, and accurate timestamp tracking for MP3, WAV, FLAC, OGG, M4A, AAC.
  - **PDF Viewer**: Native PDF renderer powered by Android's `PdfRenderer` with page navigation, zoom in/out, and high-resolution bitmap rendering.
  - **Text Editor**: Syntax-highlighted code editor with line numbers column, UTF-8 status, Edit/View toggle, and in-place file saving for Kotlin, Java, Python, JS, TS, HTML, XML, JSON, SQL, Bash, Markdown, etc.
- **Dark Mode**: Complete support for Dark, Light, and System Default theme modes with OLED-friendly dark surface tones (`#111315`, `#1E2024`) and Samsung Blue accents.
- **Persian Language Support (فارسی)**: Full localization in `res/values-fa/strings.xml` with bidirectional RTL layout engine (`LayoutDirection.Rtl`), allowing seamless switching between English and Persian via the Settings menu or following system language.

### Storage Permissions Strategy
- **Android 11+ (API 30+)**: Implements `MANAGE_EXTERNAL_STORAGE` (`Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION`) with an in-app permission banner that gracefully directs users to system settings.
- **Android 10 & below**: Declares `READ_EXTERNAL_STORAGE` and `WRITE_EXTERNAL_STORAGE` with `requestLegacyExternalStorage="true"`.
- **Inter-App Sharing**: Integrated with `androidx.core.content.FileProvider` (`<applicationId>.provider`) and `file_paths.xml` for opening and sharing files with external viewer apps safely without leaking `file://` URIs.

## 3. Directory & File Structure
```text
/
├── .github/workflows/build.yml     # Automated APK build CI workflow
├── PROJECT_CONTEXT.md              # Architectural overview (this file)
├── README.md                       # Build instructions & guide
├── app/
│   ├── build.gradle.kts            # Application Gradle build configuration
│   └── src/main/
│       ├── AndroidManifest.xml     # Permissions, activities, FileProvider
│       ├── java/com/example/
│       │   ├── MainActivity.kt     # App entry, Scaffold, TopAppBar, bottom docks
│       │   ├── data/
│       │   │   ├── model/
│       │   │   │   ├── FileItem.kt       # Immutable file representation & SortOption
│       │   │   │   ├── StorageInfo.kt    # Storage capacity metrics model
│       │   │   │   ├── TrashItem.kt      # Trashed file representation & metadata
│       │   │   │   └── FileOperation.kt  # ClipboardState & FileCategory
│       │   │   └── repository/
│       │   │       ├── FileRepository.kt  # Background IO file ops (copy, move, delete, rename, search)
│       │   │       └── TrashRepository.kt # 30-day trash lifecycle & restoration
│       │   ├── ui/
│       │   │   ├── components/
│       │   │   │   ├── BreadcrumbBar.kt           # Horizontal scrolling breadcrumb bar
│       │   │   │   ├── FileItemRow.kt             # High-performance list item
│       │   │   │   ├── FileThumbnailView.kt       # Audio cover art & APK icon thumbnail renderer
│       │   │   │   ├── OperationBottomBar.kt      # Contextual action dock & Paste dock
│       │   │   │   ├── DestinationPickerDialog.kt # Modal folder picker dialog for Copy/Move
│       │   │   │   ├── Dialogs.kt                 # Create folder, rename, delete, details, sort, settings
│       │   │   │   └── PermissionBanner.kt        # One UI permission prompt banner
│       │   │   ├── screens/
│       │   │   │   ├── HomeScreen.kt              # Categories grid, storage cards & Trash shortcut
│       │   │   │   ├── BrowserScreen.kt           # Folder browser with LazyColumn & Search
│       │   │   │   └── TrashScreen.kt             # Trash management, restore & empty screen
│       │   │   ├── viewers/
│       │   │   │   ├── ImageViewerDialog.kt       # Zoomable image viewer (pinch/rotate)
│       │   │   │   ├── VideoPlayerDialog.kt       # Video player with custom controls
│       │   │   │   ├── AudioPlayerDialog.kt       # Audio player with timeline & loop
│       │   │   │   ├── PdfViewerDialog.kt         # Native PdfRenderer document viewer
│       │   │   │   └── TextEditorDialog.kt        # Code editor with syntax highlighting & save
│       │   │   ├── theme/
│       │   │   │   ├── Color.kt                   # Samsung Blue & One UI palette
│       │   │   │   ├── Theme.kt                   # Material 3 dynamic color scheme
│       │   │   │   └── Type.kt                    # One UI typography
│       │   │   └── viewmodel/
│       │   │       └── FileViewModel.kt           # State management, selection, clipboard, search
│       │   └── util/
│       │       ├── FileUtils.kt                   # Formatters, Intent launcher, FileProvider share
│       │       └── SyntaxHighlighter.kt           # Code tokenizer & syntax styler
│       └── res/
│           ├── drawable/
│           │   ├── ic_launcher_background.xml
│           │   └── ic_launcher_foreground.xml
│           ├── values/
│           │   ├── colors.xml
│           │   ├── strings.xml
│           │   └── themes.xml
│           └── xml/
│               └── file_paths.xml
```

## 4. Key Classes & Responsibilities
- `FileViewModel`: Central StateFlow hub managing `storageVolumes`, `currentDirectory`, `selectedItems`, `clipboard`, `searchQuery`, `sortOption`, and `uiState`.
- `FileRepository`: Pure IO layer running on `Dispatchers.IO`. Provides recursive streaming copy/move, recursive deletion, folder creation, atomic renaming, and directory file listing.
- `FileUtils`: Static helpers for human-readable file sizes (`formatFileSize`), timestamps (`formatDate`), MIME resolution, and `FileProvider` intents (`openFile`, `shareFiles`).
- `MainActivity`: Jetpack Compose Scaffold host coordinating system back presses, search bar, dialogs, and top/bottom bars.
