package app.muzik.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import app.muzik.data.local.entity.FavoriteEntity
import app.muzik.data.local.entity.PlaylistEntity
import app.muzik.data.local.entity.PlaylistSummaryRow
import app.muzik.data.local.entity.PlaylistTrackEntity
import app.muzik.data.local.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {

    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE ASC")
    fun observeTracks(): Flow<List<TrackEntity>>

    @Query(
        "SELECT * FROM tracks WHERE title LIKE :pattern ESCAPE '\\' " +
            "OR artist LIKE :pattern ESCAPE '\\' OR album LIKE :pattern ESCAPE '\\' " +
            "ORDER BY title COLLATE NOCASE ASC",
    )
    fun searchTracks(pattern: String): Flow<List<TrackEntity>>

    @Query("SELECT mediaId FROM tracks")
    suspend fun getAllMediaIds(): List<Long>

    @Query("SELECT * FROM tracks WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getTrack(mediaId: Long): TrackEntity?

    @Upsert
    suspend fun upsertTracks(tracks: List<TrackEntity>)

    @Query("DELETE FROM tracks WHERE mediaId IN (:mediaIds)")
    suspend fun deleteTracks(mediaIds: List<Long>)

    @Query("DELETE FROM tracks")
    suspend fun deleteAllTracks()

    @Query("SELECT mediaId FROM favorites")
    fun observeFavoriteIds(): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaId = :mediaId)")
    suspend fun isFavorite(mediaId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mediaId = :mediaId")
    suspend fun removeFavorite(mediaId: Long)

    @Query(
        "SELECT p.id AS id, p.name AS name, p.createdAt AS createdAt, p.updatedAt AS updatedAt, " +
            "(SELECT COUNT(*) FROM playlist_tracks pt WHERE pt.playlistId = p.id) AS trackCount " +
            "FROM playlists p ORDER BY p.updatedAt DESC",
    )
    fun observePlaylists(): Flow<List<PlaylistSummaryRow>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId LIMIT 1")
    fun observePlaylist(playlistId: Long): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists WHERE id = :playlistId LIMIT 1")
    suspend fun getPlaylist(playlistId: Long): PlaylistEntity?

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name, updatedAt = :now WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, name: String, now: Long)

    @Query("UPDATE playlists SET updatedAt = :now WHERE id = :playlistId")
    suspend fun touchPlaylist(playlistId: Long, now: Long)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query(
        "SELECT t.* FROM tracks t INNER JOIN playlist_tracks pt ON t.mediaId = pt.mediaId " +
            "WHERE pt.playlistId = :playlistId ORDER BY pt.position ASC",
    )
    fun observePlaylistTracks(playlistId: Long): Flow<List<TrackEntity>>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun nextPosition(playlistId: Long): Int

    @Query("SELECT EXISTS(SELECT 1 FROM playlist_tracks WHERE playlistId = :playlistId AND mediaId = :mediaId)")
    suspend fun isTrackInPlaylist(playlistId: Long, mediaId: Long): Boolean

    @Query("SELECT playlistId FROM playlist_tracks WHERE mediaId = :mediaId")
    suspend fun playlistIdsContaining(mediaId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistTrack(entry: PlaylistTrackEntity): Long

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND mediaId = :mediaId")
    suspend fun removePlaylistTrack(playlistId: Long, mediaId: Long)

    @Query("SELECT COUNT(*) FROM tracks")
    fun observeTrackCount(): Flow<Int>
}
