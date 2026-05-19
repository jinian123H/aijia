package com.aijia.video.ui.screens.cast

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aijia.video.cast.CastManager
import com.aijia.video.ui.components.CastView
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 投屏页面Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastScreen(
    onNavigateBack: () -> Unit,
    viewModel: CastViewModel = hiltViewModel()
) {
    CastView(
        modifier = Modifier.fillMaxSize(),
        castManager = viewModel.castManager,
        onNavigateBack = onNavigateBack
    )
}

/**
 * 投屏页面ViewModel
 */
@HiltViewModel
class CastViewModel @Inject constructor(
    val castManager: CastManager
) : androidx.lifecycle.ViewModel() {
    // 这里可以添加投屏相关的状态管理
    // 目前直接使用CastManager
}
