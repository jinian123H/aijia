package com.aijia.video.ui.navigation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aijia.video.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ShortVideoNavVM"

data class ShortVideoNavigationState(
    val typeId: Int? = null,
    val isLoaded: Boolean = false
)

@HiltViewModel
class ShortVideoNavigationViewModel @Inject constructor(
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ShortVideoNavigationState())
    val state: StateFlow<ShortVideoNavigationState> = _state.asStateFlow()

    /**
     * 异步加载短视频类型ID
     */
    fun ensureLoaded() {
        val current = _state.value
        if (current.isLoaded) return

        viewModelScope.launch {
            loadTypeId()
        }
    }

    /**
     * 挂起函数：同步获取 typeId，如果未加载则等待加载完成
     * 用于点击导航时确保 typeId 已加载
     */
    suspend fun getTypeIdAsync(): Int? {
        val current = _state.value
        if (current.isLoaded) {
            return current.typeId
        }

        // 未加载，同步加载
        return loadTypeId()
    }

    /**
     * 内部加载方法，返回加载的 typeId
     */
    private suspend fun loadTypeId(): Int? {
        try {
            // 优先从 API 获取
            val configResult = videoRepository.getShortVideoConfig()
            if (configResult.isSuccess) {
                val typeId = configResult.getOrNull()?.typeId
                if (typeId != null && typeId > 0) {
                    Log.d(TAG, "loadTypeId from API: $typeId")
                    _state.value = ShortVideoNavigationState(
                        typeId = typeId,
                        isLoaded = true
                    )
                    return typeId
                }
            }

            // API 失败，尝试从分类列表查找
            val typesResult = videoRepository.getVideoTypes()
            if (typesResult.isSuccess) {
                val types = typesResult.getOrNull()
                // 查找 type_en 为 "duanju" 的分类
                val duanjuType = types?.firstOrNull {
                    it.typeEn.equals("duanju", ignoreCase = true)
                }
                if (duanjuType != null) {
                    Log.d(TAG, "loadTypeId from types (duanju): ${duanjuType.id}")
                    _state.value = ShortVideoNavigationState(
                        typeId = duanjuType.id,
                        isLoaded = true
                    )
                    return duanjuType.id
                }

                // 如果找不到 duanju，尝试查找名称包含"短"的分类
                val shortType = types?.firstOrNull {
                    it.name.contains("短", ignoreCase = true)
                }
                if (shortType != null) {
                    Log.d(TAG, "loadTypeId from types (短): ${shortType.id}")
                    _state.value = ShortVideoNavigationState(
                        typeId = shortType.id,
                        isLoaded = true
                    )
                    return shortType.id
                }
            }

            // 都失败，使用默认值 10
            Log.d(TAG, "loadTypeId fallback to default: 10")
            _state.value = ShortVideoNavigationState(
                typeId = 10,
                isLoaded = true
            )
            return 10
        } catch (e: Exception) {
            Log.e(TAG, "loadTypeId error", e)
            _state.value = ShortVideoNavigationState(
                typeId = 10, // 默认值
                isLoaded = true
            )
            return 10
        }
    }
}
