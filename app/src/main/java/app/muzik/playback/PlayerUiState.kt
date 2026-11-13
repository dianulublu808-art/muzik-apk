package app.muzik.playback

data class NowPlaying(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUri: String?,
    val durationMs: Long,
)

data class PlayerUiState(
    val isConnected: Boolean = false,
    val currentTrack: NowPlaying? = null,
    val queue: List<NowPlaying> = emptyList(),
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = 0,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val error: String? = null,
) {
    val progress: Float
        get() = if (durationMs > 0L) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}
