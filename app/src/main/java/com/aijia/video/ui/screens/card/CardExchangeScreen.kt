package com.aijia.video.ui.screens.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.aijia.video.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CardExchangeState(
    val cardPwd: String = "",
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class CardExchangeViewModel @Inject constructor(
    private val videoRepository: VideoRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CardExchangeState())
    val uiState: StateFlow<CardExchangeState> = _uiState.asStateFlow()
    
    fun updateCardPwd(pwd: String) {
        _uiState.value = _uiState.value.copy(cardPwd = pwd)
    }
    
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            errorMessage = null
        )
    }
    
    fun exchangeCard() {
        val cardPwd = _uiState.value.cardPwd.trim()
        if (cardPwd.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "请输入卡密密码",
                successMessage = null
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )
            
            val result = videoRepository.useCard(cardPwd)
            _uiState.value = result.fold(
                onSuccess = {
                    CardExchangeState(
                        cardPwd = "",
                        isLoading = false,
                        successMessage = "卡密兑换成功，会员有效期已延长",
                        errorMessage = null
                    )
                },
                onFailure = {
                    CardExchangeState(
                        cardPwd = cardPwd,
                        isLoading = false,
                        successMessage = null,
                        errorMessage = it.message ?: "卡密兑换失败"
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardExchangeScreen(
    onNavigateBack: () -> Unit,
    viewModel: CardExchangeViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("卡密兑换") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 卡密输入
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "输入卡密",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall
                )
                
                OutlinedTextField(
                    value = uiState.cardPwd,
                    onValueChange = { viewModel.updateCardPwd(it) },
                    label = { Text("卡密密码") },
                    placeholder = { Text("请输入卡密密码") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Button(
                    onClick = { viewModel.exchangeCard() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading && uiState.cardPwd.isNotBlank()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(4.dp),
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("兑换")
                    }
                }
            }
            
            // 提示信息
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 错误提示信息
                uiState.errorMessage?.let {
                    Text(
                        text = it,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // 成功提示信息
                uiState.successMessage?.let {
                    Text(
                        text = it,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "卡密兑换说明",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "• 卡密兑换后将自动延长会员有效期",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• 每个卡密只能使用一次",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• 请确保输入正确的卡密密码",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
