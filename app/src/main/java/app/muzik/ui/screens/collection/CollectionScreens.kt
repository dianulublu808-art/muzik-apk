package app.muzik.ui.screens.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.muzik.domain.Track
import app.muzik.ui.components.AccentButton
import app.muzik.ui.components.Artwork
import app.muzik.ui.components.EmptyState
import app.muzik.ui.components.GlassCard
import app.muzik.ui.components.GlassIconButton
import app.muzik.ui.components.GlassPill
import app.muzik.ui.components.StatChip
import app.muzik.ui.components.TrackRow
import app.muzik.ui.screens.library.LibraryUiState
import app.muzik.ui.theme.LocalMuzikColors
import app.muzik.util.formatTotalDuration
import app.muzik.util.trackCountLabel

@Composable
fun AlbumScreen(
    albumId: Long,
    state: LibraryUiState,
    onBack: () -> Unit,
    onPlayAll: (List<Track>, Boolean) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    val album = state.albums.firstOrNull { it.id == albumId }
    CollectionContent(
        title = album?.title ?: "Альбом",
        subtitle = album?.artist ?: "",
        artworkModel = album?.artworkUri,
        artworkSeed = albumId,
        tracks = album?.tracks.orEmpty(),
        onBack = onBack,
        onPlayAll = onPlayAll,
        onPlayTrack = onPlayTrack,
        onToggleFavorite = onToggleFavorite,
        onAddToPlaylist = onAddToPlaylist,
        emptyTitle = "Альбом не найден",
        emptyMessage = "Возможно, треки были удалены с устройства. Обновите медиатеку.",
        modifier = modifier,
    )
}

@Composable
fun ArtistScreen(
    artistName: String,
    state: LibraryUiState,
    onBack: () -> Unit,
    onPlayAll: (List<Track>, Boolean) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tracks = state.tracks.filter { it.artist == artistName }
    CollectionContent(
        title = artistName,
        subtitle = "Исполнитель",
        artworkModel = tracks.firstNotNullOfOrNull { it.artworkUri },
        artworkSeed = artistName.hashCode().toLong(),
        tracks = tracks,
        onBack = onBack,
        onPlayAll = onPlayAll,
        onPlayTrack = onPlayTrack,
        onToggleFavorite = onToggleFavorite,
        onAddToPlaylist = onAddToPlaylist,
        emptyTitle = "Исполнитель не найден",
        emptyMessage = "Треки этого исполнителя отсутствуют в локальной медиатеке.",
        modifier = modifier,
    )
}

@Composable
fun FavoritesScreen(
    state: LibraryUiState,
    onBack: () -> Unit,
    onPlayAll: (List<Track>, Boolean) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    CollectionContent(
        title = "Избранное",
        subtitle = "Любимые треки",
        artworkModel = state.favorites.firstNotNullOfOrNull { it.artworkUri },
        artworkSeed = -1L,
        tracks = state.favorites,
        onBack = onBack,
        onPlayAll = onPlayAll,
        onPlayTrack = onPlayTrack,
        onToggleFavorite = onToggleFavorite,
        onAddToPlaylist = onAddToPlaylist,
        emptyTitle = "В избранном пусто",
        emptyMessage = "Отмечайте треки сердечком в медиатеке или плеере.",
        modifier = modifier,
        headerIcon = true,
    )
}

@Composable
private fun CollectionContent(
    title: String,
    subtitle: String,
    artworkModel: Any?,
    artworkSeed: Long,
    tracks: List<Track>,
    onBack: () -> Unit,
    onPlayAll: (List<Track>, Boolean) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    emptyTitle: String,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    headerIcon: Boolean = false,
) {
    val colors = LocalMuzikColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(
                icon = Icons.Rounded.ArrowBack,
                contentDescription = "Назад",
                onClick = onBack,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    strong = true,
                    contentPadding = PaddingValues(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (headerIcon) {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(colors.accent, colors.accentSecondary),
                                        ),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Rounded.Favorite,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(38.dp),
                                )
                            }
                        } else {
                            Artwork(
                                model = artworkModel,
                                seed = artworkSeed,
                                modifier = Modifier.size(92.dp),
                                cornerRadius = 20.dp,
                                iconSize = 38.dp,
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = colors.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (subtitle.isNotBlank()) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatChip(text = trackCountLabel(tracks.size))
                                StatChip(text = formatTotalDuration(tracks.sumOf { it.durationMs }))
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccentButton(
                            text = "Слушать",
                            icon = Icons.Rounded.PlayArrow,
                            enabled = tracks.isNotEmpty(),
                            onClick = { onPlayAll(tracks, false) },
                        )
                        GlassPill(
                            text = "Вперемешку",
                            selected = false,
                            icon = Icons.Rounded.Shuffle,
                            onClick = { onPlayAll(tracks, true) },
                        )
                    }
                }
            }

            if (tracks.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.Favorite,
                        title = emptyTitle,
                        message = emptyMessage,
                    )
                }
            } else {
                items(tracks, key = { it.id }) { track ->
                    TrackRow(
                        track = track,
                        onClick = { onPlayTrack(track, tracks) },
                        onToggleFavorite = { onToggleFavorite(track) },
                        onAddToPlaylist = { onAddToPlaylist(track) },
                    )
                }
            }
        }
    }
}
