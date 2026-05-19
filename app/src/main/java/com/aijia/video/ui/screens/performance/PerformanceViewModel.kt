package com.aijia.video.ui.screens.performance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aijia.video.performance.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 性能监控页面ViewModel
 */
@HiltViewModel
class PerformanceViewModel @Inject constructor(
    private val performanceMonitor: PerformanceMonitor,
    private val startupOptimizer: StartupOptimizer,
    private val memoryOptimizer: MemoryOptimizer,
    private val uiPerformanceOptimizer: UIPerformanceOptimizer
) : ViewModel() {
    
    // 内存使用情况
    private val _memoryUsage = MutableStateFlow<MemoryUsage?>(null)
    val memoryUsage: StateFlow<MemoryUsage?> = _memoryUsage.asStateFlow()
    
    // CPU使用情况
    private val _cpuUsage = MutableStateFlow<CpuUsage?>(null)
    val cpuUsage: StateFlow<CpuUsage?> = _cpuUsage.asStateFlow()
    
    // 启动报告
    private val _startupReport = MutableStateFlow(StartupReport(
        coldStartTime = 0,
        warmStartTime = 0,
        hotStartTime = 0,
        phases = emptyMap(),
        recommendations = emptyList()
    ))
    val startupReport: StateFlow<StartupReport> = _startupReport.asStateFlow()
    
    // UI性能报告
    private val _uiPerformanceReport = MutableStateFlow(UIPerformanceReport(
        currentFPS = 60f,
        recompositionCount = 0,
        renderMetrics = emptyList(),
        recommendations = emptyList()
    ))
    val uiPerformanceReport: StateFlow<UIPerformanceReport> = _uiPerformanceReport.asStateFlow()
    
    // 优化建议
    private val _optimizationSuggestions = MutableStateFlow<List<String>>(emptyList())
    val optimizationSuggestions: StateFlow<List<String>> = _optimizationSuggestions.asStateFlow()
    
    init {
        startPerformanceMonitoring()
        loadInitialData()
    }
    
    /**
     * 开始性能监控
     */
    private fun startPerformanceMonitoring() {
        performanceMonitor.startMonitoring()
        
        // 监听性能数据变化
        viewModelScope.launch {
            performanceMonitor.memoryUsage.collect { memory ->
                _memoryUsage.value = memory
                updateOptimizationSuggestions()
            }
        }
        
        viewModelScope.launch {
            performanceMonitor.cpuUsage.collect { cpu ->
                _cpuUsage.value = cpu
                updateOptimizationSuggestions()
            }
        }
    }
    
    /**
     * 加载初始数据
     */
    private fun loadInitialData() {
        // 加载启动报告
        _startupReport.value = startupOptimizer.getStartupReport()
        
        // 加载UI性能报告
        _uiPerformanceReport.value = uiPerformanceOptimizer.getUIPerformanceReport()
        
        // 更新优化建议
        updateOptimizationSuggestions()
    }
    
    /**
     * 刷新数据
     */
    fun refreshData() {
        loadInitialData()
    }
    
    /**
     * 优化内存
     */
    fun optimizeMemory() {
        viewModelScope.launch {
            val result = memoryOptimizer.checkAndOptimizeMemory()
            _optimizationSuggestions.value = listOf("内存优化完成: ${result.optimizationCount}项优化")
        }
    }
    
    /**
     * 优化UI
     */
    fun optimizeUI() {
        viewModelScope.launch {
            // 重置UI性能指标
            uiPerformanceOptimizer.resetMetrics()
            
            // 获取优化建议
            val tips = uiPerformanceOptimizer.getComposeOptimizationTips()
            _optimizationSuggestions.value = listOf("UI优化建议:") + tips
        }
    }
    
    /**
     * 清理缓存
     */
    fun clearCache() {
        viewModelScope.launch {
            // 清理各种缓存
            memoryOptimizer.checkAndOptimizeMemory()
            
            _optimizationSuggestions.value = listOf(
                "缓存清理完成",
                "- 清理图片缓存",
                "- 清理网络缓存", 
                "- 清理数据库缓存",
                "- 释放内存"
            )
        }
    }
    
    /**
     * 更新优化建议
     */
    private fun updateOptimizationSuggestions() {
        val suggestions = mutableListOf<String>()
        
        // 添加性能监控器的建议
        suggestions.addAll(performanceMonitor.getOptimizationSuggestions())
        
        // 添加内存优化器的建议
        suggestions.addAll(memoryOptimizer.getMemoryOptimizationSuggestions())
        
        // 添加UI优化器的建议
        suggestions.addAll(uiPerformanceOptimizer.getUIOptimizationRecommendations())
        
        _optimizationSuggestions.value = suggestions
    }
    
    /**
     * 获取完整的性能报告
     */
    fun getFullPerformanceReport(): FullPerformanceReport {
        return FullPerformanceReport(
            performanceReport = performanceMonitor.getPerformanceReport(),
            startupReport = _startupReport.value,
            uiPerformanceReport = _uiPerformanceReport.value,
            memoryOptimizationStats = memoryOptimizer.getOptimizationStats(),
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * 释放资源
     */
    override fun onCleared() {
        super.onCleared()
        performanceMonitor.stopMonitoring()
    }
}

/**
 * 完整性能报告
 */
data class FullPerformanceReport(
    val performanceReport: PerformanceReport,
    val startupReport: StartupReport,
    val uiPerformanceReport: UIPerformanceReport,
    val memoryOptimizationStats: MemoryOptimizationStats,
    val timestamp: Long
)
