package app.muzik.ui.screens.wave

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import app.muzik.data.remote.WaveSource
import app.muzik.data.remote.WaveTrack
import app.muzik.ui.components.AccentButton
import app.muzik.ui.components.Artwork
import app.muzik.ui.components.GlassCard
import app.muzik.ui.components.GlassPill
import app.muzik.ui.components.ScreenHeader
import app.muzik.ui.components.SectionHeader
import app.muzik.ui.theme.LocalMuzikColors
import app.muzik.util.formatDuration

@Composable
fun WaveScreen(
    state: WaveUiState,
    onStart: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMuzikColors.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScreenHeader(
                title = "Волна",
                subtitle = "Бесконечный поток из разных площадок",
            )
        }

        item {
            SourceStrip(state = state, onOpenSettings = onOpenSettings)
        }

        val current = state.current
        if (current == null) {
            item {
                StartCard(
                    isLoading = state.isLoading,
                    hasRemoteSource = state.hasRemoteSource,
                    localTrackCount = state.localTrackCount,
                    onStart = onStart,
                    onOpenSettings = onOpenSettings,
                )
            }
        } else {
            item {
                NowPlayingCard(
                    track = current,
                    state = state,
                    onTogglePlay = onTogglePlay,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onLike = onLike,
                    onDislike = onDislike,
                    onStart = onStart,
                )
            }
        }

        if (state.sourceErrors.isNotEmpty()) {
            item {
                SourceErrorsCard(errors = state.sourceErrors)
            }
        }

        if (state.upcoming.isNotEmpty()) {
            item {
                SectionHeader(title = "Дальше в волне")
            }
            items(state.upcoming, key = { it.uid }) { track ->
                UpcomingRow(track = track, onNext = onNext)
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Треки берутся из SoundCloud, YouTube и VK через открытые " +
                        "интерфейсы этих сервисов. Они неофициальные: площадки могут " +
                        "ограничить доступ, а качество и доступность зависят от вашей сети. " +
                        "Локальные треки тоже подмешиваются в волну.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun SourceStrip(
    state: WaveUiState,
    onOpenSettings: () -> Unit,
) {
    val colors = LocalMuzikColors.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Источники",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                Text(
                    text = if (state.hasRemoteSource) {
                        "Волна собирается из выбранных площадок"
                    } else {
                        "Онлайн-источники выключены"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            GlassPill(
                text = "Настроить",
                selected = false,
                icon = Icons.Rounded.Tune,
                onClick = onOpenSettings,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WaveSource.remote.forEach { source ->
                SourceChip(source = source, enabled = source in state.sources)
            }
        }
    }
}

@Composable
private fun SourceChip(source: WaveSource, enabled: Boolean) {
    val colors = LocalMuzikColors.current
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (enabled) colors.accent.copy(alpha = 0.16f) else colors.glass,
            )
            .border(
                width = 1.dp,
                color = if (enabled) colors.accent.copy(alpha = 0.5f) else colors.glassBorderBottom,
                shape = CircleShape,
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (enabled) colors.accent else colors.textSecondary.copy(alpha = 0.4f),
                ),
        )
        Text(
            text = source.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) colors.accent else colors.textSecondary,
        )
    }
}

@Composable
private fun StartCard(
    isLoading: Boolean,
    hasRemoteSource: Boolean,
    localTrackCount: Int,
    onStart: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = LocalMuzikColors.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.4f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            colors.accent.copy(alpha = 0.28f),
                            colors.accentSecondary.copy(alpha = 0.18f),
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = colors.accent, strokeWidth = 3.dp)
            } else {
                Icon(
                    imageVector = Icons.Rounded.Waves,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(64.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (isLoading) "Собираем волну…" else "Включить волну",
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = when {
                isLoading -> "Ищем треки в выбранных источниках"
                hasRemoteSource -> "Мы подберём треки и будем добавлять новые без остановки"
                localTrackCount > 0 -> "Онлайн-источники выключены — волна пойдёт из медиатеки"
                else -> "Включите хотя бы один источник в настройках «Волны»"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AccentButton(
                text = if (isLoading) "Загрузка…" else "Включить",
                icon = Icons.Rounded.PlayArrow,
                enabled = !isLoading && (hasRemoteSource || localTrackCount > 0),
                onClick = onStart,
            )
            GlassPill(
                text = "Источники",
                selected = false,
                icon = Icons.Rounded.Tune,
                onClick = onOpenSettings,
            )
        }
    }
}

@Composable
private fun NowPlayingCard(
    track: WaveTrack,
    state: WaveUiState,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onStart: () -> Unit,
) {
    val colors = LocalMuzikColors.current
    val liked = track.uid in state.liked
    val disliked = track.uid in state.disliked

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(horizontal = 8.dp),
        ) {
            Artwork(
                model = track.artworkUrl,
                seed = track.uid.hashCode().toLong(),
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 28.dp,
                iconSize = 72.dp,
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(colors.surface.copy(alpha = 0.7f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = track.source.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textPrimary,
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            text = track.title,
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = track.displayArtist,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(CircleShape)
                .background(colors.divider),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(state.progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(colors.accent, colors.accentSecondary),
                        ),
                    ),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDuration(state.positionMs),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
            Text(
                text = formatDuration(
                    if (state.durationMs > 0L) state.durationMs else track.durationMs,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WaveIconButton(
                icon = Icons.Rounded.ThumbDown,
                contentDescription = "Не нравится",
                active = disliked,
                onClick = onDislike,
            )
            WaveIconButton(
                icon = Icons.Rounded.SkipPrevious,
                contentDescription = "Предыдущий",
                onClick = onPrevious,
                size = 46.dp,
            )
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(colors.accent, colors.accentSecondary)),
                    )
                    .clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center,
            ) {
                if (state.isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
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
                        modifier = Modifier.size(38.dp),
                    )
                }
            }
            WaveIconButton(
                icon = Icons.Rounded.SkipNext,
                contentDescription = "Следующий",
                onClick = onNext,
                size = 46.dp,
            )
            WaveIconButton(
                icon = Icons.Rounded.ThumbUp,
                contentDescription = "Нравится",
                active = liked,
                onClick = onLike,
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        ) {
            GlassPill(
                text = "Новая волна",
                selected = false,
                icon = Icons.Rounded.Refresh,
                onClick = onStart,
            )
        }
    }
}

@Composable
private fun WaveIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    active: Boolean = false,
    size: Dp = 42.dp,
) {
    val colors = LocalMuzikColors.current
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) colors.accent else colors.textSecondary,
            modifier = Modifier.size(size * 0.62f),
        )
    }
}

@Composable
private fun SourceErrorsCard(errors: List<String>) {
    val colors = LocalMuzikColors.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.CloudOff,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = "Некоторые источники не ответили",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
        }
        Spacer(Modifier.height(8.dp))
        errors.forEach { error ->
            Text(
                text = "• $error",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun UpcomingRow(track: WaveTrack, onNext: () -> Unit) {
    val colors = LocalMuzikColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.rowBackground)
            .clickable(onClick = onNext)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(
            model = track.artworkUrl,
            seed = track.uid.hashCode().toLong(),
            modifier = Modifier.size(44.dp),
            cornerRadius = 12.dp,
            iconSize = 20.dp,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${track.source.label} · ${track.displayArtist}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (track.durationMs > 0L) {
            Text(
                text = formatDuration(track.durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
    }
}
