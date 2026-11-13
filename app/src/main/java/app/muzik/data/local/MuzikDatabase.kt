package app.muzik.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import app.muzik.data.local.entity.FavoriteEntity
import app.muzik.data.local.entity.PlaylistEntity
import app.muzik.data.local.entity.PlaylistTrackEntity
import app.muzik.data.local.entity.TrackEntity

@Database(
    entities = [
        TrackEntity::class,
        FavoriteEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class MuzikDatabase : RoomDatabase() {

    abstract fun musicDao(): MusicDao

    companion object {
        @Volatile
        private var instance: MuzikDatabase? = null

        fun getInstance(context: Context): MuzikDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MuzikDatabase::class.java,
                    "muzik.db",
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
