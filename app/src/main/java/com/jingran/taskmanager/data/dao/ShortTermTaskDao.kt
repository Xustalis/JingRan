package com.jingran.taskmanager.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.jingran.taskmanager.data.entity.ShortTermTask

/**
 * 短期任务数据访问对象
 * 提供短期任务的数据库操作方法
 */
@Dao
interface ShortTermTaskDao {
    
    @Query("SELECT * FROM short_term_tasks ORDER BY create_time DESC")
    fun getAllTasks(): LiveData<List<ShortTermTask>>
    
    @Query("SELECT * FROM short_term_tasks WHERE is_completed = 0 ORDER BY priority DESC, create_time ASC")
    fun getIncompleteTasks(): LiveData<List<ShortTermTask>>
    
    @Query("SELECT * FROM short_term_tasks WHERE is_completed = 0 AND (deadline IS NULL OR deadline >= :today) ORDER BY CASE priority WHEN 'high' THEN 1 WHEN 'medium' THEN 2 WHEN 'low' THEN 3 END, duration ASC")
    suspend fun getTasksForPlanning(today: Long): List<ShortTermTask>
    
    @Query("SELECT * FROM short_term_tasks WHERE deadline IS NOT NULL AND deadline >= :startOfDay AND deadline < :endOfDay AND is_completed = 0")
    suspend fun getTasksWithDeadlineToday(startOfDay: Long, endOfDay: Long): List<ShortTermTask>
    
    @Query("SELECT * FROM short_term_tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): ShortTermTask?
    
    @Query("SELECT * FROM short_term_tasks WHERE id IN (:ids)")
    suspend fun getTasksByIds(ids: List<Long>): List<ShortTermTask>
    
    @Insert
    suspend fun insertTask(task: ShortTermTask): Long
    
    @Update
    suspend fun updateTask(task: ShortTermTask)
    
    @Delete
    suspend fun deleteTask(task: ShortTermTask)
    
    @Query("UPDATE short_term_tasks SET is_completed = :isCompleted WHERE id = :id")
    suspend fun updateTaskCompletion(id: Long, isCompleted: Boolean)
    
    @Query("DELETE FROM short_term_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)
    
    @Query("SELECT * FROM short_term_tasks ORDER BY create_time DESC")
    suspend fun getAllTasksSync(): List<ShortTermTask>
    
    @Query("SELECT * FROM short_term_tasks WHERE id = :id")
    fun getTaskByIdLiveData(id: Long): LiveData<ShortTermTask?>
}