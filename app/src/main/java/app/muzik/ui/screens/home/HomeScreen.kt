package app.muzik.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.muzik.domain.Playlist
import app.muzik.domain.Track
import app.muzik.ui.components.AccentButton
import app.muzik.ui.components.Artwork
import app.muzik.ui.components.EmptyState
import app.muzik.ui.components.GlassCard
import app.muzik.ui.components.GlassPill
import app.muzik.ui.components.InlineProgress
import app.muzik.ui.components.PlaylistIcon
import app.muzik.ui.components.ScreenHeader
import app.muzik.ui.components.SectionHeader
import app.muzik.ui.components.StatChip
import app.muzik.ui.components.TrackRow
import app.muzik.ui.screens.library.LibraryUiState
import app.muzik.ui.theme.LocalMuzikColors
import app.muzik.util.trackCountLabel
import app.muzik.util.playlistCountLabel

@Composable
fun HomeScreen(
    state: LibraryUiState,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onRefresh: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onPlayAll: (List<Track>, Boolean) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMuzikColors.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            ScreenHeader(
                title = "muzik",
                subtitle = "Твоя музыка. В одном месте.",
                trailing = {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(colors.accent, colors.accentSecondary),
                                ),
                            )
                            .clickable(onClick = onOpenPlayer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = "Открыть плеер",
                            tint = colors.onAccent,
                            modifier = Modifier.size(23.dp),
                        )
                    }
                },
            )
        }

        item {
            HeroCard(
                trackCount = state.tracks.size,
                playlistCount = state.playlists.size,
                favoriteCount = state.favorites.size,
                hasTracks = state.tracks.isNotEmpty(),
                onPlayAll = { onPlayAll(state.tracks, false) },
                onShuffle = { onPlayAll(state.tracks, true) },
                onRequestPermission = onRequestPermission,
                onRefresh = onRefresh,
                hasAudioPermission = hasAudioPermission,
            )
        }

        if (!hasAudioPermission) {
            item {
                PermissionCard(
                    onRequestPermission = onRequestPermission,
                    onOpenAppSettings = onOpenAppSettings,
                )
            }
        }

        if (state.isScanning) {
            item { InlineProgress() }
        }

        if (state.recent.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Недавно добавлено",
                    actionText = "Вся медиатека",
                    onAction = onOpenLibrary,
                )
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 4.dp),
                ) {
                    items(state.recent.take(10), key = { it.id }) { track ->
                        RecentTrackCard(
                            track = track,
                            onClick = { onPlayTrack(track, state.recent) },
                        )
                    }
                }
            }
        }

        if (state.favorites.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Избранное",
                    actionText = "Все",
                    onAction = onOpenFavorites,
                )
            }
            items(state.favorites.take(4), key = { "fav-${it.id}" }) { track ->
                TrackRow(
                    track = track,
                    onClick = { onPlayTrack(track, state.favorites) },
                    onToggleFavorite = { onToggleFavorite(track) },
                )
            }
        }

        if (state.playlists.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Плейлисты",
                    actionText = "К коллекции",
                    onAction = onOpenLibrary,
                )
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 4.dp),
                ) {
                    items(state.playlists, key = { it.id }) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick = { onOpenPlaylist(playlist.id) },
                        )
                    }
                }
            }
        }

        if (state.isEmpty && hasAudioPermission && !state.isScanning) {
            item {
                EmptyState(
                    icon = Icons.Rounded.LibraryMusic,
                    title = "Медиатека пуста",
                    message = "Добавьте музыку на устройство и обновите медиатеку — треки появятся здесь автоматически.",
                    actionText = "Обновить",
                    onAction = onRefresh,
                )
            }
        }

        item {
            Text(
                text = "Вся музыка воспроизводится с вашего устройства. muzik не загружает треки из интернета.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun HeroCard(
    trackCount: Int,
    playlistCount: Int,
    favoriteCount: Int,
    hasTracks: Boolean,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    hasAudioPermission: Boolean,
) {
    val colors = LocalMuzikColors.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        strong = true,
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            colors.accent.copy(alpha = 0.28f),
                            colors.accentSecondary.copy(alpha = 0.13f),
                            colors.glass,
                        ),
                    ),
                )
                .padding(20.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(colors.accentSecondary.copy(alpha = 0.16f)),
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "ТВОЯ МУЗЫКА",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.accent,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Собери всё\nв одном месте",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary,
                )
                Text(
                    text = "Локальная медиатека, избранное, плейлисты и единый плеер.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip(text = trackCountLabel(trackCount))
                    StatChip(text = playlistCountLabel(playlistCount))
                    StatChip(text = "$favoriteCount ♥")
                }
                Spacer(Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    when {
                        !hasAudioPermission -> {
                            AccentButton(
                                text = "Разрешить доступ",
                                icon = Icons.Rounded.Lock,
                                onClick = onRequestPermission,
                            )
                        }
                        hasTracks -> {
                            AccentButton(
                                text = "Слушать всё",
                                icon = Icons.Rounded.PlayArrow,
                                onClick = onPlayAll,
                            )
                            GlassPill(
                                text = "Вперемешку",
                                selected = false,
                                icon = Icons.Rounded.Shuffle,
                                onClick = onShuffle,
                            )
                        }
                        else -> {
                            AccentButton(
                                text = "Обновить медиатеку",
                                icon = Icons.Rounded.Refresh,
                                onClick = onRefresh,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    val colors = LocalMuzikColors.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(21.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = "Нужен доступ к музыке",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                Text(
                    text = "Разрешите чтение аудиофайлов, чтобы собрать локальную медиатеку.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AccentButton(text = "Разрешить", onClick = onRequestPermission)
            GlassPill(text = "Настройки", selected = false, onClick = onOpenAppSettings)
        }
    }
}

@Composable
private fun RecentTrackCard(
    track: Track,
    onClick: () -> Unit,
) {
    val colors = LocalMuzikColors.current
    Column(
        modifier = Modifier
            .width(142.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.glass)
            .clickable(onClick = onClick)
            .padding(9.dp),
    ) {
        Artwork(
            model = track.artworkUri,
            seed = track.id,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            cornerRadius = 15.dp,
            iconSize = 34.dp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = track.title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = track.artist,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
) {
    val colors = LocalMuzikColors.current
    Column(
        modifier = Modifier
            .width(158.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.glass)
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlaylistIcon(seed = playlist.id, size = 52.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = trackCountLabel(playlist.trackCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Открыть плейлист",
            style = MaterialTheme.typography.labelMedium,
            color = colors.accent,
        )
    }
}
