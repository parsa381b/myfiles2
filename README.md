# My Files 📁

A fast, lightweight, and responsive Android file manager styled after **Samsung One UI's "My Files"**. Built with modern Android best practices: **100% Kotlin**, **Jetpack Compose (Material 3)**, and **Kotlin Coroutines**.

---

## ✨ Features
- ⚡ **Ultra-Fast & Zero Lag**: Instant folder loading with asynchronous background I/O on `Dispatchers.IO`. Smooth 120 FPS scrolling using `LazyColumn` item recycling (`key` and `contentType`).
- 🪶 **Lightweight Footprint**: No heavy database runtimes or memory-hungry bitmap caching. Generates a lean, optimized APK under 4 MB.
- 🎨 **Samsung One UI Design**:
  - **Device-Wide Category Hubs**: Clicking any category shortcut (Audio/Music, Images, Videos, Documents, Downloads, Installation files) searches for and lists every file of that type across the entire storage (internal memory, SD cards, and external drives) with containing folder paths and file sizes.
  - Squircle category shortcuts: Images, Videos, Audio, Documents, Downloads, Installation files.
  - Storage capacity card with a progress bar and usage breakdown.
  - Interactive horizontal breadcrumbs for instant directory jumping.
  - Floating bottom action dock for multi-selection and clipboard operations.
  - **Dynamic File Thumbnails**: Embedded cover art for audio files (MP3, FLAC, M4A, OGG) and live application icons extracted from APK packages.
  - **First-Time Storage Access Rationale Modal**: Prominently guides users upon first launching the app, clearly detailing why All-Files access is required (file organization, media playback, package installation, offline privacy guarantee).
  - **Archive Extractor (ZIP, RAR, 7Z)**: Built-in unzipping and archive extraction with real-time percentage progress bar, archive inspection (entry count, uncompressed size, contents preview), destination selector (subfolder or current folder), and Zip Slip path traversal security.
  - **In-App Package Installer (APK, XAPK, APKS)**: Native installation support for standalone APKs, split APK bundles (APKS), and multi-part XAPKs with automatic OBB expansion extraction, package details dialog, and seamless unknown-sources permission guidance.
  - **Show Hidden Files Toggle**: Settings option to toggle visibility of hidden dotfiles and system folders (e.g. `.nomedia`, `.config`), styled with subtle translucency for easy visual distinction.
  - **USB & External Storage Support**: Full detection and browsing for USB OTG flash drives and micro-SD cards with live hotplug listener and cross-device copy/move chips.
  - **Trash & Recycle Bin**: Full Trash system with a settings toggle ON/OFF, 30-day retention, Home screen shortcut, batch Restore, and Empty Trash.
  - **Dark Mode**: System, Light, and Dark mode theme support with high-contrast surfaces.
  - **Persian Language (فارسی)**: Full native Persian translations with authentic Right-to-Left (RTL) layout support and an in-app language switcher.
- 🎬 **Built-in Media Viewers**:
  - **Image Viewer**: High-res viewing with pinch-to-zoom (up to 6x), double-tap zoom toggle, panning, 90° rotation, and sharing (JPEG, PNG, GIF, WebP, BMP, HEIC, SVG).
  - **Video Player**: Native hardware-accelerated playback with play/pause, scrub slider, 10s forward/rewind, full-screen controls, and proper edge-to-edge window insets padding (`navigationBarsPadding()` / `statusBarsPadding()`) preventing bottom control clipping (MP4, MKV, AVI, MOV, WebM, 3GP).
  - **Audio Player**: One UI music player card with album art, scrub slider, repeat/loop mode, 10s skip, and background audio support (MP3, WAV, FLAC, OGG, M4A, AAC).
  - **PDF Reader**: Powered by **AndroidPdfViewer** with multi-page continuous vertical rendering, hardware-accelerated tile rasterization, pinch-to-zoom, double-tap zoom, fast scroll handle (`DefaultScrollHandle`), Night mode inverted reading, view mode toggle (continuous vertical scroll vs. horizontal page flip), and a floating page navigation pill with jump slider (`Page X of Y`).
  - **Text Editor**: Syntax-highlighted code editor for Kotlin, Java, Python, JS, TS, HTML, XML, JSON, SQL, Bash, Markdown, etc., with line numbers, Edit/View modes, and in-place file saving.
- 🛠️ **Full File Management**:
  - **Superfast Universal Search**: Real-time dual-engine search combining Android MediaStore indexed SQLite queries (10-30ms execution) with breadth-first file system scanning across internal storage, SD cards, and USB OTG drives.
  - **Rich Search Display**: Search result items display the file name, full folder path under the name in primary accent styling, and file size with modification date.
  - Folder browsing & fast in-folder search.
  - File operations: Rename, recursive Delete, Copy, Move with modal destination picker, Share via `FileProvider`.
  - Open files using default system viewer apps.
  - Sort by Name (A-Z / Z-A), Date modified (Newest / Oldest), and Size (Largest / Smallest).
  - Folder creation with instant refresh.

---

## 🚀 How to Get an APK (Without Android Studio)

### Method 1: Using GitHub Actions (Automated CI/CD)
You can build and download the APK directly on GitHub without installing Android Studio or the Android SDK locally:

1. Push or fork this repository to your GitHub account.
2. In your GitHub repository, navigate to the **Actions** tab.
3. Select the **Build Android APK** workflow from the left sidebar and click **Run workflow** (or simply push a commit to `main`).
4. Wait for the build workflow to finish (typically takes 1–2 minutes).
5. Click on the completed workflow run.
6. Scroll down to the **Artifacts** section at the bottom and download **`MyFiles-Debug-APK`**.
7. Extract the ZIP archive to get **`MyFiles-Debug.apk`** and transfer/install it on your Android phone.
8. *(Optional)* If you push a Git tag starting with `v` (e.g. `git tag v1.0.0 && git push --tags`), the workflow automatically publishes the APK directly to the GitHub **Releases** tab as a direct download.

---

### Method 2: Build Locally via Command Line (Terminal)
Prerequisites: JDK 17 or higher.

```bash
# Clone the repository
git clone https://github.com/<your-username>/myfiles.git
cd myfiles

# Make the gradle wrapper executable (Linux / macOS)
chmod +x gradlew

# Build the Debug APK
./gradlew assembleDebug

# On Windows:
gradlew.bat assembleDebug

# Output APK location:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 🏛️ Architecture & Documentation
For a complete summary of the code structure, design decisions, and data pipelines, see [**`PROJECT_CONTEXT.md`**](PROJECT_CONTEXT.md).
