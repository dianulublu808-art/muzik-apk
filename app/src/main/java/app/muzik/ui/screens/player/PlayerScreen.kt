package app.muzik.ui.screens.player

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import app.muzik.playback.NowPlaying
import app.muzik.playback.PlayerUiState
import app.muzik.ui.components.Artwork
import app.muzik.ui.components.EmptyState
import app.muzik.ui.components.GlassCard
import app.muzik.ui.components.GlassIconButton
import app.muzik.ui.components.GlassPill
import app.muzik.ui.theme.LocalMuzikColors
import app.muzik.util.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    state: PlayerUiState,
    favoriteIds: Set<Long>,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAddToPlaylist: (NowPlaying) -> Unit,
    onSelectQueueItem: (Int) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMuzikColors.current
    var showQueue by remember { mutableStateOf(false) }
    var dragValue by remember(state.currentTrack?.id) { mutableFloatStateOf(-1f) }
    val track = state.currentTrack

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(
                icon = Icons.Rounded.ArrowBack,
                contentDescription = "Свернуть плеер",
                onClick = onBack,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = "СЕЙЧАС ИГРАЕТ",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.accent,
                )
                Text(
                    text = track?.album?.takeIf { it.isNotBlank() } ?: "muzik",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            GlassIconButton(
                icon = Icons.Rounded.QueueMusic,
                contentDescription = "Очередь воспроизведения",
                onClick = { showQueue = true },
                enabled = state.queue.isNotEmpty(),
            )
        }

        if (track == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    icon = Icons.Rounded.MusicNote,
                    title = "Ничего не играет",
                    message = "Выберите трек в медиатеке или включите «Слушать всё» на главной.",
                    actionText = "Назад",
                    onAction = onBack,
                )
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))
            Artwork(
                model = track.artworkUri,
                seed = track.id,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(horizontal = 18.dp),
                cornerRadius = 34.dp,
                iconSize = 78.dp,
            )
            Spacer(Modifier.height(24.dp))

            Text(
                text = track.title,
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = track.artist.ifBlank { "Неизвестный исполнитель" },
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))

            val duration = state.durationMs.coerceAtLeast(1L)
            val displayedPosition = if (dragValue >= 0f) {
                dragValue.toLong()
            } else {
                state.positionMs
            }
            Slider(
                value = (displayedPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f),
                onValueChange = { fraction -> dragValue = fraction * duration.toFloat() },
                onValueChangeFinished = {
                    if (dragValue >= 0f) {
                        onSeek(dragValue.toLong())
                        dragValue = -1f
                    }
                },
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent,
                    inactiveTrackColor = colors.divider,
                    activeTickColor = colors.accent,
                    inactiveTickColor = colors.divider,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatDuration(displayedPosition),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
                Text(
                    text = formatDuration(state.durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Перемешать",
                        tint = if (state.shuffleEnabled) colors.accent else colors.textSecondary,
                    )
                }
                IconButton(onClick = onPrevious, modifier = Modifier.size(54.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Предыдущий трек",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(colors.accent, colors.accentSecondary),
                            ),
                        )
                        .clickable(onClick = onTogglePlay),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(30.dp),
                            color = colors.onAccent,
                            strokeWidth = 3.dp,
                        )
                    } else {
                        Icon(
                            imageVector = if (state.isPlaying) {
                                Icons.Rounded.Pause
                            } else {
                                Icons.Rounded.PlayArrow
                            },
                            contentDescription = if (state.isPlaying) "Пауза" else "Воспроизвести",
                            tint = colors.onAccent,
                            modifier = Modifier.size(42.dp),
                        )
                    }
                }
                IconButton(onClick = onNext, modifier = Modifier.size(54.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Следующий трек",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(36.dp),
                    )
                }
                IconButton(onClick = onCycleRepeat) {
                    Icon(
                        imageVector = if (state.repeatMode == Player.REPEAT_MODE_ONE) {
                            Icons.Rounded.RepeatOne
                        } else {
                            Icons.Rounded.Repeat
                        },
                        contentDescription = "Повтор",
                        tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) {
                            colors.accent
                        } else {
                            colors.textSecondary
                        },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                val isFavorite = track.id in favoriteIds
                GlassPill(
                    text = if (isFavorite) "В избранном" else "В избранное",
                    selected = isFavorite,
                    icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    onClick = { onToggleFavorite(track.id) },
                )
                GlassPill(
                    text = "В плейлист",
                    selected = false,
                    icon = Icons.Rounded.PlaylistAdd,
                    onClick = { onAddToPlaylist(track) },
                )
                GlassPill(
                    text = "Очередь · ${state.queue.size}",
                    selected = false,
                    icon = Icons.Rounded.QueueMusic,
                    onClick = { showQueue = true },
                )
            }

            state.error?.let { error ->
                Spacer(Modifier.height(16.dp))
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "Закрыть",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.accent,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(onClick = onDismissError)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showQueue) {
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.surface,
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Очередь",
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${state.queue.size} треков",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
                Spacer(Modifier.height(12.dp))
                val currentIndex = state.queue.indexOfFirst { it.id == state.currentTrack?.id }
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    itemsIndexed(state.queue, key = { index, item -> "$index-${item.id}" }) { index, item ->
                        val active = index == currentIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (active) {
                                        colors.accent.copy(alpha = 0.14f)
                                    } else {
                                        colors.rowBackground
                                    },
                                )
                                .clickable {
                                    onSelectQueueItem(index)
                                    showQueue = false
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Artwork(
                                model = item.artworkUri,
                                seed = item.id,
                                modifier = Modifier.size(42.dp),
                                cornerRadius = 11.dp,
                                iconSize = 18.dp,
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 10.dp),
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (active) colors.accent else colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = item.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (active) {
                                Icon(
                                    imageVector = Icons.Rounded.GraphicEq,
                                    contentDescription = "Играет сейчас",
                                    tint = colors.accent,
                                    modifier = Modifier.size(20.dp),
                                )
                            } else {
                                Text(
                                    text = formatDuration(item.durationMs),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
