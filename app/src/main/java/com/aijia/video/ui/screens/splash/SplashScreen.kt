package com.aijia.video.ui.screens.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aijia.video.R
import com.aijia.video.ui.components.AdView
import com.aijia.video.ui.components.SplashVideoPlayer

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = hiltViewModel(),
    onSplashComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.shouldShowAds &&
            uiState.adConfig?.startupAd?.enabled == true &&
            uiState.adConfig?.startupAd?.imageUrl?.isNotBlank() == true -> {
                AdView(
                    imageUrl = uiState.adConfig!!.startupAd!!.imageUrl,
                    linkUrl = uiState.adConfig!!.startupAd!!.linkUrl,
                    duration = uiState.adConfig!!.startupAd!!.durationInt,
                    onAdClick = { url ->
                        viewModel.handleAdClick(url, context)
                    },
                    onAdSkip = {
                        onSplashComplete()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                SplashVideoPlayer(
                    videoResId = R.raw.originos,
                    onComplete = onSplashComplete,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}