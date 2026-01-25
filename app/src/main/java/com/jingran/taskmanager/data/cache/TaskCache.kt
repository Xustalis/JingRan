package com.jingran.taskmanager.data.cache

import android.content.Context
import com.jingran.taskmanager.data.entity.*
import com.jingran.taskmanager.data.repository.TaskRepository
import com.jingran.utils.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

open class TaskCache(private val context: Context, private val repository: TaskRepository) {
    
    companion object {
        private const val TAG = "TaskCache"
        private const val DEFAULT_CACHE_SIZE = 100
        private const val DEFAULT_CACHE_DURATION_MINUTES = 5
    }
    
    private val shortTermTaskCache = ConcurrentHashMap<Long, ShortTermTask>()
    private val longTermTaskCache = ConcurrentHashMap<Long, LongTermTask>()
    private val planItemsCache = ConcurrentHashMap<Long, List<PlanItem>>()
    private val fixedSchedulesCache = ConcurrentHashMap<Int, List<FixedSchedule>>()
    private val cacheTimestamps = ConcurrentHashMap<String, Long>()
    
    private val cacheSize: Int = DEFAULT_CACHE_SIZE
    private val cacheDurationMinutes: Int = DEFAULT_CACHE_DURATION_MINUTES
    
    suspend fun getShortTermTask(id: Long): ShortTermTask? = withContext(Dispatchers.IO) {
        val cacheKey = "short_term_task_$id"
        
        if (isCacheValid(cacheKey)) {
            LogManager.d(TAG, "从缓存获取短期任务: $id")
            return@withContext shortTermTaskCache[id]
        }
        
        LogManager.d(TAG, "从数据库获取短期任务: $id")
        val task = repository.getShortTermTaskById(id)
        
        if (task != null) {
            putShortTermTask(task)
        }
        
        return@withContext task
    }
    
    suspend fun getLongTermTask(id: Long): LongTermTask? = withContext(Dispatchers.IO) {
        val cacheKey = "long_term_task_$id"
        
        if (isCacheValid(cacheKey)) {
            LogManager.d(TAG, "从缓存获取长期任务: $id")
            return@withContext longTermTaskCache[id]
        }
        
        LogManager.d(TAG, "从数据库获取长期任务: $id")
        val task = repository.getLongTermTaskById(id)
        
        if (task != null) {
            putLongTermTask(task)
        }
        
        return@withContext task
    }
    
    suspend fun getPlanItems(date: Long): List<PlanItem> = withContext(Dispatchers.IO) {
        val cacheKey = "plan_items_$date"
        
        if (isCacheValid(cacheKey)) {
            LogManager.d(TAG, "从缓存获取计划项: $date")
            return@withContext planItemsCache[date] ?: emptyList()
        }
        
        LogManager.d(TAG, "从数据库获取计划项: $date")
        val planItems = repository.getPlanItemsByDate(date)
        
        putPlanItems(date, planItems)
        
        return@withContext planItems
    }
    
    suspend fun getFixedSchedules(dayOfWeek: Int): List<FixedSchedule> = withContext(Dispatchers.IO) {
        val cacheKey = "fixed_schedules_$dayOfWeek"
        
        if (isCacheValid(cacheKey)) {
            LogManager.d(TAG, "从缓存获取固定日程: $dayOfWeek")
            return@withContext fixedSchedulesCache[dayOfWeek] ?: emptyList()
        }
        
        LogManager.d(TAG, "从数据库获取固定日程: $dayOfWeek")
        val schedules = repository.getFixedSchedulesByDay(dayOfWeek)
        
        putFixedSchedules(dayOfWeek, schedules)
        
        return@withContext schedules
    }
    
    suspend fun getAllShortTermTasks(): List<ShortTermTask> = withContext(Dispatchers.IO) {
        val cacheKey = "all_short_term_tasks"
        
        if (isCacheValid(cacheKey)) {
            LogManager.d(TAG, "从缓存获取所有短期任务")
            return@withContext shortTermTaskCache.values.toList()
        }
        
        LogManager.d(TAG, "从数据库获取所有短期任务")
        val tasks = repository.getAllShortTermTasks()
        
        tasks.forEach { putShortTermTask(it) }
        
        return@withContext tasks
    }
    
    suspend fun getAllLongTermTasks(): List<LongTermTask> = withContext(Dispatchers.IO) {
        val cacheKey = "all_long_term_tasks"
        
        if (isCacheValid(cacheKey)) {
            LogManager.d(TAG, "从缓存获取所有长期任务")
            return@withContext longTermTaskCache.values.toList()
        }
        
        LogManager.d(TAG, "从数据库获取所有长期任务")
        val tasks = repository.getAllLongTermTasks()
        
        tasks.forEach { putLongTermTask(it) }
        
        return@withContext tasks
    }
    
    suspend fun invalidateShortTermTask(id: Long) = withContext(Dispatchers.IO) {
        LogManager.d(TAG, "失效短期任务缓存: $id")
        shortTermTaskCache.remove(id)
        cacheTimestamps.remove("short_term_task_$id")
    }
    
    suspend fun invalidateLongTermTask(id: Long) = withContext(Dispatchers.IO) {
        LogManager.d(TAG, "失效长期任务缓存: $id")
        longTermTaskCache.remove(id)
        cacheTimestamps.remove("long_term_task_$id")
    }
    
    suspend fun invalidatePlanItems(date: Long) = withContext(Dispatchers.IO) {
        LogManager.d(TAG, "失效计划项缓存: $date")
        planItemsCache.remove(date)
        cacheTimestamps.remove("plan_items_$date")
    }
    
    suspend fun invalidateFixedSchedules(dayOfWeek: Int) = withContext(Dispatchers.IO) {
        LogManager.d(TAG, "失效固定日程缓存: $dayOfWeek")
        fixedSchedulesCache.remove(dayOfWeek)
        cacheTimestamps.remove("fixed_schedules_$dayOfWeek")
    }
    
    suspend fun invalidateAll() = withContext(Dispatchers.IO) {
        LogManager.d(TAG, "失效所有缓存")
        
        shortTermTaskCache.clear()
        longTermTaskCache.clear()
        planItemsCache.clear()
        fixedSchedulesCache.clear()
        cacheTimestamps.clear()
    }
    
    suspend fun invalidateExpired() = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        val expirationTime = currentTime - (cacheDurationMinutes * 60 * 1000L)
        
        val expiredKeys = cacheTimestamps.filter { it.value < expirationTime }.keys
        
        expiredKeys.forEach { key ->
            when {
                key.startsWith("short_term_task_") -> {
                    val id = key.removePrefix("short_term_task_").toLongOrNull()
                    id?.let { shortTermTaskCache.remove(it) }
                }
                key.startsWith("long_term_task_") -> {
                    val id = key.removePrefix("long_term_task_").toLongOrNull()
                    id?.let { longTermTaskCache.remove(it) }
                }
                key.startsWith("plan_items_") -> {
                    val date = key.removePrefix("plan_items_").toLongOrNull()
                    date?.let { planItemsCache.remove(it) }
                }
                key.startsWith("fixed_schedules_") -> {
                    val day = key.removePrefix("fixed_schedules_").toIntOrNull()
                    day?.let { fixedSchedulesCache.remove(it) }
                }
            }
            
            cacheTimestamps.remove(key)
        }
        
        if (expiredKeys.isNotEmpty()) {
            LogManager.d(TAG, "失效过期缓存: ${expiredKeys.size}个")
        }
    }
    
    private fun putShortTermTask(task: ShortTermTask) {
        if (shortTermTaskCache.size >= cacheSize) {
            evictOldestEntry(shortTermTaskCache)
        }
        
        shortTermTaskCache[task.id] = task
        cacheTimestamps["short_term_task_${task.id}"] = System.currentTimeMillis()
    }
    
    private fun putLongTermTask(task: LongTermTask) {
        if (longTermTaskCache.size >= cacheSize) {
            evictOldestEntry(longTermTaskCache)
        }
        
        longTermTaskCache[task.id] = task
        cacheTimestamps["long_term_task_${task.id}"] = System.currentTimeMillis()
    }
    
    private fun putPlanItems(date: Long, items: List<PlanItem>) {
        if (planItemsCache.size >= cacheSize) {
            evictOldestEntry(planItemsCache)
        }
        
        planItemsCache[date] = items
        cacheTimestamps["plan_items_$date"] = System.currentTimeMillis()
    }
    
    private fun putFixedSchedules(dayOfWeek: Int, schedules: List<FixedSchedule>) {
        if (fixedSchedulesCache.size >= cacheSize) {
            evictOldestEntry(fixedSchedulesCache)
        }
        
        fixedSchedulesCache[dayOfWeek] = schedules
        cacheTimestamps["fixed_schedules_$dayOfWeek"] = System.currentTimeMillis()
    }
    
    private fun isCacheValid(key: String): Boolean {
        val timestamp = cacheTimestamps[key] ?: return false
        
        val currentTime = System.currentTimeMillis()
        val age = currentTime - timestamp
        val maxAge = cacheDurationMinutes * 60 * 1000L
        
        return age < maxAge
    }
    
    private fun evictOldestEntry(cache: ConcurrentHashMap<*, *>) {
        var oldestKey: Any? = null
        var oldestTimestamp = Long.MAX_VALUE
        
        for (key in cache.keys) {
            val timestamp = cacheTimestamps[key] ?: continue
            
            if (timestamp < oldestTimestamp) {
                oldestTimestamp = timestamp
                oldestKey = key
            }
        }
        
        oldestKey?.let {
            cache.remove(it)
            cacheTimestamps.remove(it.toString())
        }
    }
    
    fun getCacheStatistics(): CacheStatistics {
        return CacheStatistics(
            shortTermTaskCacheSize = shortTermTaskCache.size,
            longTermTaskCacheSize = longTermTaskCache.size,
            planItemsCacheSize = planItemsCache.size,
            fixedSchedulesCacheSize = fixedSchedulesCache.size,
            totalCacheSize = shortTermTaskCache.size + longTermTaskCache.size + planItemsCache.size + fixedSchedulesCache.size,
            cacheHitRate = calculateCacheHitRate()
        )
    }
    
    private var cacheHits = 0
    private var cacheMisses = 0
    
    private fun recordCacheHit() {
        cacheHits++
    }
    
    private fun recordCacheMiss() {
        cacheMisses++
    }
    
    private fun calculateCacheHitRate(): Float {
        val total = cacheHits + cacheMisses
        return if (total > 0) {
            (cacheHits.toFloat() / total.toFloat()) * 100f
        } else {
            0f
        }
    }
    
    data class CacheStatistics(
        val shortTermTaskCacheSize: Int,
        val longTermTaskCacheSize: Int,
        val planItemsCacheSize: Int,
        val fixedSchedulesCacheSize: Int,
        val totalCacheSize: Int,
        val cacheHitRate: Float
    )
}