package com.jingran.taskmanager.data.repository

import androidx.lifecycle.LiveData
import com.jingran.taskmanager.data.dao.FixedScheduleDao
import com.jingran.taskmanager.data.entity.FixedSchedule

open class ScheduleRepository constructor(
    private val fixedScheduleDao: FixedScheduleDao
) : BaseRepository() {
    
    /**
     * 获取所有激活的日程
     */
    fun getAllActiveSchedules(): LiveData<List<FixedSchedule>> = 
        fixedScheduleDao.getAllActiveSchedules()
    
    /**
     * 根据星期几获取日程
     */
    suspend fun getSchedulesByDayOfWeek(dayOfWeek: Int): List<FixedSchedule> = ioCall {
        fixedScheduleDao.getSchedulesByDayOfWeek(dayOfWeek)
    }
    
    /**
     * 根据ID获取日程
     */
    suspend fun getScheduleById(id: Long): FixedSchedule? = ioCall {
        fixedScheduleDao.getScheduleById(id)
    }
    
    /**
     * 获取冲突的日程
     */
    suspend fun getConflictingSchedules(
        dayOfWeek: Int, 
        startTime: Long, 
        endTime: Long
    ): List<FixedSchedule> = ioCall {
        fixedScheduleDao.getConflictingSchedules(dayOfWeek, startTime, endTime)
    }
    
    /**
     * 检查时间段是否有冲突
     */
    suspend fun hasTimeConflict(
        dayOfWeek: Int, 
        startTime: Long, 
        endTime: Long,
        excludeId: Long? = null
    ): Boolean = ioCall {
        val conflicts = fixedScheduleDao.getConflictingSchedules(dayOfWeek, startTime, endTime)
        if (excludeId != null) {
            conflicts.any { it.id != excludeId }
        } else {
            conflicts.isNotEmpty()
        }
    }
    
    /**
     * 插入日程
     */
    suspend fun insertSchedule(schedule: FixedSchedule): Long = ioCall {
        fixedScheduleDao.insertSchedule(schedule)
    }
    
    /**
     * 批量插入日程
     */
    suspend fun insertSchedules(schedules: List<FixedSchedule>) = ioCall {
        fixedScheduleDao.insertSchedules(schedules)
    }
    
    /**
     * 更新日程
     */
    suspend fun updateSchedule(schedule: FixedSchedule) = ioCall {
        fixedScheduleDao.updateSchedule(schedule)
    }
    
    /**
     * 删除日程
     */
    suspend fun deleteSchedule(schedule: FixedSchedule) = ioCall {
        fixedScheduleDao.deleteSchedule(schedule)
    }
    
    /**
     * 根据ID删除日程
     */
    suspend fun deleteScheduleById(id: Long) = ioCall {
        fixedScheduleDao.deleteScheduleById(id)
    }
    
    /**
     * 更新日程状态
     */
    suspend fun updateScheduleStatus(id: Long, isActive: Boolean) = ioCall {
        fixedScheduleDao.updateScheduleStatus(id, isActive)
    }
    
    /**
     * 软删除日程（设置为非激活状态）
     */
    suspend fun softDeleteSchedule(id: Long) = ioCall {
        fixedScheduleDao.updateScheduleStatus(id, false)
    }
    
    /**
     * 批量软删除日程
     */
    suspend fun softDeleteSchedules(ids: List<Long>) = ioCall {
        ids.forEach { id ->
            fixedScheduleDao.updateScheduleStatus(id, false)
        }
    }
    
    /**
     * 获取所有日程（包括非激活的）
     */
    suspend fun getAllSchedules(): List<FixedSchedule> = ioCall {
        fixedScheduleDao.getAllSchedules()
    }
    
    /**
     * 清空所有日程
     */
    suspend fun deleteAllSchedules() = ioCall {
        fixedScheduleDao.deleteAllSchedules()
    }
    
    /**
     * 获取指定星期几的日程数量
     */
    suspend fun getScheduleCountByDay(dayOfWeek: Int): Int = ioCall {
        fixedScheduleDao.getSchedulesByDayOfWeek(dayOfWeek).size
    }
    
    /**
     * 获取一周的日程统计
     */
    suspend fun getWeeklyScheduleStats(): Map<Int, Int> = ioCall {
        val stats = mutableMapOf<Int, Int>()
        for (day in 1..7) {
            stats[day] = getScheduleCountByDay(day)
        }
        stats
    }
}