package com.aijia.video.performance

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UI性能优化器
 */
@Singleton
class UIPerformanceOptimizer @Inject constructor() {
    
    // 帧率监控
    private var frameCount = 0
    private var lastFrameTime = 0L
    private var currentFPS = 60f
    val fps: Float
        get() = currentFPS
    
    // 重组监控
    private var recompositionCount = 0
    private var lastRecompositionTime = 0L
    
    // 渲染性能指标
    private val renderMetrics = mutableMapOf<String, RenderMetric>()
    
    /**
     * 记录帧率
     */
    fun recordFrame() {
        val currentTime = System.currentTimeMillis()
        frameCount++
        
        if (currentTime - lastFrameTime >= 1000) {
            currentFPS = frameCount.toFloat()
            frameCount = 0
            lastFrameTime = currentTime
        }
    }
    
    /**
     * 记录重组
     */
    fun recordRecomposition(composableName: String) {
        recompositionCount++
        lastRecompositionTime = System.currentTimeMillis()
        
        val metric = renderMetrics.getOrPut(composableName) {
            RenderMetric(composableName)
        }
        metric.recompositionCount++
    }
    
    /**
     * 记录渲染时间
     */
    fun recordRenderTime(composableName: String, renderTime: Long) {
        val metric = renderMetrics.getOrPut(composableName) {
            RenderMetric(composableName)
        }
        metric.addRenderTime(renderTime)
    }
    
    /**
     * 获取UI性能报告
     */
    fun getUIPerformanceReport(): UIPerformanceReport {
        return UIPerformanceReport(
            currentFPS = currentFPS,
            recompositionCount = recompositionCount,
            renderMetrics = renderMetrics.values.toList(),
            recommendations = getUIOptimizationRecommendations()
        )
    }
    
    /**
     * 获取UI优化建议
     */
    fun getUIOptimizationRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        
        // 帧率建议
        if (currentFPS < 55) {
            recommendations.add("帧率较低 (${currentFPS.toInt()}fps)，建议：")
            recommendations.add("- 减少不必要的重组")
            recommendations.add("- 优化布局层次")
            recommendations.add("- 使用LazyColumn/LazyRow")
            recommendations.add("- 避免在绘制中进行复杂计算")
        }
        
        // 重组建议
        if (recompositionCount > 100) {
            recommendations.add("重组次数过多 (${recompositionCount})，建议：")
            recommendations.add("- 使用remember缓存计算结果")
            recommendations.add("- 避免在Composable中创建新对象")
            recommendations.add("- 使用derivedStateOf优化状态")
            recommendations.add("- 将稳定的数据类标记为@Stable")
        }
        
        // 渲染时间建议
        renderMetrics.values.forEach { metric ->
            if (metric.averageRenderTime > 16) { // 超过16ms
                recommendations.add("${metric.composableName} 渲染时间过长 (${metric.averageRenderTime}ms)")
                recommendations.add("- 优化${metric.composableName}的布局")
                recommendations.add("- 减少${metric.composableName}的复杂度")
            }
        }
        
        return recommendations
    }
    
    /**
     * 优化Compose性能的技巧
     */
    fun getComposeOptimizationTips(): List<String> {
        return listOf(
            "使用remember缓存计算结果和对象",
            "使用derivedStateOf优化派生状态",
            "避免在Composable中创建新对象",
            "使用@Stable和@Immutable注解标记稳定类",
            "使用LazyColumn/LazyRow处理长列表",
            "避免在绘制中进行复杂计算",
            "使用key()帮助Compose识别重组范围",
            "将大型Composable拆分为小型组件",
            "使用LaunchedEffect处理副作用",
            "避免在重组中进行I/O操作"
        )
    }
    
    /**
     * 重置性能指标
     */
    fun resetMetrics() {
        frameCount = 0
        lastFrameTime = 0
        currentFPS = 60f
        recompositionCount = 0
        lastRecompositionTime = 0
        renderMetrics.clear()
    }
}

/**
 * 渲染指标
 */
data class RenderMetric(
    val composableName: String,
    var recompositionCount: Int = 0,
    var renderTimes: MutableList<Long> = mutableListOf()
) {
    val averageRenderTime: Float
        get() = if (renderTimes.isEmpty()) 0f else renderTimes.average().toFloat()
    
    val maxRenderTime: Long
        get() = renderTimes.maxOrNull() ?: 0L
    
    val minRenderTime: Long
        get() = renderTimes.minOrNull() ?: 0L
    
    fun addRenderTime(time: Long) {
        renderTimes.add(time)
        // 保持最近100次渲染记录
        if (renderTimes.size > 100) {
            renderTimes.removeAt(0)
        }
    }
}

/**
 * UI性能报告
 */
data class UIPerformanceReport(
    val currentFPS: Float,
    val recompositionCount: Int,
    val renderMetrics: List<RenderMetric>,
    val recommendations: List<String>
)

/**
 * Compose性能监控装饰器
 */
@Composable
fun PerformanceMonitor(
    composableName: String,
    content: @Composable () -> Unit
) {
    val performanceOptimizer = remember { UIPerformanceOptimizer() }
    
    // 记录重组
    LaunchedEffect(Unit) {
        performanceOptimizer.recordRecomposition(composableName)
    }
    
    // 记录渲染时间
    val renderStartTime = remember { System.currentTimeMillis() }
    
    DisposableEffect(Unit) {
        val renderTime = System.currentTimeMillis() - renderStartTime
        performanceOptimizer.recordRenderTime(composableName, renderTime)
        onDispose { }
    }
    
    content()
}

/**
 * 帧率监控Composable
 */
@Composable
fun FrameRateMonitor(
    onFrameRateUpdate: (Float) -> Unit
) {
    val performanceOptimizer = remember { UIPerformanceOptimizer() }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000) // 每秒更新一次
            performanceOptimizer.recordFrame()
            onFrameRateUpdate(performanceOptimizer.fps)
        }
    }
}

/**
 * 性能优化的LazyColumn
 */
@Composable
fun OptimizedLazyColumn(
    listItems: List<Any>,
    itemContent: @Composable (Any) -> Unit
) {
    androidx.compose.foundation.lazy.LazyColumn {
        items(listItems, key = { it.hashCode() }) { item ->
            key(item.hashCode()) {
                itemContent(item)
            }
        }
    }
}

/**
 * 性能优化的图片加载
 */
@Composable
fun OptimizedImage(
    url: String,
    contentDescription: String?,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    // 使用remember缓存图片URL
    val rememberedUrl = remember(url) { url }
    
    // 使用Coil进行图片加载
    // AsyncImage(
    //     model = rememberedUrl,
    //     contentDescription = contentDescription,
    //     modifier = modifier,
    //     loading = { ProgressIndicator() },
    //     error = { Icon(Icons.Default.Error, contentDescription = "加载失败") }
    // )
}
