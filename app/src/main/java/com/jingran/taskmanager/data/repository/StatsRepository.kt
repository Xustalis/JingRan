package com.jingran.taskmanager.data.repository

import androidx.lifecycle.LiveData
import com.jingran.taskmanager.data.dao.DailyStatsDao
import com.jingran.taskmanager.data.entity.DailyStats
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 统计Repository
 * 负责效率统计相关的数据访问操作
 */
@Singleton
class StatsRepository @Inject constructor(
    private val dailyStatsDao: DailyStatsDao
) : BaseRepository() {
    
    /**
     * 根据日期获取统计数据
     */
    suspend fun getStatsByDate(date: Long): DailyStats? = ioCall {
        dailyStatsDao.getStatsByDate(date)
    }
    
    /**
     * 根据日期获取统计数据（LiveData）
     */
    fun getStatsByDateLive(date: Long): LiveData<DailyStats?> = 
        dailyStatsDao.getStatsByDateLive(date)
    
    /**
     * 获取指定日期范围内的统计数据
     */
    suspend fun getStatsInRange(startDate: Long, endDate: Long): List<DailyStats> = ioCall {
        dailyStatsDao.getStatsInRange(startDate, endDate)
    }
    
    /**
     * 获取最近的统计数据
     */
    suspend fun getRecentStats(limit: Int): List<DailyStats> = ioCall {
        dailyStatsDao.getRecentStats(limit)
    }
    
    /**
     * 获取一周的统计数据
     */
    fun getWeeklyStats(): LiveData<List<DailyStats>> = dailyStatsDao.getWeeklyStats()
    
    /**
     * 获取平均完成率
     */
    suspend fun getAverageCompletionRate(startDate: Long, endDate: Long): Float = ioCall {
        dailyStatsDao.getAverageCompletionRate(startDate, endDate) ?: 0f
    }
    
    /**
     * 获取平均效率率
     */
    suspend fun getAverageEfficiencyRate(startDate: Long, endDate: Long): Float = ioCall {
        dailyStatsDao.getAverageEfficiencyRate(startDate, endDate) ?: 0f
    }
    
    /**
     * 插入或更新统计数据
     */
    suspend fun insertOrUpdateStats(stats: DailyStats) = ioCall {
        dailyStatsDao.insertOrUpdateStats(stats)
    }
    
    /**
     * 更新统计数据
     */
    suspend fun updateStats(stats: DailyStats) = ioCall {
        dailyStatsDao.updateStats(stats)
    }
    
    /**
     * 删除统计数据
     */
    suspend fun deleteStats(stats: DailyStats) = ioCall {
        dailyStatsDao.deleteStats(stats)
    }
    
    /**
     * 根据日期删除统计数据
     */
    suspend fun deleteStatsByDate(date: Long) = ioCall {
        dailyStatsDao.deleteStatsByDate(date)
    }
    
    /**
     * 删除指定日期之前的旧统计数据
     */
    suspend fun deleteOldStats(beforeDate: Long) = ioCall {
        dailyStatsDao.deleteOldStats(beforeDate)
    }
    
    /**
     * 计算并更新每日统计数据
     */
    suspend fun calculateAndUpdateDailyStats(
        date: Long,
        totalTasks: Int,
        completedTasks: Int,
        totalPlannedDuration: Long,
        actualCompletedDuration: Long
    ) = ioCall {
        val completionRate = if (totalTasks > 0) {
            completedTasks.toFloat() / totalTasks
        } else 0f
        
        val efficiencyRate = if (totalPlannedDuration > 0) {
            (actualCompletedDuration.toFloat() / totalPlannedDuration).coerceAtMost(1f)
        } else 0f
        
        val stats = DailyStats(
            date = date,
            totalPlannedTasks = totalTasks,
            completedTasks = completedTasks,
            completionRate = completionRate,
            totalPlannedDuration = totalPlannedDuration.toInt(),
            actualCompletedDuration = actualCompletedDuration.toInt(),
            efficiencyRate = efficiencyRate,
            updateTime = System.currentTimeMillis()
        )
        
        dailyStatsDao.insertOrUpdateStats(stats)
    }
    
    /**
     * 获取统计摘要
     */
    suspend fun getStatsSummary(startDate: Long, endDate: Long): StatsSummary = ioCall {
        val stats = dailyStatsDao.getStatsInRange(startDate, endDate)
        
        if (stats.isEmpty()) {
            return@ioCall StatsSummary()
        }
        
        val totalDays = stats.size
        val totalTasks = stats.sumOf { it.totalPlannedTasks }
        val totalCompletedTasks = stats.sumOf { it.completedTasks }
        val avgCompletionRate = stats.map { it.completionRate }.average().toFloat()
        val avgEfficiencyRate = stats.map { it.efficiencyRate }.average().toFloat()
        val totalPlannedTime = stats.sumOf { it.totalPlannedDuration }
        val totalActualTime = stats.sumOf { it.actualCompletedDuration }
        
        StatsSummary(
            totalDays = totalDays,
            totalTasks = totalTasks,
            totalCompletedTasks = totalCompletedTasks,
            averageCompletionRate = avgCompletionRate,
            averageEfficiencyRate = avgEfficiencyRate,
            totalPlannedTime = totalPlannedTime.toLong(),
            totalActualTime = totalActualTime.toLong()
        )
    }
}

/**
 * 统计摘要数据类
 */
data class StatsSummary(
    val totalDays: Int = 0,
    val totalTasks: Int = 0,
    val totalCompletedTasks: Int = 0,
    val averageCompletionRate: Float = 0f,
    val averageEfficiencyRate: Float = 0f,
    val totalPlannedTime: Long = 0L,
    val totalActualTime: Long = 0L
) {
    val overallCompletionRate: Float
        get() = if (totalTasks > 0) totalCompletedTasks.toFloat() / totalTasks else 0f
    
    val overallEfficiencyRate: Float
        get() = if (totalPlannedTime > 0) (totalActualTime.toFloat() / totalPlannedTime).coerceAtMost(1f) else 0f
}