package com.jingran.taskmanager.data.repository

import androidx.lifecycle.LiveData
import com.jingran.taskmanager.data.dao.LongTermTaskDao
import com.jingran.taskmanager.data.dao.SubTaskDao
import com.jingran.taskmanager.data.dao.PlanItemDao
import com.jingran.taskmanager.data.entity.LongTermTask
import com.jingran.taskmanager.data.entity.ShortTermTask
import com.jingran.taskmanager.data.entity.SubTask
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 长期任务Repository
 * 负责长期任务相关的数据访问操作
 */
@Singleton
class LongTermTaskRepository @Inject constructor(
    private val longTermTaskDao: LongTermTaskDao,
    private val subTaskDao: SubTaskDao,
    private val planItemDao: PlanItemDao
) : BaseRepository() {
    
    /**
     * 获取所有长期任务
     */
    fun getAllTasks(): LiveData<List<LongTermTask>> = longTermTaskDao.getAllTasks()
    
    /**
     * 获取未完成的长期任务
     */
    fun getIncompleteTasks(): LiveData<List<LongTermTask>> = longTermTaskDao.getIncompleteTasks()
    
    /**
     * 根据ID获取长期任务
     */
    suspend fun getTaskById(id: Long): LongTermTask? = ioCall {
        longTermTaskDao.getTaskById(id)
    }
    
    /**
     * 插入长期任务
     */
    suspend fun insertTask(task: LongTermTask): Long = ioCall {
        longTermTaskDao.insertTask(task)
    }
    
    /**
     * 更新长期任务
     */
    suspend fun updateTask(task: LongTermTask) = ioCall {
        longTermTaskDao.updateTask(task)
    }
    
    /**
     * 删除长期任务
     */
    suspend fun deleteTask(task: LongTermTask) = ioCall {
        // 获取所有子任务
        val subTasks = subTaskDao.getSubTasksByLongTaskId(task.id)
        
        // 删除所有子任务的计划项
        subTasks.forEach { subTask ->
            planItemDao.deletePlanItemsByTaskId(subTask.shortTaskId)
        }
        
        // 删除子任务关联
        subTaskDao.deleteSubTasksByLongTaskId(task.id)
        
        // 删除长期任务
        longTermTaskDao.deleteTask(task)
    }
    
    /**
     * 更新任务完成状态
     */
    suspend fun updateTaskCompletion(id: Long, isCompleted: Boolean) = ioCall {
        longTermTaskDao.updateTaskCompletion(id, isCompleted)
    }
    
    /**
     * 获取长期任务的子任务
     */
    fun getSubTasksByLongTaskId(longTaskId: Long): LiveData<List<ShortTermTask>> = 
        subTaskDao.getShortTasksByLongTaskId(longTaskId)
    
    /**
     * 获取长期任务的未完成子任务
     */
    suspend fun getIncompleteSubTasksByLongTaskId(longTaskId: Long): List<ShortTermTask> = ioCall {
        subTaskDao.getIncompleteShortTasksByLongTaskId(longTaskId)
    }
    
    /**
     * 添加子任务关联
     */
    suspend fun addSubTask(longTaskId: Long, shortTaskId: Long) = ioCall {
        val subTask = SubTask(longTaskId = longTaskId, shortTaskId = shortTaskId)
        subTaskDao.insertSubTask(subTask)
    }
    
    /**
     * 移除子任务关联
     */
    suspend fun removeSubTask(longTaskId: Long, shortTaskId: Long) = ioCall {
        val subTasks = subTaskDao.getSubTasksByLongTaskId(longTaskId)
        val subTask = subTasks.find { it.shortTaskId == shortTaskId }
        subTask?.let {
            subTaskDao.deleteSubTask(it)
        }
    }
    
    /**
     * 根据短期任务ID获取子任务关联
     */
    suspend fun getSubTaskByShortTaskId(shortTaskId: Long): SubTask? = ioCall {
        subTaskDao.getSubTaskByShortTaskId(shortTaskId)
    }
    
    /**
     * 检查并更新长期任务的完成状态
     */
    suspend fun checkAndUpdateTaskCompletion(longTaskId: Long) = ioCall {
        val incompleteTasks = subTaskDao.getIncompleteShortTasksByLongTaskId(longTaskId)
        val isCompleted = incompleteTasks.isEmpty()
        longTermTaskDao.updateTaskCompletion(longTaskId, isCompleted)
    }
    
    /**
     * 获取所有子任务关联记录
     */
    suspend fun getAllSubTasks(): List<SubTask> = ioCall {
        subTaskDao.getAllSubTasks()
    }
}