package app.muzik.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.muzik.AppContainer
import app.muzik.playback.NowPlaying
import app.muzik.ui.components.AddToPlaylistDialog
import app.muzik.ui.components.AuroraBackground
import app.muzik.ui.components.MiniPlayer
import app.muzik.ui.components.MuzikBottomBar
import app.muzik.ui.screens.collection.AlbumScreen
import app.muzik.ui.screens.collection.ArtistScreen
import app.muzik.ui.screens.collection.FavoritesScreen
import app.muzik.ui.screens.home.HomeScreen
import app.muzik.ui.screens.library.LibraryScreen
import app.muzik.ui.screens.library.LibraryViewModel
import app.muzik.ui.screens.player.PlayerScreen
import app.muzik.ui.screens.player.PlayerViewModel
import app.muzik.ui.screens.playlist.PlaylistDetailScreen
import app.muzik.ui.screens.playlist.PlaylistViewModel
import app.muzik.ui.screens.search.SearchScreen
import app.muzik.ui.screens.search.SearchViewModel
import app.muzik.ui.screens.settings.SettingsScreen
import app.muzik.ui.screens.settings.SettingsViewModel
import app.muzik.ui.theme.MuzikTheme
import app.muzik.util.hasAudioPermission
import app.muzik.util.requiredAudioPermission

private data class AddToPlaylistTarget(val id: Long, val title: String)

@Composable
fun MuzikApp(container: AppContainer) {
    val libraryViewModel: LibraryViewModel = viewModel()
    val searchViewModel: SearchViewModel = viewModel()
    val playlistViewModel: PlaylistViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val playerViewModel: PlayerViewModel = viewModel()

    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by searchViewModel.query.collectAsStateWithLifecycle()
    val searchResults by searchViewModel.results.collectAsStateWithLifecycle()
    val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()
    val selectedPlaylist by playlistViewModel.selectedPlaylist.collectAsStateWithLifecycle()
    val playlistMessage by playlistViewModel.message.collectAsStateWithLifecycle()
    val playerState by playerViewModel.state.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    var hasAudioAccess by remember { mutableStateOf(context.hasAudioPermission()) }
    var addTarget by remember { mutableStateOf<AddToPlaylistTarget?>(null) }

    val favoriteIds = remember(libraryState.favorites) {
        libraryState.favorites.mapTo(HashSet()) { it.id }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        hasAudioAccess = context.hasAudioPermission()
        if (hasAudioAccess) {
            libraryViewModel.refreshLibrary()
        }
    }

    val requestPermissions: () -> Unit = {
        val permissions = buildList {
            add(requiredAudioPermission())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    val openAppSettings: () -> Unit = {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasAudioAccess = context.hasAudioPermission()
    }

    LaunchedEffect(libraryState.scanMessage) {
        val message = libraryState.scanMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        libraryViewModel.clearScanMessage()
    }

    LaunchedEffect(playlistMessage) {
        val message = playlistMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        playlistViewModel.clearMessage()
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route.orEmpty()
    val isPlayerRoute = currentRoute == "player"

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openPlaylist(playlistId: Long) {
        navController.navigate("playlist/$playlistId") { launchSingleTop = true }
    }

    fun openAlbum(albumId: Long) {
        navController.navigate("album/$albumId") { launchSingleTop = true }
    }

    fun openArtist(artistName: String) {
        navController.navigate("artist/${Uri.encode(artistName)}") { launchSingleTop = true }
    }

    MuzikTheme(settings = settings) {
        AuroraBackground(animated = settings.animatedBackground) {
            Scaffold(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!isPlayerRoute) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .navigationBarsPadding()
                                .padding(bottom = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (playerState.currentTrack != null) {
                                MiniPlayer(
                                    state = playerState,
                                    onOpen = {
                                        navController.navigate("player") { launchSingleTop = true }
                                    },
                                    onTogglePlay = playerViewModel::togglePlayPause,
                                    onNext = playerViewModel::next,
                                )
                            }
                            MuzikBottomBar(
                                currentRoute = currentRoute,
                                onNavigate = { route -> navigateToTab(route) },
                            )
                        }
                    }
                },
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.padding(innerPadding),
                ) {
                    composable("home") {
                        HomeScreen(
                            state = libraryState,
                            hasAudioPermission = hasAudioAccess,
                            onRequestPermission = requestPermissions,
                            onOpenAppSettings = openAppSettings,
                            onRefresh = libraryViewModel::refreshLibrary,
                            onToggleFavorite = { track ->
                                libraryViewModel.toggleFavorite(track.id)
                            },
                            onPlayTrack = { track, queue ->
                                playerViewModel.playTrack(track, queue)
                            },
                            onPlayAll = { tracks, shuffle ->
                                playerViewModel.playQueue(tracks, shuffle = shuffle)
                            },
                            onOpenPlaylist = { playlistId -> openPlaylist(playlistId) },
                            onOpenPlayer = {
                                navController.navigate("player") { launchSingleTop = true }
                            },
                            onOpenFavorites = {
                                navController.navigate("favorites") { launchSingleTop = true }
                            },
                            onOpenLibrary = { navigateToTab("library") },
                        )
                    }

                    composable("search") {
                        SearchScreen(
                            query = searchQuery,
                            results = searchResults,
                            onQueryChange = searchViewModel::onQueryChange,
                            onClearQuery = searchViewModel::clearQuery,
                            onPlayTrack = { track, queue ->
                                playerViewModel.playTrack(track, queue)
                            },
                            onToggleFavorite = { track ->
                                libraryViewModel.toggleFavorite(track.id)
                            },
                            onAddToPlaylist = { track ->
                                addTarget = AddToPlaylistTarget(track.id, track.title)
                            },
                        )
                    }

                    composable("library") {
                        LibraryScreen(
                            state = libraryState,
                            hasAudioPermission = hasAudioAccess,
                            onRequestPermission = requestPermissions,
                            onRefresh = libraryViewModel::refreshLibrary,
                            onToggleFavorite = { track ->
                                libraryViewModel.toggleFavorite(track.id)
                            },
                            onPlayTrack = { track, queue ->
                                playerViewModel.playTrack(track, queue)
                            },
                            onPlayAll = { tracks, shuffle ->
                                playerViewModel.playQueue(tracks, shuffle = shuffle)
                            },
                            onAddToPlaylist = { track ->
                                addTarget = AddToPlaylistTarget(track.id, track.title)
                            },
                            onOpenPlaylist = { playlistId -> openPlaylist(playlistId) },
                            onOpenAlbum = { albumId -> openAlbum(albumId) },
                            onOpenArtist = { artistName -> openArtist(artistName) },
                            onCreatePlaylist = { name ->
                                playlistViewModel.createPlaylist(name) { }
                            },
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            settings = settings,
                            trackCount = libraryState.tracks.size,
                            isScanning = libraryState.isScanning,
                            hasAudioPermission = hasAudioAccess,
                            onRequestPermission = requestPermissions,
                            onOpenAppSettings = openAppSettings,
                            onRefresh = libraryViewModel::refreshLibrary,
                            onThemeMode = settingsViewModel::setThemeMode,
                            onAccent = settingsViewModel::setAccent,
                            onDynamicColor = settingsViewModel::setDynamicColor,
                            onAnimatedBackground = settingsViewModel::setAnimatedBackground,
                        )
                    }

                    composable("favorites") {
                        FavoritesScreen(
                            state = libraryState,
                            onBack = { navController.popBackStack() },
                            onPlayAll = { tracks, shuffle ->
                                playerViewModel.playQueue(tracks, shuffle = shuffle)
                            },
                            onPlayTrack = { track, queue ->
                                playerViewModel.playTrack(track, queue)
                            },
                            onToggleFavorite = { track ->
                                libraryViewModel.toggleFavorite(track.id)
                            },
                            onAddToPlaylist = { track ->
                                addTarget = AddToPlaylistTarget(track.id, track.title)
                            },
                        )
                    }

                    composable(
                        route = "album/{albumId}",
                        arguments = listOf(navArgument("albumId") { type = NavType.LongType }),
                    ) { entry ->
                        val albumId = entry.arguments?.getLong("albumId") ?: 0L
                        AlbumScreen(
                            albumId = albumId,
                            state = libraryState,
                            onBack = { navController.popBackStack() },
                            onPlayAll = { tracks, shuffle ->
                                playerViewModel.playQueue(tracks, shuffle = shuffle)
                            },
                            onPlayTrack = { track, queue ->
                                playerViewModel.playTrack(track, queue)
                            },
                            onToggleFavorite = { track ->
                                libraryViewModel.toggleFavorite(track.id)
                            },
                            onAddToPlaylist = { track ->
                                addTarget = AddToPlaylistTarget(track.id, track.title)
                            },
                        )
                    }

                    composable(
                        route = "artist/{artistName}",
                        arguments = listOf(navArgument("artistName") { type = NavType.StringType }),
                    ) { entry ->
                        val artistName = entry.arguments?.getString("artistName").orEmpty()
                        ArtistScreen(
                            artistName = artistName,
                            state = libraryState,
                            onBack = { navController.popBackStack() },
                            onPlayAll = { tracks, shuffle ->
                                playerViewModel.playQueue(tracks, shuffle = shuffle)
                            },
                            onPlayTrack = { track, queue ->
                                playerViewModel.playTrack(track, queue)
                            },
                            onToggleFavorite = { track ->
                                libraryViewModel.toggleFavorite(track.id)
                            },
                            onAddToPlaylist = { track ->
                                addTarget = AddToPlaylistTarget(track.id, track.title)
                            },
                        )
                    }

                    composable(
                        route = "playlist/{playlistId}",
                        arguments = listOf(navArgument("playlistId") { type = NavType.LongType }),
                    ) { entry ->
                        val playlistId = entry.arguments?.getLong("playlistId") ?: 0L
                        LaunchedEffect(playlistId) {
                            playlistViewModel.selectPlaylist(playlistId)
                        }
                        PlaylistDetailScreen(
                            detail = selectedPlaylist,
                            onBack = { navController.popBackStack() },
                            onPlayAll = { tracks, shuffle ->
                                playerViewModel.playQueue(tracks, shuffle = shuffle)
                            },
                            onPlayTrack = { track, queue ->
                                playerViewModel.playTrack(track, queue)
                            },
                            onToggleFavorite = { track ->
                                libraryViewModel.toggleFavorite(track.id)
                            },
                            onAddToPlaylist = { track ->
                                addTarget = AddToPlaylistTarget(track.id, track.title)
                            },
                            onRemoveTrack = { mediaId ->
                                playlistViewModel.removeTrack(playlistId, mediaId)
                            },
                            onRename = { name ->
                                playlistViewModel.renamePlaylist(playlistId, name)
                            },
                            onDelete = {
                                playlistViewModel.deletePlaylist(playlistId)
                                navController.popBackStack()
                            },
                        )
                    }

                    composable("player") {
                        PlayerScreen(
                            state = playerState,
                            favoriteIds = favoriteIds,
                            onBack = { navController.popBackStack() },
                            onTogglePlay = playerViewModel::togglePlayPause,
                            onNext = playerViewModel::next,
                            onPrevious = playerViewModel::previous,
                            onSeek = playerViewModel::seekTo,
                            onToggleShuffle = playerViewModel::toggleShuffle,
                            onCycleRepeat = playerViewModel::cycleRepeatMode,
                            onToggleFavorite = { mediaId ->
                                libraryViewModel.toggleFavorite(mediaId)
                            },
                            onAddToPlaylist = { nowPlaying: NowPlaying ->
                                addTarget = AddToPlaylistTarget(nowPlaying.id, nowPlaying.title)
                            },
                            onSelectQueueItem = playerViewModel::seekToQueueItem,
                            onDismissError = playerViewModel::dismissError,
                        )
                    }
                }
            }
        }

        val target = addTarget
        if (target != null) {
            AddToPlaylistDialog(
                trackId = target.id,
                trackTitle = target.title,
                playlists = playlists,
                onDismiss = { addTarget = null },
                loadContainedIds = { mediaId -> playlistViewModel.playlistsContaining(mediaId) },
                onAdd = { playlistId ->
                    playlistViewModel.addTrackNow(playlistId, target.id)
                },
                onCreateAndAdd = { name ->
                    playlistViewModel.createAndAddNow(name, target.id)
                },
            )
        }
    }
}
