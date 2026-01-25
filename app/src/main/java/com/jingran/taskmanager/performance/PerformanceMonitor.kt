package com.jingran.taskmanager.performance

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.jingran.utils.LogManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

open class PerformanceMonitor(private val application: Application) {
    
    companion object {
        private const val TAG = "PerformanceMonitor"
        private const val MONITOR_INTERVAL_MS = 1000L
        private const val MEMORY_WARNING_THRESHOLD = 0.8f
        private const val CPU_WARNING_THRESHOLD = 0.7f
    }
    
    private val handler = Handler(Looper.getMainLooper())
    private val metrics = ConcurrentHashMap<String, Metric>()
    private val isMonitoring = AtomicLong(0)
    
    private val monitorRunnable = object : Runnable {
        override fun run() {
            if (isMonitoring.get() > 0) {
                collectMetrics()
                analyzeMetrics()
            }
            handler.postDelayed(this, MONITOR_INTERVAL_MS)
        }
    }
    
    data class Metric(
        val name: String,
        val value: Float,
        val unit: String,
        val timestamp: Long,
        val category: MetricCategory
    )
    
    enum class MetricCategory {
        MEMORY,
        CPU,
        DATABASE,
        UI,
        NETWORK,
        CUSTOM
    }
    
    fun startMonitoring() {
        LogManager.i(TAG, "开始性能监控")
        isMonitoring.incrementAndGet()
        handler.post(monitorRunnable)
    }
    
    fun stopMonitoring() {
        LogManager.i(TAG, "停止性能监控")
        isMonitoring.decrementAndGet()
        handler.removeCallbacks(monitorRunnable)
    }
    
    private fun collectMetrics() {
        collectMemoryMetrics()
        collectDatabaseMetrics()
        collectUIMetrics()
    }
    
    private fun collectMemoryMetrics() {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory().toFloat()
        val totalMemory = runtime.totalMemory().toFloat()
        val freeMemory = runtime.freeMemory().toFloat()
        val usedMemory = totalMemory - freeMemory
        val memoryUsageRatio = usedMemory / maxMemory
        
        recordMetric(
            "memory_usage_ratio",
            memoryUsageRatio,
            "%",
            MetricCategory.MEMORY
        )
        
        recordMetric(
            "memory_used_mb",
            usedMemory / (1024 * 1024),
            "MB",
            MetricCategory.MEMORY
        )
        
        recordMetric(
            "memory_free_mb",
            freeMemory / (1024 * 1024),
            "MB",
            MetricCategory.MEMORY
        )
        
        if (memoryUsageRatio > MEMORY_WARNING_THRESHOLD) {
            LogManager.w(TAG, "内存使用率过高: ${(memoryUsageRatio * 100).toInt()}%")
        }
    }
    
    private fun collectDatabaseMetrics() {
        val activityManager = application.getSystemService(Application.ACTIVITY_SERVICE) as? android.app.ActivityManager
        
        activityManager?.let { am ->
            val memoryInfo = android.app.ActivityManager.MemoryInfo()
            am.getMemoryInfo(memoryInfo)
            
            val memoryClass = am.memoryClass
            val totalMemory = memoryInfo.totalMem
            val availableMemory = memoryInfo.availMem
            val usedMemory = totalMemory - availableMemory
            
            val memoryUsageRatio = usedMemory.toFloat() / totalMemory.toFloat()
            
            recordMetric(
                "database_memory_usage_ratio",
                memoryUsageRatio,
                "%",
                MetricCategory.DATABASE
            )
            
            if (memoryUsageRatio > MEMORY_WARNING_THRESHOLD) {
                LogManager.w(TAG, "数据库内存使用率过高: ${(memoryUsageRatio * 100).toInt()}%")
            }
        }
    }
    
    private fun collectUIMetrics() {
        val mainThread = android.os.Looper.getMainLooper().thread
        val threadCount = Thread.activeCount()
        
        recordMetric(
            "active_thread_count",
            threadCount.toFloat(),
            "count",
            MetricCategory.UI
        )
        
        if (threadCount > 10) {
            LogManager.w(TAG, "活跃线程数过多: $threadCount")
        }
    }
    
    private fun analyzeMetrics() {
        val recentMetrics = getRecentMetrics(60)
        
        analyzeMemoryTrends(recentMetrics)
        analyzePerformanceTrends(recentMetrics)
    }
    
    private fun analyzeMemoryTrends(metrics: List<Metric>) {
        val memoryMetrics = metrics.filter { it.category == MetricCategory.MEMORY }
        
        if (memoryMetrics.size < 10) return
        
        val avgMemoryUsage = memoryMetrics
            .filter { it.name == "memory_usage_ratio" }
            .map { it.value }
            .average()
        
        if (avgMemoryUsage > MEMORY_WARNING_THRESHOLD) {
            LogManager.w(TAG, "平均内存使用率过高: ${(avgMemoryUsage * 100).toInt()}%")
        }
        
        val maxMemoryUsage = memoryMetrics
            .filter { it.name == "memory_usage_ratio" }
            .map { it.value }
            .maxOrNull() ?: 0f
        
        if (maxMemoryUsage > 0.9f) {
            LogManager.e(TAG, "内存使用率接近上限: ${(maxMemoryUsage * 100).toInt()}%")
        }
    }
    
    private fun analyzePerformanceTrends(metrics: List<Metric>) {
        val memoryMetrics = metrics.filter { it.category == MetricCategory.MEMORY }
        val databaseMetrics = metrics.filter { it.category == MetricCategory.DATABASE }
        
        val memoryTrend = calculateTrend(
            memoryMetrics.filter { it.name == "memory_usage_ratio" }
        )
        
        val databaseTrend = calculateTrend(
            databaseMetrics.filter { it.name == "database_memory_usage_ratio" }
        )
        
        if (memoryTrend > 0.1f) {
            LogManager.w(TAG, "内存使用率呈上升趋势")
        }
        
        if (databaseTrend > 0.1f) {
            LogManager.w(TAG, "数据库内存使用率呈上升趋势")
        }
    }
    
    private fun calculateTrend(metrics: List<Metric>): Float {
        if (metrics.size < 2) return 0f
        
        val recent = metrics.takeLast(10)
        val older = metrics.dropLast(10)
        
        if (older.isEmpty()) return 0f
        
        val recentAvg = recent.map { it.value }.average()
        val olderAvg = older.map { it.value }.average()
        
        return if (olderAvg > 0) {
            ((recentAvg - olderAvg) / olderAvg).toFloat()
        } else {
            0f
        }
    }
    
    private fun recordMetric(name: String, value: Float, unit: String, category: MetricCategory) {
        val metric = Metric(
            name = name,
            value = value,
            unit = unit,
            timestamp = System.currentTimeMillis(),
            category = category
        )
        
        metrics["$name:${metric.timestamp}"] = metric
        
        if (metrics.size > 1000) {
            val oldestKey = metrics.keys.minOrNull()
            oldestKey?.let { metrics.remove(it) }
        }
    }
    
    fun recordCustomMetric(name: String, value: Float, unit: String) {
        recordMetric(name, value, unit, MetricCategory.CUSTOM)
    }
    
    fun recordDatabaseOperation(operation: String, durationMs: Long) {
        val durationSeconds = durationMs / 1000f
        recordMetric(
            "db_operation_${operation}",
            durationSeconds,
            "s",
            MetricCategory.DATABASE
        )
        
        if (durationSeconds > 1.0f) {
            LogManager.w(TAG, "数据库操作耗时过长: $operation, ${durationSeconds}秒")
        }
    }
    
    fun recordUIOperation(operation: String, durationMs: Long) {
        val durationSeconds = durationMs / 1000f
        recordMetric(
            "ui_operation_${operation}",
            durationSeconds,
            "s",
            MetricCategory.UI
        )
        
        if (durationSeconds > 0.5f) {
            LogManager.w(TAG, "UI操作耗时过长: $operation, ${durationSeconds}秒")
        }
    }
    
    fun recordNetworkOperation(operation: String, durationMs: Long, success: Boolean) {
        val durationSeconds = durationMs / 1000f
        recordMetric(
            "network_operation_${operation}",
            durationSeconds,
            "s",
            MetricCategory.NETWORK
        )
        
        if (!success) {
            recordMetric(
                "network_operation_${operation}_failed",
                1f,
                "count",
                MetricCategory.NETWORK
            )
        }
    }
    
    fun getRecentMetrics(seconds: Int): List<Metric> {
        val cutoffTime = System.currentTimeMillis() - (seconds * 1000L)
        return metrics.values.filter { it.timestamp >= cutoffTime }.sortedBy { it.timestamp }
    }
    
    fun getMetricsByCategory(category: MetricCategory): List<Metric> {
        return metrics.values.filter { it.category == category }.sortedBy { it.timestamp }
    }
    
    fun getMetricsByName(name: String): List<Metric> {
        return metrics.values.filter { it.name == name }.sortedBy { it.timestamp }
    }
    
    fun getPerformanceReport(): PerformanceReport {
        val allMetrics = metrics.values.toList()
        
        val memoryMetrics = allMetrics.filter { it.category == MetricCategory.MEMORY }
        val databaseMetrics = allMetrics.filter { it.category == MetricCategory.DATABASE }
        val uiMetrics = allMetrics.filter { it.category == MetricCategory.UI }
        val networkMetrics = allMetrics.filter { it.category == MetricCategory.NETWORK }
        
        val avgMemoryUsage = memoryMetrics
            .filter { it.name == "memory_usage_ratio" }
            .map { it.value }
            .average()
        
        val avgDatabaseOperationTime = databaseMetrics
            .filter { it.name.startsWith("db_operation_") }
            .map { it.value }
            .average()
        
        val avgUIOperationTime = uiMetrics
            .filter { it.name.startsWith("ui_operation_") }
            .map { it.value }
            .average()
        
        val avgNetworkOperationTime = networkMetrics
            .filter { it.name.startsWith("network_operation_") }
            .map { it.value }
            .average()
        
        return PerformanceReport(
            totalMetricsCount = allMetrics.size,
            memoryMetricsCount = memoryMetrics.size,
            databaseMetricsCount = databaseMetrics.size,
            uiMetricsCount = uiMetrics.size,
            networkMetricsCount = networkMetrics.size,
            avgMemoryUsageRatio = avgMemoryUsage.toFloat(),
            avgDatabaseOperationTime = avgDatabaseOperationTime.toFloat(),
            avgUIOperationTime = avgUIOperationTime.toFloat(),
            avgNetworkOperationTime = avgNetworkOperationTime.toFloat(),
            recommendations = generateRecommendations(
                avgMemoryUsage.toFloat(),
                avgDatabaseOperationTime.toFloat(),
                avgUIOperationTime.toFloat(),
                avgNetworkOperationTime.toFloat()
            )
        )
    }
    
    private fun generateRecommendations(
        avgMemoryUsage: Float,
        avgDatabaseOperationTime: Float,
        avgUIOperationTime: Float,
        avgNetworkOperationTime: Float
    ): List<String> {
        
        val recommendations = mutableListOf<String>()
        
        if (avgMemoryUsage > MEMORY_WARNING_THRESHOLD) {
            recommendations.add("内存使用率较高，建议优化数据加载和缓存策略")
        }
        
        if (avgDatabaseOperationTime > 1.0f) {
            recommendations.add("数据库操作耗时较长，建议优化查询和索引")
        }
        
        if (avgUIOperationTime > 0.5f) {
            recommendations.add("UI操作耗时较长，建议优化布局和渲染逻辑")
        }
        
        if (avgNetworkOperationTime > 2.0f) {
            recommendations.add("网络操作耗时较长，建议优化网络请求和缓存")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("性能表现良好，继续保持")
        }
        
        return recommendations
    }
    
    fun clearMetrics() {
        LogManager.i(TAG, "清除所有性能指标")
        metrics.clear()
    }
    
    fun clearMetricsByCategory(category: MetricCategory) {
        val keysToRemove = metrics.keys.filter { 
            metrics[it]?.category == category 
        }
        keysToRemove.forEach { metrics.remove(it) }
        
        LogManager.i(TAG, "清除${category.name}类别的性能指标，共${keysToRemove.size}个")
    }
    
    data class PerformanceReport(
        val totalMetricsCount: Int,
        val memoryMetricsCount: Int,
        val databaseMetricsCount: Int,
        val uiMetricsCount: Int,
        val networkMetricsCount: Int,
        val avgMemoryUsageRatio: Float,
        val avgDatabaseOperationTime: Float,
        val avgUIOperationTime: Float,
        val avgNetworkOperationTime: Float,
        val recommendations: List<String>
    )
}