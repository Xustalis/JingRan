package com.jingran.taskmanager.data.repository

import androidx.lifecycle.LiveData
import com.jingran.taskmanager.data.entity.LongTermTask
import com.jingran.taskmanager.data.dao.LongTermTaskDao
import com.jingran.utils.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

open class LongTermTaskRepository(
    private val longTermTaskDao: LongTermTaskDao
) : BaseRepository() {
    
    fun getAllTasks(): LiveData<List<LongTermTask>> = longTermTaskDao.getAllTasks()
    
    fun getIncompleteTasks(): LiveData<List<LongTermTask>> = longTermTaskDao.getIncompleteTasks()
    
    suspend fun getTaskById(id: Long): LongTermTask? = withContext(Dispatchers.IO) {
        longTermTaskDao.getTaskById(id)
    }
    
    fun getTaskByIdLiveData(id: Long): LiveData<LongTermTask?> {
        return longTermTaskDao.getTaskByIdLiveData(id)
    }
    
    suspend fun insertTask(task: LongTermTask): Long {
        validateNotNull(task, "task")
        validateNotEmpty(task.title, "task.title")
        
        return ioCall("插入长期任务") {
            logDbOperation("INSERT", "long_term_tasks", "title: ${task.title}")
            longTermTaskDao.insertTask(task)
        }
    }
    
    suspend fun updateTask(task: LongTermTask) {
        validateNotNull(task, "task")
        validateInput(task.id > 0, "任务ID必须大于0")
        
        ioCall("更新长期任务") {
            logDbOperation("UPDATE", "long_term_tasks", "id: ${task.id}, title: ${task.title}")
            longTermTaskDao.updateTask(task)
        }
    }
    
    suspend fun deleteTask(task: LongTermTask) {
        validateNotNull(task, "task")
        validateInput(task.id > 0, "任务ID必须大于0")
        
        ioCall("删除长期任务") {
            logDbOperation("DELETE", "long_term_tasks", "id: ${task.id}, title: ${task.title}")
            longTermTaskDao.deleteTask(task)
        }
    }
    
    suspend fun updateTaskCompletion(id: Long, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        longTermTaskDao.updateTaskCompletion(id, isCompleted)
    }
    
    suspend fun getAllTasksList(): List<LongTermTask> = ioCall("获取所有长期任务") {
        try {
            longTermTaskDao.getAllTasks().value ?: emptyList()
        } catch (e: Exception) {
            LogManager.e(tag, "获取长期任务列表失败，返回空列表", e)
            emptyList()
        }
    }
    
    suspend fun getAllTasksSync(): List<LongTermTask> = ioCall {
        longTermTaskDao.getAllTasksSync()
    }
    
    suspend fun insertTasksBatch(tasks: List<LongTermTask>): List<Long> = ioCall("批量插入长期任务") {
        validateInput(tasks.isNotEmpty(), "任务列表不能为空")
        
        tasks.forEach { task ->
            validateLongTermTaskData(task)
        }
        
        val results = mutableListOf<Long>()
        try {
            tasks.forEach { task ->
                val id = longTermTaskDao.insertTask(task)
                results.add(id)
                logDbOperation("INSERT", "long_term_tasks", "ID: $id, Title: ${task.title}")
            }
            LogManager.d(tag, "成功批量插入${tasks.size}个长期任务")
            results
        } catch (e: Exception) {
            LogManager.e(tag, "批量插入长期任务失败", e)
            throw e
        }
    }
    
    suspend fun updateTasksBatch(tasks: List<LongTermTask>) = ioCall("批量更新长期任务") {
        validateInput(tasks.isNotEmpty(), "任务列表不能为空")
        
        tasks.forEach { task ->
            validateInput(task.id > 0, "任务ID必须大于0")
            validateLongTermTaskData(task)
        }
        
        try {
            tasks.forEach { task ->
                longTermTaskDao.updateTask(task)
                logDbOperation("UPDATE", "long_term_tasks", "ID: ${task.id}, Title: ${task.title}")
            }
            LogManager.d(tag, "成功批量更新${tasks.size}个长期任务")
        } catch (e: Exception) {
            LogManager.e(tag, "批量更新长期任务失败", e)
            throw e
        }
    }
    
    private fun validateLongTermTaskData(task: LongTermTask) {
        validateNotNull(task, "task")
        validateNotEmpty(task.title, "task.title")
        validateInput(task.title.length <= 200, "任务标题不能超过200个字符")
        validateInput(task.description.length <= 1000, "任务描述不能超过1000个字符")
        validateInput(task.startDate <= task.endDate, "开始时间不能晚于结束时间")
    }
}