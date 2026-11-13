package app.muzik.ui.screens.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.muzik.data.remote.WaveEngine
import app.muzik.data.remote.WaveSource
import app.muzik.data.remote.WaveTrack
import app.muzik.data.repository.MusicRepository
import app.muzik.data.settings.MuzikSettings
import app.muzik.domain.Track
import app.muzik.muzikContainer
import app.muzik.playback.PlaybackManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RemoteSearchState(
    val tracks: List<WaveTrack> = emptyList(),
    val isLoading: Boolean = false,
    val errors: List<String> = emptyList(),
    val enabledSources: Set<WaveSource> = emptySet(),
) {
    val hasResults: Boolean get() = tracks.isNotEmpty()
    val isActive: Boolean get() = enabledSources.any { it != WaveSource.LOCAL }
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val container = application.muzikContainer
    private val repository: MusicRepository = container.musicRepository
    private val playbackManager: PlaybackManager = container.playbackManager

    private var settings = MuzikSettings()

    private val engine = WaveEngine(
        musicRepository = repository,
        youtubeInstanceProvider = { settings.waveYoutubeInstance },
        vkTokenProvider = { settings.waveVkToken },
    )

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        viewModelScope.launch {
            container.settingsRepository.settings.collect { settings = it }
        }
    }

    val results: StateFlow<List<Track>> = _query
        .debounce(180)
        .distinctUntilChanged()
        .flatMapLatest { value ->
            if (value.isBlank()) flowOf(emptyList()) else repository.search(value)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val remote: StateFlow<RemoteSearchState> = _query
        .debounce(450)
        .distinctUntilChanged()
        .flatMapLatest { value ->
            val sources = settings.waveSources
            if (value.isBlank() || sources.none { it != WaveSource.LOCAL }) {
                flowOf(RemoteSearchState(enabledSources = sources))
            } else {
                flow {
                    emit(RemoteSearchState(isLoading = true, enabledSources = sources))
                    val batch = engine.search(value, sources)
                    emit(
                        RemoteSearchState(
                            tracks = batch.tracks,
                            errors = batch.errors,
                            enabledSources = sources,
                        ),
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RemoteSearchState(),
        )

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun clearQuery() {
        _query.value = ""
    }

    /** Играет удалённый трек: докачивает потоки и запускает очередь из площадки. */
    fun playRemote(track: WaveTrack, queue: List<WaveTrack>) {
        viewModelScope.launch {
            val pool = queue.filter { it.source == track.source }.ifEmpty { listOf(track) }
            val resolved = engine.resolveAll(pool)
            if (resolved.isEmpty()) {
                _message.value = "Не удалось получить поток: ${track.source.label}"
                return@launch
            }
            val index = resolved.indexOfFirst { it.uid == track.uid }.coerceAtLeast(0)
            playbackManager.playWave(resolved, index)
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
