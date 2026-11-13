package app.muzik.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import app.muzik.data.settings.AccentColor

@Immutable
data class MuzikColors(
    val isDark: Boolean,
    val background: Color,
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val accent: Color,
    val accentSecondary: Color,
    val onAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val glass: Color,
    val glassStrong: Color,
    val glassHighlight: Color,
    val glassBorderTop: Color,
    val glassBorderBottom: Color,
    val glassShadow: Color,
    val rowBackground: Color,
    val divider: Color,
    val orb1: Color,
    val orb2: Color,
    val orb3: Color,
)

fun AccentColor.primary(dark: Boolean): Color = when (this) {
    AccentColor.PURPLE -> if (dark) Color(0xFFB68CFF) else Color(0xFF7042C8)
    AccentColor.PINK -> if (dark) Color(0xFFFF8AD0) else Color(0xFFB3377F)
    AccentColor.BLUE -> if (dark) Color(0xFF85B8FF) else Color(0xFF2F6BC4)
    AccentColor.GREEN -> if (dark) Color(0xFF7FE3B0) else Color(0xFF1F7F58)
    AccentColor.ORANGE -> if (dark) Color(0xFFFFB27A) else Color(0xFFA85A1F)
}

fun AccentColor.secondary(dark: Boolean): Color = when (this) {
    AccentColor.PURPLE -> if (dark) Color(0xFFFF78C8) else Color(0xFFC13E86)
    AccentColor.PINK -> if (dark) Color(0xFFFFB07A) else Color(0xFFB45A1F)
    AccentColor.BLUE -> if (dark) Color(0xFFB68CFF) else Color(0xFF7042C8)
    AccentColor.GREEN -> if (dark) Color(0xFF9FD4FF) else Color(0xFF2F6BC4)
    AccentColor.ORANGE -> if (dark) Color(0xFFFF8A9B) else Color(0xFFB3377F)
}

fun AccentColor.preview(): Color = primary(dark = true)

fun muzikColors(dark: Boolean, accent: Color, accentSecondary: Color): MuzikColors =
    if (dark) {
        MuzikColors(
            isDark = true,
            background = Color(0xFF06040B),
            backgroundTop = Color(0xFF100A1C),
            backgroundBottom = Color(0xFF050309),
            surface = Color(0xFF151020),
            surfaceVariant = Color(0xFF211931),
            accent = accent,
            accentSecondary = accentSecondary,
            onAccent = Color(0xFF181022),
            textPrimary = Color(0xFFF7F3FF),
            textSecondary = Color(0xFFA79FBA),
            glass = Color.White.copy(alpha = 0.055f),
            glassStrong = Color.White.copy(alpha = 0.095f),
            glassHighlight = Color.White.copy(alpha = 0.15f),
            glassBorderTop = Color.White.copy(alpha = 0.30f),
            glassBorderBottom = Color.White.copy(alpha = 0.04f),
            glassShadow = Color.Black.copy(alpha = 0.45f),
            rowBackground = Color.White.copy(alpha = 0.035f),
            divider = Color.White.copy(alpha = 0.08f),
            orb1 = accent.copy(alpha = 0.55f),
            orb2 = accentSecondary.copy(alpha = 0.38f),
            orb3 = Color(0xFF4A6FFF).copy(alpha = 0.30f),
        )
    } else {
        MuzikColors(
            isDark = false,
            background = Color(0xFFF5F1FB),
            backgroundTop = Color(0xFFFFFFFF),
            backgroundBottom = Color(0xFFEDE6F7),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFF0EAF8),
            accent = accent,
            accentSecondary = accentSecondary,
            onAccent = Color.White,
            textPrimary = Color(0xFF1A1426),
            textSecondary = Color(0xFF6B617A),
            glass = Color.White.copy(alpha = 0.55f),
            glassStrong = Color.White.copy(alpha = 0.78f),
            glassHighlight = Color.White.copy(alpha = 0.96f),
            glassBorderTop = Color.White.copy(alpha = 0.95f),
            glassBorderBottom = Color.White.copy(alpha = 0.20f),
            glassShadow = Color(0xFF3A2A55).copy(alpha = 0.16f),
            rowBackground = Color.White.copy(alpha = 0.46f),
            divider = Color(0xFF1A1426).copy(alpha = 0.08f),
            orb1 = accent.copy(alpha = 0.35f),
            orb2 = accentSecondary.copy(alpha = 0.26f),
            orb3 = Color(0xFF7FA7FF).copy(alpha = 0.22f),
        )
    }
