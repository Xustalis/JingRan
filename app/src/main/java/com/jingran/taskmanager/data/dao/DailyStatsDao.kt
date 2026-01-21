package com.jingran.taskmanager.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.jingran.taskmanager.data.entity.DailyStats

/**
 * 每日统计数据访问对象
 * 提供效率统计的数据库操作方法
 */
@Dao
interface DailyStatsDao {
    
    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun getStatsByDate(date: Long): DailyStats?
    
    @Query("SELECT * FROM daily_stats WHERE date = :date")
    fun getStatsByDateLive(date: Long): LiveData<DailyStats?>
    
    @Query("SELECT * FROM daily_stats WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    suspend fun getStatsInRange(startDate: Long, endDate: Long): List<DailyStats>
    
    @Query("SELECT * FROM daily_stats ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentStats(limit: Int): List<DailyStats>
    
    @Query("SELECT * FROM daily_stats ORDER BY date DESC LIMIT 7")
    fun getWeeklyStats(): LiveData<List<DailyStats>>
    
    @Query("SELECT AVG(completionRate) FROM daily_stats WHERE date >= :startDate AND date <= :endDate")
    suspend fun getAverageCompletionRate(startDate: Long, endDate: Long): Float?
    
    @Query("SELECT AVG(efficiencyRate) FROM daily_stats WHERE date >= :startDate AND date <= :endDate")
    suspend fun getAverageEfficiencyRate(startDate: Long, endDate: Long): Float?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStats(stats: DailyStats)
    
    @Update
    suspend fun updateStats(stats: DailyStats)
    
    @Delete
    suspend fun deleteStats(stats: DailyStats)
    
    @Query("DELETE FROM daily_stats WHERE date = :date")
    suspend fun deleteStatsByDate(date: Long)
    
    @Query("DELETE FROM daily_stats WHERE date < :beforeDate")
    suspend fun deleteOldStats(beforeDate: Long)
}