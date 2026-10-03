# Release Notes

## Version 1.6.0 — Remember Folder Sort Preference

**Release Date:** October 3, 2026

---

### Highlights

1. **"Remember Folder Sort" Setting**
   - Added a new toggle in the Settings dialog under **Sorting preference**: **"Remember folder sort"** (*"Save custom sort for each folder individually (default: A to Z)"*).
   - **Per-Folder Persistence**: When enabled, the app tracks and remembers the sorting option (Name A-Z, Name Z-A, Date Newest, Date Oldest, Size Largest, Size Smallest) chosen for each individual directory on your storage.
   - **Default A to Z**: Whenever you enter a folder that has not yet had a custom sort option selected, the app automatically sorts its items by **Name A to Z** (`NAME_ASC`).
   - Changing the sort order in any folder immediately associates and remembers that sort order for subsequent visits to that folder.
   - Fully localized in English and Persian (فارسی).

---

## Version 1.5.0 — Storage Usage & Duplicate File Finder in Advanced Features

**Release Date:** October 3, 2026

---

### Highlights

1. **Storage Usage Analyzer**
   - **Visual Storage Breakdown**: View comprehensive internal memory analytics showing total, used, and free capacity with percentage and category-colored bar gauges.
   - **Category Breakdown**: Live byte count and file totals for Images, Videos, Audio, Documents, Installation Files (APKs/XAPKs), Archives, and System/Other.
   - **Large Files Hunter (>50 MB)**: Automatically aggregates heavy files across all folders, allowing users to inspect containing directories, open them with built-in media viewers, or delete them to quickly free space.
   - **Empty Folder Cleaner**: Detects abandoned empty directories across storage and cleans them with a single click.

2. **Duplicate File Finder**
   - **High-Speed Dual-Stage Detection**: Employs size-collision grouping followed by MD5 header/footer and full SHA-256 cryptographic verification for 100% accuracy.
   - **Intelligent Batch Presets**:
     - *"Keep Oldest"*: Automatically selects all newer duplicate copies for deletion while preserving the original file.
     - *"Keep Newest"*: Automatically selects older copies for deletion.
     - Individual checkbox control for custom fine-grained cleanup.
   - **Safe Recovery**: Real-time counter of recoverable space and safe movement to Trash with deletion confirmation safeguards.

---

## Version 1.4.2 — Advanced Features in Utilities

**Release Date:** October 3, 2026

---

### Highlights

1. **Advanced Features in Utilities (Under Trash)**
   - Added an **Advanced Features** card directly below **Trash** inside the **Utilities** section of the Home screen.
   - Styled with a modern purple squircle badge (`Icons.Default.Tune`), title, and subtitle (*"Tools & experimental capabilities"*).
   - Tapping the card opens the new **`AdvancedFeaturesScreen`**, featuring a dedicated One UI view with hardware back navigation (`BackHandler`).
   - Prepared modular feature slots ready to be populated with your upcoming capabilities whenever you are ready.
   - Fully localized in English and Persian (فارسی).

---

## Version 1.4.1 — Select All & Deselect All Toggle

**Release Date:** October 3, 2026

---

### Highlights

1. **Two-Way Select All / Deselect All Toggle**
   - **Click Once to Select All**: When files are not all selected, clicking the Select All button selects every file in the current folder or category.
   - **Click Again to Deselect All**: When all files are already selected, clicking the button again immediately clears the selection and deselects all items.
   - **Visual State Feedback**:
     - The icon automatically switches between `Icons.Default.SelectAll` and `Icons.Default.Deselect`.
     - The button highlights with the theme's primary accent color when all items are selected.
     - Screen readers announce "Select All" or "Deselect All" according to the current state.
   - Available across both directory browsing (`BrowserScreen`) and category hubs (`CategoryFilesScreen`).

---

## Version 1.4.0 — File Sharing Reliability & AndroidPdfViewer Integration

**Release Date:** October 3, 2026

---

### Highlights

1. **Repaired System File Sharing Functionality**
   - **Root Cause Resolved**: Sharing previously encountered failures or silent drops when invoked from Compose dialogs, secondary threads, or external storage volumes where `FileProvider` authorities or intent flags were insufficient.
   - **`FLAG_ACTIVITY_NEW_TASK` on Chooser**: Added proper task flags on the `Intent.createChooser` intent itself, preventing Android runtime crashes when starting the share sheet outside an Activity context.
   - **Modern `ClipData` Grants**: Attached `ClipData` containing all content URIs to both `ACTION_SEND` and `ACTION_SEND_MULTIPLE`. This ensures receiving applications on Android 10, 11, 12, 13, 14, and 15 (e.g. Gmail, WhatsApp, Telegram, Google Drive) receive explicit read permission without encountering `SecurityException: Permission Denial`.
   - **SD Card & Removable Media Cache Fallback**: When sharing files located on external SD cards or USB OTG mount points that cannot be resolved directly by standard internal FileProvider paths, the application automatically stages the file to a secure app cache directory and provisions the URI seamlessly.
   - **Package Permission Resolution Loop**: Added an explicit permission loop calling `grantUriPermission()` for all target applications resolving the share intent.
   - **Folder Validation**: Selecting folders now informs the user that directory objects cannot be shared directly, asking them to select files instead.

2. **Upgraded PDF Reader with `AndroidPdfViewer`**
   - **Full Multi-Page Rendering**: Render multi-hundred-page documents with smooth continuous vertical scrolling and hardware-accelerated tile rasterization.
   - **Interactive Reading Controls**:
     - Fast scroll handle (`DefaultScrollHandle`) showing real-time page numbers.
     - View mode toggle (continuous vertical scroll vs. horizontal e-book page flip).
     - Night mode inverted color toggle for night-time reading.
     - Floating page indicator pill with quick jump slider (`Page X of Y`).
     - Pinch-to-zoom, double-tap zoom, and Fit-to-Screen reset.

3. **Storage-Wide Category Hubs**
   - Clicking category shortcuts (Music/Audio, Images, Videos, Documents, Downloads, Installation files) searches all storage volumes (Internal Memory, SD cards, and USB drives) for matching files, displaying containing folder paths, file sizes, and quick actions.

---

### Verification & Testing
- Unit tests added and passing for file sharing URI parameters and MIME type mappings.
- Clean compilation and lint verification with zero errors.
