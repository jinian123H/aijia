package com.aijia.video.ui.update

import android.content.Context
import androidx.lifecycle.ViewModel
import com.aijia.video.BuildConfig
import androidx.lifecycle.viewModelScope
import com.aijia.video.data.model.VersionUpdateInfo
import com.aijia.video.data.repository.VersionRepository
import com.aijia.video.util.UpdateGuard
import com.aijia.video.util.VersionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UpdateUiState(
    val isChecking: Boolean = false,
    val needUpdate: Boolean = false,
    val forceUpdate: VersionUpdateInfo? = null,
    val errorMessage: String? = null,
    val currentVersionCode: Int = 0,
    val currentVersionName: String = ""
)

@HiltViewModel
class UpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val versionRepository: VersionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    /** 已触发的强制更新版本号 — 一旦设置，只有用户实际升级后才清除 */
    private var pendingForceVersionCode: Int = -1

    init {
        val currentCode = VersionUtils.getVersionCode(context)
        val currentName = VersionUtils.getVersionName(context)
        _uiState.value = _uiState.value.copy(
            currentVersionCode = currentCode,
            currentVersionName = currentName
        )
    }

    /**
     * 检查更新
     *
     * 强制更新弹窗一旦显示，后续每次 onResume 都会重新请求 API，
     * 但只要本地 version_code 没变（用户没实际安装新版），弹窗永不消失。
     * 即使服务端因任何原因返回 needUpdate=false，客户端也忽略。
     */
    fun checkUpdate() {
        if (BuildConfig.DEBUG) {
            UpdateGuard.logCurrentSignature(context)
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChecking = true, errorMessage = null)

            val result = versionRepository.checkForceUpdate(
                context = context,
                currentUpdateVersionId = ""
            )

            result.fold(
                onSuccess = { response ->
                    if (response.needUpdate && response.forceUpdate && response.content != null) {
                        // 服务端要求更新 → 记录版本号并显示弹窗
                        pendingForceVersionCode = response.content.versionCode
                        val forceUpdateInfo = VersionUpdateInfo(
                            versionCode = response.content.versionCode,
                            versionName = response.content.versionName,
                            downloadUrl = response.content.downloadUrl,
                            releaseNotes = response.content.releaseNotes,
                            updateVersionId = response.updateVersionId
                        )
                        _uiState.value = _uiState.value.copy(
                            isChecking = false,
                            needUpdate = true,
                            forceUpdate = forceUpdateInfo
                        )
                    } else {
                        // 服务端说不需要更新 → 检查用户是否真的已升级
                        val currentCode = _uiState.value.currentVersionCode
                        if (pendingForceVersionCode > 0 && currentCode < pendingForceVersionCode) {
                            // 用户并未实际升级 → 保持弹窗
                            _uiState.value = _uiState.value.copy(isChecking = false)
                        } else {
                            // 确实不需要更新了
                            pendingForceVersionCode = -1
                            _uiState.value = _uiState.value.copy(
                                isChecking = false,
                                needUpdate = false,
                                forceUpdate = null
                            )
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isChecking = false,
                        errorMessage = error.message
                    )
                }
            )
        }
    }

    /**
     * 关闭更新弹窗（所有更新均为强制更新，不可关闭）
     */
    fun dismissUpdate() {
        // 强制更新不允许关闭
    }
}
