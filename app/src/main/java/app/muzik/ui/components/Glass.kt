package app.muzik.ui.components

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.muzik.ui.theme.LocalMuzikColors

@Composable
fun AuroraBackground(
    animated: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalMuzikColors.current
    val transition = rememberInfiniteTransition(label = "aurora")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "drift",
    )
    val shift = if (animated) drift else 0.4f
    val blurModifier = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.blur(100.dp, BlurredEdgeTreatment.Unbounded)
        } else {
            Modifier
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.backgroundTop,
                        colors.background,
                        colors.backgroundBottom,
                    ),
                ),
            ),
    ) {
        Box(
            Modifier
                .size(430.dp)
                .offset(x = (-130).dp, y = (-40).dp)
                .graphicsLayer {
                    translationX = shift * 34f
                    translationY = shift * 22f
                }
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(colors.orb1, Color.Transparent)))
                .then(blurModifier),
        )
        Box(
            Modifier
                .size(390.dp)
                .offset(x = 190.dp, y = 170.dp)
                .graphicsLayer {
                    translationX = shift * -26f
                    translationY = shift * 34f
                }
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(colors.orb2, Color.Transparent)))
                .then(blurModifier),
        )
        Box(
            Modifier
                .size(360.dp)
                .offset(x = 70.dp, y = 470.dp)
                .graphicsLayer {
                    translationX = shift * 22f
                    translationY = shift * -28f
                }
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(colors.orb3, Color.Transparent)))
                .then(blurModifier),
        )
        content()
    }
}

@Composable
fun Modifier.glassSurface(shape: Shape, strong: Boolean = false): Modifier {
    val colors = LocalMuzikColors.current
    val fill = if (strong) colors.glassStrong else colors.glass
    return this
        .shadow(
            elevation = 18.dp,
            shape = shape,
            ambientColor = colors.glassShadow,
            spotColor = colors.glassShadow,
        )
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(colors.glassHighlight, fill, fill),
            ),
        )
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(colors.glassBorderTop, colors.glassBorderBottom),
            ),
            shape = shape,
        )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    strong: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .glassSurface(shape, strong)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun AccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = LocalMuzikColors.current
    val alpha = if (enabled) 1f else 0.35f
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        colors.accent.copy(alpha = alpha),
                        colors.accentSecondary.copy(alpha = alpha),
                    ),
                ),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.onAccent.copy(alpha = alpha),
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = text,
            color = colors.onAccent.copy(alpha = alpha),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    tint: Color = LocalMuzikColors.current.textPrimary,
    enabled: Boolean = true,
) {
    val colors = LocalMuzikColors.current
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.glassStrong)
            .border(1.dp, colors.glassBorderTop.copy(alpha = 0.35f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint.copy(alpha = if (enabled) 1f else 0.35f),
            modifier = Modifier.size(size * 0.48f),
        )
    }
}

@Composable
fun GlassPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = LocalMuzikColors.current
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(if (selected) colors.accent.copy(alpha = 0.18f) else colors.glass)
            .border(
                1.dp,
                if (selected) colors.accent.copy(alpha = 0.55f) else colors.glassBorderBottom,
                CircleShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) colors.accent else colors.textSecondary,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = text,
            color = if (selected) colors.accent else colors.textSecondary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    actionIcon: ImageVector? = null,
) {
    val colors = LocalMuzikColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        if (onAction != null && actionText != null) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (actionIcon != null) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = actionText,
                    color = colors.accent,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = LocalMuzikColors.current
    GlassCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(22.dp)) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            AccentButton(text = actionText, onClick = onAction)
        }
    }
}

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = LocalMuzikColors.current
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall,
                color = colors.textPrimary,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun ThinDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LocalMuzikColors.current.divider),
    )
}

@Composable
fun StatChip(text: String, modifier: Modifier = Modifier) {
    val colors = LocalMuzikColors.current
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(colors.glassStrong)
            .border(1.dp, colors.glassBorderBottom, CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = text,
            color = colors.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun HorizontalSpacer(width: Dp) {
    Spacer(Modifier.width(width))
}
