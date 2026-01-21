package com.jingran.taskmanager.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.jingran.taskmanager.data.entity.FixedSchedule

/**
 * 固定日程数据访问对象
 * 提供固定日程的数据库操作方法
 */
@Dao
interface FixedScheduleDao {
    
    @Query("SELECT * FROM fixed_schedules WHERE is_active = 1 ORDER BY day_of_week ASC, start_time ASC")
    fun getAllActiveSchedules(): LiveData<List<FixedSchedule>>
    
    @Query("SELECT * FROM fixed_schedules WHERE day_of_week = :dayOfWeek AND is_active = 1 ORDER BY start_time ASC")
    suspend fun getSchedulesByDayOfWeek(dayOfWeek: Int): List<FixedSchedule>
    
    @Query("SELECT * FROM fixed_schedules WHERE id = :id")
    suspend fun getScheduleById(id: Long): FixedSchedule?
    
    @Query("""SELECT * FROM fixed_schedules 
        WHERE day_of_week = :dayOfWeek 
        AND is_active = 1 
        AND ((start_time <= :endTime AND end_time >= :startTime))
        ORDER BY start_time ASC""")
    suspend fun getConflictingSchedules(dayOfWeek: Int, startTime: Long, endTime: Long): List<FixedSchedule>
    
    @Insert
    suspend fun insertSchedule(schedule: FixedSchedule): Long
    
    @Insert
    suspend fun insertSchedules(schedules: List<FixedSchedule>)
    
    @Update
    suspend fun updateSchedule(schedule: FixedSchedule)
    
    @Delete
    suspend fun deleteSchedule(schedule: FixedSchedule)
    
    @Query("UPDATE fixed_schedules SET is_active = :isActive WHERE id = :id")
    suspend fun updateScheduleStatus(id: Long, isActive: Boolean)
    
    @Query("DELETE FROM fixed_schedules WHERE id = :id")
    suspend fun deleteScheduleById(id: Long)
    
    @Query("DELETE FROM fixed_schedules")
    suspend fun deleteAllSchedules()
    
    /**
     * 获取所有固定日程（包括非激活的）
     */
    @Query("SELECT * FROM fixed_schedules ORDER BY day_of_week ASC, start_time ASC")
    suspend fun getAllSchedules(): List<FixedSchedule>
}