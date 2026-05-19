package com.aijia.video.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aijia.video.data.model.AppPermission
import com.aijia.video.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppPermissionViewModel @Inject constructor(
    private val videoRepository: VideoRepository
) : ViewModel() {
    /**
     * 权限数据由 [SplashViewModel] 在启动阶段获取，
     * 此 ViewModel 仅从 [SessionManager.permissionFlow] 消费缓存，
     * 登录/注册后的刷新由对应业务逻辑直接触发 [VideoRepository.getUserPermission]。
     */
    val permission: StateFlow<AppPermission> = videoRepository.observeUserPermission()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), videoRepository.getCachedUserPermission())
}
