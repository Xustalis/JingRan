package com.jingran.taskmanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jingran.taskmanager.data.database.TaskDatabase
import com.jingran.taskmanager.data.entity.LongTermTask
import com.jingran.taskmanager.data.repository.TaskRepository
import com.jingran.utils.CoroutineErrorHandler
import com.jingran.utils.LogManager
import kotlinx.coroutines.launch

/**
 * 长期任务 ViewModel
 * 专注于长期任务的管理
 */
class LongTermTaskViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: TaskRepository
    
    // LiveData for UI observation
    val allLongTermTasks: LiveData<List<LongTermTask>>
    val incompleteLongTermTasks: LiveData<List<LongTermTask>>
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    companion object {
        private const val TAG = "LongTermTaskViewModel"
    }
    
    init {
        LogManager.d(TAG, "LongTermTaskViewModel initialization started")
        
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
        
        allLongTermTasks = repository.getAllLongTermTasks()
        incompleteLongTermTasks = repository.getIncompleteLongTermTasks()
        
        LogManager.d(TAG, "LongTermTaskViewModel initialization completed")
    }
    
    // 长期任务操作
    fun insertLongTermTask(task: LongTermTask) {
        LogManager.enterMethod(TAG, "insertLongTermTask")
        LogManager.d(TAG, "插入长期任务: ${task.title}")
        
        CoroutineErrorHandler.safeViewModelExecute(
            scope = viewModelScope,
            loadingState = _isLoading,
            errorState = _errorMessage,
            onError = { errorInfo ->
                LogManager.e(TAG, "插入长期任务失败: ${errorInfo.message}", errorInfo.throwable)
            }
        ) {
            repository.insertLongTermTask(task)
            LogManager.i(TAG, "长期任务插入成功: ${task.title}")
        }
    }
    
    fun updateLongTermTask(task: LongTermTask) {
        LogManager.enterMethod(TAG, "updateLongTermTask")
        LogManager.d(TAG, "更新长期任务: ${task.title}")
        
        CoroutineErrorHandler.safeViewModelExecute(
            scope = viewModelScope,
            loadingState = _isLoading,
            errorState = _errorMessage,
            onError = { errorInfo ->
                LogManager.e(TAG, "更新长期任务失败: ${errorInfo.message}", errorInfo.throwable)
            }
        ) {
            repository.updateLongTermTask(task)
            LogManager.i(TAG, "长期任务更新成功: ${task.title}")
        }
    }
    
    fun deleteLongTermTask(task: LongTermTask) {
        LogManager.enterMethod(TAG, "deleteLongTermTask")
        LogManager.d(TAG, "删除长期任务: ${task.title}")
        
        CoroutineErrorHandler.safeViewModelExecute(
            scope = viewModelScope,
            loadingState = _isLoading,
            errorState = _errorMessage,
            onError = { errorInfo ->
                LogManager.e(TAG, "删除长期任务失败: ${errorInfo.message}", errorInfo.throwable)
            }
        ) {
            repository.deleteLongTermTask(task)
            LogManager.i(TAG, "长期任务删除成功: ${task.title}")
        }
    }
    
    fun getLongTermTaskById(taskId: Long): LiveData<LongTermTask?> {
        return repository.getLongTermTaskByIdLiveData(taskId)
    }
    
    fun markLongTermTaskCompleted(taskId: Long, completed: Boolean) {
        viewModelScope.launch {
            repository.updateLongTermTaskCompletion(taskId, completed)
        }
    }
}
