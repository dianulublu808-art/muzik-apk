package app.muzik.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AccentColor { PURPLE, PINK, BLUE, GREEN, ORANGE }

data class MuzikSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accent: AccentColor = AccentColor.PURPLE,
    val dynamicColor: Boolean = false,
    val animatedBackground: Boolean = true,
    val lastScanAt: Long = 0L,
)

private val Context.muzikDataStore by preferencesDataStore(name = "muzik_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val accent = stringPreferencesKey("accent")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val animatedBackground = booleanPreferencesKey("animated_background")
        val lastScanAt = longPreferencesKey("last_scan_at")
    }

    val settings: Flow<MuzikSettings> = context.muzikDataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences ->
            MuzikSettings(
                themeMode = preferences[Keys.themeMode]
                    ?.let { value -> ThemeMode.entries.firstOrNull { it.name == value } }
                    ?: ThemeMode.DARK,
                accent = preferences[Keys.accent]
                    ?.let { value -> AccentColor.entries.firstOrNull { it.name == value } }
                    ?: AccentColor.PURPLE,
                dynamicColor = preferences[Keys.dynamicColor] ?: false,
                animatedBackground = preferences[Keys.animatedBackground] ?: true,
                lastScanAt = preferences[Keys.lastScanAt] ?: 0L,
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.muzikDataStore.edit { it[Keys.themeMode] = mode.name }
    }

    suspend fun setAccent(accent: AccentColor) {
        context.muzikDataStore.edit { it[Keys.accent] = accent.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.muzikDataStore.edit { it[Keys.dynamicColor] = enabled }
    }

    suspend fun setAnimatedBackground(enabled: Boolean) {
        context.muzikDataStore.edit { it[Keys.animatedBackground] = enabled }
    }

    suspend fun setLastScanAt(timestamp: Long) {
        context.muzikDataStore.edit { it[Keys.lastScanAt] = timestamp }
    }
}
