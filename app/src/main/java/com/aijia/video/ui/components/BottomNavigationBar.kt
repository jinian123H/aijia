package com.aijia.video.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.aijia.video.R

/**
 * 底部导航栏项目
 */
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val fixedIconRes: Int? = null,
    val fixedSelectedIconRes: Int? = null,
    val floatingIconRes: Int? = null,
    val floatingSelectedIconRes: Int? = null
) {
    object Home : BottomNavItem(
        route = "home",
        title = "首页",
        icon = Icons.Outlined.Home,
        selectedIcon = Icons.Filled.Home,
        fixedIconRes = R.drawable.sy,
        floatingIconRes = R.drawable.sy2
    )

    object Rank : BottomNavItem(
        route = "rank",
        title = "排行榜",
        icon = Icons.Outlined.Star,
        selectedIcon = Icons.Filled.Star,
        fixedIconRes = R.drawable.phb,
        floatingIconRes = R.drawable.phb2
    )

    object ShortVideo : BottomNavItem(
        route = "short_video",
        title = "短剧",
        icon = Icons.Outlined.PlayArrow,
        selectedIcon = Icons.Filled.PlayArrow,
        fixedIconRes = R.drawable.dj,
        floatingIconRes = R.drawable.dj2
    )

    object Profile : BottomNavItem(
        route = "profile",
        title = "我的",
        icon = Icons.Outlined.Person,
        selectedIcon = Icons.Filled.Person,
        fixedIconRes = R.drawable.wd,
        floatingIconRes = R.drawable.wd2
    )
}

/**
 * 底部导航栏
 */
@Composable
fun BottomNavigationBar(
    navController: NavController,
    isFixed: Boolean,
    items: List<BottomNavItem> = listOf(
        BottomNavItem.Home,
        BottomNavItem.Rank,
        BottomNavItem.ShortVideo,
        BottomNavItem.Profile
    )
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val containerModifier = if (isFixed) {
        Modifier.fillMaxWidth()
    } else {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 30.dp)
    }

    val shape = if (isFixed) {
        RoundedCornerShape(0.dp)
    } else {
        RoundedCornerShape(28.dp)
    }

    val borderColor = if (isFixed) {
        MaterialTheme.colorScheme.outlineVariant
    } else {
        Color.White.copy(alpha = 0.18f)
    }

    val contentModifier = if (isFixed) {
        Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
    }

    Surface(
        modifier = containerModifier,
        shape = shape,
        tonalElevation = if (isFixed) 4.dp else 2.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isFixed) 0.5.dp else 1.dp,
            color = borderColor
        )
    ) {
        Row(
            modifier = contentModifier,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                BottomNavigationItemChip(
                    item = item,
                    selected = currentRoute == item.route || (item.route == "short_video" && currentRoute?.startsWith("short_video") == true),
                    isFixed = isFixed,
                    onClick = {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun RowScope.BottomNavigationItemChip(
    item: BottomNavItem,
    selected: Boolean,
    isFixed: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .weight(1f)
            .wrapContentWidth()
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        shape = RoundedCornerShape(if (isFixed) 16.dp else 22.dp),
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = if (isFixed) 0.2f else 0.12f)
            )
        } else {
            null
        },
        color = if (selected) {
            if (isFixed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary.copy(alpha = 0.11f)
        } else {
            if (isFixed) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0f)
        }
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (isFixed) 10.dp else 8.dp,
                vertical = if (isFixed) 8.dp else 6.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val iconRes = if (isFixed) {
                if (selected && item.fixedSelectedIconRes != null) item.fixedSelectedIconRes else item.fixedIconRes
            } else {
                if (selected && item.floatingSelectedIconRes != null) item.floatingSelectedIconRes else item.floatingIconRes
            }
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = item.title,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(if (isFixed) 24.dp else 20.dp)
                )
            } else {
                Icon(
                    imageVector = if (selected) item.selectedIcon else item.icon,
                    contentDescription = item.title,
                    tint = contentColor,
                    modifier = Modifier.size(if (isFixed) 20.dp else 18.dp)
                )
            }
            Text(
                text = item.title,
                color = contentColor,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = if (isFixed) 4.dp else 2.dp)
            )
        }
    }
}
