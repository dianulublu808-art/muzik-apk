package app.muzik.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import app.muzik.domain.Track
import app.muzik.ui.components.EmptyState
import app.muzik.ui.components.ScreenHeader
import app.muzik.ui.components.TrackRow
import app.muzik.ui.theme.LocalMuzikColors

@Composable
fun SearchScreen(
    query: String,
    results: List<Track>,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
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
            subtitle = "Треки, исполнители и альбомы",
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
                message = "Начните вводить название трека, имя исполнителя или альбом. Поиск работает по локальной медиатеке.",
            )
        } else {
            Text(
                text = if (results.isEmpty()) {
                    "Ничего не найдено"
                } else {
                    "Найдено: ${results.size}"
                },
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
            if (results.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.SearchOff,
                    title = "Пусто",
                    message = "Попробуйте изменить запрос или проверьте, что музыка есть в локальной медиатеке.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(results, key = { it.id }) { track ->
                        TrackRow(
                            track = track,
                            onClick = { onPlayTrack(track, results) },
                            onToggleFavorite = { onToggleFavorite(track) },
                            onAddToPlaylist = { onAddToPlaylist(track) },
                        )
                    }
                }
            }
        }
    }
}
