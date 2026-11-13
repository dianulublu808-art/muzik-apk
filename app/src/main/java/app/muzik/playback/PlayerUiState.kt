package app.muzik.playback

data class NowPlaying(
    val id: Long,
    val uid: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUri: String?,
    val durationMs: Long,
    val sourceLabel: String? = null,
    val isRemote: Boolean = false,
) {
    /** Стабильный ключ для градиента-заглушки обложки. */
    val artworkSeed: Long
        get() = if (isRemote) uid.hashCode().toLong() else id
}

data class PlayerUiState(
    val isConnected: Boolean = false,
    val currentTrack: NowPlaying? = null,
    val queue: List<NowPlaying> = emptyList(),
    val currentIndex: Int = 0,
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
