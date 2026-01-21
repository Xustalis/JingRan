package com.jingran.taskmanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jingran.taskmanager.data.database.TaskDatabase
import com.jingran.taskmanager.data.repository.TaskRepository
import com.jingran.taskmanager.data.repository.ImportRepository
import com.jingran.utils.LogManager

/**
 * 主界面 ViewModel
 * 用于替代原有的TaskViewModel，提供基本功能
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: TaskRepository
    private val importRepository: ImportRepository
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    companion object {
        private const val TAG = "MainViewModel"
    }
    
    init {
        LogManager.d(TAG, "MainViewModel initialization started")
        
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
        
        importRepository = ImportRepository(
            database.importRecordDao()
        )
        
        LogManager.d(TAG, "MainViewModel initialization completed")
    }
    
    // 提供对repository的公开访问
    fun getRepository(): TaskRepository = repository
    
    fun getImportRepository(): ImportRepository = importRepository
    
    // 清除错误消息
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
