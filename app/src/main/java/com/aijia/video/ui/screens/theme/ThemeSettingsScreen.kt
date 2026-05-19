package com.aijia.video.ui.screens.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aijia.video.data.repository.AppThemeDefaults
import com.aijia.video.ui.screens.profile.ProfileViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val navigationStyle by viewModel.navigationStyle.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val darkMode by viewModel.darkMode.collectAsStateWithLifecycle()
    val fontMode by viewModel.fontMode.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "主题设置",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ThemeSettingSection(
                title = "颜色与暗黑",
                icon = Icons.Default.DarkMode
            ) {
                ThemeSettingGroup(
                    title = "主题配色",
                    options = listOf(
                        ThemeOption(
                            title = "经典主题",
                            selected = themeMode == AppThemeDefaults.THEME_CLASSIC,
                            onClick = { viewModel.saveThemeMode(AppThemeDefaults.THEME_CLASSIC) }
                        ),
                        ThemeOption(
                            title = "紫金主题",
                            selected = themeMode == AppThemeDefaults.THEME_GOLDEN,
                            onClick = { viewModel.saveThemeMode(AppThemeDefaults.THEME_GOLDEN) }
                        )
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                ThemeSettingGroup(
                    title = "昼夜模式",
                    options = listOf(
                        ThemeOption(
                            title = "浅色模式",
                            selected = darkMode == AppThemeDefaults.DARK_MODE_LIGHT,
                            onClick = { viewModel.saveDarkMode(AppThemeDefaults.DARK_MODE_LIGHT) }
                        ),
                        ThemeOption(
                            title = "暗黑模式",
                            selected = darkMode == AppThemeDefaults.DARK_MODE_DARK,
                            onClick = { viewModel.saveDarkMode(AppThemeDefaults.DARK_MODE_DARK) }
                        )
                    )
                )
            }

            ThemeSettingSection(
                title = "字体管理",
                icon = Icons.Default.TextFields
            ) {
                ThemeSettingGroup(
                    title = "字体方案",
                    options = listOf(
                        ThemeOption(
                            title = "系统字体",
                            selected = fontMode == AppThemeDefaults.FONT_SYSTEM,
                            onClick = { viewModel.saveFontMode(AppThemeDefaults.FONT_SYSTEM) }
                        ),
                        ThemeOption(
                            title = "刀隶体",
                            selected = fontMode == AppThemeDefaults.FONT_1,
                            onClick = { viewModel.saveFontMode(AppThemeDefaults.FONT_1) }
                        ),
                        ThemeOption(
                            title = "楷体",
                            selected = fontMode == AppThemeDefaults.FONT_2,
                            onClick = { viewModel.saveFontMode(AppThemeDefaults.FONT_2) }
                        ),
                        ThemeOption(
                            title = "宋体",
                            selected = fontMode == AppThemeDefaults.FONT_3,
                            onClick = { viewModel.saveFontMode(AppThemeDefaults.FONT_3) }
                        )
                    )
                )
            }
        }
    }
}

@Composable
private fun ThemeSettingGroup(
    title: String,
    options: List<ThemeOption>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            options.chunked(2).forEach { rowOptions ->
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowOptions.forEach { option ->
                        ThemeOptionChip(
                            title = option.title,
                            selected = option.selected,
                            onClick = option.onClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowOptions.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSettingSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(icon, contentDescription = title)
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun ThemeOptionChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

data class ThemeOption(
    val title: String,
    val selected: Boolean,
    val onClick: () -> Unit
)
