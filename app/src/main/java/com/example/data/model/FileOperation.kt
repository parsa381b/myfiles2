package com.example.data.model

import java.io.File

enum class OperationType {
    COPY,
    MOVE
}

data class ClipboardState(
    val sourceFiles: List<File>,
    val type: OperationType
)

enum class FileCategory(val title: String) {
    IMAGES("Images"),
    VIDEOS("Videos"),
    AUDIO("Audio"),
    DOCUMENTS("Documents"),
    DOWNLOADS("Downloads"),
    INSTALLATION_FILES("Installation files")
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppLanguage {
    SYSTEM,
    ENGLISH,
    PERSIAN
}
