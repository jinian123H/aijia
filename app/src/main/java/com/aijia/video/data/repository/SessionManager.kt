package com.aijia.video.data.repository

import android.content.Context
import com.aijia.video.SecurityCheck
import com.aijia.video.data.model.AppPermission
import com.aijia.video.data.model.UserInfo
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

object AppThemeDefaults {
    const val NAVIGATION_FLOATING = "floating"
    const val NAVIGATION_FIXED = "fixed"
    const val THEME_CLASSIC = "classic"
    const val THEME_GOLDEN = "golden"
    const val DARK_MODE_FOLLOW_SYSTEM = "follow_system"
    const val DARK_MODE_LIGHT = "light"
    const val DARK_MODE_DARK = "dark"
    const val FONT_SYSTEM = "system"
    const val FONT_1 = "font_1"
    const val FONT_2 = "font_2"
    const val FONT_3 = "font_3"
}

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context,
    private val gson: Gson
) {
    private val prefs = context.getSharedPreferences("aijia_session", Context.MODE_PRIVATE)
    private val _permissionFlow = MutableStateFlow(readPermission() ?: AppPermission.guestDefault())
    val permissionFlow: StateFlow<AppPermission> = _permissionFlow.asStateFlow()

    // 导航样式状态流
    private val _navigationStyleFlow = MutableStateFlow(getNavigationStyle())
    val navigationStyleFlow: StateFlow<String> = _navigationStyleFlow.asStateFlow()

    // 颜色主题状态流
    private val _themeModeFlow = MutableStateFlow(getThemeMode())
    val themeModeFlow: StateFlow<String> = _themeModeFlow.asStateFlow()

    // 暗黑模式状态流
    private val _darkModeFlow = MutableStateFlow(getDarkMode())
    val darkModeFlow: StateFlow<String> = _darkModeFlow.asStateFlow()

    // 字体模式状态流
    private val _fontModeFlow = MutableStateFlow(getFontMode())
    val fontModeFlow: StateFlow<String> = _fontModeFlow.asStateFlow()

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun saveSession(token: String, userInfo: UserInfo?) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            if (userInfo != null) {
                putString(KEY_USER_INFO, gson.toJson(userInfo))
            } else {
                remove(KEY_USER_INFO)
            }
        }.apply()
    }

    fun getUserInfo(): UserInfo? {
        val raw = prefs.getString(KEY_USER_INFO, null) ?: return null
        return runCatching { gson.fromJson(raw, UserInfo::class.java) }.getOrNull()
    }

    fun updateUserInfo(userInfo: UserInfo) {
        prefs.edit().putString(KEY_USER_INFO, gson.toJson(userInfo)).apply()
    }

    fun savePermission(permission: AppPermission) {
        if (SecurityCheck.compromised) {
            _permissionFlow.value = AppPermission.guestDefault()
            return
        }
        val fp = getToken()?.takeLast(16) ?: ""
        val wrapper = mapOf("perm" to gson.toJson(permission), "fp" to fp)
        prefs.edit().putString(KEY_PERMISSION, gson.toJson(wrapper)).apply()
        _permissionFlow.value = permission
    }

    fun getPermission(): AppPermission {
        return readPermission() ?: AppPermission.guestDefault()
    }

    // ========== 导航栏样式管理 ==========
    fun saveNavigationStyle(style: String) {
        val normalizedStyle = when (style) {
            AppThemeDefaults.NAVIGATION_FIXED -> AppThemeDefaults.NAVIGATION_FIXED
            else -> AppThemeDefaults.NAVIGATION_FLOATING
        }
        prefs.edit().putString(KEY_NAVIGATION_STYLE, normalizedStyle).apply()
        _navigationStyleFlow.value = normalizedStyle
    }

    fun getNavigationStyle(): String {
        return when (prefs.getString(KEY_NAVIGATION_STYLE, AppThemeDefaults.NAVIGATION_FLOATING)) {
            AppThemeDefaults.NAVIGATION_FIXED -> AppThemeDefaults.NAVIGATION_FIXED
            else -> AppThemeDefaults.NAVIGATION_FLOATING
        }
    }

    // ========== 颜色主题管理 ==========
    fun saveThemeMode(mode: String) {
        val normalizedMode = when (mode) {
            AppThemeDefaults.THEME_GOLDEN -> AppThemeDefaults.THEME_GOLDEN
            else -> AppThemeDefaults.THEME_CLASSIC
        }
        prefs.edit().putString(KEY_THEME_MODE, normalizedMode).apply()
        _themeModeFlow.value = normalizedMode
    }

    fun getThemeMode(): String {
        return when (prefs.getString(KEY_THEME_MODE, AppThemeDefaults.THEME_GOLDEN)) {
            AppThemeDefaults.THEME_CLASSIC -> AppThemeDefaults.THEME_CLASSIC
            else -> AppThemeDefaults.THEME_GOLDEN
        }
    }

    // ========== 暗黑模式管理 ==========
    fun saveDarkMode(mode: String) {
        val normalizedMode = when (mode) {
            AppThemeDefaults.DARK_MODE_DARK -> AppThemeDefaults.DARK_MODE_DARK
            else -> AppThemeDefaults.DARK_MODE_LIGHT
        }
        prefs.edit().putString(KEY_DARK_MODE, normalizedMode).apply()
        _darkModeFlow.value = normalizedMode
    }

    fun getDarkMode(): String {
        return when (prefs.getString(KEY_DARK_MODE, AppThemeDefaults.DARK_MODE_LIGHT)) {
            AppThemeDefaults.DARK_MODE_DARK -> AppThemeDefaults.DARK_MODE_DARK
            else -> AppThemeDefaults.DARK_MODE_LIGHT
        }
    }

    // ========== 字体模式管理 ==========
    fun saveFontMode(mode: String) {
        val normalizedMode = when (mode) {
            AppThemeDefaults.FONT_1 -> AppThemeDefaults.FONT_1
            AppThemeDefaults.FONT_2 -> AppThemeDefaults.FONT_2
            AppThemeDefaults.FONT_3 -> AppThemeDefaults.FONT_3
            else -> AppThemeDefaults.FONT_SYSTEM
        }
        prefs.edit().putString(KEY_FONT_MODE, normalizedMode).apply()
        _fontModeFlow.value = normalizedMode
    }

    fun getFontMode(): String {
        return when (prefs.getString(KEY_FONT_MODE, AppThemeDefaults.FONT_SYSTEM)) {
            AppThemeDefaults.FONT_1 -> AppThemeDefaults.FONT_1
            AppThemeDefaults.FONT_2 -> AppThemeDefaults.FONT_2
            AppThemeDefaults.FONT_3 -> AppThemeDefaults.FONT_3
            else -> AppThemeDefaults.FONT_SYSTEM
        }
    }

    fun clearSession() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_USER_INFO).remove(KEY_PERMISSION).apply()
        _permissionFlow.value = AppPermission.guestDefault()
    }

    private fun readPermission(): AppPermission? {
        if (SecurityCheck.compromised) return AppPermission.guestDefault()
        val raw = prefs.getString(KEY_PERMISSION, null) ?: return null
        return runCatching {
            @Suppress("UNCHECKED_CAST")
            val wrapper = gson.fromJson(raw, Map::class.java) as Map<String, String>
            val fp = wrapper["fp"] ?: return null
            val token = getToken() ?: ""
            if (token.takeLast(16) != fp) return null
            gson.fromJson(wrapper["perm"], AppPermission::class.java)
        }.getOrNull()
    }

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_INFO = "user_info"
        private const val KEY_PERMISSION = "permission"
        private const val KEY_NAVIGATION_STYLE = "navigation_style"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_FONT_MODE = "font_mode"
    }
}
