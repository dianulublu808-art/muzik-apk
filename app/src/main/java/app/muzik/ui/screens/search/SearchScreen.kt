package app.muzik.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import app.muzik.data.remote.WaveSource
import app.muzik.data.remote.WaveTrack
import app.muzik.domain.Track
import app.muzik.ui.components.Artwork
import app.muzik.ui.components.EmptyState
import app.muzik.ui.components.GlassCard
import app.muzik.ui.components.ScreenHeader
import app.muzik.ui.components.TrackRow
import app.muzik.ui.theme.LocalMuzikColors
import app.muzik.util.formatDuration

@Composable
fun SearchScreen(
    query: String,
    results: List<Track>,
    remote: RemoteSearchState,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onPlayRemote: (WaveTrack, List<WaveTrack>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMuzikColors.current
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        ScreenHeader(
            title = "Поиск",
            subtitle = "Локальная медиатека и площадки",
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Название, исполнитель или альбом") },
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = null)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClearQuery) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Очистить поиск")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.glassBorderTop.copy(alpha = 0.35f),
                focusedContainerColor = colors.glass,
                unfocusedContainerColor = colors.glass,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                cursorColor = colors.accent,
                focusedPlaceholderColor = colors.textSecondary,
                unfocusedPlaceholderColor = colors.textSecondary,
                focusedLeadingIconColor = colors.accent,
                unfocusedLeadingIconColor = colors.textSecondary,
                focusedTrailingIconColor = colors.textSecondary,
                unfocusedTrailingIconColor = colors.textSecondary,
            ),
        )
        Spacer(Modifier.height(14.dp))

        if (query.isBlank()) {
            EmptyState(
                icon = Icons.Rounded.Search,
                title = "Найдите свою музыку",
                message = "Поиск идёт по локальной медиатеке, а также по SoundCloud, " +
                    "YouTube и VK — если источники включены в настройках «Волны».",
            )
            return@Column
        }

        val nothing = results.isEmpty() && !remote.hasResults && !remote.isLoading
        if (nothing) {
            EmptyState(
                icon = Icons.Rounded.SearchOff,
                title = "Пусто",
                message = if (remote.isActive) {
                    "Ничего не нашлось ни в медиатеке, ни на площадках."
                } else {
                    "Ничего не нашлось в медиатеке. Включите онлайн-источники " +
                        "в настройках «Волны», чтобы искать и там."
                },
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (remote.isActive) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 2.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (remote.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = colors.accent,
                                strokeWidth = 2.dp,
                            )
                        }
                        Text(
                            text = if (remote.isLoading) {
                                "Ищем на площадках…"
                            } else {
                                "Площадки: ${remote.tracks.size}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary,
                        )
                    }
                }
            }

            if (results.isNotEmpty()) {
                item {
                    SectionLabel(text = "В медиатеке · ${results.size}")
                }
                items(results, key = { "local-${it.id}" }) { track ->
                    TrackRow(
                        track = track,
                        onClick = { onPlayTrack(track, results) },
                        onToggleFavorite = { onToggleFavorite(track) },
                        onAddToPlaylist = { onAddToPlaylist(track) },
                    )
                }
            }

            WaveSource.remote.forEach { source ->
                val sourceTracks = remote.tracks.filter { it.source == source }
                if (sourceTracks.isEmpty()) return@forEach
                item(key = "header-${source.name}") {
                    SectionLabel(text = "${source.label} · ${sourceTracks.size}")
                }
                items(sourceTracks, key = { it.uid }) { track ->
                    RemoteTrackRow(
                        track = track,
                        onClick = { onPlayRemote(track, sourceTracks) },
                    )
                }
            }

            if (remote.errors.isNotEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        contentPadding = PaddingValues(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CloudOff,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Часть источников не ответила",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        remote.errors.forEach { error ->
                            Text(
                                text = "• $error",
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

@Composable
private fun SectionLabel(text: String) {
    val colors = LocalMuzikColors.current
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = colors.textPrimary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun RemoteTrackRow(
    track: WaveTrack,
    onClick: () -> Unit,
) {
    val colors = LocalMuzikColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.rowBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(
            model = track.artworkUrl,
            seed = track.uid.hashCode().toLong(),
            modifier = Modifier.size(46.dp),
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = " ${track.source.label} · ${track.displayArtist}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
