package com.aijia.video.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Message

import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person



import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import androidx.compose.material3.ExperimentalMaterial3Api
import com.aijia.video.data.model.UserInfo
import com.aijia.video.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border

import com.aijia.video.data.repository.AppThemeDefaults
import com.aijia.video.data.repository.SessionManager
import com.aijia.video.R
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val userInfo: UserInfo? = null,
    val displayName: String = "未登录",
    val statusLabel: String = "",
    val endTimeText: String = "",
    val isLoggedIn: Boolean = false,
    val authMessage: String? = null,
    val phoneAuthConfig: com.aijia.video.data.model.PhoneAuthConfig? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val downloadRepository: com.aijia.video.data.repository.DownloadRepository,
    private val versionCacheManager: com.aijia.video.util.VersionCacheManager,
    private val adCacheManager: com.aijia.video.util.AdCacheManager,
    private val sessionManager: SessionManager,
    private val deviceInfo: com.aijia.video.data.repository.DeviceInfo,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    val favoriteVideos = videoRepository.getFavoriteVideos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val navigationStyle = sessionManager.navigationStyleFlow
    val themeMode = sessionManager.themeModeFlow
    val darkMode = sessionManager.darkModeFlow
    val fontMode = sessionManager.fontModeFlow
    fun loadProfile() {
        viewModelScope.launch {
            if (!videoRepository.isLoggedIn()) {
                _uiState.value = ProfileUiState(
                    isLoading = false,
                    userInfo = videoRepository.getCachedUserInfo(),
                    displayName = "未登录",
                    statusLabel = "",
                    endTimeText = "",
                    isLoggedIn = false,
                    authMessage = null,
                    errorMessage = "登录后可查看完整会员信息"
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = videoRepository.getCurrentUserInfo()
            _uiState.value = result.fold(
                onSuccess = { user ->
                    // 添加调试日志
                    android.util.Log.d("ProfileViewModel", "User info: $user")
                    android.util.Log.d("ProfileViewModel", "vipEndTime: ${user.vipEndTime}")
                    android.util.Log.d("ProfileViewModel", "Formatted end time: ${formatEndTime(user.vipEndTime)}")
                    ProfileUiState(
                        isLoading = false,
                        userInfo = user,
                        displayName = user.nickName?.takeIf { it.isNotBlank() }
                            ?: user.userName?.takeIf { it.isNotBlank() }
                            ?: "未设置昵称",
                        statusLabel = user.groupName?.takeIf { it.isNotBlank() } ?: "",
                        endTimeText = formatEndTime(user.vipEndTime),
                        isLoggedIn = true,
                        authMessage = null,
                        errorMessage = null
                    )
                },
                onFailure = {
                    android.util.Log.e("ProfileViewModel", "Failed to load user info", it)
                    ProfileUiState(
                        isLoading = false,
                        userInfo = null,
                        displayName = "未登录",
                        statusLabel = "",
                        endTimeText = "",
                        isLoggedIn = false,
                        authMessage = null,
                        errorMessage = "未登录或用户信息加载失败"
                    )
                }
            )
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, authMessage = null)
            val result = videoRepository.login(username, password)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        authMessage = "登录成功",
                        isLoggedIn = true
                    )
                    // 先通知UI返回，再加载用户信息和刷新权限
                    viewModelScope.launch {
                        delay(500)
                        loadProfile()
                        // 清除权限版本缓存+重新获取，确保拉取最新权限（非游客权限）
                        versionCacheManager.clearCache(com.aijia.video.util.VersionCacheManager.KEY_USER_PERMISSION)
                        videoRepository.getUserPermission()
                    }
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        authMessage = null,
                        errorMessage = it.message ?: "登录失败"
                    )
                }
            )
        }
    }

    fun register(username: String, password: String, confirmPassword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, authMessage = null)
            val result = videoRepository.register(username, password, confirmPassword)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        authMessage = "注册成功",
                        isLoggedIn = true
                    )
                    // 先通知UI返回，再加载用户信息和刷新权限
                    viewModelScope.launch {
                        delay(500)
                        loadProfile()
                        // 清除权限版本缓存+重新获取，确保拉取最新权限（非游客权限）
                        versionCacheManager.clearCache(com.aijia.video.util.VersionCacheManager.KEY_USER_PERMISSION)
                        videoRepository.getUserPermission()
                    }
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        authMessage = null,
                        errorMessage = it.message ?: "注册失败"
                    )
                }
            )
        }
    }

    fun oneclickLogin() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, authMessage = null)
            val result = videoRepository.oneclickLogin(deviceInfo.deviceId, deviceInfo.deviceName)
            result.fold(
                onSuccess = { data ->
                    if (data.token.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            authMessage = if (data.isNew) "注册成功" else "登录成功",
                            isLoggedIn = true
                        )
                        viewModelScope.launch {
                            delay(500)
                            loadProfile()
                            versionCacheManager.clearCache(com.aijia.video.util.VersionCacheManager.KEY_USER_PERMISSION)
                            videoRepository.getUserPermission()
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "一键登录失败，请重试"
                        )
                    }
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = it.message ?: "一键登录失败"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun loadAuthConfig() {
        viewModelScope.launch {
            val result = videoRepository.getPhoneAuthConfig()
            result.onSuccess { config ->
                _uiState.value = _uiState.value.copy(phoneAuthConfig = config)
            }
        }
    }

    fun logout() {
        videoRepository.logout()
        _uiState.value = ProfileUiState(
            isLoading = false,
            displayName = "未登录",
            statusLabel = "",
            endTimeText = "",
            isLoggedIn = false,
            authMessage = null,
            errorMessage = "已退出登录"
        )
    }

    fun clearAuthMessage() {
        _uiState.value = _uiState.value.copy(authMessage = null)
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun saveNavigationStyle(style: String) {
        sessionManager.saveNavigationStyle(style)
    }

    fun saveThemeMode(mode: String) {
        sessionManager.saveThemeMode(mode)
        // 经典主题→悬浮样式，紫金主题→固定样式
        val navStyle = when (mode) {
            AppThemeDefaults.THEME_GOLDEN -> AppThemeDefaults.NAVIGATION_FIXED
            else -> AppThemeDefaults.NAVIGATION_FLOATING
        }
        sessionManager.saveNavigationStyle(navStyle)
    }

    fun saveDarkMode(mode: String) {
        sessionManager.saveDarkMode(mode)
    }

    fun saveFontMode(mode: String) {
        sessionManager.saveFontMode(mode)
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun clearCache() {
        viewModelScope.launch {
            versionCacheManager.clearAllCache()
            adCacheManager.clearCache()
            // 清 Coil 内存缓存
            coil.Coil.imageLoader(context).memoryCache?.clear()
            // 清 Coil 磁盘缓存 + 应用缓存目录
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                coil.Coil.imageLoader(context).diskCache?.clear()
                context.cacheDir.deleteRecursively()
            }
        }
    }

    private fun formatEndTime(rawTime: Long?): String {
        if (rawTime == null || rawTime <= 0L) return ""
        val millis = if (rawTime < 10_000_000_000L) rawTime * 1000 else rawTime
        return "会员有效至 " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(Date(millis))
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToFavorites: () -> Unit,
    onNavigateToDownload: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToMessage: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {},
    onNavigateToCardExchange: () -> Unit = {},
    onNavigateToThemeSettings: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val favoriteVideos by viewModel.favoriteVideos.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isGoldenTheme = themeMode == AppThemeDefaults.THEME_GOLDEN
    val context = LocalContext.current
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // 获取本地版本（remember 避免每次重组访问 PackageManager）
    val localVersionCode = remember {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode
        }
    }
    

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    LaunchedEffect(uiState.authMessage) {
        if (!uiState.authMessage.isNullOrBlank()) {
            viewModel.clearAuthMessage()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.48f),
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiState.isLoggedIn) {
                                AvatarSection(
                                    avatar = uiState.userInfo?.avatar,
                                    displayName = uiState.displayName
                                )
                            } else {
                                // δ��¼ʱ��ʾĬ��ͷ������
                                Box(
                                    modifier = Modifier
                                        .size(78.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = "δ��¼",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = uiState.displayName,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isLoggedIn) 
                                        MaterialTheme.colorScheme.onBackground 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (uiState.statusLabel.isNotBlank()) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ) {
                                    Text(
                                        text = uiState.statusLabel.ifBlank { "未登录" },
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (uiState.statusLabel.isNotBlank()) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }

                        }

                        // 会员到期时间显示区域（头像区域以外）
                        if (uiState.isLoggedIn) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (uiState.endTimeText.isNotBlank()) uiState.endTimeText else "会员未设置到期时间",
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }

                        if (!uiState.isLoggedIn) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
                                                )
                                            )
                                        )
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 左侧：图标 + 标题
                                        Row(
                                            modifier = Modifier.weight(0.65f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CardGiftcard,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "注册送好礼",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        // 右侧：登录/注册按钮
                                        Button(
                                            onClick = { onNavigateToLogin() },
                                            modifier = Modifier
                                                .weight(0.45f)
                                                .height(48.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.inverseSurface
                                            )
                                        ) {
                                            Text(
                                                text = "登录 / 注册",
                                                maxLines = 1,
                                                softWrap = false,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 功能菜单列表
                    if (isGoldenTheme) {
                        // 紫金主题：4列网格布局
                        data class GridItem(
                            val painter: androidx.compose.ui.graphics.painter.Painter? = null,
                            val icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
                            val title: String,
                            val onClick: () -> Unit
                        )
                        val gridItems = listOf(
                            GridItem(painter = painterResource(R.drawable.ls1), title = "观看历史", onClick = onNavigateToHistory),
                            GridItem(painter = painterResource(R.drawable.sc1), title = "我的收藏", onClick = onNavigateToFavorites),
                            GridItem(painter = painterResource(R.drawable.xz1), title = "我的下载", onClick = onNavigateToDownload),
                            GridItem(painter = painterResource(R.drawable.fk1), title = "扫一扫", onClick = {
                                val intent = android.content.Intent(context, com.aijia.video.ui.activity.ScanQRActivity::class.java)
                                context.startActivity(intent)
                            }),
                            GridItem(painter = painterResource(R.drawable.km1), title = "卡密兑换", onClick = onNavigateToCardExchange),
                            GridItem(painter = painterResource(R.drawable.zt1), title = "主题设置", onClick = onNavigateToThemeSettings),
                            GridItem(painter = painterResource(R.drawable.fk1), title = "留言反馈", onClick = onNavigateToMessage),
                            GridItem(painter = painterResource(R.drawable.ql1), title = "清理缓存", onClick = { showClearCacheDialog = true }),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            gridItems.chunked(4).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowItems.forEach { item ->
                                        GoldenMenuItem(
                                            painter = item.painter,
                                            icon = item.icon,
                                            title = item.title,
                                            onClick = item.onClick,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    repeat(4 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    } else {
                        // 经典主题：原有列表布局
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column {
                                ProfileMenuItem(
                                    painter = painterResource(id = R.drawable.gkls),
                                    title = "观看历史",
                                    onClick = onNavigateToHistory
                                )
                                MenuDivider()
                                ProfileMenuItem(
                                    painter = painterResource(id = R.drawable.wdsc),
                                    title = "我的收藏",
                                    onClick = onNavigateToFavorites
                                )
                                MenuDivider()
                                ProfileMenuItem(
                                    painter = painterResource(id = R.drawable.wdxz),
                                    title = "我的下载",
                                    onClick = onNavigateToDownload
                                )
                                MenuDivider()
                                ProfileMenuItem(
                                    painter = painterResource(id = R.drawable.lyfk),
                                    title = "留言反馈",
                                    onClick = onNavigateToMessage
                                )
                                MenuDivider()
                                ProfileMenuItem(
                                    painter = painterResource(id = R.drawable.fk1),
                                    title = "扫一扫",
                                    onClick = {
                                        val intent = android.content.Intent(context, com.aijia.video.ui.activity.ScanQRActivity::class.java)
                                        context.startActivity(intent)
                                    }
                                )
                                MenuDivider()
                                ProfileMenuItem(
                                    painter = painterResource(id = R.drawable.kmdh),
                                    title = "卡密兑换",
                                    onClick = onNavigateToCardExchange
                                )
                                MenuDivider()
                                ProfileMenuItem(
                                    painter = painterResource(id = R.drawable.ztsz),
                                    title = "主题设置",
                                    onClick = onNavigateToThemeSettings
                                )
                                MenuDivider()
                                ProfileMenuItem(
                                    painter = painterResource(id = R.drawable.qlhc),
                                    title = "清理缓存",
                                    onClick = { showClearCacheDialog = true }
                                )
                                if (uiState.isLoggedIn) {
                                    MenuDivider()
                                    ProfileMenuItem(
                                        icon = Icons.AutoMirrored.Filled.Logout,
                                        title = "退出登录",
                                        onClick = { viewModel.logout() },
                                        isLast = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showClearCacheDialog) {
            AlertDialog(
                onDismissRequest = { showClearCacheDialog = false },
                title = { Text("清理缓存") },
                text = {
                    Text(
                        "此操作将清除图片缓存、广告缓存及应用临时数据，观看历史、收藏和下载内容不会被清除。\n\n确定要继续吗？",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearCacheDialog = false
                            viewModel.clearCache()
                            android.widget.Toast.makeText(context, "缓存已清理", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("确定清除")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearCacheDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }

        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                title = { Text("关于爱家视频") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("爱家视频 v$localVersionCode", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text("一款聚合视频播放应用", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("提供海量影视资源在线观看", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAboutDialog = false }) {
                        Text("确定")
                    }
                }
            )
        }

    }
}

@Composable
private fun ProfileActionCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    painter: androidx.compose.ui.graphics.painter.Painter? = null,
    title: String,
    subtitle: String? = null,
    highlight: Boolean = false,
    onClick: () -> Unit
) {
    val containerColor = if (highlight) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val iconTint = if (highlight) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.primary
    }
    val titleColor = if (highlight) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val subtitleColor = if (highlight) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = if (highlight) 4.dp else 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (highlight) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (highlight) {
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.82f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (painter != null) {
                    Icon(
                        painter = painter,
                        contentDescription = title,
                        tint = if (highlight) MaterialTheme.colorScheme.onPrimary else Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (highlight) MaterialTheme.colorScheme.onPrimary else iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun MenuDivider() {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
    )
}

@Composable
private fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    painter: androidx.compose.ui.graphics.painter.Painter? = null,
    title: String,
    isLast: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (painter != null) {
                Icon(
                    painter = painter,
                    contentDescription = title,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun GoldenMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    painter: androidx.compose.ui.graphics.painter.Painter? = null,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (painter != null) {
                    Icon(
                        painter = painter,
                        contentDescription = title,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(22.dp)
                    )
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AvatarSection(
    avatar: String?,
    displayName: String
) {
    val initial = displayName.firstOrNull()?.toString() ?: "我"
    if (!avatar.isNullOrBlank()) {
        AsyncImage(
            model = avatar,
            contentDescription = "头像",
            modifier = Modifier
                .size(78.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.primary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}


@Composable
private fun MenuGridItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
