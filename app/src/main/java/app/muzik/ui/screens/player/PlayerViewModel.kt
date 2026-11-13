package app.muzik.ui.screens.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import app.muzik.domain.Track
import app.muzik.muzikContainer
import app.muzik.playback.PlayerUiState
import app.muzik.playback.PlaybackManager
import kotlinx.coroutines.flow.StateFlow

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val playbackManager: PlaybackManager =
        application.muzikContainer.playbackManager

    val state: StateFlow<PlayerUiState> = playbackManager.state

    fun playQueue(tracks: List<Track>, startIndex: Int = 0, shuffle: Boolean = false) {
        playbackManager.playQueue(tracks, startIndex, shuffle)
    }

    fun playTrack(track: Track, queue: List<Track>) {
        playbackManager.playTrack(track, queue)
    }

    fun togglePlayPause() = playbackManager.togglePlayPause()

    fun next() = playbackManager.next()

    fun previous() = playbackManager.previous()

    fun seekTo(positionMs: Long) = playbackManager.seekTo(positionMs)

    fun seekToQueueItem(index: Int) = playbackManager.seekToQueueItem(index)

    fun toggleShuffle() = playbackManager.toggleShuffle()

    fun cycleRepeatMode() = playbackManager.cycleRepeatMode()

    fun dismissError() = playbackManager.dismissError()
}
