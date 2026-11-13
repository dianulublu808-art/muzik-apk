package app.muzik.ui.screens.playlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.muzik.data.repository.AddToPlaylistResult
import app.muzik.data.repository.MusicRepository
import app.muzik.domain.Playlist
import app.muzik.domain.Track
import app.muzik.muzikContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlaylistDetail(
    val playlist: Playlist,
    val tracks: List<Track>,
)

class PlaylistViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MusicRepository = application.muzikContainer.musicRepository

    private val selectedId = MutableStateFlow<Long?>(null)

    val playlists: StateFlow<List<Playlist>> = repository.playlists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedPlaylist: StateFlow<PlaylistDetail?> = selectedId
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(null)
            } else {
                combine(
                    repository.observePlaylist(id),
                    repository.observePlaylistTracks(id),
                ) { playlist, tracks ->
                    playlist?.let { PlaylistDetail(it.copy(trackCount = tracks.size), tracks) }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun selectPlaylist(playlistId: Long) {
        selectedId.value = playlistId
    }

    fun createPlaylist(name: String, onCreated: (Long) -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.createPlaylist(name)
            _message.value = "Плейлист «${name.trim()}» создан"
            onCreated(id)
        }
    }

    suspend fun addTrackNow(playlistId: Long, mediaId: Long) {
        when (repository.addTrackToPlaylist(playlistId, mediaId)) {
            AddToPlaylistResult.ADDED -> _message.value = "Трек добавлен в плейлист"
            AddToPlaylistResult.ALREADY_PRESENT ->
                _message.value = "Этот трек уже есть в плейлисте"
        }
    }

    suspend fun createAndAddNow(name: String, mediaId: Long) {
        if (name.isBlank()) return
        val id = repository.createPlaylist(name)
        repository.addTrackToPlaylist(id, mediaId)
        _message.value = "Плейлист «${name.trim()}» создан, трек добавлен"
    }

    fun renamePlaylist(playlistId: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.renamePlaylist(playlistId, name)
            _message.value = "Плейлист переименован"
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            _message.value = "Плейлист удалён"
        }
    }

    fun addTrack(playlistId: Long, mediaId: Long) {
        viewModelScope.launch {
            when (repository.addTrackToPlaylist(playlistId, mediaId)) {
                AddToPlaylistResult.ADDED -> _message.value = "Трек добавлен в плейлист"
                AddToPlaylistResult.ALREADY_PRESENT ->
                    _message.value = "Этот трек уже есть в плейлисте"
            }
        }
    }

    fun createAndAdd(name: String, mediaId: Long) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.createPlaylist(name)
            repository.addTrackToPlaylist(id, mediaId)
            _message.value = "Плейлист «${name.trim()}» создан"
        }
    }

    fun removeTrack(playlistId: Long, mediaId: Long) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, mediaId)
            _message.value = "Трек удалён из плейлиста"
        }
    }

    suspend fun playlistsContaining(mediaId: Long): List<Long> =
        repository.playlistIdsContaining(mediaId)

    fun clearMessage() {
        _message.value = null
    }
}
