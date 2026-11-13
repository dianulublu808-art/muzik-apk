package app.muzik.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.muzik.domain.Track
import app.muzik.ui.theme.LocalMuzikColors
import app.muzik.util.formatDuration

@Composable
fun TrackRow(
    track: Track,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleFavorite: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    isActive: Boolean = false,
    showFavorite: Boolean = true,
) {
    val colors = LocalMuzikColors.current
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isActive) colors.accent.copy(alpha = 0.13f) else colors.rowBackground,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(
            model = track.artworkUri,
            seed = track.id,
            modifier = Modifier.size(52.dp),
            cornerRadius = 14.dp,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isActive) colors.accent else colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append(track.artist)
                    if (track.album.isNotBlank() && track.album != "Без альбома") {
                        append(" • ")
                        append(track.album)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isActive) {
            Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = "Играет сейчас",
                tint = colors.accent,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(18.dp),
            )
        }
        Text(
            text = formatDuration(track.durationMs),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            modifier = Modifier.padding(end = 2.dp),
        )
        if (showFavorite && onToggleFavorite != null) {
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(38.dp),
            ) {
                Icon(
                    imageVector = if (track.isFavorite) {
                        Icons.Rounded.Favorite
                    } else {
                        Icons.Rounded.FavoriteBorder
                    },
                    contentDescription = if (track.isFavorite) {
                        "Убрать из избранного"
                    } else {
                        "В избранное"
                    },
                    tint = if (track.isFavorite) colors.accentSecondary else colors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        if (onAddToPlaylist != null || onRemoveFromPlaylist != null) {
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Действия с треком",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    if (onToggleFavorite != null) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (track.isFavorite) "Убрать из избранного" else "В избранное",
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    if (track.isFavorite) {
                                        Icons.Rounded.Favorite
                                    } else {
                                        Icons.Rounded.FavoriteBorder
                                    },
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onToggleFavorite()
                            },
                        )
                    }
                    if (onAddToPlaylist != null) {
                        DropdownMenuItem(
                            text = { Text("Добавить в плейлист") },
                            leadingIcon = {
                                Icon(Icons.Rounded.PlaylistAdd, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                onAddToPlaylist()
                            },
                        )
                    }
                    if (onRemoveFromPlaylist != null) {
                        DropdownMenuItem(
                            text = { Text("Удалить из плейлиста") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Delete, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                onRemoveFromPlaylist()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrackListHeader(
    countLabel: String,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMuzikColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.glass)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = countLabel,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        GlassPill(
            text = "Слушать",
            selected = false,
            onClick = onPlayAll,
            icon = Icons.Rounded.PlayArrow,
        )
        GlassPill(
            text = "Вперемешку",
            selected = false,
            onClick = onShuffle,
            icon = Icons.Rounded.Shuffle,
        )
    }
}

@Composable
fun PlaylistIcon(
    seed: Long,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 52.dp,
) {
    val colors = LocalMuzikColors.current
    val palette = listOf(colors.accent, colors.accentSecondary)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(palette),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.QueueMusic,
            contentDescription = null,
            tint = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier.size(size * 0.46f),
        )
    }
}

@Composable
fun SmallActionRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

@Composable
fun InlineProgress(modifier: Modifier = Modifier) {
    val colors = LocalMuzikColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            color = colors.accent,
            strokeWidth = 2.dp,
        )
        Text(
            text = "Обновляем медиатеку…",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
    }
}

