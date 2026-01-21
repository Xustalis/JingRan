package com.jingran.taskmanager.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import com.jingran.taskmanager.data.database.TaskDatabase
import com.jingran.taskmanager.data.entity.LongTermTask
import com.jingran.taskmanager.data.entity.PlanItem
import com.jingran.taskmanager.data.entity.ShortTermTask
import com.jingran.taskmanager.data.entity.TaskPriority
import com.jingran.taskmanager.data.repository.TaskRepository
import com.jingran.taskmanager.data.repository.ImportRepository
import com.jingran.taskmanager.service.PlanningService
import com.jingran.taskmanager.service.EnhancedPlanningService
import com.jingran.taskmanager.service.SubTaskManager
import com.jingran.utils.NotificationHelper
import com.jingran.utils.ErrorHandler
import com.jingran.utils.CoroutineErrorHandler
import com.jingran.utils.LogManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


/**
 * 任务管理 ViewModel
 * 实现 MVVM 架构的业务逻辑层
 */
class TaskViewModel(
    application: Application,
    private val dependencies: TaskViewModelDependencies = TaskViewModelDependencies.from(application)
) : AndroidViewModel(application) {
    
    private val repository: TaskRepository = dependencies.repository
    private val importRepository: ImportRepository = dependencies.importRepository
    private val planningService: PlanningService = dependencies.planningService
    private val enhancedPlanningService: EnhancedPlanningService = dependencies.enhancedPlanningService
    private val subTaskManager: SubTaskManager = dependencies.subTaskManager
    private val notificationHelper: NotificationHelper = dependencies.notificationHelper
    
    // 提供对repository的公开访问
    fun getRepository(): TaskRepository = repository
    
    fun getImportRepository(): ImportRepository = importRepository
    
    // LiveData for UI observation
    val allShortTermTasks: LiveData<List<ShortTermTask>> = repository.getAllShortTermTasks()
    val incompleteShortTermTasks: LiveData<List<ShortTermTask>> = repository.getIncompleteShortTermTasks()
    val allLongTermTasks: LiveData<List<LongTermTask>> = repository.getAllLongTermTasks()
    val incompleteLongTermTasks: LiveData<List<LongTermTask>> = repository.getIncompleteLongTermTasks()
    val allCourseSchedules: Flow<List<com.jingran.taskmanager.data.entity.CourseSchedule>> = repository.getAllCourseSchedules()
    
    private val _todayPlanItems = MutableLiveData<List<PlanItem>>()
    val todayPlanItems: LiveData<List<PlanItem>> = _todayPlanItems
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    private val _fixedSchedules = MutableLiveData<List<com.jingran.taskmanager.data.entity.FixedSchedule>>()
    val fixedSchedules: LiveData<List<com.jingran.taskmanager.data.entity.FixedSchedule>> = _fixedSchedules
    
    companion object {
        private const val TAG = "TaskViewModel"
    }
    
    // 重新安排被推迟的任务
    private fun reschedulePostponedTasks(postponedTasks: List<ShortTermTask>, originalDate: Long) {
        CoroutineErrorHandler.safeViewModelExecute(
            scope = viewModelScope,
            loadingState = _isLoading,
            onError = { errorInfo ->
                _errorMessage.value = errorInfo.message
                LogManager.e(TAG, "重新安排任务失败: ${errorInfo.message}", errorInfo.throwable)
            }
        ) {
            val calendar = java.util.Calendar.getInstance().apply {
                timeInMillis = originalDate
            }
            
            // 尝试在接下来的3天内重新安排任务
            for (dayOffset in 1..3) {
                calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
                val targetDate = calendar.timeInMillis
                
                // 获取目标日期的现有计划
                val existingPlan = repository.getPlanItemsByDateSync(targetDate)
                val existingTaskIds = existingPlan.map { it.taskId }
                
                // 过滤出还未安排的任务
                val unscheduledTasks = postponedTasks.filter { task ->
                    !existingTaskIds.contains(task.id)
                }
                
                if (unscheduledTasks.isNotEmpty()) {
                    // 使用智能规划服务重新安排任务
                    val planningResult = enhancedPlanningService.generateIntelligentPlan(
                        date = targetDate,
                        tasks = unscheduledTasks
                    )
                    
                    // 保存新的计划项
                    if (planningResult.planItems.isNotEmpty()) {
                        repository.insertPlanItems(planningResult.planItems)
                    }
                    
                    // 如果所有任务都已安排，退出循环
                    if (planningResult.unscheduledTasks.isEmpty()) {
                        break
                    }
                }
            }
        }
    }
    
    init {
        Log.d(TAG, "TaskViewModel initialization started")
        loadTodayPlan()
        Log.d(TAG, "TaskViewModel initialization completed")
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
            // 重新生成今日计划
            regenerateTodayPlan()
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
            // 重新设置提醒
            notificationHelper.cancelTaskReminder(task.id)
            notificationHelper.scheduleTaskReminder(task)
            // 重新生成今日计划
            regenerateTodayPlan()
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
            // 取消提醒
            notificationHelper.cancelTaskReminder(task.id)
            // 重新生成今日计划
            regenerateTodayPlan()
            LogManager.i(TAG, "短期任务删除成功: ${task.title}")
        }
    }
    
    fun toggleShortTermTaskCompletion(taskId: Long, isCompleted: Boolean) {
        CoroutineErrorHandler.safeViewModelExecute(
            scope = viewModelScope,
            onError = { errorInfo ->
                _errorMessage.value = "更新任务状态失败: ${errorInfo.message}"
            }
        ) {
            repository.updateShortTermTaskCompletion(taskId, isCompleted)
        }
    }
    
    // 长期任务操作
    fun insertLongTermTask(task: LongTermTask): Long {
        LogManager.enterMethod(TAG, "insertLongTermTask")
        LogManager.d(TAG, "插入长期任务: ${task.title}")
        
        var taskId: Long = -1
        CoroutineErrorHandler.safeViewModelExecute(
            scope = viewModelScope,
            loadingState = _isLoading,
            errorState = _errorMessage,
            onError = { errorInfo ->
                LogManager.e(TAG, "插入长期任务失败: ${errorInfo.message}", errorInfo.throwable)
            }
        ) {
            taskId = repository.insertLongTermTask(task)
            LogManager.i(TAG, "长期任务插入成功: ${task.title}, ID: $taskId")
        }
        return taskId
    }
    
    fun updateLongTermTask(task: LongTermTask) {
        CoroutineErrorHandler.safeViewModelExecute(
            scope = viewModelScope,
            onError = { errorInfo ->
                _errorMessage.value = "更新长期任务失败: ${errorInfo.message}"
            }
        ) {
            repository.updateLongTermTask(task)
        }
    }
    
    fun deleteLongTermTask(task: LongTermTask) {
        CoroutineErrorHandler.safeViewModelExecute(
            scope = viewModelScope,
            onError = { errorInfo ->
                _errorMessage.value = "删除长期任务失败: ${errorInfo.message}"
            }
        ) {
            repository.deleteLongTermTask(task)
            // 重新生成今日计划
            regenerateTodayPlan()
        }
    }
    
    // 子任务关联操作
    fun getShortTasksByLongTaskId(longTaskId: Long): LiveData<List<ShortTermTask>> {
        return repository.getShortTasksByLongTaskId(longTaskId)
    }
    
    fun addSubTask(longTaskId: Long, shortTaskId: Long) {
        CoroutineErrorHandler.safeViewModelExecute(
            scope = viewModelScope,
            onError = { errorInfo ->
                _errorMessage.value = "添加子任务失败: ${errorInfo.message}"
            }
        ) {
            repository.addSubTask(longTaskId, shortTaskId)
        }
    }
    
    fun removeSubTask(longTaskId: Long, shortTaskId: Long) {
        CoroutineErrorHandler.safeViewModelExecute(
            scope = viewModelScope,
            onError = { errorInfo ->
                _errorMessage.value = "移除子任务失败: ${errorInfo.message}"
            }
        ) {
            repository.removeSubTask(longTaskId, shortTaskId)
        }
    }
    
    // 计划相关操作
    fun loadTodayPlan() {
        CoroutineErrorHandler.safeViewModelExecute(
            scope = viewModelScope,
            loadingState = _isLoading,
            errorState = _errorMessage,
            onError = { errorInfo ->
                LogManager.e("TaskViewModel", "加载今日计划失败: ${errorInfo.message}", errorInfo.throwable)
            }
        ) {
            val today = planningService.getTodayStartTimestamp()
            val planItems = repository.getPlanItemsByDateSync(today)
            _todayPlanItems.value = planItems
        }
    }
    
    fun regenerateTodayPlan() {
        CoroutineErrorHandler.safeViewModelExecute(
            scope = viewModelScope,
            loadingState = _isLoading,
            errorState = _errorMessage,
            onError = { errorInfo ->
                LogManager.e("TaskViewModel", "生成今日计划失败: ${errorInfo.message}", errorInfo.throwable)
            }
        ) {
            val today = planningService.getTodayStartTimestamp()
            
            // 获取待安排的任务
            val tasks = repository.getTasksForPlanning(today)
            
            // 生成新的计划
            val newPlanItems = planningService.generateDailyPlan(today, tasks)
            
            // 清除旧的计划
            repository.deletePlanItemsByDate(today)
            
            // 保存新的计划
            if (newPlanItems.isNotEmpty()) {
                repository.insertPlanItems(newPlanItems)
            }
            
            // 更新 UI
            _todayPlanItems.value = newPlanItems
        }
    }
    
    fun updatePlanItemCompletion(planItemId: Long, isCompleted: Boolean) {
        CoroutineErrorHandler.safeViewModelExecute(
            scope = viewModelScope,
            onError = { errorInfo ->
                _errorMessage.value = "更新计划状态失败: ${errorInfo.message}"
            }
        ) {
            repository.updatePlanItemCompletion(planItemId, isCompleted)
        }
    }
    
    // 获取任务详情
    suspend fun getShortTermTaskById(id: Long): ShortTermTask? {
        return try {
            repository.getShortTermTaskById(id)
        } catch (e: Exception) {
            _errorMessage.value = "获取任务详情失败: ${e.message}"
            null
        }
    }
    
    suspend fun getLongTermTaskById(id: Long): LongTermTask? {
        return try {
            repository.getLongTermTaskById(id)
        } catch (e: Exception) {
            _errorMessage.value = "获取长期任务详情失败: ${e.message}"
            null
        }
    }
    
    // 清除错误消息
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    
    // 生成智能每日计划
    fun generateIntelligentPlan(date: Long, userWorkingHours: List<Pair<Int, Int>>? = null) {
        CoroutineErrorHandler.safeViewModelExecute(
            scope = viewModelScope,
            onError = { errorInfo ->
                _errorMessage.value = "生成智能计划失败: ${errorInfo.message}"
            }
        ) {
            val tasks = repository.getTasksForPlanning(date)
            val result = enhancedPlanningService.generateIntelligentPlan(date, tasks, userWorkingHours)
            
            // 清除当日旧计划
            repository.deletePlanItemsByDate(date)
            
            // 保存新计划
            if (result.planItems.isNotEmpty()) {
                repository.insertPlanItems(result.planItems)
            }
            
            // 更新统计数据
            updateDailyStats(date, result.stats)
            
            _todayPlanItems.value = result.planItems
        }
    }
    
    // 插入紧急任务
    fun insertEmergencyTask(emergencyTask: ShortTermTask, date: Long) {
        CoroutineErrorHandler.safeViewModelExecute(
            scope = viewModelScope,
            onError = { errorInfo ->
                _errorMessage.value = "插入紧急任务失败: ${errorInfo.message}"
            }
        ) {
            // 先保存紧急任务
            val insertedTaskId = repository.insertShortTermTask(emergencyTask)
            val insertedTask = emergencyTask.copy(id = insertedTaskId)
            
            // 获取当前计划
            val currentPlan = repository.getPlanItemsByDateSync(date)
            
            // 使用增强规划服务插入紧急任务
            val result = enhancedPlanningService.insertEmergencyTask(insertedTask, currentPlan, date)
            
            if (result.success) {
                // 清除当日旧计划
                repository.deletePlanItemsByDate(date)
                
                // 保存调整后的计划
                if (result.adjustedPlan.isNotEmpty()) {
                    repository.insertPlanItems(result.adjustedPlan)
                }
                
                // 如果有任务被推迟，尝试重新安排到后续时间
                if (result.postponedTasks.isNotEmpty()) {
                    reschedulePostponedTasks(result.postponedTasks, date)
                }
                
                _todayPlanItems.value = result.adjustedPlan
                
                // 显示成功消息
                val message = if (result.postponedTasks.isEmpty()) {
                    "紧急任务已成功插入"
                } else {
                    "紧急任务已插入，${result.postponedTasks.size}个任务已重新安排"
                }
                _errorMessage.value = message
            } else {
                _errorMessage.value = result.message
            }
        }
    }
    
    // 更新每日统计
    private suspend fun updateDailyStats(date: Long, stats: com.jingran.taskmanager.service.PlanningStats) {
        try {
            val dailyStats = com.jingran.taskmanager.data.entity.DailyStats(
                date = date,
                totalPlannedTasks = stats.totalTasks,
                completedTasks = 0, // 初始为0，任务完成时更新
                totalPlannedDuration = stats.scheduledDuration,
                actualCompletedDuration = 0,
                completionRate = 0f,
                efficiencyRate = 0f,
                emergencyTasksAdded = 0,
                tasksPostponed = stats.unscheduledTasks,
                updateTime = System.currentTimeMillis()
            )
            
            repository.insertOrUpdateDailyStats(dailyStats)
        } catch (e: Exception) {
             Log.e(TAG, "Error updating daily stats", e)
         }
     }
     
     // 子任务管理方法
     
     /**
      * 为长期任务创建子任务
      */
     fun createSubTask(
         longTermTaskId: Long,
         subTaskTitle: String,
         description: String? = null,
         estimatedDuration: Int = 30,
         priority: TaskPriority = TaskPriority.MEDIUM
     ) {
         CoroutineErrorHandler.safeViewModelExecute(
             scope = viewModelScope,
             onError = { errorInfo ->
                 _errorMessage.value = "创建子任务失败: ${errorInfo.message}"
             }
         ) {
             val subTaskId = subTaskManager.createSubTask(
                 longTermTaskId, subTaskTitle, description, estimatedDuration, priority
             )
             
             // 数据会通过LiveData自动更新
             refreshSubTasks(longTermTaskId)
         }
     }
     
     /**
      * 完成子任务
      */
     fun completeSubTask(subTaskId: Long) {
         CoroutineErrorHandler.safeViewModelExecute(
             scope = viewModelScope,
             onError = { errorInfo ->
                 _errorMessage.value = "完成子任务失败: ${errorInfo.message}"
             }
         ) {
             subTaskManager.completeSubTask(subTaskId)
             
             // 数据会通过LiveData自动更新
         }
     }
     
     /**
      * 取消完成子任务
      */
     fun uncompleteSubTask(subTaskId: Long) {
         CoroutineErrorHandler.safeViewModelExecute(
             scope = viewModelScope,
             onError = { errorInfo ->
                 _errorMessage.value = "取消完成子任务失败: ${errorInfo.message}"
             }
         ) {
             subTaskManager.uncompleteSubTask(subTaskId)
             
             // 数据会通过LiveData自动更新
         }
     }
     
     /**
      * 删除子任务
      */
     fun deleteSubTask(subTaskId: Long) {
         CoroutineErrorHandler.safeViewModelExecute(
             scope = viewModelScope,
             onError = { errorInfo ->
                 _errorMessage.value = "删除子任务失败: ${errorInfo.message}"
             }
         ) {
             subTaskManager.deleteSubTask(subTaskId)
             
             // 数据会通过LiveData自动更新
         }
     }
     
     /**
      * 批量创建子任务
      */
     fun createSubTasksBatch(longTermTaskId: Long, subTaskTitles: List<String>) {
         CoroutineErrorHandler.safeViewModelExecute(
             scope = viewModelScope,
             onError = { errorInfo ->
                 _errorMessage.value = "批量创建子任务失败: ${errorInfo.message}"
             }
         ) {
             val subTaskIds = subTaskManager.createSubTasksBatch(longTermTaskId, subTaskTitles)
             
             // 数据会通过LiveData自动更新
             refreshSubTasks(longTermTaskId)
         }
     }
     
     /**
      * 获取长期任务进度
      */
     suspend fun getTaskProgress(longTermTaskId: Long): com.jingran.taskmanager.service.TaskProgress? {
         return try {
             subTaskManager.getTaskProgress(longTermTaskId)
         } catch (e: Exception) {
             Log.e(TAG, "Error getting task progress", e)
             null
         }
     }
     
     /**
      * 生成子任务执行计划
      */
     fun generateSubTaskSchedule(longTermTask: LongTermTask) {
         CoroutineErrorHandler.safeViewModelExecute(
             scope = viewModelScope,
             onError = { errorInfo ->
                 _errorMessage.value = "生成子任务计划失败: ${errorInfo.message}"
             }
         ) {
             val schedule = subTaskManager.generateSubTaskSchedule(longTermTask)
             // 这里可以通过LiveData通知UI显示推荐的子任务
             Log.d(TAG, "Sub task schedule: ${schedule.message}")
         }
     }
     
     /**
      * 刷新子任务数据
      */
     private fun refreshSubTasks(longTermTaskId: Long) {
         CoroutineErrorHandler.safeViewModelExecute(
             scope = viewModelScope,
             onError = { errorInfo ->
                 _errorMessage.value = "刷新子任务失败: ${errorInfo.message}"
             }
         ) {
             // 这里可以添加刷新特定长期任务的子任务列表的逻辑
             Log.d(TAG, "Refreshing subtasks for long term task: $longTermTaskId")
         }
     }
     
     /**
      * 根据星期几获取固定日程
      */
     fun loadFixedSchedulesByDay(dayOfWeek: Int) {
         CoroutineErrorHandler.safeViewModelExecute(
            scope = viewModelScope,
            loadingState = _isLoading,
            onError = { errorInfo ->
                _errorMessage.value = "加载固定日程失败: ${errorInfo.message}"
            },
             onFinally = {
                 _isLoading.value = false
             }
         ) {
             val schedules = repository.getFixedSchedulesByDay(dayOfWeek)
             _fixedSchedules.value = schedules
         }
      }
     
     /**
      * 根据日期加载计划项
      */
     fun loadPlanItemsByDate(date: Long) {
         CoroutineErrorHandler.safeViewModelExecute(
            scope = viewModelScope,
            loadingState = _isLoading,
            onError = { errorInfo ->
                _errorMessage.value = "加载计划项失败: ${errorInfo.message}"
            },
             onFinally = {
                 _isLoading.value = false
             }
         ) {
             val planItems = repository.getPlanItemsByDateSync(date)
             _todayPlanItems.value = planItems
         }
     }
     
     /**
      * 导入固定日程
      */
     fun importFixedSchedules(schedules: List<com.jingran.taskmanager.data.entity.FixedSchedule>) {
         CoroutineErrorHandler.safeViewModelExecute(
             scope = viewModelScope,
             onError = { errorInfo ->
                 _errorMessage.value = "导入课程表失败: ${errorInfo.message}"
             }
         ) {
             // 批量插入固定日程
             val insertedCount = repository.insertFixedSchedules(schedules)
             
             // 刷新固定日程数据
             val currentDayOfWeek = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
             loadFixedSchedulesByDay(currentDayOfWeek)
             
             _errorMessage.value = "成功导入 $insertedCount 个课程安排"
         }
     }
     
     /**
      * 获取每日统计数据的LiveData
      */
     fun getDailyStatsLive(date: Long): LiveData<com.jingran.taskmanager.data.entity.DailyStats?> {
         return repository.getDailyStatsLive(date)
     }
     
     /**
      * 加载每日统计数据
      */
     fun loadDailyStats(date: Long) {
         CoroutineErrorHandler.safeViewModelExecute(
             scope = viewModelScope,
             onError = { errorInfo ->
                 _errorMessage.value = "加载每日统计失败: ${errorInfo.message}"
             }
         ) {
             // 数据会通过LiveData自动更新，这里可以触发数据刷新
             val stats = repository.getDailyStats(date)
             if (stats == null) {
                 // 如果没有统计数据，可以创建一个初始的统计记录
                 val initialStats = com.jingran.taskmanager.data.entity.DailyStats(
                     date = date,
                     totalPlannedTasks = 0,
                     completedTasks = 0,
                     totalPlannedDuration = 0,
                     actualCompletedDuration = 0,
                     completionRate = 0f,
                     efficiencyRate = 0f,
                     emergencyTasksAdded = 0,
                     tasksPostponed = 0,
                     updateTime = System.currentTimeMillis()
                 )
                 repository.insertOrUpdateDailyStats(initialStats)
             }
         }
     }
     
     /**
      * 检查任务是否已完成
      */
     fun isTaskCompleted(taskId: Long): Boolean {
         return try {
             // PlanItem只关联短期任务，所以只检查短期任务
             runBlocking {
                 val task = repository.getShortTermTaskById(taskId)
                 task?.isCompleted ?: false
             }
         } catch (e: Exception) {
             Log.e(TAG, "Error checking task completion", e)
             false
         }
     }
     
     // 课程表相关操作
     fun insertCourseSchedule(course: com.jingran.taskmanager.data.entity.CourseSchedule) {
         CoroutineErrorHandler.safeViewModelExecute(
             scope = viewModelScope,
             loadingState = _isLoading,
             onError = { errorInfo ->
                 _errorMessage.value = "添加课程失败: ${errorInfo.message}"
                 LogManager.e(TAG, "添加课程失败: ${errorInfo.message}", errorInfo.throwable)
             }
         ) {
             repository.insertCourseSchedule(course)
         }
     }
     
     fun deleteCourseSchedule(course: com.jingran.taskmanager.data.entity.CourseSchedule) {
         CoroutineErrorHandler.safeViewModelExecute(
             scope = viewModelScope,
             loadingState = _isLoading,
             onError = { errorInfo ->
                 _errorMessage.value = "删除课程失败: ${errorInfo.message}"
                 LogManager.e(TAG, "删除课程失败: ${errorInfo.message}", errorInfo.throwable)
             }
         ) {
             repository.deleteCourseSchedule(course)
         }
     }
     
     fun updateCourseSchedule(course: com.jingran.taskmanager.data.entity.CourseSchedule) {
         CoroutineErrorHandler.safeViewModelExecute(
             scope = viewModelScope,
             loadingState = _isLoading,
             onError = { errorInfo ->
                 _errorMessage.value = "更新课程失败: ${errorInfo.message}"
                 LogManager.e(TAG, "更新课程失败: ${errorInfo.message}", errorInfo.throwable)
             }
         ) {
             repository.updateCourseSchedule(course)
         }
     }
}

data class TaskViewModelDependencies(
    val repository: TaskRepository,
    val importRepository: ImportRepository,
    val planningService: PlanningService,
    val enhancedPlanningService: EnhancedPlanningService,
    val subTaskManager: SubTaskManager,
    val notificationHelper: NotificationHelper
) {
    companion object {
        fun from(application: Application): TaskViewModelDependencies {
            val database = TaskDatabase.getDatabase(application)
            val repository = TaskRepository(
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
            val importRepository = ImportRepository(database.importRecordDao())
            return TaskViewModelDependencies(
                repository = repository,
                importRepository = importRepository,
                planningService = PlanningService(),
                enhancedPlanningService = EnhancedPlanningService(repository),
                subTaskManager = SubTaskManager(repository),
                notificationHelper = NotificationHelper(application)
            )
        }
    }
}
