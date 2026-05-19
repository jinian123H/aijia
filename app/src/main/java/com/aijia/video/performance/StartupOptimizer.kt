package com.aijia.video.performance

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 启动优化器
 */
@Singleton
class StartupOptimizer @Inject constructor(
    @ApplicationContext private val context: Context
) : DefaultLifecycleObserver {
    
    private var startupStartTime: Long = 0
    private var coldStartTime: Long = 0
    private var warmStartTime: Long = 0
    private var hotStartTime: Long = 0
    
    // 启动阶段时间记录
    private val startupPhases = mutableMapOf<StartupPhase, Long>()
    
    init {
        // 记录应用启动开始时间
        startupStartTime = System.currentTimeMillis()
    }
    
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        recordPhase(StartupPhase.ON_CREATE)
    }
    
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        recordPhase(StartupPhase.ON_START)
    }
    
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        recordPhase(StartupPhase.ON_RESUME)
        calculateStartupTimes()
    }
    
    /**
     * 记录启动阶段
     */
    fun recordPhase(phase: StartupPhase) {
        val currentTime = System.currentTimeMillis()
        startupPhases[phase] = currentTime - startupStartTime
    }
    
    /**
     * 计算启动时间
     */
    private fun calculateStartupTimes() {
        val totalTime = System.currentTimeMillis() - startupStartTime
        
        // 判断启动类型（简化实现）
        when {
            totalTime > 3000 -> {
                coldStartTime = totalTime
            }
            totalTime > 1000 -> {
                warmStartTime = totalTime
            }
            else -> {
                hotStartTime = totalTime
            }
        }
    }
    
    /**
     * 获取启动报告
     */
    fun getStartupReport(): StartupReport {
        return StartupReport(
            coldStartTime = coldStartTime,
            warmStartTime = warmStartTime,
            hotStartTime = hotStartTime,
            phases = startupPhases.toMap(),
            recommendations = getStartupRecommendations()
        )
    }
    
    /**
     * 获取启动优化建议
     */
    private fun getStartupRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        
        // 冷启动优化建议
        if (coldStartTime > 3000) {
            recommendations.add("冷启动时间过长 (${coldStartTime}ms)，建议：")
            recommendations.add("- 减少Application初始化工作")
            recommendations.add("- 使用懒加载初始化非必需组件")
            recommendations.add("- 优化布局加载")
        }
        
        // 热启动优化建议
        if (warmStartTime > 1500) {
            recommendations.add("热启动时间过长 (${warmStartTime}ms)，建议：")
            recommendations.add("- 优化Activity重建流程")
            recommendations.add("- 减少onCreate中的工作")
        }
        
        // 阶段优化建议
        startupPhases[StartupPhase.ON_CREATE]?.let { time ->
            if (time > 1000) {
                recommendations.add("onCreate阶段耗时过长 (${time}ms)，建议：")
                recommendations.add("- 移除onCreate中的耗时操作")
                recommendations.add("- 使用异步初始化")
            }
        }
        
        return recommendations
    }
    
    /**
     * 预初始化关键组件
     */
    fun preInitializeCriticalComponents() {
        // 在后台线程预初始化关键组件
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 预初始化数据库
                // preInitializeDatabase()
                
                // 预初始化网络库
                // preInitializeNetwork()
                
                // 预初始化图片加载库
                // preInitializeImageLoader()
                
                // 预初始化播放器
                // preInitializePlayer()
                
            } catch (e: Exception) {
                // 记录预初始化异常
            }
        }
    }
    
    /**
     * 优化启动流程
     */
    fun optimizeStartupFlow() {
        // 1. 延迟初始化非关键组件
        // 2. 使用启动屏幕优化用户体验
        // 3. 预加载关键数据
        // 4. 优化布局层次
        // 5. 减少启动时的I/O操作
    }
    
    /**
     * 重置启动时间记录
     */
    fun reset() {
        startupStartTime = System.currentTimeMillis()
        startupPhases.clear()
        coldStartTime = 0
        warmStartTime = 0
        hotStartTime = 0
    }
}

/**
 * 启动阶段
 */
enum class StartupPhase {
    APPLICATION_CREATE,
    ON_CREATE,
    ON_START,
    ON_RESUME,
    FIRST_DRAW,
    DATA_LOADED,
    UI_READY
}

/**
 * 启动报告
 */
data class StartupReport(
    val coldStartTime: Long,
    val warmStartTime: Long,
    val hotStartTime: Long,
    val phases: Map<StartupPhase, Long>,
    val recommendations: List<String>
) {
    val totalStartupTime get() = coldStartTime.coerceAtLeast(warmStartTime).coerceAtLeast(hotStartTime)
    
    fun formatTime(timeMs: Long): String {
        return when {
            timeMs < 1000 -> "${timeMs}ms"
            timeMs < 60000 -> "${timeMs / 1000}.${(timeMs % 1000) / 100}s"
            else -> "${timeMs / 60000}m ${(timeMs % 60000) / 1000}s"
        }
    }
}
