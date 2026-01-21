package com.jingran.taskmanager.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.jingran.taskmanager.data.entity.LongTermTask

/**
 * 长期任务数据访问对象
 * 提供长期任务的数据库操作方法
 */
@Dao
interface LongTermTaskDao {
    
    @Query("SELECT * FROM long_term_tasks ORDER BY create_time DESC")
    fun getAllTasks(): LiveData<List<LongTermTask>>
    
    @Query("SELECT * FROM long_term_tasks WHERE is_completed = 0 ORDER BY create_time DESC")
    fun getIncompleteTasks(): LiveData<List<LongTermTask>>
    
    @Query("SELECT * FROM long_term_tasks WHERE id = :id")
    fun getTaskByIdLiveData(id: Long): LiveData<LongTermTask?>

    @Query("SELECT * FROM long_term_tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): LongTermTask?
    
    @Insert
    suspend fun insertTask(task: LongTermTask): Long
    
    @Update
    suspend fun updateTask(task: LongTermTask)
    
    @Delete
    suspend fun deleteTask(task: LongTermTask)
    
    @Query("UPDATE long_term_tasks SET is_completed = :isCompleted WHERE id = :id")
    suspend fun updateTaskCompletion(id: Long, isCompleted: Boolean)
    
    @Query("DELETE FROM long_term_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)
    
    @Query("SELECT * FROM long_term_tasks ORDER BY create_time DESC")
    suspend fun getAllTasksSync(): List<LongTermTask>
}
