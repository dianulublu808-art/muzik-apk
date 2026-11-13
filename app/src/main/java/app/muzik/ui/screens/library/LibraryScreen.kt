package app.muzik.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.muzik.domain.Album
import app.muzik.domain.Artist
import app.muzik.domain.Playlist
import app.muzik.domain.Track
import app.muzik.ui.components.AccentButton
import app.muzik.ui.components.ArtistAvatar
import app.muzik.ui.components.Artwork
import app.muzik.ui.components.CreatePlaylistDialog
import app.muzik.ui.components.EmptyState
import app.muzik.ui.components.GlassCard
import app.muzik.ui.components.GlassIconButton
import app.muzik.ui.components.GlassPill
import app.muzik.ui.components.InlineProgress
import app.muzik.ui.components.PlaylistIcon
import app.muzik.ui.components.ScreenHeader
import app.muzik.ui.components.TrackListHeader
import app.muzik.ui.components.TrackRow
import app.muzik.ui.theme.LocalMuzikColors
import app.muzik.util.formatTotalDuration
import app.muzik.util.trackCountLabel

private enum class LibraryTab(val label: String) {
    TRACKS("Треки"),
    ALBUMS("Альбомы"),
    ARTISTS("Исполнители"),
    PLAYLISTS("Плейлисты"),
    FAVORITES("Избранное"),
}

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onPlayAll: (List<Track>, Boolean) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenArtist: (String) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMuzikColors.current
    var selectedTab by rememberSaveable { mutableStateOf(LibraryTab.TRACKS) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        ScreenHeader(
            title = "Медиатека",
            subtitle = "${trackCountLabel(state.tracks.size)} • ${formatTotalDuration(state.tracks.sumOf { it.durationMs })}",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            trailing = {
                GlassIconButton(
                    icon = Icons.Rounded.Refresh,
                    contentDescription = "Обновить медиатеку",
                    onClick = onRefresh,
                    enabled = !state.isScanning,
                )
            },
        )

        if (!hasAudioPermission) {
            PermissionHint(
                onRequestPermission = onRequestPermission,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }

        if (state.isScanning) {
            InlineProgress(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(LibraryTab.entries.toList()) { tab ->
                GlassPill(
                    text = tab.label,
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                )
            }
        }

        when (selectedTab) {
            LibraryTab.TRACKS -> TracksTab(
                tracks = state.tracks,
                onPlayTrack = onPlayTrack,
                onPlayAll = onPlayAll,
                onToggleFavorite = onToggleFavorite,
                onAddToPlaylist = onAddToPlaylist,
                onRefresh = onRefresh,
            )

            LibraryTab.ALBUMS -> AlbumsTab(
                albums = state.albums,
                onOpenAlbum = onOpenAlbum,
                onPlayAll = onPlayAll,
            )

            LibraryTab.ARTISTS -> ArtistsTab(
                artists = state.artists,
                tracks = state.tracks,
                onOpenArtist = onOpenArtist,
                onPlayAll = onPlayAll,
            )

            LibraryTab.PLAYLISTS -> PlaylistsTab(
                playlists = state.playlists,
                onOpenPlaylist = onOpenPlaylist,
                onCreatePlaylist = { showCreateDialog = true },
            )

            LibraryTab.FAVORITES -> FavoritesTab(
                favorites = state.favorites,
                onPlayTrack = onPlayTrack,
                onPlayAll = onPlayAll,
                onToggleFavorite = onToggleFavorite,
                onAddToPlaylist = onAddToPlaylist,
            )
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                showCreateDialog = false
                onCreatePlaylist(name)
            },
        )
    }
}

@Composable
private fun PermissionHint(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMuzikColors.current
    GlassCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(
                    text = "Нет доступа к медиатеке",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                Text(
                    text = "Разрешите доступ к аудиофайлам устройства.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            AccentButton(text = "Разрешить", onClick = onRequestPermission)
        }
    }
}

@Composable
private fun TracksTab(
    tracks: List<Track>,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onPlayAll: (List<Track>, Boolean) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onRefresh: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (tracks.isNotEmpty()) {
            item {
                TrackListHeader(
                    countLabel = trackCountLabel(tracks.size),
                    onPlayAll = { onPlayAll(tracks, false) },
                    onShuffle = { onPlayAll(tracks, true) },
                )
            }
            items(tracks, key = { it.id }) { track ->
                TrackRow(
                    track = track,
                    onClick = { onPlayTrack(track, tracks) },
                    onToggleFavorite = { onToggleFavorite(track) },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                )
            }
        } else {
            item {
                EmptyState(
                    icon = Icons.Rounded.LibraryMusic,
                    title = "В медиатеке пусто",
                    message = "Добавьте музыку на устройство и обновите медиатеку.",
                    actionText = "Обновить",
                    onAction = onRefresh,
                )
            }
        }
    }
}

@Composable
private fun AlbumsTab(
    albums: List<Album>,
    onOpenAlbum: (Long) -> Unit,
    onPlayAll: (List<Track>, Boolean) -> Unit,
) {
    val colors = LocalMuzikColors.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (albums.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.Album,
                    title = "Альбомов нет",
                    message = "Альбомы появятся после сканирования локальной медиатеки.",
                )
            }
        } else {
            items(albums, key = { "${it.id}-${it.title}" }) { album ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.rowBackground)
                        .clickable { onOpenAlbum(album.id) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Artwork(
                        model = album.artworkUri,
                        seed = album.id,
                        modifier = Modifier.size(54.dp),
                        cornerRadius = 14.dp,
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp),
                    ) {
                        Text(
                            text = album.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${album.artist} • ${trackCountLabel(album.tracks.size)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { onPlayAll(album.tracks, false) }) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Слушать альбом",
                            tint = colors.accent,
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistsTab(
    artists: List<Artist>,
    tracks: List<Track>,
    onOpenArtist: (String) -> Unit,
    onPlayAll: (List<Track>, Boolean) -> Unit,
) {
    val colors = LocalMuzikColors.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (artists.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.Person,
                    title = "Исполнителей нет",
                    message = "Исполнители появятся после сканирования локальной медиатеки.",
                )
            }
        } else {
            items(artists, key = { it.name }) { artist ->
                val artistTracks = tracks.filter { it.artist == artist.name }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.rowBackground)
                        .clickable { onOpenArtist(artist.name) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ArtistAvatar(
                        name = artist.name,
                        seed = artist.name.hashCode().toLong(),
                        size = 52.dp,
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp),
                    ) {
                        Text(
                            text = artist.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${trackCountLabel(artist.trackCount)} • ${artist.albumCount} альбомов",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            maxLines = 1,
                        )
                    }
                    IconButton(onClick = { onPlayAll(artistTracks, false) }) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Слушать исполнителя",
                            tint = colors.accent,
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistsTab(
    playlists: List<Playlist>,
    onOpenPlaylist: (Long) -> Unit,
    onCreatePlaylist: () -> Unit,
) {
    val colors = LocalMuzikColors.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${playlists.size} плейлистов",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                GlassPill(
                    text = "Создать",
                    selected = false,
                    icon = Icons.Rounded.Add,
                    onClick = onCreatePlaylist,
                )
            }
        }
        if (playlists.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.PlaylistPlay,
                    title = "Плейлистов пока нет",
                    message = "Создайте плейлист и добавляйте в него треки из медиатеки.",
                    actionText = "Создать плейлист",
                    onAction = onCreatePlaylist,
                )
            }
        } else {
            items(playlists, key = { it.id }) { playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.rowBackground)
                        .clickable { onOpenPlaylist(playlist.id) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlaylistIcon(seed = playlist.id)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp),
                    ) {
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = trackCountLabel(playlist.trackCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoritesTab(
    favorites: List<Track>,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onPlayAll: (List<Track>, Boolean) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (favorites.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.LibraryMusic,
                    title = "В избранном пусто",
                    message = "Нажмите на сердечко у трека, чтобы добавить его сюда.",
                )
            }
        } else {
            item {
                TrackListHeader(
                    countLabel = trackCountLabel(favorites.size),
                    onPlayAll = { onPlayAll(favorites, false) },
                    onShuffle = { onPlayAll(favorites, true) },
                )
            }
            items(favorites, key = { it.id }) { track ->
                TrackRow(
                    track = track,
                    onClick = { onPlayTrack(track, favorites) },
                    onToggleFavorite = { onToggleFavorite(track) },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                )
            }
        }
    }
}
