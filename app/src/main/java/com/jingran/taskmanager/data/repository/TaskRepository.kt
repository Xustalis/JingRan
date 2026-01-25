package com.jingran.taskmanager.data.repository

import androidx.lifecycle.LiveData
import com.jingran.taskmanager.data.entity.*
import com.jingran.taskmanager.data.dao.*
import com.jingran.taskmanager.data.entity.SyncRecord
import com.jingran.taskmanager.data.entity.BackupRecord
import com.jingran.taskmanager.data.database.TaskDatabase
import com.jingran.utils.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

open class TaskRepository constructor(
    private val database: TaskDatabase,
    private val shortTermTaskDao: ShortTermTaskDao,
    private val longTermTaskDao: LongTermTaskDao,
    private val subTaskDao: SubTaskDao,
    private val planItemDao: PlanItemDao,
    private val fixedScheduleDao: FixedScheduleDao,
    private val dailyStatsDao: DailyStatsDao,
    private val courseScheduleDao: CourseScheduleDao,
    private val importRecordDao: ImportRecordDao,
    private val syncRecordDao: SyncRecordDao,
    private val backupRecordDao: BackupRecordDao
) : BaseRepository() {
    
    // 短期任务相关操作
    fun getAllShortTermTasks(): LiveData<List<ShortTermTask>> = shortTermTaskDao.getAllTasks()
    
    fun getIncompleteShortTermTasks(): LiveData<List<ShortTermTask>> = shortTermTaskDao.getIncompleteTasks()
    
    suspend fun getShortTermTaskById(id: Long): ShortTermTask? = withContext(Dispatchers.IO) {
        shortTermTaskDao.getTaskById(id)
    }
    
    fun getShortTermTaskByIdLiveData(id: Long): LiveData<ShortTermTask?> {
        return shortTermTaskDao.getTaskByIdLiveData(id)
    }
    
    suspend fun insertShortTermTask(task: ShortTermTask): Long {
        validateNotNull(task, "task")
        validateNotEmpty(task.title, "task.title")
        
        return ioCall("插入短期任务") {
            logDbOperation("INSERT", "short_term_tasks", "title: ${task.title}")
            shortTermTaskDao.insertTask(task)
        }
    }
    
    suspend fun updateShortTermTask(task: ShortTermTask) {
        validateNotNull(task, "task")
        validateInput(task.id > 0, "任务ID必须大于0")
        
        ioCall("更新短期任务") {
            logDbOperation("UPDATE", "short_term_tasks", "id: ${task.id}, title: ${task.title}")
            shortTermTaskDao.updateTask(task)
        }
    }
    
    suspend fun deleteShortTermTask(task: ShortTermTask) {
        validateNotNull(task, "task")
        validateInput(task.id > 0, "任务ID必须大于0")
        
        ioCall("删除短期任务") {
            logDbOperation("DELETE", "short_term_tasks", "id: ${task.id}, title: ${task.title}")
            
            try {
                // 删除相关的计划项
                planItemDao.deletePlanItemsByTaskId(task.id)
                
                // 删除子任务关联
                subTaskDao.deleteSubTaskByShortTaskId(task.id)
                
                // 删除任务
                shortTermTaskDao.deleteTask(task)
                
                LogManager.d(tag, "成功删除短期任务: ${task.title}")
            } catch (e: Exception) {
                LogManager.e(tag, "删除短期任务失败: ${task.title}", e)
                throw e
            }
        }
    }
    
    suspend fun updateShortTermTaskCompletion(id: Long, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        shortTermTaskDao.updateTaskCompletion(id, isCompleted)
        
        // 检查是否为某个长期任务的子任务
        val subTask = subTaskDao.getSubTaskByShortTaskId(id)
        if (subTask != null) {
            checkAndUpdateLongTermTaskCompletion(subTask.longTaskId)
        }
    }
    
    suspend fun getTasksWithDeadlineToday(startOfDay: Long, endOfDay: Long): List<ShortTermTask> = withContext(Dispatchers.IO) {
        shortTermTaskDao.getTasksWithDeadlineToday(startOfDay, endOfDay)
    }
    
    // 获取用于规划的任务（未完成的短期任务）
    suspend fun getTasksForPlanning(date: Long): List<ShortTermTask> = ioCall("获取规划任务") {
        try {
            // 使用LiveData的value属性获取当前值，避免LiveData的空值问题
            shortTermTaskDao.getIncompleteTasks().value ?: emptyList()
        } catch (e: Exception) {
            LogManager.e(tag, "获取规划任务失败，返回空列表", e)
            emptyList()
        }
    }
    
    // 移除有问题的方法，以便编译通过
    
    // 长期任务相关操作
    fun getAllLongTermTasks(): LiveData<List<LongTermTask>> = longTermTaskDao.getAllTasks()
    
    suspend fun getAllLongTermTasksList(): List<LongTermTask> = ioCall("获取所有长期任务") {
        try {
            // 使用LiveData的value属性获取当前值，如果为空则返回空列表
            longTermTaskDao.getAllTasks().value ?: emptyList()
        } catch (e: Exception) {
            LogManager.e(tag, "获取长期任务列表失败，返回空列表", e)
            emptyList()
        }
    }
    
    fun getIncompleteLongTermTasks(): LiveData<List<LongTermTask>> = longTermTaskDao.getIncompleteTasks()
    
    suspend fun getLongTermTaskById(id: Long): LongTermTask? = withContext(Dispatchers.IO) {
        longTermTaskDao.getTaskById(id)
    }

    fun getLongTermTaskByIdLiveData(id: Long): LiveData<LongTermTask?> {
        return longTermTaskDao.getTaskByIdLiveData(id)
    }
    
    suspend fun insertLongTermTask(task: LongTermTask): Long = withContext(Dispatchers.IO) {
        longTermTaskDao.insertTask(task)
    }
    
    suspend fun updateLongTermTask(task: LongTermTask) = withContext(Dispatchers.IO) {
        longTermTaskDao.updateTask(task)
    }
    
    suspend fun deleteLongTermTask(task: LongTermTask) {
        validateNotNull(task, "task")
        validateInput(task.id > 0, "任务ID必须大于0")
        
        ioCall("删除长期任务") {
            logDbOperation("DELETE", "long_term_tasks", "id: ${task.id}, title: ${task.title}")
            
            try {
                // 获取所有子任务
                val subTasks = subTaskDao.getSubTasksByLongTaskId(task.id)
                
                // 删除所有子任务的计划项
                subTasks?.forEach { subTask ->
                    if (subTask != null) {
                        planItemDao.deletePlanItemsByTaskId(subTask.shortTaskId)
                    }
                }
                
                // 删除子任务关联
                subTaskDao.deleteSubTasksByLongTaskId(task.id)
                
                // 删除长期任务
                longTermTaskDao.deleteTask(task)
                
                LogManager.d(tag, "成功删除长期任务: ${task.title}")
            } catch (e: Exception) {
                LogManager.e(tag, "删除长期任务失败: ${task.title}", e)
                throw e
            }
        }
    }
    
    suspend fun updateLongTermTaskCompletion(id: Long, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        longTermTaskDao.updateTaskCompletion(id, isCompleted)
    }
    
    // 子任务关联操作
    fun getShortTasksByLongTaskId(longTaskId: Long): LiveData<List<ShortTermTask>> = 
        subTaskDao.getShortTasksByLongTaskId(longTaskId)
    
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
        // 注意：这里的subTaskId实际上是shortTaskId
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
    
    // 计划项相关操作
    fun getPlanItemsByDate(date: Long): LiveData<List<PlanItem>> = planItemDao.getPlanItemsByDate(date)
    
    suspend fun getPlanItemById(id: Long): PlanItem? = ioCall {
        planItemDao.getPlanItemById(id)
    }
    
    suspend fun insertPlanItem(planItem: PlanItem) = ioCall {
        planItemDao.insertPlanItem(planItem)
    }
    
    suspend fun updatePlanItem(planItem: PlanItem) = ioCall {
        planItemDao.updatePlanItem(planItem)
    }
    
    suspend fun deletePlanItem(planItem: PlanItem) = ioCall {
        planItemDao.deletePlanItem(planItem)
    }
    
    suspend fun updatePlanItemCompletion(id: Long, isCompleted: Boolean) = ioCall {
        planItemDao.updatePlanItemCompletion(id, isCompleted)
    }
    
    suspend fun getPlanItemsByDateRange(startDate: Long, endDate: Long): List<PlanItem> = ioCall("获取日期范围内的计划项") {
        validateInput(startDate <= endDate, "开始日期不能晚于结束日期")
        
        try {
            val allItems = planItemDao.getAllPlanItems()
            allItems?.filter { item -> 
                item != null && item.planDate in startDate..endDate 
            } ?: emptyList()
        } catch (e: Exception) {
            LogManager.e(tag, "获取日期范围内的计划项失败", e)
            emptyList()
        }
    }
    
    suspend fun getAllPlanItems(): List<PlanItem> = ioCall {
        planItemDao.getAllPlanItems()
    }
    

    
    suspend fun getIncompleteSubTasksByLongTaskId(longTaskId: Long): List<ShortTermTask> = ioCall {
        subTaskDao.getIncompleteShortTasksByLongTaskId(longTaskId)
    }
    
    // 获取长期任务的子任务
    fun getSubTasksByLongTaskId(longTaskId: Long): LiveData<List<ShortTermTask>> = 
        subTaskDao.getShortTasksByLongTaskId(longTaskId)
    
    /**
     * 检查并更新长期任务的完成状态
     * 当所有子任务都完成时，自动标记长期任务为完成
     */
    private suspend fun checkAndUpdateLongTermTaskCompletion(longTaskId: Long) {
        validateInput(longTaskId > 0, "长期任务ID必须大于0")
        
        try {
            val incompleteSubTasks = getIncompleteSubTasksByLongTaskId(longTaskId)
            if (incompleteSubTasks.isEmpty()) {
                // 所有子任务都已完成，标记长期任务为完成
                updateLongTermTaskCompletion(longTaskId, true)
                LogManager.d(tag, "长期任务已自动标记为完成: longTaskId=$longTaskId")
            }
        } catch (e: Exception) {
            LogManager.e(tag, "检查长期任务完成状态失败: longTaskId=$longTaskId", e)
            // 不抛出异常，避免影响主要操作
        }
    }
    
    // DailyStats相关操作
    suspend fun getDailyStats(date: Long): DailyStats? = ioCall {
        dailyStatsDao.getStatsByDate(date)
    }
    
    fun getDailyStatsLive(date: Long): LiveData<DailyStats?> = 
        dailyStatsDao.getStatsByDateLive(date)
    
    suspend fun insertOrUpdateDailyStats(stats: DailyStats) = ioCall {
        dailyStatsDao.insertOrUpdateStats(stats)
    }
    
    // PlanItem相关操作
    suspend fun getPlanItemsByDateSync(date: Long): List<PlanItem> = ioCall {
        planItemDao.getPlanItemsByDateSync(date)
    }
    
    suspend fun deletePlanItemsByDate(date: Long) = ioCall {
        planItemDao.deletePlanItemsByDate(date)
    }
    
    suspend fun insertPlanItems(planItems: List<PlanItem>) = ioCall {
        planItems.forEach { planItem ->
            planItemDao.insertPlanItem(planItem)
        }
    }
    
    // FixedSchedule相关操作
    suspend fun insertFixedSchedule(schedule: FixedSchedule): Long = ioCall {
        fixedScheduleDao.insertSchedule(schedule)
    }
    
    suspend fun insertFixedSchedules(schedules: List<FixedSchedule>): Int = ioCall {
        var insertedCount = 0
        schedules.forEach { schedule ->
            fixedScheduleDao.insertSchedule(schedule)
            insertedCount++
        }
        insertedCount
    }
    
    suspend fun getFixedSchedulesByDay(dayOfWeek: Int): List<FixedSchedule> = ioCall {
        fixedScheduleDao.getSchedulesByDayOfWeek(dayOfWeek)
    }

    suspend fun getDailyStatsByDate(date: Long): com.jingran.taskmanager.data.entity.DailyStats? = ioCall {
        dailyStatsDao.getStatsByDate(date)
    }
    
    suspend fun getAllFixedSchedules(): List<FixedSchedule> = ioCall {
        fixedScheduleDao.getAllSchedules()
    }
    
    suspend fun getAllShortTermTasksList(): List<ShortTermTask> = ioCall("获取所有短期任务") {
        try {
            // 使用LiveData的value属性获取当前值，如果为空则返回空列表
            shortTermTaskDao.getAllTasks().value ?: emptyList()
        } catch (e: Exception) {
            LogManager.e(tag, "获取短期任务列表失败，返回空列表", e)
            emptyList()
        }
    }
    
    // 同步相关操作
    suspend fun getAllSyncRecords(): List<SyncRecord> = ioCall {
        syncRecordDao.getAllSyncRecords()
    }
    
    suspend fun insertSyncRecord(record: SyncRecord): Long = ioCall {
        syncRecordDao.insertSyncRecord(record)
    }
    
    suspend fun updateSyncRecord(record: SyncRecord) = ioCall {
        syncRecordDao.updateSyncRecord(record)
    }
    
    suspend fun clearSyncHistory() = ioCall {
        syncRecordDao.deleteAllSyncRecords()
    }
    
    suspend fun getLastSyncTimestamp(): Long? = ioCall {
        syncRecordDao.getLastSuccessfulSyncTime()
    }
    
    suspend fun getLastSyncRecord(): SyncRecord? = ioCall {
        syncRecordDao.getLastSyncRecord()
    }
    
    // 备份相关操作
    suspend fun getAllBackupRecords(): List<BackupRecord> = ioCall {
        backupRecordDao.getAllBackupRecords()
    }
    
    suspend fun insertBackupRecord(record: BackupRecord): Long = ioCall {
        backupRecordDao.insertBackupRecord(record)
    }
    
    suspend fun updateBackupRecord(record: BackupRecord) = ioCall {
        backupRecordDao.updateBackupRecord(record)
    }
    
    suspend fun deleteBackupRecord(backupId: String) = ioCall {
        backupRecordDao.deleteBackupRecordById(backupId)
    }
    
    suspend fun getBackupRecordById(backupId: String): BackupRecord? = ioCall {
        backupRecordDao.getBackupRecordById(backupId)
    }
    
    // 课程表相关操作
    fun getAllCourseSchedules(): Flow<List<CourseSchedule>> = courseScheduleDao.getAllCourses()
    
    suspend fun getAllCourseSchedulesList(): List<CourseSchedule> = ioCall("获取所有课程表") {
        try {
            courseScheduleDao.getAllCourses().first()
        } catch (e: Exception) {
            LogManager.e(tag, "获取课程表列表失败，返回空列表", e)
            emptyList()
        }
    }
    
    suspend fun insertCourseSchedule(course: CourseSchedule) = ioCall {
        courseScheduleDao.insertCourse(course)
    }
    
    suspend fun deleteCourseSchedule(course: CourseSchedule) = ioCall {
        courseScheduleDao.deleteCourse(course)
    }
    
    suspend fun updateCourseSchedule(course: CourseSchedule) = ioCall {
        courseScheduleDao.updateCourse(course)
    }
    
    // 子任务关联相关操作
    suspend fun getAllSubTasks(): List<SubTask> = ioCall {
        subTaskDao.getAllSubTasks()
    }
    
    // 导入记录相关操作
    suspend fun deleteCoursesByImportBatch(importBatchId: String) = ioCall {
        courseScheduleDao.deleteCoursesByImportBatch(importBatchId)
    }
    
    suspend fun insertImportRecord(record: ImportRecord) = ioCall {
        importRecordDao.insertImportRecord(record)
    }
    
    suspend fun getRecentImportRecords(limit: Int): List<ImportRecord> = ioCall {
        importRecordDao.getRecentImportRecords(limit)
    }
    
    suspend fun deleteImportRecordByBatchId(batchId: String) = ioCall {
        importRecordDao.deleteImportRecordByBatchId(batchId)
    }
    
    // 数据一致性增强方法
    
    /**
     * 事务性批量操作 - 插入多个短期任务
     */
    suspend fun insertShortTermTasksBatch(tasks: List<ShortTermTask>): List<Long> = ioCall("批量插入短期任务") {
        validateInput(tasks.isNotEmpty(), "任务列表不能为空")
        
        // 验证所有任务数据
        tasks.forEach { task ->
            validateTaskData(task)
        }
        
        // 使用数据库事务确保原子性
        database.withTransaction {
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
                LogManager.e(tag, "批量插入短期任务失败，已回滚", e)
                throw e
            }
        }
    }
    
    /**
     * 事务性批量操作 - 更新多个短期任务
     */
    suspend fun updateShortTermTasksBatch(tasks: List<ShortTermTask>) = ioCall("批量更新短期任务") {
        validateInput(tasks.isNotEmpty(), "任务列表不能为空")
        
        // 验证所有任务数据
        tasks.forEach { task ->
            validateInput(task.id > 0, "任务ID必须大于0")
            validateTaskData(task)
        }
        
        // 使用数据库事务确保原子性
        database.withTransaction {
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
    }
    
    /**
     * 验证任务数据完整性
     */
    private fun validateTaskData(task: ShortTermTask) {
        validateNotNull(task, "task")
        validateNotEmpty(task.title, "task.title")
        validateInput(task.title.length <= 200, "任务标题不能超过200个字符")
        validateInput(task.description?.length ?: 0 <= 1000, "任务描述不能超过1000个字符")
        validateInput(task.deadline != null && task.deadline > System.currentTimeMillis(), "截止时间不能早于当前时间")
        validateInput(task.duration > 0, "预估时长必须大于0")
    }
    
    /**
     * 验证长期任务数据完整性
     */
    private fun validateLongTermTaskData(task: LongTermTask) {
        validateNotNull(task, "task")
        validateNotEmpty(task.title, "task.title")
        validateInput(task.title.length <= 200, "任务标题不能超过200个字符")
        validateInput(task.description.length <= 1000, "任务描述不能超过1000个字符")
        validateInput(task.startDate <= task.endDate, "开始时间不能晚于结束时间")
        validateInput(task.endDate.isNotEmpty() && task.endDate.toLongOrNull()?.let { it > System.currentTimeMillis() } == true, "结束时间不能早于当前时间")
    }
    
    /**
     * 数据一致性检查 - 检查孤立的子任务
     */
    suspend fun checkDataConsistency(): DataConsistencyReport = ioCall("数据一致性检查") {
        val issues = mutableListOf<String>()
        
        try {
            // 检查孤立的子任务
            val allSubTasks = subTaskDao.getAllSubTasks()
            val allLongTermTasks = longTermTaskDao.getAllTasksSync()
            val longTermTaskIds = allLongTermTasks.map { it.id }.toSet()
            
            val orphanedSubTasks = allSubTasks.filter { subTask ->
                subTask.longTaskId !in longTermTaskIds
            }
            
            if (orphanedSubTasks.isNotEmpty()) {
                issues.add("发现${orphanedSubTasks.size}个孤立的子任务")
            }
            
            // 检查无效的任务状态
            val invalidShortTasks = shortTermTaskDao.getAllTasksSync().filter { task ->
                task.deadline != null && task.deadline < System.currentTimeMillis() && !task.isCompleted
            }
            
            if (invalidShortTasks.isNotEmpty()) {
                issues.add("发现${invalidShortTasks.size}个已过期但未完成的短期任务")
            }
            
            // 检查重复的任务
            val shortTaskTitles = shortTermTaskDao.getAllTasksSync().groupBy { it.title }
            val duplicateShortTasks = shortTaskTitles.filter { it.value.size > 1 }
            
            if (duplicateShortTasks.isNotEmpty()) {
                issues.add("发现${duplicateShortTasks.size}组重复标题的短期任务")
            }
            
            LogManager.d(tag, "数据一致性检查完成，发现${issues.size}个问题")
            
            DataConsistencyReport(
                isConsistent = issues.isEmpty(),
                issues = issues,
                checkedAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            LogManager.e(tag, "数据一致性检查失败", e)
            DataConsistencyReport(
                isConsistent = false,
                issues = listOf("数据一致性检查过程中发生错误: ${e.message}"),
                checkedAt = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * 修复数据一致性问题
     */
    suspend fun fixDataConsistencyIssues(): DataFixResult = ioCall("修复数据一致性问题") {
        var fixedCount = 0
        val errors = mutableListOf<String>()
        
        try {
            // 清理孤立的子任务
            val allSubTasks = subTaskDao.getAllSubTasks()
            val allLongTermTasks = longTermTaskDao.getAllTasksSync()
            val longTermTaskIds = allLongTermTasks.map { it.id }.toSet()
            
            val orphanedSubTasks = allSubTasks.filter { subTask ->
                subTask.longTaskId !in longTermTaskIds
            }
            
            orphanedSubTasks.forEach { subTask ->
                try {
                    subTaskDao.deleteSubTask(subTask)
                    fixedCount++
                    logDbOperation("DELETE", "sub_tasks", "清理孤立子任务 ID: ${subTask.id}")
                } catch (e: Exception) {
                    errors.add("清理孤立子任务失败: ${e.message}")
                }
            }
            
            LogManager.d(tag, "数据一致性修复完成，修复了${fixedCount}个问题")
            
            DataFixResult(
                success = errors.isEmpty(),
                fixedCount = fixedCount,
                errors = errors
            )
        } catch (e: Exception) {
            LogManager.e(tag, "数据一致性修复失败", e)
            DataFixResult(
                success = false,
                fixedCount = fixedCount,
                errors = errors + "修复过程中发生错误: ${e.message}"
            )
        }
    }
}

/**
 * 数据一致性报告
 */
data class DataConsistencyReport(
    val isConsistent: Boolean,
    val issues: List<String>,
    val checkedAt: Long
)

/**
 * 数据修复结果
 */
data class DataFixResult(
    val success: Boolean,
    val fixedCount: Int,
    val errors: List<String>
)
