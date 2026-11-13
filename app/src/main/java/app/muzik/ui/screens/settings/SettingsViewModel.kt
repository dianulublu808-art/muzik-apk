package app.muzik.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.muzik.data.repository.MusicRepository
import app.muzik.data.settings.AccentColor
import app.muzik.data.settings.MuzikSettings
import app.muzik.data.settings.SettingsRepository
import app.muzik.data.settings.ThemeMode
import app.muzik.muzikContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository: SettingsRepository =
        application.muzikContainer.settingsRepository
    private val musicRepository: MusicRepository = application.muzikContainer.musicRepository

    val settings: StateFlow<MuzikSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MuzikSettings(),
    )

    val isScanning: StateFlow<Boolean> = musicRepository.isScanning
    val scanMessage: StateFlow<String?> = musicRepository.scanMessage
    val trackCount: StateFlow<Int> = musicRepository.trackCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 0,
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setAccent(accent: AccentColor) {
        viewModelScope.launch { settingsRepository.setAccent(accent) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    }

    fun setAnimatedBackground(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAnimatedBackground(enabled) }
    }

    fun refreshLibrary() {
        viewModelScope.launch { musicRepository.refreshLibrary() }
    }

    fun clearScanMessage() {
        musicRepository.clearScanMessage()
    }
}
