package app.muzik.ui.screens.wave

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.muzik.data.remote.WaveEngine
import app.muzik.data.remote.WaveSource
import app.muzik.data.remote.WaveTrack
import app.muzik.data.settings.MuzikSettings
import app.muzik.data.settings.SettingsRepository
import app.muzik.muzikContainer
import app.muzik.playback.PlaybackManager
import app.muzik.playback.PlayerUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WaveUiState(
    val isLoading: Boolean = false,
    val isActive: Boolean = false,
    val current: WaveTrack? = null,
    val upcoming: List<WaveTrack> = emptyList(),
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val progress: Float = 0f,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val sources: Set<WaveSource> = emptySet(),
    val liked: Set<String> = emptySet(),
    val disliked: Set<String> = emptySet(),
    val sourceErrors: List<String> = emptyList(),
    val message: String? = null,
    val localTrackCount: Int = 0,
) {
    val hasRemoteSource: Boolean get() = sources.any { it != WaveSource.LOCAL }
}

/**
 * Логика «Волны»: собирает порции треков, докачивает следующие,
 * хранит лайки/дизлайки и управляет очередью плеера.
 */
class WaveViewModel(application: Application) : AndroidViewModel(application) {

    private val container = application.muzikContainer
    private val settingsRepository: SettingsRepository = container.settingsRepository
    private val playbackManager: PlaybackManager = container.playbackManager

    private var currentSettings = MuzikSettings()

    private val engine = WaveEngine(
        musicRepository = container.musicRepository,
        youtubeInstanceProvider = { currentSettings.waveYoutubeInstance },
        vkTokenProvider = { currentSettings.waveVkToken },
    )

    private val _state = MutableStateFlow(WaveUiState())
    val state: StateFlow<WaveUiState> = _state.asStateFlow()

    private val known = LinkedHashMap<String, WaveTrack>()
    private var loadingMore = false
    private var extendCooldownUntil = 0L
    private var startJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                currentSettings = settings
                _state.update {
                    it.copy(
                        sources = settings.waveSources,
                        liked = settings.waveLiked,
                        disliked = settings.waveDisliked,
                    )
                }
            }
        }
        viewModelScope.launch {
            container.musicRepository.trackCount.collect { count ->
                _state.update { it.copy(localTrackCount = count) }
            }
        }
        viewModelScope.launch {
            playbackManager.state.collect(::onPlayerState)
        }
    }

    fun startWave() {
        if (startJob?.isActive == true) return
        startJob = viewModelScope.launch {
            _state.update {
                it.copy(isLoading = true, message = null, sourceErrors = emptyList(), current = null)
            }
            known.clear()
            extendCooldownUntil = 0L

            val locals = engine.localTracks(LOCAL_MIX)
            val batch = engine.nextBatch(
                sources = currentSettings.waveSources,
                seeds = seeds(),
                disliked = currentSettings.waveDisliked,
            )
            val candidates = mix(locals, batch.tracks)
            val errors = visibleErrors(batch.errors)

            if (candidates.isEmpty()) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isActive = false,
                        sourceErrors = errors,
                        message = errors.firstOrNull() ?: "Не удалось собрать волну",
                    )
                }
                return@launch
            }

            val first = engine.resolveAll(candidates.take(FIRST_CHUNK))
            if (first.isEmpty()) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isActive = false,
                        sourceErrors = errors,
                        message = "Источники не отдали ни одного воспроизводимого трека",
                    )
                }
                return@launch
            }

            remember(first)
            playbackManager.playWave(first)
            _state.update {
                it.copy(isLoading = false, isActive = true, sourceErrors = errors)
            }

            val rest = engine.resolveAll(candidates.drop(FIRST_CHUNK))
            if (rest.isNotEmpty()) {
                remember(rest)
                playbackManager.appendWave(rest)
            }
        }
    }

    fun togglePlayPause() = playbackManager.togglePlayPause()

    fun skip() = playbackManager.next()

    fun previous() = playbackManager.previous()

    fun like() {
        val track = _state.value.current ?: return
        viewModelScope.launch {
            settingsRepository.likeWaveTrack(track.uid, track.artist.ifBlank { track.title })
        }
    }

    fun dislike() {
        val track = _state.value.current ?: return
        viewModelScope.launch { settingsRepository.dislikeWaveTrack(track.uid) }
        playbackManager.next()
    }

    fun clearFeedback() {
        viewModelScope.launch {
            settingsRepository.clearWaveFeedback()
            _state.update { it.copy(message = "Предпочтения «Волны» сброшены") }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun onPlayerState(playerState: PlayerUiState) {
        val track = playerState.currentTrack
        val waveTrack = track?.let { known[it.uid] }
        val active = waveTrack != null

        val upcoming = if (active) {
            playerState.queue
                .drop(playerState.currentIndex + 1)
                .mapNotNull { known[it.uid] }
        } else {
            emptyList()
        }

        _state.update {
            it.copy(
                isActive = active,
                current = if (active) waveTrack else null,
                upcoming = upcoming,
                isPlaying = playerState.isPlaying,
                isBuffering = playerState.isBuffering,
                progress = playerState.progress,
                positionMs = playerState.positionMs,
                durationMs = playerState.durationMs,
            )
        }

        if (active) {
            val remaining = known.size - 1 - playerState.currentIndex
            if (remaining <= EXTEND_THRESHOLD) extend()
        }
    }

    private fun extend() {
        if (loadingMore) return
        if (System.currentTimeMillis() < extendCooldownUntil) return
        loadingMore = true
        viewModelScope.launch {
            try {
                val batch = engine.nextBatch(
                    sources = currentSettings.waveSources,
                    seeds = seeds(),
                    disliked = currentSettings.waveDisliked,
                )
                val fresh = batch.tracks.filter { it.uid !in known }
                val resolved = engine.resolveAll(fresh.take(FIRST_CHUNK))
                if (resolved.isNotEmpty()) {
                    remember(resolved)
                    playbackManager.appendWave(resolved)
                } else {
                    extendCooldownUntil = System.currentTimeMillis() + EXTEND_COOLDOWN_MS
                }
                _state.update { it.copy(sourceErrors = visibleErrors(batch.errors)) }
            } finally {
                loadingMore = false
            }
        }
    }

    private fun visibleErrors(errors: List<String>): List<String> =
        if (currentSettings.waveSources.any { it != WaveSource.LOCAL }) errors else emptyList()

    private fun remember(tracks: List<WaveTrack>) {
        tracks.forEach { known[it.uid] = it }
    }

    private fun seeds(): List<String> =
        (currentSettings.waveSeeds.toList() + WaveEngine.DEFAULT_SEEDS).distinct()

    private fun mix(locals: List<WaveTrack>, remote: List<WaveTrack>): List<WaveTrack> {
        if (locals.isEmpty()) return remote
        val result = mutableListOf<WaveTrack>()
        val remoteIterator = remote.iterator()
        locals.forEachIndexed { index, local ->
            result += local
            if (index % LOCAL_EVERY == LOCAL_EVERY - 1 && remoteIterator.hasNext()) {
                result += remoteIterator.next()
            }
        }
        while (remoteIterator.hasNext()) result += remoteIterator.next()
        return result
    }

    private companion object {
        const val FIRST_CHUNK = 6
        const val LOCAL_MIX = 3
        const val LOCAL_EVERY = 3
        const val EXTEND_THRESHOLD = 2
        const val EXTEND_COOLDOWN_MS = 12_000L
    }
}
