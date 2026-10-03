# Changelog

All notable changes to the **My Files** Android application are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [1.6.0] - 2026-10-03

### Added
- **Remember Folder Sort Preference**:
  - Added **"Remember Folder Sort"** toggle under a dedicated **Sorting preference** section in Settings.
  - When enabled, the app remembers the custom sorting method selected by the user for each directory individually (persisted in SharedPreferences by folder canonical path).
  - Newly visited folders automatically default to **Name A to Z** (`NAME_ASC`) as requested.
  - Changing the sort method in any folder automatically saves and associates that sort order with that specific folder.
  - Full localization in English and Persian (فارسی).

---

## [1.5.0] - 2026-10-03

### Added
- **Storage Usage Analyzer in Advanced Features**:
  - Full graphical storage analysis dashboard showing total, used, and free capacity with color-coded multi-category progress gauge.
  - Granular breakdown of storage consumed by Images, Videos, Audio, Documents, Apps / APKs, Archives, and System/Other.
  - **Large Files Detection (>50 MB)**: Automatically identifies space hogs across all internal memory and SD cards, with tap-to-open and individual delete action.
  - **Empty Folder Cleaner**: Scans for cluttering empty directories and safely removes them with a single click.
- **Duplicate File Finder in Advanced Features**:
  - Lightning-fast dual-stage duplicate scanning combining byte-size grouping and MD5/SHA-256 cryptographic verification.
  - Grouped duplicate listing displaying identical copies, containing folder paths, last modified timestamps, and badges indicating the original oldest file vs. copies.
  - Smart batch selection presets: **"Keep Oldest"** (marks all newer duplicate copies for deletion) and **"Keep Newest"**.
  - Bottom action bar computing recoverable megabytes/gigabytes in real-time with confirmation dialog prior to trash movement or deletion.

---

## [1.4.2] - 2026-10-03

### Added
- **Advanced Features Hub in Utilities**:
  - Added **Advanced Features** shortcut card under **Trash** in the **Utilities** section of the Home screen.
  - Implemented `AdvancedFeaturesScreen` with Samsung One UI card architecture and status bar inset padding.
  - Integrated modular feature slots ready to be populated with custom capabilities (Deep Storage Analyzer, Duplicate File Finder, Batch File Renamer, Local Network & Wi-Fi Sharing).
  - Added full English and Persian localization (`R.string.advanced_features`, `R.string.advanced_features_subtitle`).

---

## [1.4.1] - 2026-10-03

### Changed
- **Select All / Deselect All Toggle**:
  - Tapping the "Select All" button when all items are selected now deselects all files immediately.
  - Added dynamic icon switching between `Icons.Default.SelectAll` and `Icons.Default.Deselect` in both `BrowserScreen` and `CategoryFilesScreen`.
  - Added primary accent tinting and accessible content description ("Deselect All") when in the all-selected state.

---

## [1.4.0] - 2026-10-03

### Fixed
- **System File Sharing (`FileUtils.shareFiles`)**:
  - Fixed an issue where the system share sheet failed or crashed due to missing `FLAG_ACTIVITY_NEW_TASK` on the chooser intent when initiated from dialogs or non-activity contexts.
  - Added Android 10+ `ClipData` attachment to `Intent.ACTION_SEND` and `Intent.ACTION_SEND_MULTIPLE`, preventing `SecurityException: Permission Denial` when target apps (WhatsApp, Gmail, Telegram, Google Drive) read the shared stream.
  - Added explicit `context.grantUriPermission()` resolution loop granting read permissions to all matched target package candidates.
  - Added automated fallback caching: if `FileProvider` cannot resolve a file on secondary/removable SD card partitions or external USB mount points, the file is safely staged to `cacheDir/shared_cache/` so sharing always succeeds.
  - Added folder validation: prevents sharing directory objects directly and displays a clear message to select files instead.
- **External App Launcher (`FileUtils.openFile`)**:
  - Added fallback caching and `ClipData` URI grants to ensure "Open with" reliably launches external reader applications.

### Added
- **AndroidPdfViewer Library Integration**:
  - Integrated `com.github.mhiew:android-pdf-viewer:3.2.0-beta.3` from Maven Central.
  - Implemented continuous vertical page scrolling with hardware-accelerated tile rasterization for smooth multi-page document rendering.
  - Added draggable fast-scroll handle (`DefaultScrollHandle`) displaying current page index in real time.
  - Added Dual View Modes: continuous vertical scroll vs. horizontal page-flip.
  - Added Night Mode toggle with color inversion for comfortable night reading.
  - Added Floating Page Navigation Pill with quick jump slider (`Page X of Y`).
- **Device-Wide Category Scanning**:
  - Clicking any category shortcut (Music/Audio, Images, Videos, Documents, Downloads, Installation files) searches the entire storage (internal storage, SD cards, USB OTG) instead of opening a single static directory.
  - Dedicated `CategoryFilesScreen` showing item counts, total storage size, sort dialog, containing folder paths, and direct viewer launch.

---

## [1.3.0] - 2026-10-02

### Added
- **Archive Extraction Hub**: Built-in decompression for ZIP, RAR, and 7Z archives with real-time extraction progress bar and Zip Slip path traversal security.
- **In-App Package Installer**: Native installation support for APK, XAPK, and APKS package files with automatic OBB extraction and unknown-sources permission guidance.
- **Show Hidden Files**: Toggle in settings to show or hide dotfiles and hidden system folders (`.nomedia`, etc.).
- **Trash & Recycle Bin**: 30-day retention trash system with batch restore, permanent delete, and empty trash.
- **Persian Language Support**: Full Persian translations with authentic RTL layout.

---

## [1.2.0] - 2026-10-01

### Added
- **Universal Search Engine**: Dual-engine search combining Android MediaStore indexed SQLite queries with breadth-first file system scanning across all storage volumes.
- **Media Viewers**:
  - Image Viewer with pinch-to-zoom (up to 6x), panning, double-tap zoom, and rotation.
  - Video Player with hardware-accelerated playback and edge-to-edge window insets padding.
  - Audio Player with One UI player card, album art extraction, and scrub slider.
  - Text & Code Editor with syntax highlighting for 20+ programming languages and in-place saving.

---

## [1.1.0] - 2026-09-30

### Added
- **Samsung One UI Design System**:
  - Squircle category shortcuts.
  - Storage capacity card with usage breakdown and progress indicator.
  - Interactive breadcrumb bar for folder hierarchy navigation.
  - Floating bottom action dock for multi-selection operations (Copy, Move, Rename, Delete, Share, Details).

---

## [1.0.0] - 2026-09-28

### Added
- Initial release of Samsung One UI inspired file manager for Android.
- Core directory navigation, file operations, and storage volume detection.
