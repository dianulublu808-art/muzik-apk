package app.muzik.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import app.muzik.data.remote.WaveSource
import app.muzik.data.remote.WaveTrack
import app.muzik.domain.Track
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Единая точка управления Media3-сессией.
 * Живёт на уровне Application, поэтому воспроизведение не прерывается
 * при пересоздании Activity или переходе между экранами.
 */
class PlaybackManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var pendingAction: ((MediaController) -> Unit)? = null
    private var ticker: Job? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            updateState(player)
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.update {
                it.copy(error = error.errorCodeName.ifBlank { "Ошибка воспроизведения" })
            }
        }
    }

    init {
        connect()
        ticker = scope.launch {
            while (isActive) {
                controller?.let { player ->
                    if (player.isPlaying) {
                        updateProgress(player)
                    }
                }
                delay(PROGRESS_UPDATE_MS)
            }
        }
    }

    private fun connect() {
        if (controller != null || controllerFuture != null) return
        val token = SessionToken(
            context,
            ComponentName(context, MuzikPlaybackService::class.java),
        )
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                try {
                    val connected = future.get()
                    controller = connected
                    connected.addListener(listener)
                    updateState(connected)
                    pendingAction?.invoke(connected)
                    pendingAction = null
                } catch (t: Throwable) {
                    _state.update {
                        it.copy(
                            isConnected = false,
                            error = "Не удалось подключиться к плееру",
                        )
                    }
                }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    fun playQueue(tracks: List<Track>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (tracks.isEmpty()) return
        val mediaItems = tracks.map { it.toMediaItem() }
        val safeIndex = startIndex.coerceIn(0, mediaItems.lastIndex)
        _state.update { it.copy(error = null) }
        withController { player ->
            player.shuffleModeEnabled = shuffle
            player.setMediaItems(mediaItems, safeIndex, 0L)
            player.prepare()
            player.play()
        }
    }

    fun playTrack(track: Track, queue: List<Track>) {
        val actualQueue = if (queue.any { it.id == track.id }) queue else listOf(track)
        val index = actualQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playQueue(actualQueue, index, shuffle = false)
    }

    /** Запускает «Волну»: очередь из локальных и сетевых треков. */
    fun playWave(tracks: List<WaveTrack>, startIndex: Int = 0) {
        val playable = tracks.filter { it.isPlayable }
        if (playable.isEmpty()) return
        val mediaItems = playable.map { it.toMediaItem() }
        val safeIndex = startIndex.coerceIn(0, mediaItems.lastIndex)
        _state.update { it.copy(error = null) }
        withController { player ->
            player.shuffleModeEnabled = false
            player.setMediaItems(mediaItems, safeIndex, 0L)
            player.prepare()
            player.play()
        }
    }

    /** Добавляет в конец очереди очередную порцию «Волны». */
    fun appendWave(tracks: List<WaveTrack>) {
        val items = tracks.filter { it.isPlayable }.map { it.toMediaItem() }
        if (items.isEmpty()) return
        withController { player ->
            player.addMediaItems(items)
            updateState(player)
        }
    }

    fun togglePlayPause() {
        _state.update { it.copy(error = null) }
        withController { player ->
            if (player.isPlaying) player.pause() else player.play()
        }
    }

    fun next() {
        withController { player ->
            if (player.hasNextMediaItem()) player.seekToNextMediaItem()
        }
    }

    fun previous() {
        withController { player ->
            if (player.currentPosition > RESTART_THRESHOLD_MS) {
                player.seekTo(0L)
            } else if (player.hasPreviousMediaItem()) {
                player.seekToPreviousMediaItem()
            } else {
                player.seekTo(0L)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        withController { player ->
            val duration = player.duration
            val target = if (duration > 0L) {
                positionMs.coerceIn(0L, duration)
            } else {
                positionMs.coerceAtLeast(0L)
            }
            player.seekTo(target)
        }
    }

    fun seekToQueueItem(index: Int) {
        withController { player ->
            if (index in 0 until player.mediaItemCount) {
                player.seekToDefaultPosition(index)
                player.play()
            }
        }
    }

    fun toggleShuffle() {
        withController { player ->
            player.shuffleModeEnabled = !player.shuffleModeEnabled
        }
    }

    fun cycleRepeatMode() {
        withController { player ->
            player.repeatMode = when (player.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private fun withController(action: (MediaController) -> Unit) {
        val current = controller
        if (current != null) {
            action(current)
            updateState(current)
        } else {
            pendingAction = action
            connect()
        }
    }

    private fun updateState(player: Player) {
        val currentItem = player.currentMediaItem
        val currentTrack = currentItem?.toNowPlaying()
        val queue = buildList {
            for (index in 0 until player.mediaItemCount) {
                player.getMediaItemAt(index).toNowPlaying()?.let(::add)
            }
        }
        _state.update { previous ->
            previous.copy(
                isConnected = true,
                currentTrack = currentTrack,
                queue = queue,
                currentIndex = player.currentMediaItemIndex.coerceAtLeast(0),
                isPlaying = player.isPlaying,
                isBuffering = player.playbackState == Player.STATE_BUFFERING,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = player.duration
                    .takeIf { it > 0L }
                    ?: currentTrack?.durationMs
                    ?: 0L,
                bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L),
                shuffleEnabled = player.shuffleModeEnabled,
                repeatMode = player.repeatMode,
                hasNext = player.hasNextMediaItem(),
                hasPrevious = player.hasPreviousMediaItem(),
            )
        }
    }

    private fun updateProgress(player: Player) {
        val duration = player.duration.takeIf { it > 0L } ?: _state.value.durationMs
        _state.update {
            it.copy(
                isPlaying = player.isPlaying,
                isBuffering = player.playbackState == Player.STATE_BUFFERING,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = duration,
                bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L),
                hasNext = player.hasNextMediaItem(),
                hasPrevious = player.hasPreviousMediaItem(),
            )
        }
    }

    private fun Track.toMediaItem(): MediaItem {
        val extras = Bundle().apply { putLong(KEY_DURATION_MS, durationMs) }
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(artworkUri)
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .setExtras(extras)
            .build()
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun WaveTrack.toMediaItem(): MediaItem {
        val extras = Bundle().apply {
            putLong(KEY_DURATION_MS, durationMs)
            putString(KEY_SOURCE_LABEL, source.label)
            putBoolean(KEY_REMOTE, source != WaveSource.LOCAL)
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(displayArtist)
            .setArtworkUri(artworkUrl?.let(Uri::parse))
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .setExtras(extras)
            .build()
        return MediaItem.Builder()
            .setMediaId(uid)
            .setUri(streamUrl)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun MediaItem.toNowPlaying(): NowPlaying? {
        val metadata = mediaMetadata
        if (mediaId.isBlank() && metadata.title == null) return null
        val extras = metadata.extras
        val remote = extras?.getBoolean(KEY_REMOTE) ?: mediaId.contains(':')
        return NowPlaying(
            id = if (remote) 0L else mediaId.toLongOrNull() ?: 0L,
            uid = mediaId,
            title = metadata.title?.toString()?.takeIf { it.isNotBlank() } ?: "Без названия",
            artist = metadata.artist?.toString().orEmpty(),
            album = metadata.albumTitle?.toString().orEmpty(),
            artworkUri = metadata.artworkUri?.toString(),
            durationMs = extras?.getLong(KEY_DURATION_MS) ?: 0L,
            sourceLabel = extras?.getString(KEY_SOURCE_LABEL),
            isRemote = remote,
        )
    }

    companion object {
        private const val KEY_DURATION_MS = "muzik_duration_ms"
        private const val KEY_SOURCE_LABEL = "muzik_source_label"
        private const val KEY_REMOTE = "muzik_remote"
        private const val PROGRESS_UPDATE_MS = 500L
        private const val RESTART_THRESHOLD_MS = 3_000L
    }
}
