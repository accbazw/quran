package com.uthmani.quran.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsStore by preferencesDataStore(name = "quran_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val fontSize: Int,
    val themeMode: ThemeMode,
    val lastSurah: Int,
    val lastAyah: Int,
) {
    companion object {
        const val FONT_SIZE_MIN = 18
        const val FONT_SIZE_MAX = 40
        const val FONT_SIZE_DEFAULT = 26
        val DEFAULT = AppSettings(FONT_SIZE_DEFAULT, ThemeMode.SYSTEM, 0, 0)
    }
}

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val FONT_SIZE = intPreferencesKey("font_size")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LAST_SURAH = intPreferencesKey("last_surah")
        val LAST_AYAH = intPreferencesKey("last_ayah")
    }

    val settings: Flow<AppSettings> = context.settingsStore.data.map { prefs ->
        AppSettings(
            fontSize = prefs[Keys.FONT_SIZE] ?: AppSettings.FONT_SIZE_DEFAULT,
            themeMode = prefs[Keys.THEME_MODE]
                ?.let { mode -> ThemeMode.entries.firstOrNull { it.name == mode } }
                ?: ThemeMode.SYSTEM,
            lastSurah = prefs[Keys.LAST_SURAH] ?: 0,
            lastAyah = prefs[Keys.LAST_AYAH] ?: 0,
        )
    }

    suspend fun setFontSize(size: Int) {
        context.settingsStore.edit {
            it[Keys.FONT_SIZE] = size.coerceIn(AppSettings.FONT_SIZE_MIN, AppSettings.FONT_SIZE_MAX)
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setLastRead(surah: Int, ayah: Int) {
        context.settingsStore.edit {
            it[Keys.LAST_SURAH] = surah
            it[Keys.LAST_AYAH] = ayah
        }
    }
}
