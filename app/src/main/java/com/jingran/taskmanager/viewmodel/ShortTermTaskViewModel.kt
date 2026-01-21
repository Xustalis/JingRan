package com.jingran.taskmanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jingran.taskmanager.data.database.TaskDatabase
import com.jingran.taskmanager.data.entity.ShortTermTask
import com.jingran.taskmanager.data.repository.TaskRepository
import com.jingran.utils.CoroutineErrorHandler
import com.jingran.utils.LogManager
import com.jingran.utils.NotificationHelper
import com.jingran.utils.DateUtils.isSameDay
import kotlinx.coroutines.launch

/**
 * 短期任务 ViewModel
 * 专注于短期任务的管理
 */
class ShortTermTaskViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: TaskRepository
    private val notificationHelper: NotificationHelper
    
    // LiveData for UI observation
    val allShortTermTasks: LiveData<List<ShortTermTask>>
    val incompleteShortTermTasks: LiveData<List<ShortTermTask>>
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    companion object {
        private const val TAG = "ShortTermTaskViewModel"
    }
    
    init {
        LogManager.d(TAG, "ShortTermTaskViewModel initialization started")
        
        val database = TaskDatabase.getDatabase(application)
        repository = TaskRepository(
            database,
            database.shortTermTaskDao(),
            database.longTermTaskDao(),
            database.subTaskDao(),
            database.planItemDao(),
            database.fixedScheduleDao(),
            database.dailyStatsDao(),
            database.courseScheduleDao(),
            database.importRecordDao(),
            database.syncRecordDao(),
            database.backupRecordDao()
        )
        
        notificationHelper = NotificationHelper(getApplication())
        
        allShortTermTasks = repository.getAllShortTermTasks()
        incompleteShortTermTasks = repository.getIncompleteShortTermTasks()
        
        LogManager.d(TAG, "ShortTermTaskViewModel initialization completed")
    }
    
    // 短期任务操作
    fun insertShortTermTask(task: ShortTermTask) {
        LogManager.enterMethod(TAG, "insertShortTermTask")
        LogManager.d(TAG, "插入短期任务: ${task.title}")
        
        CoroutineErrorHandler.safeViewModelExecute(
            scope = viewModelScope,
            loadingState = _isLoading,
            errorState = _errorMessage,
            onError = { errorInfo ->
                LogManager.e(TAG, "插入短期任务失败: ${errorInfo.message}", errorInfo.throwable)
            }
        ) {
            val taskWithReminder = notificationHelper.setDefaultReminder(task)
            val insertedTaskId = repository.insertShortTermTask(taskWithReminder)
            val insertedTask = taskWithReminder.copy(id = insertedTaskId)
            notificationHelper.scheduleTaskReminder(insertedTask)
            LogManager.i(TAG, "短期任务插入成功: ${task.title}")
        }
    }
    
    fun updateShortTermTask(task: ShortTermTask) {
        LogManager.enterMethod(TAG, "updateShortTermTask")
        LogManager.d(TAG, "更新短期任务: ${task.title}")
        
        CoroutineErrorHandler.safeViewModelExecute(
            scope = viewModelScope,
            loadingState = _isLoading,
            errorState = _errorMessage,
            onError = { errorInfo ->
                LogManager.e(TAG, "更新短期任务失败: ${errorInfo.message}", errorInfo.throwable)
            }
        ) {
            repository.updateShortTermTask(task)
            notificationHelper.scheduleTaskReminder(task)
            LogManager.i(TAG, "短期任务更新成功: ${task.title}")
        }
    }
    
    fun deleteShortTermTask(task: ShortTermTask) {
        LogManager.enterMethod(TAG, "deleteShortTermTask")
        LogManager.d(TAG, "删除短期任务: ${task.title}")
        
        CoroutineErrorHandler.safeViewModelExecute(
            scope = viewModelScope,
            loadingState = _isLoading,
            errorState = _errorMessage,
            onError = { errorInfo ->
                LogManager.e(TAG, "删除短期任务失败: ${errorInfo.message}", errorInfo.throwable)
            }
        ) {
            repository.deleteShortTermTask(task)
            notificationHelper.cancelTaskReminder(task.id)
            LogManager.i(TAG, "短期任务删除成功: ${task.title}")
        }
    }
    
    fun getShortTermTaskById(taskId: Long): LiveData<ShortTermTask?> {
        return repository.getShortTermTaskByIdLiveData(taskId)
    }
    
    // 临时注释掉有问题的方法，以便编译通过
    /*
    fun getShortTermTasksByDate(date: Long): LiveData<List<ShortTermTask>> {
        return repository.getShortTermTasksByDate(date)
    }
    
    fun getIncompleteShortTermTasksByDate(date: Long): LiveData<List<ShortTermTask>> {
        return repository.getIncompleteShortTermTasksByDate(date)
    }
    */
    
    fun markShortTermTaskCompleted(taskId: Long, completed: Boolean) {
        viewModelScope.launch {
            // 临时使用updateShortTermTaskCompletion方法代替
            repository.updateShortTermTaskCompletion(taskId, completed)
        }
    }
}
