package com.aijia.video.ui.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aijia.video.data.model.Video
import com.aijia.video.data.repository.AppThemeDefaults
import com.aijia.video.data.repository.SessionManager
import com.aijia.video.ui.components.BottomNavItem
import com.aijia.video.ui.components.BottomNavigationBar
import com.aijia.video.ui.screens.home.HomeScreen
import com.aijia.video.ui.screens.rank.RankScreen
import com.aijia.video.ui.screens.shortvideo.ShortVideoScreen
import com.aijia.video.ui.screens.profile.ProfileScreen
import com.aijia.video.ui.screens.theme.ThemeSettingsScreen
import com.aijia.video.ui.screens.player.PlayerScreen
import com.aijia.video.ui.screens.search.SearchScreen
import com.aijia.video.ui.screens.download.DownloadScreen
import com.aijia.video.ui.screens.favorite.FavoriteScreen
import com.aijia.video.ui.screens.history.HistoryScreen
import com.aijia.video.ui.screens.filter.FilterScreen
import com.aijia.video.ui.screens.message.MessageScreen
import com.aijia.video.ui.screens.splash.SplashScreen
import com.aijia.video.ui.screens.auth.AuthScreen
import com.aijia.video.ui.screens.card.CardExchangeScreen
import kotlinx.coroutines.launch

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface NavigationSessionManagerEntryPoint {
    fun sessionManager(): SessionManager
}

/**
 * 应用导航
 */
@Composable
fun AijiaNavigation(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
    onSplashComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val permissionViewModel: AppPermissionViewModel = hiltViewModel()
    val appPermission by permissionViewModel.permission.collectAsStateWithLifecycle()
    val shortVideoNavigationViewModel: ShortVideoNavigationViewModel = hiltViewModel()
    val shortVideoNavigationState by shortVideoNavigationViewModel.state.collectAsStateWithLifecycle()
    val sessionManager = remember(context.applicationContext) {
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext,
            NavigationSessionManagerEntryPoint::class.java
        ).sessionManager()
    }
    val navigationStyle by sessionManager.navigationStyleFlow.collectAsStateWithLifecycle()
    val isFixedNavigation = navigationStyle == AppThemeDefaults.NAVIGATION_FIXED
    var shortVideoControlsVisible by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val bottomNavItems = remember(appPermission.discoverVisible) {
        listOfNotNull(
            BottomNavItem.Home,
            BottomNavItem.Rank,
            BottomNavItem.ShortVideo,
            BottomNavItem.Profile
        )
    }
    val shouldShowBottomNav = currentRoute == "home" || currentRoute == "rank" || (currentRoute?.startsWith("short_video") == true && shortVideoControlsVisible) || currentRoute == "profile"

    fun showPermissionTip(key: String, fallback: String) {
        Toast.makeText(context, appPermission.tipFor(key, fallback), Toast.LENGTH_SHORT).show()
    }

    // 短视频类型ID延后到首次进入短视频页面时加载（启动阶段不请求）
    LaunchedEffect(currentRoute) {
        if (currentRoute?.startsWith("short_video") == true) {
            shortVideoNavigationViewModel.ensureLoaded()
        }
    }

    fun navigateToVideo(video: Video) {
        coroutineScope.launch {
            // 同步等待 typeId 加载完成
            val configuredShortVideoTypeId = shortVideoNavigationViewModel.getTypeIdAsync()
            if (configuredShortVideoTypeId != null && video.typeId == configuredShortVideoTypeId) {
                navController.navigate("short_video?videoId=${video.id}&typeId=${video.typeId}") {
                    launchSingleTop = true
                }
            } else {
                navController.navigate("player/${video.id}") {
                    launchSingleTop = true
                }
            }
        }
    }

    val navHostContent: @Composable (Modifier) -> Unit = { navHostModifier ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = navHostModifier
        ) {
            // 启动页
            composable("splash") {
                SplashScreen(onSplashComplete = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                    onSplashComplete()
                })
            }

            // 底部导航栏页面
            composable("home") {
                HomeScreen(
                    onNavigateToPlayer = ::navigateToVideo,
                    onNavigateToSearch = {
                        if (appPermission.hasPermission("search")) {
                            navController.navigate("search") { launchSingleTop = true }
                        } else {
                            showPermissionTip("search", "当前账号暂无搜索权限")
                        }
                    },
                    onNavigateToProfile = {
                        navController.navigate("profile") { launchSingleTop = true }
                    },
                    onNavigateToHistory = {
                        navController.navigate("history") { launchSingleTop = true }
                    },
                    onNavigateToDownload = {
                        if (appPermission.hasPermission("download")) {
                            navController.navigate("download") { launchSingleTop = true }
                        } else {
                            showPermissionTip("download", "当前账号暂无下载权限")
                        }
                    }
                )
            }

            composable("rank") {
                RankScreen(
                    onNavigateToPlayer = ::navigateToVideo
                )
            }

            composable(
                route = "short_video?videoId={videoId}&typeId={typeId}",
                arguments = listOf(
                    navArgument("videoId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("typeId") {
                        type = NavType.IntType
                        defaultValue = 0
                    }
                )
            ) { backStackEntry ->
                ShortVideoScreen(
                    initialVideoId = backStackEntry.arguments?.getString("videoId"),
                    initialTypeId = backStackEntry.arguments?.getInt("typeId")?.takeIf { it > 0 },
                    appPermission = appPermission,
                    onControlsVisibilityChange = { shortVideoControlsVisible = it }
                )
            }

            composable("profile") {
                ProfileScreen(
                    onNavigateToFavorites = {
                        navController.navigate("favorites") { launchSingleTop = true }
                    },
                    onNavigateToDownload = {
                        if (appPermission.hasPermission("download")) {
                            navController.navigate("download") { launchSingleTop = true }
                        } else {
                            showPermissionTip("download", "当前账号暂无下载权限")
                        }
                    },
                    onNavigateToHistory = {
                        navController.navigate("history") { launchSingleTop = true }
                    },
                    onNavigateToMessage = {
                        navController.navigate("message") { launchSingleTop = true }
                    },
                    onNavigateToLogin = {
                        navController.navigate("auth/login") { launchSingleTop = true }
                    },
                    onNavigateToRegister = {
                        navController.navigate("auth/register") { launchSingleTop = true }
                    },
                    onNavigateToCardExchange = {
                        navController.navigate("card_exchange") { launchSingleTop = true }
                    },
                    onNavigateToThemeSettings = {
                        navController.navigate("theme_settings") { launchSingleTop = true }
                    }
                )
            }

            composable("theme_settings") {
                ThemeSettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable("auth/login") {
                AuthScreen(
                    onBackClick = { navController.popBackStack() },
                    isLogin = true
                )
            }

            composable("auth/register") {
                AuthScreen(
                    onBackClick = { navController.popBackStack() },
                    isLogin = false,
                    onNavigateToProfile = {
                        navController.navigate("profile") {
                            popUpTo("auth/register") { inclusive = true }
                        }
                    }
                )
            }

            // 其他页面
            composable("search") {
                if (appPermission.hasPermission("search")) {
                    SearchScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToPlayer = ::navigateToVideo
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Text(text = appPermission.tipFor("search", "当前账号暂无搜索权限"))
                    }
                }
            }

            composable("player/{videoId}") { backStackEntry ->
                val videoId = backStackEntry.arguments?.getString("videoId")?.toIntOrNull() ?: 0
                PlayerScreen(
                    videoId = videoId,
                    appPermission = appPermission,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable("history") {
                HistoryScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToPlayer = ::navigateToVideo
                )
            }

            composable("download") {
                if (appPermission.hasPermission("download")) {
                    DownloadScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToPlayer = { downloadRecord ->
                            val title = if (downloadRecord.episodeName != null) {
                                "${downloadRecord.videoTitle} - ${downloadRecord.episodeName}"
                            } else {
                                downloadRecord.videoTitle
                            }
                            navController.navigate("local_player?path=${android.net.Uri.encode(downloadRecord.localPath)}&title=${android.net.Uri.encode(title)}") {
                                launchSingleTop = true
                            }
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Text(text = appPermission.tipFor("download", "当前账号暂无下载权限"))
                    }
                }
            }

            composable("favorites") {
                FavoriteScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToPlayer = ::navigateToVideo
                )
            }

            composable(
                route = "local_player?path={path}&title={title}",
                arguments = listOf(
                    navArgument("path") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val path = backStackEntry.arguments?.getString("path") ?: ""
                val title = backStackEntry.arguments?.getString("title") ?: ""
                PlayerScreen(
                    videoId = 0,
                    appPermission = appPermission,
                    onNavigateBack = { navController.popBackStack() },
                    localVideoPath = path,
                    localVideoTitle = title
                )
            }

            composable("filter") {
                FilterScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToPlayer = ::navigateToVideo
                )
            }

            composable("message") {
                MessageScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable("card_exchange") {
                CardExchangeScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable("cast") {
                com.aijia.video.ui.screens.cast.CastScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }

    val isShortVideoRoute = currentRoute?.startsWith("short_video") == true

    if (isFixedNavigation && shouldShowBottomNav && !isShortVideoRoute) {
        Box(modifier = modifier.fillMaxSize().statusBarsPadding()) {
            navHostContent(Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                BottomNavigationBar(
                    navController = navController,
                    isFixed = true,
                    items = bottomNavItems
                )
            }
        }
    } else if (isShortVideoRoute) {
        Box(modifier = modifier.fillMaxSize()) {
            navHostContent(Modifier.fillMaxSize())

            if (shouldShowBottomNav) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    BottomNavigationBar(
                        navController = navController,
                        isFixed = isFixedNavigation,
                        items = bottomNavItems
                    )
                }
            }
        }
    } else {
        Box(modifier = modifier.fillMaxSize().statusBarsPadding()) {
            navHostContent(Modifier.fillMaxSize())

            if (shouldShowBottomNav) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    BottomNavigationBar(
                        navController = navController,
                        isFixed = false,
                        items = bottomNavItems
                    )
                }
            }
        }
    }
}
