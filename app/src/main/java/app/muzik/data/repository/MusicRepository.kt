package app.muzik.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.room.withTransaction
import app.muzik.data.local.MusicDao
import app.muzik.data.local.MuzikDatabase
import app.muzik.data.local.entity.FavoriteEntity
import app.muzik.data.local.entity.PlaylistEntity
import app.muzik.data.local.entity.PlaylistTrackEntity
import app.muzik.data.local.entity.TrackEntity
import app.muzik.data.media.MediaStoreScanner
import app.muzik.data.settings.SettingsRepository
import app.muzik.domain.Playlist
import app.muzik.domain.Track
import app.muzik.util.hasAudioPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface ScanOutcome {
    data class Success(val trackCount: Int) : ScanOutcome
    data object NoPermission : ScanOutcome
    data object AlreadyRunning : ScanOutcome
    data object Failed : ScanOutcome
}

enum class AddToPlaylistResult { ADDED, ALREADY_PRESENT }

class MusicRepository(
    private val context: Context,
    private val database: MuzikDatabase,
    private val dao: MusicDao,
    private val scanner: MediaStoreScanner,
    private val settingsRepository: SettingsRepository,
) {

    private val scanMutex = Mutex()
    private var lastScanAt = 0L

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanMessage = MutableStateFlow<String?>(null)
    val scanMessage: StateFlow<String?> = _scanMessage.asStateFlow()

    val tracks: Flow<List<Track>> = combine(
        dao.observeTracks(),
        dao.observeFavoriteIds(),
    ) { entities, favoriteIds ->
        val favorites = favoriteIds.toHashSet()
        entities.map { it.toDomain(favorites.contains(it.mediaId)) }
    }
        .distinctUntilChanged()

    val trackCount: Flow<Int> = dao.observeTrackCount()

    val playlists: Flow<List<Playlist>> = dao.observePlaylists()
        .map { rows ->
            rows.map { row ->
                Playlist(
                    id = row.id,
                    name = row.name,
                    trackCount = row.trackCount,
                    createdAt = row.createdAt,
                    updatedAt = row.updatedAt,
                )
            }
        }
        .distinctUntilChanged()

    fun search(query: String): Flow<List<Track>> {
        val pattern = "%${escapeLike(query.trim())}%"
        return combine(
            dao.searchTracks(pattern),
            dao.observeFavoriteIds(),
        ) { entities, favoriteIds ->
            val favorites = favoriteIds.toHashSet()
            entities.map { it.toDomain(favorites.contains(it.mediaId)) }
        }.distinctUntilChanged()
    }

    fun observePlaylist(playlistId: Long): Flow<Playlist?> =
        dao.observePlaylist(playlistId).map { entity ->
            entity?.let {
                Playlist(
                    id = it.id,
                    name = it.name,
                    trackCount = 0,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                )
            }
        }

    fun observePlaylistTracks(playlistId: Long): Flow<List<Track>> = combine(
        dao.observePlaylistTracks(playlistId),
        dao.observeFavoriteIds(),
    ) { entities, favoriteIds ->
        val favorites = favoriteIds.toHashSet()
        entities.map { it.toDomain(favorites.contains(it.mediaId)) }
    }.distinctUntilChanged()

    suspend fun toggleFavorite(mediaId: Long) = withContext(Dispatchers.IO) {
        if (dao.isFavorite(mediaId)) {
            dao.removeFavorite(mediaId)
        } else {
            dao.addFavorite(FavoriteEntity(mediaId, System.currentTimeMillis()))
        }
    }

    suspend fun refreshIfNeeded() {
        if (lastScanAt == 0L) {
            refreshLibrary()
        }
    }

    suspend fun refreshLibrary(): ScanOutcome {
        if (!context.hasAudioPermission()) {
            _scanMessage.value = "Нет доступа к локальной медиатеке"
            return ScanOutcome.NoPermission
        }
        return scanMutex.withLock {
            if (_isScanning.value) return@withLock ScanOutcome.AlreadyRunning
            _isScanning.value = true
            try {
                val scanned = withContext(Dispatchers.IO) { scanner.scan() }
                    ?: return@withLock ScanOutcome.Failed.also {
                        _scanMessage.value = "Не удалось прочитать медиатеку"
                    }

                withContext(Dispatchers.IO) {
                    database.withTransaction {
                        if (scanned.isEmpty()) {
                            dao.deleteAllTracks()
                        } else {
                            dao.upsertTracks(scanned)
                            val currentIds = dao.getAllMediaIds().toHashSet()
                            val scannedIds = scanned.mapTo(HashSet()) { it.mediaId }
                            val staleIds = currentIds.filterNot { it in scannedIds }
                            staleIds.chunked(DELETE_CHUNK_SIZE).forEach { chunk ->
                                dao.deleteTracks(chunk)
                            }
                        }
                    }
                }

                lastScanAt = System.currentTimeMillis()
                settingsRepository.setLastScanAt(lastScanAt)
                _scanMessage.value = if (scanned.isEmpty()) {
                    "В медиатеке не найдено музыки"
                } else {
                    "Медиатека обновлена: ${scanned.size} треков"
                }
                ScanOutcome.Success(scanned.size)
            } catch (t: Throwable) {
                Log.e(TAG, "Ошибка обновления медиатеки", t)
                _scanMessage.value = "Ошибка обновления медиатеки"
                ScanOutcome.Failed
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun clearScanMessage() {
        _scanMessage.value = null
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        dao.insertPlaylist(
            PlaylistEntity(
                name = name.trim(),
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun renamePlaylist(playlistId: Long, name: String) = withContext(Dispatchers.IO) {
        dao.renamePlaylist(playlistId, name.trim(), System.currentTimeMillis())
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        dao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, mediaId: Long): AddToPlaylistResult =
        withContext(Dispatchers.IO) {
            database.withTransaction {
                if (dao.isTrackInPlaylist(playlistId, mediaId)) {
                    return@withTransaction AddToPlaylistResult.ALREADY_PRESENT
                }
                val position = dao.nextPosition(playlistId)
                dao.insertPlaylistTrack(
                    PlaylistTrackEntity(
                        playlistId = playlistId,
                        mediaId = mediaId,
                        position = position,
                        addedAt = System.currentTimeMillis(),
                    ),
                )
                dao.touchPlaylist(playlistId, System.currentTimeMillis())
                AddToPlaylistResult.ADDED
            }
        }

    suspend fun removeTrackFromPlaylist(playlistId: Long, mediaId: Long) =
        withContext(Dispatchers.IO) {
            database.withTransaction {
                dao.removePlaylistTrack(playlistId, mediaId)
                dao.touchPlaylist(playlistId, System.currentTimeMillis())
            }
        }

    suspend fun playlistIdsContaining(mediaId: Long): List<Long> = withContext(Dispatchers.IO) {
        dao.playlistIdsContaining(mediaId)
    }

    suspend fun trackById(mediaId: Long): Track? = withContext(Dispatchers.IO) {
        val entity = dao.getTrack(mediaId) ?: return@withContext null
        val favorite = dao.isFavorite(mediaId)
        entity.toDomain(favorite)
    }

    private fun TrackEntity.toDomain(isFavorite: Boolean): Track = Track(
        id = mediaId,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        durationMs = durationMs,
        uri = Uri.parse(uri),
        artworkUri = artworkUri?.let(Uri::parse),
        dateAdded = dateAdded,
        trackNumber = trackNumber,
        isFavorite = isFavorite,
    )

    private fun escapeLike(value: String): String =
        value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    companion object {
        private const val TAG = "MusicRepository"
        private const val DELETE_CHUNK_SIZE = 400
    }
}
