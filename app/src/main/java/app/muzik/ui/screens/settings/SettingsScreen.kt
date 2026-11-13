package app.muzik.ui.screens.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.muzik.BuildConfig
import app.muzik.data.remote.WaveSource
import app.muzik.data.settings.AccentColor
import app.muzik.data.settings.MuzikSettings
import app.muzik.data.settings.ThemeMode
import app.muzik.ui.components.AccentButton
import app.muzik.ui.components.GlassCard
import app.muzik.ui.components.GlassPill
import app.muzik.ui.components.InlineProgress
import app.muzik.ui.components.ScreenHeader
import app.muzik.ui.components.SectionHeader
import app.muzik.ui.theme.LocalMuzikColors
import app.muzik.ui.theme.primary
import app.muzik.util.trackCountLabel

@Composable
fun SettingsScreen(
    settings: MuzikSettings,
    trackCount: Int,
    isScanning: Boolean,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onRefresh: () -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onAccent: (AccentColor) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onAnimatedBackground: (Boolean) -> Unit,
    onWaveSource: (WaveSource, Boolean) -> Unit,
    onWaveYoutubeInstance: (String) -> Unit,
    onWaveVkToken: (String) -> Unit,
    onClearWaveFeedback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMuzikColors.current
    var youtubeInstance by remember(settings.waveYoutubeInstance) {
        mutableStateOf(settings.waveYoutubeInstance)
    }
    var vkToken by remember(settings.waveVkToken) {
        mutableStateOf(settings.waveVkToken)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            ScreenHeader(
                title = "Настройки",
                subtitle = "Внешний вид и медиатека",
            )
        }

        item {
            SectionHeader(title = "Тема оформления")
        }
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        GlassPill(
                            text = when (mode) {
                                ThemeMode.SYSTEM -> "Системная"
                                ThemeMode.LIGHT -> "Светлая"
                                ThemeMode.DARK -> "Тёмная"
                            },
                            selected = settings.themeMode == mode,
                            onClick = { onThemeMode(mode) },
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(title = "Акцентный цвет")
        }
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AccentColor.entries.forEach { accent ->
                        val selected = settings.accent == accent
                        Box(
                            modifier = Modifier
                                .size(if (selected) 42.dp else 36.dp)
                                .clip(CircleShape)
                                .background(accent.primary(dark = true))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) colors.textPrimary else colors.glassBorderTop,
                                    shape = CircleShape,
                                )
                                .clickable { onAccent(accent) },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Акцент используется в кнопках, прогрессе и подсветке.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }

        item {
            SectionHeader(title = "Оформление")
        }
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                SettingSwitchRow(
                    icon = Icons.Rounded.Palette,
                    title = "Динамические цвета",
                    subtitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        "Цвета Material You из системной темы"
                    } else {
                        "Доступно на Android 12 и новее"
                    },
                    checked = settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    onCheckedChange = onDynamicColor,
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.divider),
                )
                SettingSwitchRow(
                    icon = Icons.Rounded.Wallpaper,
                    title = "Анимированный фон",
                    subtitle = "Плавное движение световых пятен",
                    checked = settings.animatedBackground,
                    enabled = true,
                    onCheckedChange = onAnimatedBackground,
                )
            }
        }

        item {
            SectionHeader(title = "Медиатека")
        }
        item {
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
                            imageVector = Icons.Rounded.LibraryMusic,
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
                            text = trackCountLabel(trackCount),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = "Локальные аудиофайлы на устройстве",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                    AccentButton(
                        text = "Обновить",
                        icon = Icons.Rounded.Refresh,
                        enabled = !isScanning,
                        onClick = onRefresh,
                    )
                }
                if (isScanning) {
                    Spacer(Modifier.height(12.dp))
                    InlineProgress()
                }
                if (!hasAudioPermission) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Нет доступа к аудиофайлам",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                        )
                        GlassPill(
                            text = "Разрешить",
                            selected = false,
                            onClick = onRequestPermission,
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(title = "Волна")
        }
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                WaveSource.remote.forEachIndexed { index, source ->
                    SettingSwitchRow(
                        icon = Icons.Rounded.Cloud,
                        title = source.label,
                        subtitle = when (source) {
                            WaveSource.SOUNDCLOUD -> "Поиск и потоки из каталога SoundCloud"
                            WaveSource.YOUTUBE -> "Через ваш Invidious или Piped сервер"
                            WaveSource.VK -> "Нужен ваш access token ниже"
                            WaveSource.LOCAL -> "Локальные файлы"
                        },
                        checked = source in settings.waveSources,
                        enabled = true,
                        onCheckedChange = { enabled -> onWaveSource(source, enabled) },
                    )
                    if (index != WaveSource.remote.lastIndex) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.divider),
                        )
                    }
                }
            }
        }
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                SettingTextField(
                    label = "Сервер YouTube",
                    hint = "https://inv.nadeko.net",
                    value = youtubeInstance,
                    onValueChange = {
                        youtubeInstance = it
                        onWaveYoutubeInstance(it)
                    },
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.divider),
                )
                SettingTextField(
                    label = "VK access token",
                    hint = "Вставьте токен с правами на аудио",
                    value = vkToken,
                    secret = true,
                    onValueChange = {
                        vkToken = it
                        onWaveVkToken(it)
                    },
                )
            }
        }
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Waves,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Как работает «Волна»",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "«Волна» — бесконечный поток треков. Приложение обращается к " +
                        "открытым интерфейсам SoundCloud, YouTube и VK: это не официальные " +
                        "API, они могут перестать отвечать или ограничить доступ. YouTube " +
                        "работает через указанный вами сервер Invidious/Piped, VK требует " +
                        "ваш личный токен. Лайки и дизлайки влияют на подбор.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(12.dp))
                GlassPill(
                    text = "Сбросить предпочтения",
                    selected = false,
                    icon = Icons.Rounded.Refresh,
                    onClick = onClearWaveFeedback,
                )
            }
        }

        item {
            SectionHeader(title = "О приложении")
        }
        item {
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
                            imageVector = Icons.Rounded.Info,
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
                            text = "muzik ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = "Локальный музыкальный плеер",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "muzik играет музыку из памяти устройства. Треки «Волны» " +
                        "подгружаются из открытых интерфейсов SoundCloud, YouTube и VK " +
                        "только пока вы слушаете волну.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassPill(
                        text = "Настройки приложения",
                        selected = false,
                        icon = Icons.Rounded.Settings,
                        onClick = onOpenAppSettings,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = LocalMuzikColors.current
    val contentAlpha = if (enabled) 1f else 0.45f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.accent.copy(alpha = contentAlpha),
            modifier = Modifier.size(22.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary.copy(alpha = contentAlpha),
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary.copy(alpha = contentAlpha),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onAccent,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = colors.textSecondary,
                uncheckedTrackColor = colors.surfaceVariant,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun SettingTextField(
    label: String,
    hint: String,
    value: String,
    onValueChange: (String) -> Unit,
    secret: Boolean = false,
) {
    val colors = LocalMuzikColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            },
            visualTransformation = if (secret) {
                PasswordVisualTransformation()
            } else {
                androidx.compose.ui.text.input.VisualTransformation.None
            },
        )
    }
}
