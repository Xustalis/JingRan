package com.jingran.taskmanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jingran.taskmanager.data.database.TaskDatabase
import com.jingran.taskmanager.data.entity.DailyStats
import com.jingran.taskmanager.data.entity.PlanItem
import com.jingran.taskmanager.data.entity.ShortTermTask
import com.jingran.taskmanager.data.entity.FixedSchedule
import com.jingran.taskmanager.data.repository.TaskRepository
import com.jingran.taskmanager.service.EnhancedPlanningService
import com.jingran.utils.CoroutineErrorHandler
import com.jingran.utils.LogManager
import com.jingran.utils.DateUtils.isSameDay
import kotlinx.coroutines.launch

/**
 * 计划管理 ViewModel
 * 专注于日程计划的管理
 */
class PlanningViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: TaskRepository
    private val planningService: EnhancedPlanningService
    
    private val _todayPlanItems = MutableLiveData<List<PlanItem>>()
    val todayPlanItems: LiveData<List<PlanItem>> = _todayPlanItems
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _fixedSchedules = MutableLiveData<List<FixedSchedule>>()
    val fixedSchedules: LiveData<List<FixedSchedule>> = _fixedSchedules

    private val _dailyStats = MutableLiveData<DailyStats?>()
    val dailyStats: LiveData<DailyStats?> = _dailyStats
    
    companion object {
        private const val TAG = "PlanningViewModel"
    }
    
    init {
        LogManager.d(TAG, "PlanningViewModel initialization started")
        
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
        
        planningService = EnhancedPlanningService(repository)
        
        // 初始化今日计划
        loadTodayPlan()
        
        LogManager.d(TAG, "PlanningViewModel initialization completed")
    }
    
    fun loadTodayPlan() {
        LogManager.enterMethod(TAG, "loadTodayPlan")
        
        CoroutineErrorHandler.safeViewModelExecute(
            scope = viewModelScope,
            loadingState = _isLoading,
            errorState = _errorMessage,
            onError = { errorInfo ->
                LogManager.e(TAG, "加载今日计划失败: ${errorInfo.message}", errorInfo.throwable)
            }
        ) {
            val calendar = java.util.Calendar.getInstance()
            val todayMillis = calendar.timeInMillis
            
            val planItems = repository.getPlanItemsByDateSync(todayMillis)
            _todayPlanItems.postValue(planItems)
            
            LogManager.i(TAG, "今日计划加载成功，共${planItems.size}项")
        }
    }
    
    fun generatePlan(date: Long, tasks: List<ShortTermTask>) {
        LogManager.enterMethod(TAG, "generatePlan")
        
        CoroutineErrorHandler.safeViewModelExecute(
            scope = viewModelScope,
            loadingState = _isLoading,
            errorState = _errorMessage,
            onError = { errorInfo ->
                LogManager.e(TAG, "生成计划失败: ${errorInfo.message}", errorInfo.throwable)
            }
        ) {
            val planningResult = planningService.generateIntelligentPlan(date, tasks)
            
            // 保存计划项
            if (planningResult.planItems.isNotEmpty()) {
                repository.insertPlanItems(planningResult.planItems)
                
                // 如果是今天的计划，更新LiveData
                val calendar = java.util.Calendar.getInstance()
                val todayMillis = calendar.timeInMillis
                if (isSameDay(date, todayMillis)) {
                    loadTodayPlan()
                }
            }
            
            LogManager.i(TAG, "计划生成成功，共${planningResult.planItems.size}项")
        }
    }
    
    fun updatePlanItem(planItem: PlanItem) {
        viewModelScope.launch {
            repository.updatePlanItem(planItem)
            
            // 如果是今天的计划项，刷新今日计划
            val calendar = java.util.Calendar.getInstance()
            val todayMillis = calendar.timeInMillis
            if (isSameDay(planItem.planDate, todayMillis)) {
                loadTodayPlan()
            }
        }
    }
    
    fun deletePlanItem(planItem: PlanItem) {
        viewModelScope.launch {
            repository.deletePlanItem(planItem)
            
            // 如果是今天的计划项，刷新今日计划
            val calendar = java.util.Calendar.getInstance()
            val todayMillis = calendar.timeInMillis
            if (isSameDay(planItem.planDate, todayMillis)) {
                loadTodayPlan()
            }
        }
    }
    
    fun getPlanItemsByDate(date: Long): LiveData<List<PlanItem>> {
        return repository.getPlanItemsByDate(date)
    }

    suspend fun isTaskCompleted(taskId: Long): Boolean {
        val shortTermTask = repository.getShortTermTaskById(taskId)
        if (shortTermTask != null) {
            return shortTermTask.isCompleted
        }
        val longTermTask = repository.getLongTermTaskById(taskId)
        if (longTermTask != null) {
            return longTermTask.isCompleted
        }
        return false
    }

    fun importFixedSchedules(schedules: List<FixedSchedule>) {
        viewModelScope.launch {
            repository.insertFixedSchedules(schedules)
        }
    }

    fun insertEmergencyTask(task: ShortTermTask, date: Long) {
        viewModelScope.launch {
            val taskId = repository.insertShortTermTask(task)
            val newTask = task.copy(id = taskId)
            val planningResult = planningService.generateIntelligentPlan(date, listOf(newTask))
            if (planningResult.planItems.isNotEmpty()) {
                repository.insertPlanItems(planningResult.planItems)
                if (isSameDay(date, System.currentTimeMillis())) {
                    loadTodayPlan()
                }
            }
        }
    }

    fun loadFixedSchedulesByDay(dayOfWeek: Int) {
        viewModelScope.launch {
            _fixedSchedules.postValue(repository.getFixedSchedulesByDay(dayOfWeek))
        }
    }

    fun loadDailyStats(date: Long) {
        viewModelScope.launch {
            _dailyStats.postValue(repository.getDailyStatsByDate(date))
        }
    }
    fun getDailyStatsLive(date: Long): LiveData<DailyStats?> {
        return repository.getDailyStatsLive(date)
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
