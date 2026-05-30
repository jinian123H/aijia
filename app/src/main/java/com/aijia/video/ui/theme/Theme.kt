package com.aijia.video.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.aijia.video.data.repository.AppThemeDefaults

// 经典主题 - Light
private val ClassicLightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = Color(0xFF0D47A1),
    secondary = Color(0xFFFF9800),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF3E0),
    onSecondaryContainer = Color(0xFFE65100),
    tertiary = Color(0xFF4CAF50),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8F5E8),
    onTertiaryContainer = Color(0xFF1B5E20),
    error = Color(0xFFF44336),
    onError = Color.White,
    background = Color(0xFFFFF8E7),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFF8E7),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = Color(0xFF90CAF9)
)

// 经典主题 - Dark
private val ClassicDarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF0D47A1),
    primaryContainer = Color(0xFF1565C0),
    onPrimaryContainer = Color(0xFFE3F2FD),
    secondary = Color(0xFFFFCC80),
    onSecondary = Color(0xFFE65100),
    secondaryContainer = Color(0xFFFF9800),
    onSecondaryContainer = Color(0xFFFFF3E0),
    tertiary = Color(0xFF81C784),
    onTertiary = Color(0xFF1B5E20),
    tertiaryContainer = Color(0xFF4CAF50),
    onTertiaryContainer = Color(0xFFE8F5E8),
    error = Color(0xFFCF6679),
    onError = Color(0xFF690005),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF313033),
    inversePrimary = Color(0xFF1976D2)
)

// 主题二 - Light
private val GoldenLightColorScheme = lightColorScheme(
    primary = Color(0xFF6F46D9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEE4FF),
    onPrimaryContainer = Color(0xFF2E146A),
    secondary = Color(0xFFC98B1E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE8BE),
    onSecondaryContainer = Color(0xFF452B00),
    tertiary = Color(0xFFAF5C9C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD6F2),
    onTertiaryContainer = Color(0xFF3F002F),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    background = Color(0xFFFFF8E7),
    onBackground = Color(0xFF1D1A24),
    surface = Color(0xFFFFF8E7),
    onSurface = Color(0xFF1D1A24),
    surfaceVariant = Color(0xFFEBE0F3),
    onSurfaceVariant = Color(0xFF4B4456),
    outline = Color(0xFF7C7488),
    outlineVariant = Color(0xFFCDC3D5),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF322F39),
    inverseOnSurface = Color(0xFFF5EFFA),
    inversePrimary = Color(0xFFD5BCFF)
)

// 主题二 - Dark
private val GoldenDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD5BCFF),
    onPrimary = Color(0xFF3E1E9D),
    primaryContainer = Color(0xFF5730BF),
    onPrimaryContainer = Color(0xFFEEE4FF),
    secondary = Color(0xFFF0C978),
    onSecondary = Color(0xFF3E2D00),
    secondaryContainer = Color(0xFF5A4300),
    onSecondaryContainer = Color(0xFFFFE8BE),
    tertiary = Color(0xFFFFACE7),
    onTertiary = Color(0xFF5C114A),
    tertiaryContainer = Color(0xFF7A2A65),
    onTertiaryContainer = Color(0xFFFFD6F2),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    background = Color(0xFF15121B),
    onBackground = Color(0xFFE8E0EC),
    surface = Color(0xFF15121B),
    onSurface = Color(0xFFE8E0EC),
    surfaceVariant = Color(0xFF4B4456),
    onSurfaceVariant = Color(0xFFCDC3D5),
    outline = Color(0xFF968D9F),
    outlineVariant = Color(0xFF4B4456),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE8E0EC),
    inverseOnSurface = Color(0xFF322F39),
    inversePrimary = Color(0xFF6F46D9)
)

private fun resolveDarkTheme(darkMode: String, systemDarkTheme: Boolean): Boolean {
    return when (darkMode) {
        AppThemeDefaults.DARK_MODE_DARK -> true
        AppThemeDefaults.DARK_MODE_LIGHT -> false
        else -> systemDarkTheme
    }
}

/**
 * Aijia视频应用主题
 */
@Composable
fun AijiaVideoTheme(
    themeMode: String = AppThemeDefaults.THEME_GOLDEN,
    darkMode: String = AppThemeDefaults.DARK_MODE_FOLLOW_SYSTEM,
    fontMode: String = AppThemeDefaults.FONT_SYSTEM,
    content: @Composable () -> Unit
) {
    val isDarkTheme = resolveDarkTheme(
        darkMode = darkMode,
        systemDarkTheme = isSystemInDarkTheme()
    )

    val colorScheme = when {
        themeMode == AppThemeDefaults.THEME_GOLDEN && isDarkTheme -> GoldenDarkColorScheme
        themeMode == AppThemeDefaults.THEME_GOLDEN -> GoldenLightColorScheme
        isDarkTheme -> ClassicDarkColorScheme
        else -> ClassicLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = appTypography(fontMode),
        content = content
    )
}
