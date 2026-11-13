package app.muzik.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.muzik.ui.theme.LocalMuzikColors
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent

private val fallbackPalettes = listOf(
    listOf(Color(0xFF6A4C93), Color(0xFFB68CFF)),
    listOf(Color(0xFF7B4B72), Color(0xFFFF8AD0)),
    listOf(Color(0xFF315B78), Color(0xFF85B8FF)),
    listOf(Color(0xFF477B69), Color(0xFF7FE3B0)),
    listOf(Color(0xFF9A5B3B), Color(0xFFFFB27A)),
    listOf(Color(0xFF4C3B8F), Color(0xFF9FD4FF)),
    listOf(Color(0xFF7A3050), Color(0xFFFFB0B8)),
)

@Composable
fun Artwork(
    model: Any?,
    seed: Long,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    iconSize: Dp = 26.dp,
) {
    val colors = LocalMuzikColors.current
    val palette = remember(seed) {
        fallbackPalettes[(seed.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }) % fallbackPalettes.size]
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.linearGradient(palette)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.88f),
            modifier = Modifier.size(iconSize),
        )
        if (model != null) {
            SubcomposeAsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            ) {
                if (painter.state is AsyncImagePainter.State.Success) {
                    SubcomposeAsyncImageContent()
                }
            }
        }
    }
}

@Composable
fun ArtistAvatar(
    name: String,
    seed: Long,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
) {
    val palette = remember(seed) {
        fallbackPalettes[(seed.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }) % fallbackPalettes.size]
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(Brush.linearGradient(palette)),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Text(
            text = name.firstOrNull()?.uppercase() ?: "?",
            color = Color.White,
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
        )
    }
}
