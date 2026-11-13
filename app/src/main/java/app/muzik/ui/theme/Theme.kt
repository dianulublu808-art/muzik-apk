package app.muzik.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import app.muzik.data.settings.MuzikSettings
import app.muzik.data.settings.ThemeMode
import android.app.Activity
import android.os.Build

val LocalMuzikColors = staticCompositionLocalOf {
    muzikColors(dark = true, accent = Color(0xFFB68CFF), accentSecondary = Color(0xFFFF78C8))
}

val MuzikTypography = Typography(
    displaySmall = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 27.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 27.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 21.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

val MuzikShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

@Composable
fun MuzikTheme(
    settings: MuzikSettings,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val dynamicScheme = if (
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    ) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        null
    }

    val accent = dynamicScheme?.primary ?: settings.accent.primary(darkTheme)
    val secondary = dynamicScheme?.tertiary ?: settings.accent.secondary(darkTheme)
    val colors = remember(darkTheme, accent, secondary) {
        muzikColors(darkTheme, accent, secondary)
    }
    val colorScheme = remember(colors, dynamicScheme) {
        dynamicScheme ?: buildColorScheme(colors)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalMuzikColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MuzikTypography,
            shapes = MuzikShapes,
            content = content,
        )
    }
}

private fun buildColorScheme(colors: MuzikColors) = if (colors.isDark) {
    darkColorScheme(
        primary = colors.accent,
        onPrimary = Color(0xFF181022),
        primaryContainer = lerp(colors.surface, colors.accent, 0.22f),
        onPrimaryContainer = colors.textPrimary,
        secondary = colors.accentSecondary,
        onSecondary = Color(0xFF181022),
        secondaryContainer = lerp(colors.surface, colors.accentSecondary, 0.18f),
        onSecondaryContainer = colors.textPrimary,
        tertiary = colors.accentSecondary,
        background = colors.background,
        onBackground = colors.textPrimary,
        surface = colors.surface,
        onSurface = colors.textPrimary,
        surfaceVariant = colors.surfaceVariant,
        onSurfaceVariant = colors.textSecondary,
        outline = colors.textSecondary.copy(alpha = 0.55f),
        outlineVariant = colors.textSecondary.copy(alpha = 0.22f),
        error = Color(0xFFFF8A8A),
        onError = Color(0xFF2B0A0A),
        scrim = Color.Black,
    )
} else {
    lightColorScheme(
        primary = colors.accent,
        onPrimary = Color.White,
        primaryContainer = lerp(colors.surface, colors.accent, 0.16f),
        onPrimaryContainer = colors.textPrimary,
        secondary = colors.accentSecondary,
        onSecondary = Color.White,
        secondaryContainer = lerp(colors.surface, colors.accentSecondary, 0.14f),
        onSecondaryContainer = colors.textPrimary,
        tertiary = colors.accentSecondary,
        background = colors.background,
        onBackground = colors.textPrimary,
        surface = colors.surface,
        onSurface = colors.textPrimary,
        surfaceVariant = colors.surfaceVariant,
        onSurfaceVariant = colors.textSecondary,
        outline = colors.textSecondary.copy(alpha = 0.55f),
        outlineVariant = colors.textSecondary.copy(alpha = 0.20f),
        error = Color(0xFFB3261E),
        onError = Color.White,
        scrim = Color.Black,
    )
}
