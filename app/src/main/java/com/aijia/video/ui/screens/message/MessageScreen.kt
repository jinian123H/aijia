package com.aijia.video.ui.screens.message

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.aijia.video.data.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 留言ViewModel
 */
@HiltViewModel
class MessageViewModel @Inject constructor(
    private val messageRepository: MessageRepository
) : ViewModel() {
    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _submitSuccess = MutableStateFlow(false)
    val submitSuccess: StateFlow<Boolean> = _submitSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun submitMessage(content: String, contact: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null

            messageRepository.submitMessage(content, contact).fold(
                onSuccess = {
                    _submitSuccess.value = true
                    _isSubmitting.value = false
                },
                onFailure = {
                    _errorMessage.value = it.message ?: "哎呀，留言没发出去 😣\n请检查网络后重试，或稍后再试。"
                    _isSubmitting.value = false
                }
            )
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun resetSuccess() {
        _submitSuccess.value = false
    }
}

/**
 * 留言反馈页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageScreen(
    onNavigateBack: () -> Unit,
    viewModel: MessageViewModel = hiltViewModel()
) {
    var messageContent by remember { mutableStateOf("") }
    var contactInfo by remember { mutableStateOf("") }
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val submitSuccess by viewModel.submitSuccess.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 提交成功后返回
    LaunchedEffect(submitSuccess) {
        if (submitSuccess) {
            Toast.makeText(
                context,
                "感谢您的反馈！❤️\n我们已收到您的留言，会尽快查看并改进。",
                Toast.LENGTH_LONG
            ).show()
            viewModel.resetSuccess()
            onNavigateBack()
        }
    }

    // 显示错误提示
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("留言反馈") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 顶部引导语
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "您的声音，我们认真听 👂",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "无论是 bug、建议，还是夸奖，都欢迎留下！\n我们会认真阅读每一条留言，并持续优化体验。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            // 留言内容输入框
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "您的反馈",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = messageContent,
                    onValueChange = { messageContent = it },
                    placeholder = {
                        Text(
                            text = "有任何建议、问题或想法？告诉我们吧～\n\n例如：功能不好用？界面看不懂？还是特别喜欢某个地方？",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    maxLines = 10,
                    enabled = !isSubmitting,
                    shape = MaterialTheme.shapes.medium
                )
                Text(
                    text = "${messageContent.length}/500 字",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (messageContent.length > 500) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.align(Alignment.End)
                )
            }

            // 联系方式输入框
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "联系方式（选填）",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = contactInfo,
                    onValueChange = { contactInfo = it },
                    placeholder = {
                        Text(
                            text = "QQ / 微信 / 手机号等（方便我们联系您）",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSubmitting,
                    shape = MaterialTheme.shapes.medium
                )
            }

            // 温馨提示
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "💡 温馨提示：\n• 留言内容至少5个字\n• 我们会在1-3个工作日内处理\n• 留下联系方式可以获得更快回复",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 提交按钮
            Button(
                onClick = {
                    when {
                        messageContent.isBlank() -> {
                            Toast.makeText(
                                context,
                                "还没写内容呢！\n告诉我们您想说的吧 ✍️",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        messageContent.length < 5 -> {
                            Toast.makeText(
                                context,
                                "请输入您的反馈内容～\n哪怕一句话也行！",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        messageContent.length > 500 -> {
                            Toast.makeText(
                                context,
                                "留言内容不能超过500字哦～",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else -> {
                            viewModel.submitMessage(messageContent, contactInfo)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("提交中...")
                } else {
                    Text(
                        text = "提交反馈 🚀",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
