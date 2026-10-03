package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

import com.example.data.model.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = SamsungBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF00488F),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFF4C8DF5),
    onSecondary = Color.White,
    background = SurfaceDark,
    onBackground = TextPrimaryDark,
    surface = CardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF26292E),
    onSurfaceVariant = TextSecondaryDark,
    outline = DividerDark
)

private val LightColorScheme = lightColorScheme(
    primary = SamsungBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF1E88E5),
    onSecondary = Color.White,
    background = SurfaceLight,
    onBackground = TextPrimaryLight,
    surface = CardLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFEFF2F5),
    onSurfaceVariant = TextSecondaryLight,
    outline = DividerLight
)

@Composable
fun MyFilesTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemInDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
