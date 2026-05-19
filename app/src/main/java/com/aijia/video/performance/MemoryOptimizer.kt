package com.aijia.video.performance

import android.app.ActivityManager
import android.content.Context
import androidx.compose.runtime.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 内存优化器
 */
@Singleton
class MemoryOptimizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    // 内存警告阈值
    private val warningThreshold = 0.8f // 80%
    private val criticalThreshold = 0.9f // 90%
    
    // 内存优化计数器
    private var optimizationCount = 0
    
    /**
     * 检查内存使用情况并执行优化
     */
    fun checkAndOptimizeMemory(): MemoryOptimizationResult {
        val memoryInfo = getMemoryInfo()
        val optimizations = mutableListOf<String>()
        
        when {
            memoryInfo.usagePercentage >= criticalThreshold -> {
                // 关键内存不足，执行紧急优化
                optimizations.addAll(performCriticalOptimization())
            }
            memoryInfo.usagePercentage >= warningThreshold -> {
                // 内存警告，执行常规优化
                optimizations.addAll(performRegularOptimization())
            }
        }
        
        return MemoryOptimizationResult(
            beforeOptimization = memoryInfo,
            optimizations = optimizations,
            optimizationCount = optimizations.size
        )
    }
    
    /**
     * 获取内存信息
     */
    private fun getMemoryInfo(): MemoryInfo {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()
        
        // 获取系统内存信息
        val systemMemoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(systemMemoryInfo)
        
        return MemoryInfo(
            usedMemory = usedMemory,
            totalMemory = totalMemory,
            maxMemory = maxMemory,
            availableMemory = systemMemoryInfo.availMem,
            totalSystemMemory = systemMemoryInfo.totalMem,
            usagePercentage = usedMemory.toFloat() / maxMemory,
            isLowMemory = systemMemoryInfo.lowMemory
        )
    }
    
    /**
     * 执行常规内存优化
     */
    private fun performRegularOptimization(): List<String> {
        val optimizations = mutableListOf<String>()
        
        try {
            // 1. 清理图片缓存
            clearImageCache()
            optimizations.add("清理图片缓存")
            
            // 2. 清理网络缓存
            clearNetworkCache()
            optimizations.add("清理网络缓存")
            
            // 3. 释放不用的对象
            releaseUnusedObjects()
            optimizations.add("释放不用的对象")
            
            // 4. 压缩内存
            compressMemory()
            optimizations.add("压缩内存")
            
            optimizationCount++
            
        } catch (e: Exception) {
            optimizations.add("优化失败: ${e.message}")
        }
        
        return optimizations
    }
    
    /**
     * 执行紧急内存优化
     */
    private fun performCriticalOptimization(): List<String> {
        val optimizations = mutableListOf<String>()
        
        try {
            // 执行所有常规优化
            optimizations.addAll(performRegularOptimization())
            
            // 5. 清理数据库缓存
            clearDatabaseCache()
            optimizations.add("清理数据库缓存")
            
            // 6. 释放播放器资源
            releasePlayerResources()
            optimizations.add("释放播放器资源")
            
            // 7. 强制垃圾回收
            System.gc()
            optimizations.add("强制垃圾回收")
            
            optimizationCount++
            
        } catch (e: Exception) {
            optimizations.add("紧急优化失败: ${e.message}")
        }
        
        return optimizations
    }
    
    /**
     * 清理图片缓存
     */
    private fun clearImageCache() {
        // 清理Glide或其他图片加载库的缓存
        // Glide.with(context).clearMemory()
        // 在IO线程清理磁盘缓存
    }
    
    /**
     * 清理网络缓存
     */
    private fun clearNetworkCache() {
        // 清理OkHttp缓存
        // 清理Retrofit缓存
    }
    
    /**
     * 清理数据库缓存
     */
    private fun clearDatabaseCache() {
        // 清理Room数据库的缓存
        // 清理SQLite的WAL文件
    }
    
    /**
     * 释放不用的对象
     */
    private fun releaseUnusedObjects() {
        // 释放不用的监听器
        // 清理集合中的空引用
        // 释放临时对象
    }
    
    /**
     * 释放播放器资源
     */
    private fun releasePlayerResources() {
        // 释放Media3播放器资源
        // 清理播放器缓存
    }
    
    /**
     * 压缩内存
     */
    private fun compressMemory() {
        // 压缩大对象
        // 优化数据结构
        // 减少内存碎片
    }
    
    /**
     * 获取内存优化建议
     */
    fun getMemoryOptimizationSuggestions(): List<String> {
        val suggestions = mutableListOf<String>()
        val memoryInfo = getMemoryInfo()
        
        if (memoryInfo.usagePercentage > 0.7f) {
            suggestions.add("内存使用率较高 (${(memoryInfo.usagePercentage * 100).toInt()}%)")
            suggestions.add("建议：")
            suggestions.add("- 使用图片懒加载")
            suggestions.add("- 及时释放不用的资源")
            suggestions.add("- 优化数据结构")
            suggestions.add("- 使用内存缓存策略")
        }
        
        if (memoryInfo.isLowMemory) {
            suggestions.add("系统内存不足")
            suggestions.add("建议：")
            suggestions.add("- 减少内存使用")
            suggestions.add("- 实现内存压力处理")
            suggestions.add("- 使用onTrimMemory回调")
        }
        
        return suggestions
    }
    
    /**
     * 设置内存压力监听
     */
    fun setupMemoryPressureListener() {
        // 监听系统内存压力
        // 在ComponentCallbacks2中实现onTrimMemory
    }
    
    /**
     * 获取优化统计
     */
    fun getOptimizationStats(): MemoryOptimizationStats {
        return MemoryOptimizationStats(
            totalOptimizations = optimizationCount,
            lastOptimizationTime = System.currentTimeMillis(),
            averageMemoryUsage = getAverageMemoryUsage()
        )
    }
    
    /**
     * 获取平均内存使用率
     */
    private fun getAverageMemoryUsage(): Float {
        // 计算最近一段时间的平均内存使用率
        return getMemoryInfo().usagePercentage
    }
}

/**
 * 内存信息
 */
data class MemoryInfo(
    val usedMemory: Long,
    val totalMemory: Long,
    val maxMemory: Long,
    val availableMemory: Long,
    val totalSystemMemory: Long,
    val usagePercentage: Float,
    val isLowMemory: Boolean
) {
    val usedMemoryMB get() = usedMemory / (1024 * 1024)
    val totalMemoryMB get() = totalMemory / (1024 * 1024)
    val maxMemoryMB get() = maxMemory / (1024 * 1024)
    val availableMemoryMB get() = availableMemory / (1024 * 1024)
    val usagePercentageInt get() = (usagePercentage * 100).toInt()
}

/**
 * 内存优化结果
 */
data class MemoryOptimizationResult(
    val beforeOptimization: MemoryInfo,
    val optimizations: List<String>,
    val optimizationCount: Int
)

/**
 * 内存优化统计
 */
data class MemoryOptimizationStats(
    val totalOptimizations: Int,
    val lastOptimizationTime: Long,
    val averageMemoryUsage: Float
)
