package com.jingran.taskmanager.data.repository

import androidx.lifecycle.LiveData
import com.jingran.taskmanager.data.entity.ShortTermTask
import com.jingran.taskmanager.data.dao.ShortTermTaskDao
import com.jingran.utils.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

open class ShortTermTaskRepository(
    private val shortTermTaskDao: ShortTermTaskDao
) : BaseRepository() {
    
    fun getAllTasks(): LiveData<List<ShortTermTask>> = shortTermTaskDao.getAllTasks()
    
    fun getIncompleteTasks(): LiveData<List<ShortTermTask>> = shortTermTaskDao.getIncompleteTasks()
    
    suspend fun getTaskById(id: Long): ShortTermTask? = withContext(Dispatchers.IO) {
        shortTermTaskDao.getTaskById(id)
    }
    
    fun getTaskByIdLiveData(id: Long): LiveData<ShortTermTask?> {
        return shortTermTaskDao.getTaskByIdLiveData(id)
    }
    
    suspend fun insertTask(task: ShortTermTask): Long {
        validateNotNull(task, "task")
        validateNotEmpty(task.title, "task.title")
        
        return ioCall("插入短期任务") {
            logDbOperation("INSERT", "short_term_tasks", "title: ${task.title}")
            shortTermTaskDao.insertTask(task)
        }
    }
    
    suspend fun updateTask(task: ShortTermTask) {
        validateNotNull(task, "task")
        validateInput(task.id > 0, "任务ID必须大于0")
        
        ioCall("更新短期任务") {
            logDbOperation("UPDATE", "short_term_tasks", "id: ${task.id}, title: ${task.title}")
            shortTermTaskDao.updateTask(task)
        }
    }
    
    suspend fun deleteTask(task: ShortTermTask) {
        validateNotNull(task, "task")
        validateInput(task.id > 0, "任务ID必须大于0")
        
        ioCall("删除短期任务") {
            logDbOperation("DELETE", "short_term_tasks", "id: ${task.id}, title: ${task.title}")
            shortTermTaskDao.deleteTask(task)
        }
    }
    
    suspend fun updateTaskCompletion(id: Long, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        shortTermTaskDao.updateTaskCompletion(id, isCompleted)
    }
    
    suspend fun getTasksWithDeadlineToday(startOfDay: Long, endOfDay: Long): List<ShortTermTask> = withContext(Dispatchers.IO) {
        shortTermTaskDao.getTasksWithDeadlineToday(startOfDay, endOfDay)
    }
    
    suspend fun getAllTasksList(): List<ShortTermTask> = ioCall("获取所有短期任务") {
        try {
            shortTermTaskDao.getAllTasks().value ?: emptyList()
        } catch (e: Exception) {
            LogManager.e(tag, "获取短期任务列表失败，返回空列表", e)
            emptyList()
        }
    }
    
    suspend fun getAllTasksSync(): List<ShortTermTask> = ioCall {
        shortTermTaskDao.getAllTasksSync()
    }
    
    suspend fun insertTasksBatch(tasks: List<ShortTermTask>): List<Long> = ioCall("批量插入短期任务") {
        validateInput(tasks.isNotEmpty(), "任务列表不能为空")
        
        tasks.forEach { task ->
            validateTaskData(task)
        }
        
        val results = mutableListOf<Long>()
        try {
            tasks.forEach { task ->
                val id = shortTermTaskDao.insertTask(task)
                results.add(id)
                logDbOperation("INSERT", "short_term_tasks", "ID: $id, Title: ${task.title}")
            }
            LogManager.d(tag, "成功批量插入${tasks.size}个短期任务")
            results
        } catch (e: Exception) {
            LogManager.e(tag, "批量插入短期任务失败", e)
            throw e
        }
    }
    
    suspend fun updateTasksBatch(tasks: List<ShortTermTask>) = ioCall("批量更新短期任务") {
        validateInput(tasks.isNotEmpty(), "任务列表不能为空")
        
        tasks.forEach { task ->
            validateInput(task.id > 0, "任务ID必须大于0")
            validateTaskData(task)
        }
        
        try {
            tasks.forEach { task ->
                shortTermTaskDao.updateTask(task)
                logDbOperation("UPDATE", "short_term_tasks", "ID: ${task.id}, Title: ${task.title}")
            }
            LogManager.d(tag, "成功批量更新${tasks.size}个短期任务")
        } catch (e: Exception) {
            LogManager.e(tag, "批量更新短期任务失败", e)
            throw e
        }
    }
    
    private fun validateTaskData(task: ShortTermTask) {
        validateNotNull(task, "task")
        validateNotEmpty(task.title, "task.title")
        validateInput(task.title.length <= 200, "任务标题不能超过200个字符")
        validateInput(task.description?.length ?: 0 <= 1000, "任务描述不能超过1000个字符")
        validateInput(task.duration > 0, "预估时长必须大于0")
    }
}