package com.jingran.taskmanager.service

import com.jingran.taskmanager.data.entity.*
import com.jingran.taskmanager.data.repository.TaskRepository
import com.jingran.utils.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

open class TaskSchedulingService(private val repository: TaskRepository) {
    
    companion object {
        private const val TAG = "TaskSchedulingService"
        private const val DEFAULT_TASK_DURATION = 60
        private const val MIN_SCHEDULING_INTERVAL = 15
        private const val MAX_RESCHEDULE_ATTEMPTS = 3
    }
    
    data class SchedulingResult(
        val scheduledTasks: List<ShortTermTask>,
        val unscheduledTasks: List<ShortTermTask>,
        val conflicts: List<SchedulingConflict>,
        val adjustments: List<SchedulingAdjustment>
    )
    
    data class SchedulingConflict(
        val task1Id: Long,
        val task2Id: Long,
        val conflictType: ConflictType,
        val suggestedResolution: String
    )
    
    enum class ConflictType {
        TIME_OVERLAP,
        RESOURCE_CONSTRAINT,
        ENERGY_MISMATCH,
        PRIORITY_VIOLATION
    }
    
    data class SchedulingAdjustment(
        val taskId: Long,
        val originalStartTime: Long,
        val adjustedStartTime: Long,
        val reason: String,
        val impact: AdjustmentImpact
    )
    
    enum class AdjustmentImpact {
        MINOR,
        MODERATE,
        SIGNIFICANT
    }
    
    suspend fun optimizeTaskSchedule(
        date: Long,
        tasks: List<ShortTermTask>,
        existingPlans: List<PlanItem>,
        userPreferences: SchedulingPreferences = SchedulingPreferences()
    ): SchedulingResult = withContext(Dispatchers.IO) {
        
        LogManager.d(TAG, "开始优化任务调度，日期: $date, 任务数: ${tasks.size}")
        
        val sortedTasks = sortTasksByPriorityAndDeadline(tasks)
        
        val (scheduledTasks, unscheduledTasks, conflicts) = scheduleTasksWithConflictResolution(
            sortedTasks,
            existingPlans,
            userPreferences
        )
        
        val adjustments = analyzeScheduleAdjustments(scheduledTasks, tasks)
        
        LogManager.i(TAG, "调度优化完成: 已安排${scheduledTasks.size}个任务，未安排${unscheduledTasks.size}个任务，发现${conflicts.size}个冲突")
        
        SchedulingResult(
            scheduledTasks = scheduledTasks,
            unscheduledTasks = unscheduledTasks,
            conflicts = conflicts,
            adjustments = adjustments
        )
    }
    
    data class SchedulingPreferences(
        val allowParallelTasks: Boolean = false,
        val maxConcurrentTasks: Int = 1,
        val energyAwareScheduling: Boolean = true,
        val breakBetweenTasks: Int = 5,
        val flexibleDeadlines: Boolean = true,
        val priorityDeadlineThreshold: Int = 24
    )
    
    private fun sortTasksByPriorityAndDeadline(tasks: List<ShortTermTask>): List<ShortTermTask> {
        return tasks.sortedWith(compareBy<ShortTermTask>(
            { it.priority.ordinal },
            { it.deadline },
            { it.duration }
        ))
    }
    
    private fun comparePriority(priority1: TaskPriority, priority2: TaskPriority): Int {
        val priorityOrder = listOf(TaskPriority.URGENT, TaskPriority.HIGH, TaskPriority.MEDIUM, TaskPriority.LOW)
        val index1 = priorityOrder.indexOf(priority1)
        val index2 = priorityOrder.indexOf(priority2)
        return index1.compareTo(index2)
    }
    
    private fun compareDeadlines(deadline1: Long?, deadline2: Long?): Int {
        return when {
            deadline1 == null && deadline2 == null -> 0
            deadline1 == null -> 1
            deadline2 == null -> -1
            else -> deadline1.compareTo(deadline2)
        }
    }
    
    private fun compareDurations(duration1: Int, duration2: Int): Int {
        return duration1.compareTo(duration2)
    }
    
    private fun scheduleTasksWithConflictResolution(
        tasks: List<ShortTermTask>,
        existingPlans: List<PlanItem>,
        preferences: SchedulingPreferences
    ): Triple<List<ShortTermTask>, List<ShortTermTask>, List<SchedulingConflict>> {
        
        val scheduledTasks = mutableListOf<ShortTermTask>()
        val unscheduledTasks = mutableListOf<ShortTermTask>()
        val conflicts = mutableListOf<SchedulingConflict>()
        
        val dayStart = getDayStart(existingPlans.firstOrNull()?.planDate ?: System.currentTimeMillis())
        val dayEnd = getDayEnd(dayStart)
        
        var currentTime = dayStart
        val usedTimeSlots = existingPlans.map { Pair(it.startTime, it.endTime) }.toMutableList()
        
        for (task in tasks) {
            val taskDurationMs = task.duration * 60 * 1000L
            val earliestStartTime = findEarliestStartTime(task, dayStart, dayEnd, preferences)
            
            var scheduled = false
            var attempts = 0
            
            while (!scheduled && attempts < MAX_RESCHEDULE_ATTEMPTS) {
                val proposedStartTime = maxOf(currentTime, earliestStartTime)
                val proposedEndTime = proposedStartTime + taskDurationMs
                
                if (proposedEndTime > dayEnd) {
                    unscheduledTasks.add(task)
                    break
                }
                
                val conflict = checkForConflict(
                    proposedStartTime,
                    proposedEndTime,
                    usedTimeSlots,
                    preferences
                )
                
                if (conflict == null) {
                    scheduledTasks.add(task)
                    usedTimeSlots.add(Pair(proposedStartTime, proposedEndTime))
                    
                    if (preferences.breakBetweenTasks > 0) {
                        currentTime = proposedEndTime + (preferences.breakBetweenTasks * 60 * 1000L)
                    } else {
                        currentTime = proposedEndTime
                    }
                    
                    scheduled = true
                } else {
                    conflicts.add(conflict)
                    
                    val resolution = resolveConflict(conflict, task, usedTimeSlots, preferences)
                    
                    if (resolution != null) {
                        currentTime = resolution.adjustedStartTime
                        usedTimeSlots.add(Pair(resolution.adjustedStartTime, resolution.adjustedStartTime + taskDurationMs))
                        scheduledTasks.add(task)
                        scheduled = true
                    } else {
                        currentTime = proposedEndTime + (MIN_SCHEDULING_INTERVAL * 60 * 1000L)
                    }
                }
                
                attempts++
            }
            
            if (!scheduled) {
                unscheduledTasks.add(task)
            }
        }
        
        return Triple(scheduledTasks, unscheduledTasks, conflicts)
    }
    
    private fun findEarliestStartTime(
        task: ShortTermTask,
        dayStart: Long,
        dayEnd: Long,
        preferences: SchedulingPreferences
    ): Long {
        
        if (task.deadline != null && task.deadline < dayEnd) {
            val latestStartTime = task.deadline - (task.duration * 60 * 1000L)
            return maxOf(dayStart, latestStartTime)
        }
        
        return dayStart
    }
    
    private fun checkForConflict(
        startTime: Long,
        endTime: Long,
        usedTimeSlots: List<Pair<Long, Long>>,
        preferences: SchedulingPreferences
    ): SchedulingConflict? {
        
        for ((slotStart, slotEnd) in usedTimeSlots) {
            if (isTimeOverlapping(startTime, endTime, slotStart, slotEnd)) {
                return SchedulingConflict(
                    task1Id = -1,
                    task2Id = -1,
                    conflictType = ConflictType.TIME_OVERLAP,
                    suggestedResolution = "调整任务时间以避免重叠"
                )
            }
        }
        
        if (preferences.breakBetweenTasks > 0) {
            val minimumGap = preferences.breakBetweenTasks * 60 * 1000L
            
            for ((slotStart, slotEnd) in usedTimeSlots) {
                val gap = startTime - slotEnd
                if (gap > 0 && gap < minimumGap) {
                    return SchedulingConflict(
                        task1Id = -1,
                        task2Id = -1,
                        conflictType = ConflictType.TIME_OVERLAP,
                        suggestedResolution = "增加任务间隔至${preferences.breakBetweenTasks}分钟"
                    )
                }
            }
        }
        
        return null
    }
    
    private fun isTimeOverlapping(
        start1: Long,
        end1: Long,
        start2: Long,
        end2: Long
    ): Boolean {
        return start1 < end2 && end1 > start2
    }
    
    private fun resolveConflict(
        conflict: SchedulingConflict,
        task: ShortTermTask,
        usedTimeSlots: List<Pair<Long, Long>>,
        preferences: SchedulingPreferences
    ): SchedulingAdjustment? {
        
        return when (conflict.conflictType) {
            ConflictType.TIME_OVERLAP -> {
                val taskDurationMs = task.duration * 60 * 1000L
                val adjustedStartTime = findNextAvailableSlot(
                    usedTimeSlots,
                    taskDurationMs,
                    preferences
                )
                
                if (adjustedStartTime != null) {
                    SchedulingAdjustment(
                        taskId = task.id,
                        originalStartTime = 0,
                        adjustedStartTime = adjustedStartTime,
                        reason = "时间冲突：调整到下一个可用时段",
                        impact = AdjustmentImpact.MODERATE
                    )
                } else {
                    null
                }
            }
            
            ConflictType.RESOURCE_CONSTRAINT -> {
                SchedulingAdjustment(
                    taskId = task.id,
                    originalStartTime = 0,
                    adjustedStartTime = 0,
                    reason = "资源约束：无法安排",
                    impact = AdjustmentImpact.SIGNIFICANT
                )
            }
            
            ConflictType.ENERGY_MISMATCH -> {
                SchedulingAdjustment(
                    taskId = task.id,
                    originalStartTime = 0,
                    adjustedStartTime = 0,
                    reason = "能量水平不匹配：建议调整任务类型",
                    impact = AdjustmentImpact.MODERATE
                )
            }
            
            ConflictType.PRIORITY_VIOLATION -> {
                SchedulingAdjustment(
                    taskId = task.id,
                    originalStartTime = 0,
                    adjustedStartTime = 0,
                    reason = "优先级违规：调整任务顺序",
                    impact = AdjustmentImpact.MINOR
                )
            }
        }
    }
    
    private fun findNextAvailableSlot(
        usedTimeSlots: List<Pair<Long, Long>>,
        requiredDuration: Long,
        preferences: SchedulingPreferences
    ): Long? {
        
        val sortedSlots = usedTimeSlots.sortedBy { it.first }
        
        var currentTime = getDayStart(System.currentTimeMillis())
        
        for (i in sortedSlots.indices) {
            val (slotStart, slotEnd) = sortedSlots[i]
            
            val availableStart = currentTime + (preferences.breakBetweenTasks * 60 * 1000L)
            
            if (availableStart + requiredDuration <= slotStart) {
                return availableStart
            }
            
            currentTime = slotEnd
        }
        
        val lastSlotEnd = sortedSlots.lastOrNull()?.second ?: currentTime
        val availableStart = lastSlotEnd + (preferences.breakBetweenTasks * 60 * 1000L)
        
        if (availableStart + requiredDuration <= getDayEnd(currentTime)) {
            return availableStart
        }
        
        return null
    }
    
    private fun analyzeScheduleAdjustments(
        scheduledTasks: List<ShortTermTask>,
        originalTasks: List<ShortTermTask>
    ): List<SchedulingAdjustment> {
        
        val adjustments = mutableListOf<SchedulingAdjustment>()
        
        for (task in scheduledTasks) {
            val originalTask = originalTasks.find { it.id == task.id }
            
            if (originalTask != null && task.deadline != originalTask.deadline) {
                adjustments.add(
                    SchedulingAdjustment(
                        taskId = task.id,
                        originalStartTime = originalTask.deadline ?: 0,
                        adjustedStartTime = task.deadline ?: 0,
                        reason = "截止时间调整",
                        impact = AdjustmentImpact.MINOR
                    )
                )
            }
        }
        
        return adjustments
    }
    
    suspend fun rescheduleTask(
        taskId: Long,
        newStartTime: Long,
        reason: String
    ): Result<ShortTermTask> = withContext(Dispatchers.IO) {
        
        LogManager.d(TAG, "重新安排任务: $taskId, 新开始时间: $newStartTime, 原因: $reason")
        
        val task = repository.getShortTermTaskById(taskId)
        
        if (task == null) {
            return@withContext Result.failure(Exception("任务不存在: $taskId"))
        }
        
        val updatedTask = task.copy(
            deadline = newStartTime
        )
        
        return@withContext try {
            repository.updateShortTermTask(updatedTask)
            Result.success(updatedTask)
        } catch (e: Exception) {
            LogManager.e(TAG, "重新安排任务失败", e)
            Result.failure(e)
        }
    }
    
    suspend fun batchRescheduleTasks(
        rescheduleRequests: List<RescheduleRequest>
    ): Result<List<ShortTermTask>> = withContext(Dispatchers.IO) {
        
        LogManager.d(TAG, "批量重新安排任务，数量: ${rescheduleRequests.size}")
        
        val results = mutableListOf<ShortTermTask>()
        val failures = mutableListOf<Exception>()
        
        for (request in rescheduleRequests) {
            val result = rescheduleTask(request.taskId, request.newStartTime, request.reason)
            
            result.onSuccess { task ->
                results.add(task)
            }.onFailure { exception ->
                failures.add(exception)
            }
        }
        
        if (failures.isNotEmpty()) {
            LogManager.w(TAG, "批量重新安排完成，失败: ${failures.size}个")
            return@withContext Result.failure(Exception("部分任务重新安排失败"))
        }
        
        LogManager.i(TAG, "批量重新安排完成，成功: ${results.size}个")
        return@withContext Result.success(results)
    }
    
    data class RescheduleRequest(
        val taskId: Long,
        val newStartTime: Long,
        val reason: String
    )
    
    private fun getDayStart(date: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
    
    private fun getDayEnd(date: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = date
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }
    
    suspend fun getOptimalTaskOrder(
        tasks: List<ShortTermTask>,
        context: SchedulingContext
    ): List<ShortTermTask> = withContext(Dispatchers.IO) {
        
        val scoredTasks = tasks.map { task ->
            val score = calculateTaskScore(task, context)
            Pair(task, score)
        }
        
        return scoredTasks.sortedByDescending { it.second }.map { it.first }
    }
    
    data class SchedulingContext(
        val date: Long,
        val currentEnergyLevel: EnergyLevel,
        val historicalData: HistoricalPerformanceData,
        val userPreferences: SchedulingPreferences
    )
    
    data class HistoricalPerformanceData(
        val averageCompletionRate: Float,
        val peakProductivityHours: List<Int>,
        val taskTypeSuccessRates: Map<TaskType, Float>
    )
    
    private fun calculateTaskScore(
        task: ShortTermTask,
        context: SchedulingContext
    ): Float {
        
        val priorityScore = when (task.priority) {
            TaskPriority.URGENT -> 4.0f
            TaskPriority.HIGH -> 3.0f
            TaskPriority.MEDIUM -> 2.0f
            TaskPriority.LOW -> 1.0f
        }
        
        val energyMatchScore = if (task.energyLevel == context.currentEnergyLevel) {
            1.0f
        } else {
            0.5f
        }
        
        val historicalSuccessScore = context.historicalData.taskTypeSuccessRates[task.taskType] ?: 0.5f
        
        val urgencyScore = if (task.deadline != null) {
            val hoursUntilDeadline = (task.deadline!! - context.date) / (60 * 60 * 1000L)
            when {
                hoursUntilDeadline < 2 -> 1.0f
                hoursUntilDeadline < 6 -> 0.8f
                hoursUntilDeadline < 12 -> 0.6f
                hoursUntilDeadline < 24 -> 0.4f
                else -> 0.2f
            }
        } else {
            0.0f
        }
        
        return priorityScore * 0.4f +
               energyMatchScore * 0.3f +
               historicalSuccessScore * 0.2f +
               urgencyScore * 0.1f
    }
}