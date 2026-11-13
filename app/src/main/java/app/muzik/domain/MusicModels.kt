package app.muzik.domain

import android.net.Uri

/**
 * Модель трека, с которой работает UI и плеер.
 * Данные приходят из MediaStore и хранятся в Room.
 */
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: Uri,
    val artworkUri: Uri?,
    val dateAdded: Long,
    val trackNumber: Int,
    val isFavorite: Boolean = false,
)

data class Playlist(
    val id: Long,
    val name: String,
    val trackCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val artworkUri: Uri?,
    val tracks: List<Track>,
) {
    val durationMs: Long get() = tracks.sumOf { it.durationMs }
}

data class Artist(
    val name: String,
    val trackCount: Int,
    val artworkUri: Uri?,
    val albumCount: Int,
)
