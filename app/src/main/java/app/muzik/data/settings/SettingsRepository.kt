package app.muzik.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.muzik.data.remote.WaveSource
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
    val waveSources: Set<WaveSource> = setOf(WaveSource.SOUNDCLOUD),
    val waveYoutubeInstance: String = DEFAULT_YOUTUBE_INSTANCE,
    val waveVkToken: String = "",
    val waveLiked: Set<String> = emptySet(),
    val waveDisliked: Set<String> = emptySet(),
    val waveSeeds: Set<String> = emptySet(),
) {
    companion object {
        const val DEFAULT_YOUTUBE_INSTANCE = "https://inv.nadeko.net"
    }
}

private val Context.muzikDataStore by preferencesDataStore(name = "muzik_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val accent = stringPreferencesKey("accent")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val animatedBackground = booleanPreferencesKey("animated_background")
        val lastScanAt = longPreferencesKey("last_scan_at")
        val waveSources = stringSetPreferencesKey("wave_sources")
        val waveYoutubeInstance = stringPreferencesKey("wave_youtube_instance")
        val waveVkToken = stringPreferencesKey("wave_vk_token")
        val waveLiked = stringSetPreferencesKey("wave_liked")
        val waveDisliked = stringSetPreferencesKey("wave_disliked")
        val waveSeeds = stringSetPreferencesKey("wave_seeds")
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
                waveSources = preferences[Keys.waveSources]
                    ?.mapNotNull(WaveSource::fromName)
                    ?.toSet()
                    ?: setOf(WaveSource.SOUNDCLOUD),
                waveYoutubeInstance = preferences[Keys.waveYoutubeInstance]
                    ?.takeIf { it.isNotBlank() }
                    ?: MuzikSettings.DEFAULT_YOUTUBE_INSTANCE,
                waveVkToken = preferences[Keys.waveVkToken].orEmpty(),
                waveLiked = preferences[Keys.waveLiked].orEmpty(),
                waveDisliked = preferences[Keys.waveDisliked].orEmpty(),
                waveSeeds = preferences[Keys.waveSeeds].orEmpty(),
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

    suspend fun setWaveSource(source: WaveSource, enabled: Boolean) {
        context.muzikDataStore.edit { preferences ->
            val current = preferences[Keys.waveSources]
                ?.mapNotNull(WaveSource::fromName)
                ?.toMutableSet()
                ?: mutableSetOf(WaveSource.SOUNDCLOUD)
            if (enabled) current += source else current -= source
            preferences[Keys.waveSources] = current.mapTo(mutableSetOf()) { it.name }
        }
    }

    suspend fun setWaveYoutubeInstance(instance: String) {
        context.muzikDataStore.edit { it[Keys.waveYoutubeInstance] = instance.trim() }
    }

    suspend fun setWaveVkToken(token: String) {
        context.muzikDataStore.edit { it[Keys.waveVkToken] = token.trim() }
    }

    suspend fun likeWaveTrack(uid: String, seed: String?) {
        context.muzikDataStore.edit { preferences ->
            preferences[Keys.waveLiked] =
                (preferences[Keys.waveLiked].orEmpty() + uid).toList().takeLast(MAX_FEEDBACK).toSet()
            preferences[Keys.waveDisliked] = preferences[Keys.waveDisliked].orEmpty() - uid
            val cleanSeed = seed?.trim().orEmpty()
            if (cleanSeed.isNotBlank()) {
                preferences[Keys.waveSeeds] =
                    (preferences[Keys.waveSeeds].orEmpty() + cleanSeed)
                        .toList()
                        .takeLast(MAX_SEEDS)
                        .toSet()
            }
        }
    }

    suspend fun dislikeWaveTrack(uid: String) {
        context.muzikDataStore.edit { preferences ->
            preferences[Keys.waveDisliked] =
                (preferences[Keys.waveDisliked].orEmpty() + uid).toList().takeLast(MAX_FEEDBACK).toSet()
            preferences[Keys.waveLiked] = preferences[Keys.waveLiked].orEmpty() - uid
        }
    }

    suspend fun clearWaveFeedback() {
        context.muzikDataStore.edit { preferences ->
            preferences[Keys.waveLiked] = emptySet()
            preferences[Keys.waveDisliked] = emptySet()
            preferences[Keys.waveSeeds] = emptySet()
        }
    }

    private companion object {
        const val MAX_FEEDBACK = 300
        const val MAX_SEEDS = 40
    }
}
