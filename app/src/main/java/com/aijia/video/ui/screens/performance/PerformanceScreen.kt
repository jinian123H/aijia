package com.aijia.video.ui.screens.performance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aijia.video.performance.MemoryUsage
import com.aijia.video.performance.CpuUsage
import com.aijia.video.performance.StartupReport

/**
 * 性能监控页面Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceScreen(
    onNavigateBack: () -> Unit,
    viewModel: PerformanceViewModel = hiltViewModel()
) {
    val memoryUsage by viewModel.memoryUsage.collectAsStateWithLifecycle()
    val cpuUsage by viewModel.cpuUsage.collectAsStateWithLifecycle()
    val startupReport by viewModel.startupReport.collectAsStateWithLifecycle()
    val uiPerformanceReport by viewModel.uiPerformanceReport.collectAsStateWithLifecycle()
    val optimizationSuggestions by viewModel.optimizationSuggestions.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("性能监控") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 启动性能
            item {
                StartupPerformanceCard(report = startupReport)
            }
            
            // 内存使用
            item {
                MemoryUsageCard(memoryUsage = memoryUsage)
            }
            
            // CPU使用
            item {
                CpuUsageCard(cpuUsage = cpuUsage)
            }
            
            // UI性能
            item {
                UIPerformanceCard(report = uiPerformanceReport)
            }
            
            // 优化建议
            if (optimizationSuggestions.isNotEmpty()) {
                item {
                    OptimizationSuggestionsCard(suggestions = optimizationSuggestions)
                }
            }
            
            // 操作按钮
            item {
                ActionButtonsCard(
                    onOptimizeMemory = { viewModel.optimizeMemory() },
                    onOptimizeUI = { viewModel.optimizeUI() },
                    onClearCache = { viewModel.clearCache() }
                )
            }
        }
    }
}

/**
 * 启动性能卡片
 */
@Composable
private fun StartupPerformanceCard(report: StartupReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "启动性能",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "冷启动",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (report.coldStartTime > 0) "${report.coldStartTime}ms" else "N/A",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column {
                    Text(
                        text = "热启动",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (report.warmStartTime > 0) "${report.warmStartTime}ms" else "N/A",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column {
                    Text(
                        text = "温启动",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (report.hotStartTime > 0) "${report.hotStartTime}ms" else "N/A",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * 内存使用卡片
 */
@Composable
private fun MemoryUsageCard(memoryUsage: MemoryUsage?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "内存使用",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            memoryUsage?.let { memory ->
                // 内存使用率
                LinearProgressIndicator(
                    progress = { memory.usagePercentage / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "使用率: ${memory.usagePercentage}%",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "已使用",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${memory.usedMemoryMB}MB",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    Column {
                        Text(
                            text = "总内存",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${memory.maxMemoryMB}MB",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    Column {
                        Text(
                            text = "系统可用",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${memory.availableMemoryMB}MB",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } ?: run {
                Text(
                    text = "暂无数据",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * CPU使用卡片
 */
@Composable
private fun CpuUsageCard(cpuUsage: CpuUsage?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "CPU使用",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            cpuUsage?.let { cpu ->
                Text(
                    text = "使用率: ${cpu.cpuUsage.toInt()}%",
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "进程: ${cpu.processName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "PID: ${cpu.pid}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } ?: run {
                Text(
                    text = "暂无数据",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * UI性能卡片
 */
@Composable
private fun UIPerformanceCard(report: com.aijia.video.performance.UIPerformanceReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "UI性能",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "帧率",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${report.currentFPS.toInt()}fps",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column {
                    Text(
                        text = "重组次数",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${report.recompositionCount}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column {
                    Text(
                        text = "组件数量",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${report.renderMetrics.size}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * 优化建议卡片
 */
@Composable
private fun OptimizationSuggestionsCard(suggestions: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "优化建议",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            suggestions.forEach { suggestion ->
                Text(
                    text = "• $suggestion",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * 操作按钮卡片
 */
@Composable
private fun ActionButtonsCard(
    onOptimizeMemory: () -> Unit,
    onOptimizeUI: () -> Unit,
    onClearCache: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "性能优化",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOptimizeMemory,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("优化内存")
                }
                
                Button(
                    onClick = onOptimizeUI,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("优化UI")
                }
                
                OutlinedButton(
                    onClick = onClearCache,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("清理缓存")
                }
            }
        }
    }
}
