package app.muzik

import android.app.Application
import android.content.Context
import app.muzik.data.local.MuzikDatabase
import app.muzik.data.media.MediaStoreScanner
import app.muzik.data.repository.MusicRepository
import app.muzik.data.settings.SettingsRepository
import app.muzik.playback.PlaybackManager

class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val database = MuzikDatabase.getInstance(appContext)

    val settingsRepository = SettingsRepository(appContext)

    val musicRepository = MusicRepository(
        context = appContext,
        database = database,
        dao = database.musicDao(),
        scanner = MediaStoreScanner(appContext),
        settingsRepository = settingsRepository,
    )

    val playbackManager: PlaybackManager by lazy { PlaybackManager(appContext) }
}

class MuzikApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Application.muzikContainer: AppContainer
    get() = (this as MuzikApplication).container
