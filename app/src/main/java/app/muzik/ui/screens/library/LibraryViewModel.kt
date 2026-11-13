package app.muzik.ui.screens.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.muzik.data.repository.MusicRepository
import app.muzik.domain.Album
import app.muzik.domain.Artist
import app.muzik.domain.Playlist
import app.muzik.domain.Track
import app.muzik.muzikContainer
import app.muzik.util.hasAudioPermission
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val tracks: List<Track> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val favorites: List<Track> = emptyList(),
    val recent: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val isScanning: Boolean = false,
    val scanMessage: String? = null,
) {
    val isEmpty: Boolean get() = tracks.isEmpty()
}

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MusicRepository = application.muzikContainer.musicRepository

    val uiState: StateFlow<LibraryUiState> = combine(
        repository.tracks,
        repository.playlists,
        repository.isScanning,
        repository.scanMessage,
    ) { tracks, playlists, isScanning, scanMessage ->
        LibraryUiState(
            tracks = tracks,
            playlists = playlists,
            favorites = tracks.filter { it.isFavorite }.sortedBy { it.title.lowercase() },
            recent = tracks.sortedByDescending { it.dateAdded },
            albums = buildAlbums(tracks),
            artists = buildArtists(tracks),
            isScanning = isScanning,
            scanMessage = scanMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState(isScanning = true),
    )

    init {
        viewModelScope.launch {
            if (getApplication<Application>().hasAudioPermission()) {
                repository.refreshIfNeeded()
            }
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch { repository.refreshLibrary() }
    }

    fun toggleFavorite(mediaId: Long) {
        viewModelScope.launch { repository.toggleFavorite(mediaId) }
    }

    fun clearScanMessage() {
        repository.clearScanMessage()
    }

    private fun buildAlbums(tracks: List<Track>): List<Album> =
        tracks.groupBy { it.albumId to it.album }
            .map { (key, albumTracks) ->
                Album(
                    id = key.first,
                    title = key.second,
                    artist = albumTracks
                        .groupingBy { it.artist }
                        .eachCount()
                        .maxByOrNull { it.value }
                        ?.key
                        ?: "Неизвестный исполнитель",
                    artworkUri = albumTracks.firstNotNullOfOrNull { it.artworkUri },
                    tracks = albumTracks.sortedWith(
                        compareBy({ it.trackNumber }, { it.title.lowercase() }),
                    ),
                )
            }
            .sortedBy { it.title.lowercase() }

    private fun buildArtists(tracks: List<Track>): List<Artist> =
        tracks.groupBy { it.artist }
            .map { (name, artistTracks) ->
                Artist(
                    name = name,
                    trackCount = artistTracks.size,
                    artworkUri = artistTracks.firstNotNullOfOrNull { it.artworkUri },
                    albumCount = artistTracks.map { it.albumId }.distinct().size,
                )
            }
            .sortedBy { it.name.lowercase() }
}
