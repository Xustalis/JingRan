package com.jingran.taskmanager.data.repository

import androidx.lifecycle.LiveData
import com.jingran.taskmanager.data.dao.ShortTermTaskDao
import com.jingran.taskmanager.data.dao.PlanItemDao
import com.jingran.taskmanager.data.dao.SubTaskDao
import com.jingran.taskmanager.data.entity.ShortTermTask
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 短期任务Repository
 * 负责短期任务相关的数据访问操作
 */
@Singleton
class ShortTermTaskRepository @Inject constructor(
    private val shortTermTaskDao: ShortTermTaskDao,
    private val planItemDao: PlanItemDao,
    private val subTaskDao: SubTaskDao
) : BaseRepository() {
    
    /**
     * 获取所有短期任务
     */
    fun getAllTasks(): LiveData<List<ShortTermTask>> = shortTermTaskDao.getAllTasks()
    
    /**
     * 获取未完成的短期任务
     */
    fun getIncompleteTasks(): LiveData<List<ShortTermTask>> = shortTermTaskDao.getIncompleteTasks()
    
    /**
     * 根据ID获取短期任务
     */
    suspend fun getTaskById(id: Long): ShortTermTask? = ioCall {
        shortTermTaskDao.getTaskById(id)
    }
    
    /**
     * 根据多个ID获取短期任务
     */
    suspend fun getTasksByIds(ids: List<Long>): List<ShortTermTask> = ioCall {
        shortTermTaskDao.getTasksByIds(ids)
    }
    
    /**
     * 获取用于规划的任务
     */
    suspend fun getTasksForPlanning(today: Long): List<ShortTermTask> = ioCall {
        shortTermTaskDao.getTasksForPlanning(today)
    }
    
    /**
     * 获取今天有截止日期的任务
     */
    suspend fun getTasksWithDeadlineToday(startOfDay: Long, endOfDay: Long): List<ShortTermTask> = ioCall {
        shortTermTaskDao.getTasksWithDeadlineToday(startOfDay, endOfDay)
    }
    
    /**
     * 插入短期任务
     */
    suspend fun insertTask(task: ShortTermTask): Long = ioCall {
        shortTermTaskDao.insertTask(task)
    }
    
    /**
     * 更新短期任务
     */
    suspend fun updateTask(task: ShortTermTask) = ioCall {
        shortTermTaskDao.updateTask(task)
    }
    
    /**
     * 删除短期任务
     */
    suspend fun deleteTask(task: ShortTermTask) = ioCall {
        // 删除相关的计划项
        planItemDao.deletePlanItemsByTaskId(task.id)
        // 删除子任务关联
        subTaskDao.deleteSubTaskByShortTaskId(task.id)
        // 删除任务
        shortTermTaskDao.deleteTask(task)
    }
    
    /**
     * 根据ID删除短期任务
     */
    suspend fun deleteTaskById(id: Long) = ioCall {
        // 删除相关的计划项
        planItemDao.deletePlanItemsByTaskId(id)
        // 删除子任务关联
        subTaskDao.deleteSubTaskByShortTaskId(id)
        // 删除任务
        shortTermTaskDao.deleteTaskById(id)
    }
    
    /**
     * 更新任务完成状态
     */
    suspend fun updateTaskCompletion(id: Long, isCompleted: Boolean) = ioCall {
        shortTermTaskDao.updateTaskCompletion(id, isCompleted)
    }
    
    /**
     * 批量更新任务完成状态
     */
    suspend fun updateTasksCompletion(taskIds: List<Long>, isCompleted: Boolean) = ioCall {
        taskIds.forEach { id ->
            shortTermTaskDao.updateTaskCompletion(id, isCompleted)
        }
    }
}