package com.jingran.taskmanager.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.jingran.taskmanager.data.entity.SubTask
import com.jingran.taskmanager.data.entity.ShortTermTask

/**
 * 子任务关联数据访问对象
 * 提供长期任务和短期任务关联关系的数据库操作方法
 */
@Dao
interface SubTaskDao {
    
    @Query("SELECT * FROM sub_tasks WHERE longTaskId = :longTaskId")
    suspend fun getSubTasksByLongTaskId(longTaskId: Long): List<SubTask>
    
    @Query("""
        SELECT st.* FROM short_term_tasks st 
        INNER JOIN sub_tasks sub ON st.id = sub.shortTaskId 
        WHERE sub.longTaskId = :longTaskId
        ORDER BY st.create_time ASC
    """)
    fun getShortTasksByLongTaskId(longTaskId: Long): LiveData<List<ShortTermTask>>
    
    @Query("""
        SELECT st.* FROM short_term_tasks st 
        INNER JOIN sub_tasks sub ON st.id = sub.shortTaskId 
        WHERE sub.longTaskId = :longTaskId AND st.is_completed = 0
    """)
    suspend fun getIncompleteShortTasksByLongTaskId(longTaskId: Long): List<ShortTermTask>
    
    @Query("SELECT * FROM sub_tasks WHERE shortTaskId = :shortTaskId")
    suspend fun getSubTaskByShortTaskId(shortTaskId: Long): SubTask?
    
    @Insert
    suspend fun insertSubTask(subTask: SubTask)
    
    @Delete
    suspend fun deleteSubTask(subTask: SubTask)
    
    @Query("DELETE FROM sub_tasks WHERE longTaskId = :longTaskId")
    suspend fun deleteSubTasksByLongTaskId(longTaskId: Long)
    
    @Query("DELETE FROM sub_tasks WHERE shortTaskId = :shortTaskId")
    suspend fun deleteSubTaskByShortTaskId(shortTaskId: Long)
    
    /**
     * 获取所有子任务关联记录
     */
    @Query("SELECT * FROM sub_tasks")
    suspend fun getAllSubTasks(): List<SubTask>
}