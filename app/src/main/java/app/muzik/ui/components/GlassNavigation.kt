package app.muzik.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.muzik.ui.theme.LocalMuzikColors

data class BottomBarItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val bottomBarItems = listOf(
    BottomBarItem("home", "Главная", Icons.Rounded.Home),
    BottomBarItem("wave", "Волна", Icons.Rounded.Waves),
    BottomBarItem("search", "Поиск", Icons.Rounded.Search),
    BottomBarItem("library", "Медиатека", Icons.Rounded.LibraryMusic),
    BottomBarItem("settings", "Настройки", Icons.Rounded.Settings),
)

@Composable
fun MuzikBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMuzikColors.current
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        strong = true,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(5.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            bottomBarItems.forEach { item ->
                val selected = isRouteSelected(currentRoute, item.route)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(21.dp))
                        .background(
                            if (selected) {
                                colors.accent.copy(alpha = 0.16f)
                            } else {
                                Color.Transparent
                            },
                        )
                        .clickable { onNavigate(item.route) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (selected) colors.accent else colors.textSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = if (selected) colors.accent else colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

fun isRouteSelected(currentRoute: String, tabRoute: String): Boolean {
    if (currentRoute == tabRoute) return true
    return when (tabRoute) {
        "library" -> currentRoute == "favorites" ||
            currentRoute.startsWith("playlist/") ||
            currentRoute.startsWith("album/") ||
            currentRoute.startsWith("artist/")
        "home" -> currentRoute == "player"
        else -> false
    }
}
