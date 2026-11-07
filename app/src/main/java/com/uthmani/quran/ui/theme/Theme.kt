package com.uthmani.quran.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.uthmani.quran.R

/** Quranic text font: Amiri Quran (SIL OFL 1.1) — see SOURCES.md. */
val QuranFontFamily: FontFamily = FontFamily(Font(R.font.amiri_quran))

// ---------- Light palette (green-based; no blue/indigo) ----------
private val LightColors = lightColorScheme(
    primary = Color(0xFF3B6939),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBDF0BA),
    onPrimaryContainer = Color(0xFF002105),
    secondary = Color(0xFF52634F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD5E8CF),
    onSecondaryContainer = Color(0xFF101F10),
    tertiary = Color(0xFF6F5B00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE08C),
    onTertiaryContainer = Color(0xFF221B00),
    background = Color(0xFFF9FAF3),
    onBackground = Color(0xFF1A1C18),
    surface = Color(0xFFF9FAF3),
    onSurface = Color(0xFF1A1C18),
    surfaceVariant = Color(0xFFDFE4D8),
    onSurfaceVariant = Color(0xFF43483F),
    outline = Color(0xFF73796E),
)

// ---------- Dark palette ----------
private val DarkColors = darkColorScheme(
    primary = Color(0xFFA2D39F),
    onPrimary = Color(0xFF0A390F),
    primaryContainer = Color(0xFF235023),
    onPrimaryContainer = Color(0xFFBDF0BA),
    secondary = Color(0xFFB9CCB4),
    onSecondary = Color(0xFF243424),
    secondaryContainer = Color(0xFF3A4B39),
    onSecondaryContainer = Color(0xFFD5E8CF),
    tertiary = Color(0xFFE6C449),
    onTertiary = Color(0xFF3B2F00),
    tertiaryContainer = Color(0xFF554500),
    onTertiaryContainer = Color(0xFFFFE08C),
    background = Color(0xFF121410),
    onBackground = Color(0xFFE2E3DC),
    surface = Color(0xFF121410),
    onSurface = Color(0xFFE2E3DC),
    surfaceVariant = Color(0xFF43483F),
    onSurfaceVariant = Color(0xFFC3C8BC),
    outline = Color(0xFF8D9287),
)

@Composable
fun QuranTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
