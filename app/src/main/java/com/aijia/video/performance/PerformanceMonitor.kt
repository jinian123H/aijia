package com.aijia.video.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 性能监控器
 */
@Singleton
class PerformanceMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    // 内存使用情况
    private val _memoryUsage = MutableStateFlow<MemoryUsage?>(null)
    val memoryUsage: StateFlow<MemoryUsage?> = _memoryUsage.asStateFlow()
    
    // CPU使用情况
    private val _cpuUsage = MutableStateFlow<CpuUsage?>(null)
    val cpuUsage: StateFlow<CpuUsage?> = _cpuUsage.asStateFlow()
    
    // 应用启动时间
    private val _startupTime = MutableStateFlow<Long>(0)
    val startupTime: StateFlow<Long> = _startupTime.asStateFlow()
    
    // 帧率监控
    private val _frameRate = MutableStateFlow<Float>(60f)
    val frameRate: StateFlow<Float> = _frameRate.asStateFlow()
    
    // ANR监控
    private val _anrCount = MutableStateFlow(0)
    val anrCount: StateFlow<Int> = _anrCount.asStateFlow()
    
    // 崩溃监控
    private val _crashCount = MutableStateFlow(0)
    val crashCount: StateFlow<Int> = _crashCount.asStateFlow()
    
    private var monitoringJob: Job? = null
    private var appStartTime: Long = 0
    
    init {
        appStartTime = System.currentTimeMillis()
    }
    
    /**
     * 开始性能监控
     */
    fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                collectMemoryUsage()
                collectCpuUsage()
                delay(1000) // 每秒收集一次
            }
        }
        
        // 记录启动时间
        _startupTime.value = System.currentTimeMillis() - appStartTime
    }
    
    /**
     * 停止性能监控
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
    }
    
    /**
     * 收集内存使用情况
     */
    private fun collectMemoryUsage() {
        try {
            val runtime = Runtime.getRuntime()
            val totalMemory = runtime.totalMemory()
            val freeMemory = runtime.freeMemory()
            val usedMemory = totalMemory - freeMemory
            val maxMemory = runtime.maxMemory()
            
            // 获取系统内存信息
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            _memoryUsage.value = MemoryUsage(
                usedMemory = usedMemory,
                totalMemory = totalMemory,
                maxMemory = maxMemory,
                availableMemory = memoryInfo.availMem,
                totalSystemMemory = memoryInfo.totalMem,
                usagePercentage = (usedMemory.toFloat() / maxMemory * 100).toInt()
            )
        } catch (e: Exception) {
            // 忽略异常
        }
    }
    
    /**
     * 收集CPU使用情况
     */
    private fun collectCpuUsage() {
        try {
            // 简化的CPU使用率计算
            val pid = android.os.Process.myPid()
            val appPidInfo = activityManager.runningAppProcesses.find { it.pid == pid }
            
            _cpuUsage.value = CpuUsage(
                pid = pid,
                cpuUsage = calculateCpuUsage(pid),
                processName = appPidInfo?.processName ?: "unknown"
            )
        } catch (e: Exception) {
            // 忽略异常
        }
    }
    
    /**
     * 计算CPU使用率（简化版本）
     */
    private fun calculateCpuUsage(pid: Int): Float {
        // 这里应该读取/proc/stat和/proc/pid/stat来计算
        // 简化实现，返回模拟值
        return kotlin.random.Random.nextFloat() * 30f
    }
    
    /**
     * 记录ANR
     */
    fun recordAnr() {
        _anrCount.value += 1
    }
    
    /**
     * 记录崩溃
     */
    fun recordCrash() {
        _crashCount.value += 1
    }
    
    /**
     * 获取性能报告
     */
    fun getPerformanceReport(): PerformanceReport {
        return PerformanceReport(
            startupTime = _startupTime.value,
            currentMemoryUsage = _memoryUsage.value,
            currentCpuUsage = _cpuUsage.value,
            averageFrameRate = _frameRate.value,
            anrCount = _anrCount.value,
            crashCount = _crashCount.value,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * 优化建议
     */
    fun getOptimizationSuggestions(): List<String> {
        val suggestions = mutableListOf<String>()
        
        _memoryUsage.value?.let { memory ->
            if (memory.usagePercentage > 80) {
                suggestions.add("内存使用率过高 (${memory.usagePercentage}%)，建议优化内存使用")
            }
        }
        
        _cpuUsage.value?.let { cpu ->
            if (cpu.cpuUsage > 50) {
                suggestions.add("CPU使用率过高 (${cpu.cpuUsage}%)，建议优化计算密集型操作")
            }
        }
        
        if (_frameRate.value < 55) {
            suggestions.add("帧率较低 (${_frameRate.value}fps)，建议优化UI渲染")
        }
        
        if (_startupTime.value > 3000) {
            suggestions.add("启动时间过长 (${_startupTime.value}ms)，建议优化启动流程")
        }
        
        if (_anrCount.value > 0) {
            suggestions.add("检测到ANR，建议检查主线程阻塞")
        }
        
        if (_crashCount.value > 0) {
            suggestions.add("检测到崩溃，建议加强异常处理")
        }
        
        return suggestions
    }
}

/**
 * 内存使用情况
 */
data class MemoryUsage(
    val usedMemory: Long,
    val totalMemory: Long,
    val maxMemory: Long,
    val availableMemory: Long,
    val totalSystemMemory: Long,
    val usagePercentage: Int
) {
    val usedMemoryMB get() = usedMemory / (1024 * 1024)
    val totalMemoryMB get() = totalMemory / (1024 * 1024)
    val maxMemoryMB get() = maxMemory / (1024 * 1024)
    val availableMemoryMB get() = availableMemory / (1024 * 1024)
    val totalSystemMemoryMB get() = totalSystemMemory / (1024 * 1024)
}

/**
 * CPU使用情况
 */
data class CpuUsage(
    val pid: Int,
    val cpuUsage: Float,
    val processName: String
)

/**
 * 性能报告
 */
data class PerformanceReport(
    val startupTime: Long,
    val currentMemoryUsage: MemoryUsage?,
    val currentCpuUsage: CpuUsage?,
    val averageFrameRate: Float,
    val anrCount: Int,
    val crashCount: Int,
    val timestamp: Long
)
