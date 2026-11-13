package app.muzik.ui.screens.playlist

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.muzik.domain.Track
import app.muzik.ui.components.AccentButton
import app.muzik.ui.components.ConfirmDeleteDialog
import app.muzik.ui.components.EmptyState
import app.muzik.ui.components.GlassCard
import app.muzik.ui.components.GlassIconButton
import app.muzik.ui.components.GlassPill
import app.muzik.ui.components.PlaylistIcon
import app.muzik.ui.components.RenamePlaylistDialog
import app.muzik.ui.components.StatChip
import app.muzik.ui.components.TrackRow
import app.muzik.ui.theme.LocalMuzikColors
import app.muzik.util.formatTotalDuration
import app.muzik.util.trackCountLabel

@Composable
fun PlaylistDetailScreen(
    detail: PlaylistDetail?,
    onBack: () -> Unit,
    onPlayAll: (List<Track>, Boolean) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onRemoveTrack: (Long) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMuzikColors.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    val playlist = detail?.playlist
    val tracks = detail?.tracks.orEmpty()

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
                    text = playlist?.name ?: "Плейлист",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = trackCountLabel(tracks.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            Box {
                GlassIconButton(
                    icon = Icons.Rounded.MoreVert,
                    contentDescription = "Действия с плейлистом",
                    onClick = { menuExpanded = true },
                )
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Переименовать") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            showRename = true
                        },
                        enabled = playlist != null,
                    )
                    DropdownMenuItem(
                        text = { Text("Удалить плейлист") },
                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            showDelete = true
                        },
                        enabled = playlist != null,
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
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlaylistIcon(seed = playlist?.id ?: 0L, size = 92.dp)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text(
                                text = playlist?.name ?: "Плейлист",
                                style = MaterialTheme.typography.headlineSmall,
                                color = colors.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
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

            if (detail != null && tracks.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.PlaylistPlay,
                        title = "Плейлист пуст",
                        message = "Добавляйте треки через меню «…» в медиатеке, поиске или плеере.",
                    )
                }
            } else {
                items(tracks, key = { it.id }) { track ->
                    TrackRow(
                        track = track,
                        onClick = { onPlayTrack(track, tracks) },
                        onToggleFavorite = { onToggleFavorite(track) },
                        onAddToPlaylist = { onAddToPlaylist(track) },
                        onRemoveFromPlaylist = {
                            playlist?.let { onRemoveTrack(it.id) }
                        },
                    )
                }
            }
        }
    }

    if (showRename && playlist != null) {
        RenamePlaylistDialog(
            currentName = playlist.name,
            onDismiss = { showRename = false },
            onRename = { name ->
                showRename = false
                onRename(name)
            },
        )
    }

    if (showDelete && playlist != null) {
        ConfirmDeleteDialog(
            title = "Удалить плейлист?",
            message = "Плейлист «${playlist.name}» будет удалён. Треки останутся в медиатеке.",
            onDismiss = { showDelete = false },
            onConfirm = {
                showDelete = false
                onDelete()
            },
        )
    }
}
