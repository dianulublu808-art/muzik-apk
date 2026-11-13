package app.muzik.data.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import app.muzik.data.local.entity.TrackEntity

/**
 * Сканирует локальную медиатеку устройства через MediaStore.
 * Возвращает null, если запрос не удался (например, отозвано разрешение),
 * чтобы вызывающая сторона не удалила библиотеку ошибочно.
 */
class MediaStoreScanner(private val context: Context) {

    fun scan(): List<TrackEntity>? {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DISPLAY_NAME,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        return try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder,
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

                buildList {
                    while (cursor.moveToNext()) {
                        val mediaId = cursor.getLong(idColumn)
                        val durationMs = cursor.getLong(durationColumn)
                        if (mediaId <= 0L || durationMs < MIN_DURATION_MS) continue

                        val albumId = cursor.getLong(albumIdColumn)
                        val rawTitle = cursor.getString(titleColumn)
                        val displayName = cursor.getString(displayNameColumn)
                        val title = rawTitle?.takeIf { it.isNotBlank() }
                            ?: displayName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
                            ?: "Без названия"

                        add(
                            TrackEntity(
                                mediaId = mediaId,
                                title = title,
                                artist = normalizeArtist(cursor.getString(artistColumn)),
                                album = cursor.getString(albumColumn)
                                    ?.takeIf { it.isNotBlank() && it != "<unknown>" }
                                    ?: "Без альбома",
                                albumId = albumId,
                                durationMs = durationMs,
                                uri = ContentUris.withAppendedId(collection, mediaId).toString(),
                                artworkUri = artworkUri(albumId),
                                dateAdded = cursor.getLong(dateColumn) * 1000L,
                                trackNumber = cursor.getInt(trackColumn) % 1000,
                            ),
                        )
                    }
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Не удалось прочитать медиатеку", t)
            null
        }
    }

    private fun normalizeArtist(raw: String?): String =
        raw?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Неизвестный исполнитель"

    private fun artworkUri(albumId: Long): String? {
        if (albumId <= 0L) return null
        return ContentUris.withAppendedId(ALBUM_ART_URI, albumId).toString()
    }

    companion object {
        private const val TAG = "MediaStoreScanner"
        private const val MIN_DURATION_MS = 3_000L
        private val ALBUM_ART_URI: Uri = Uri.parse("content://media/external/audio/albumart")
    }
}
