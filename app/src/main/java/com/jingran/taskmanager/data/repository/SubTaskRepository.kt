package com.jingran.taskmanager.data.repository

import androidx.lifecycle.LiveData
import com.jingran.taskmanager.data.entity.SubTask
import com.jingran.taskmanager.data.entity.ShortTermTask
import com.jingran.taskmanager.data.dao.SubTaskDao
import com.jingran.taskmanager.data.dao.PlanItemDao
import com.jingran.utils.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

open class SubTaskRepository(
    private val subTaskDao: SubTaskDao,
    private val planItemDao: PlanItemDao
) : BaseRepository() {
    
    fun getShortTasksByLongTaskId(longTaskId: Long): LiveData<List<ShortTermTask>> = 
        subTaskDao.getShortTasksByLongTaskId(longTaskId)
    
    suspend fun getIncompleteShortTasksByLongTaskId(longTaskId: Long): List<ShortTermTask> = ioCall {
        subTaskDao.getIncompleteShortTasksByLongTaskId(longTaskId)
    }
    
    suspend fun addSubTask(longTaskId: Long, shortTaskId: Long) = withContext(Dispatchers.IO) {
        val subTask = SubTask(longTaskId = longTaskId, shortTaskId = shortTaskId)
        subTaskDao.insertSubTask(subTask)
    }
    
    suspend fun removeSubTask(longTaskId: Long, shortTaskId: Long) {
        validateInput(longTaskId > 0, "长期任务ID必须大于0")
        validateInput(shortTaskId > 0, "短期任务ID必须大于0")
        
        ioCall("移除子任务关联") {
            try {
                val subTasks = subTaskDao.getSubTasksByLongTaskId(longTaskId)
                val subTask = subTasks?.find { it?.shortTaskId == shortTaskId }
                
                if (subTask != null) {
                    subTaskDao.deleteSubTask(subTask)
                    LogManager.d(tag, "成功移除子任务关联: longTaskId=$longTaskId, shortTaskId=$shortTaskId")
                } else {
                    LogManager.w(tag, "未找到要移除的子任务关联: longTaskId=$longTaskId, shortTaskId=$shortTaskId")
                }
            } catch (e: Exception) {
                LogManager.e(tag, "移除子任务关联失败", e)
                throw e
            }
        }
    }
    
    suspend fun getSubTaskById(subTaskId: Long): SubTask? = withContext(Dispatchers.IO) {
        subTaskDao.getSubTaskByShortTaskId(subTaskId)
    }
    
    suspend fun getSubTaskByShortTaskId(shortTaskId: Long): SubTask? = withContext(Dispatchers.IO) {
        subTaskDao.getSubTaskByShortTaskId(shortTaskId)
    }
    
    suspend fun deleteSubTask(subTask: SubTask) = withContext(Dispatchers.IO) {
        subTaskDao.deleteSubTask(subTask)
    }
    
    suspend fun insertSubTask(subTask: SubTask) = withContext(Dispatchers.IO) {
        subTaskDao.insertSubTask(subTask)
    }
    
    suspend fun getAllSubTasks(): List<SubTask> = ioCall {
        subTaskDao.getAllSubTasks()
    }
    
    suspend fun deleteSubTaskByShortTaskId(shortTaskId: Long) = withContext(Dispatchers.IO) {
        subTaskDao.deleteSubTaskByShortTaskId(shortTaskId)
    }
    
    suspend fun deleteSubTasksByLongTaskId(longTaskId: Long) = withContext(Dispatchers.IO) {
        subTaskDao.deleteSubTasksByLongTaskId(longTaskId)
    }
    
    suspend fun deleteSubTasksByLongTaskIdWithCleanup(longTaskId: Long) {
        validateInput(longTaskId > 0, "长期任务ID必须大于0")
        
        ioCall("删除长期任务的子任务关联") {
            try {
                val subTasks = subTaskDao.getSubTasksByLongTaskId(longTaskId)
                
                subTasks?.forEach { subTask ->
                    if (subTask != null) {
                        planItemDao.deletePlanItemsByTaskId(subTask.shortTaskId)
                    }
                }
                
                subTaskDao.deleteSubTasksByLongTaskId(longTaskId)
                
                LogManager.d(tag, "成功删除长期任务的子任务关联: longTaskId=$longTaskId")
            } catch (e: Exception) {
                LogManager.e(tag, "删除长期任务的子任务关联失败", e)
                throw e
            }
        }
    }
}