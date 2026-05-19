package com.aijia.video

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.aijia.video.data.repository.SessionManager
import com.aijia.video.ui.navigation.AijiaNavigation
import com.aijia.video.ui.theme.AijiaVideoTheme
import com.aijia.video.ui.update.UpdateDialog
import com.aijia.video.ui.update.UpdateViewModel
import com.aijia.video.util.AppUpdateManager
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface MainActivitySessionManagerEntryPoint {
    fun sessionManager(): SessionManager
}

/**
 * 主Activity
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val sessionManager = remember(applicationContext) {
                EntryPointAccessors.fromApplication(
                    applicationContext,
                    MainActivitySessionManagerEntryPoint::class.java
                ).sessionManager()
            }
            val themeMode by sessionManager.themeModeFlow.collectAsStateWithLifecycle()
            val darkMode by sessionManager.darkModeFlow.collectAsStateWithLifecycle()
            val fontMode by sessionManager.fontModeFlow.collectAsStateWithLifecycle()

            AijiaVideoTheme(
                themeMode = themeMode,
                darkMode = darkMode,
                fontMode = fontMode
            ) {
                val updateViewModel: UpdateViewModel = hiltViewModel()
                val updateState by updateViewModel.uiState.collectAsStateWithLifecycle()
                val updateManager = remember { AppUpdateManager(this) }
                val downloadState by updateManager.downloadState.collectAsStateWithLifecycle()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AijiaNavigation(
                        navController = navController,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 每次恢复到前台时重新检查更新
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            updateViewModel.checkUpdate()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                // 强制更新弹窗（始终为强制更新）
                updateState.forceUpdate?.let { force ->
                    UpdateDialog(
                        versionInfo = force,
                        isForceUpdate = true,
                        currentVersion = updateState.currentVersionName,
                        downloadState = downloadState,
                        onUpdate = {
                            updateManager.downloadAndInstall(
                                force.downloadUrl,
                                force.versionName
                            )
                        },
                        onDismiss = {
                            updateManager.resetDownloadState()
                            updateViewModel.dismissUpdate()
                        },
                        onCancelDownload = {
                            updateManager.cancelDownload()
                            updateViewModel.dismissUpdate()
                        }
                    )
                }
            }
        }
    }
}
