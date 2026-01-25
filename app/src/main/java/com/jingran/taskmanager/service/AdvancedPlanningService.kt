package com.jingran.taskmanager.service

import com.jingran.taskmanager.data.entity.*
import com.jingran.taskmanager.data.repository.TaskRepository
import com.jingran.utils.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

open class AdvancedPlanningService(private val repository: TaskRepository) {
    
    companion object {
        private const val MORNING_START_HOUR = 9
        private const val MORNING_END_HOUR = 12
        private const val AFTERNOON_START_HOUR = 14
        private const val AFTERNOON_END_HOUR = 18
        private const val EVENING_START_HOUR = 19
        private const val EVENING_END_HOUR = 22
        
        private const val TASK_INTERVAL_MINUTES = 5
        private const val MIN_TASK_DURATION = 15
        private const val BREAK_DURATION = 15
        private const val MAX_CONTINUOUS_WORK = 120
        
        private const val TAG = "AdvancedPlanningService"
    }
    
    data class PlanningContext(
        val date: Long,
        val userPreferences: UserPreferences,
        val historicalData: HistoricalData,
        val currentEnergyLevel: EnergyLevel,
        val availableSlots: List<TimeSlot>
    )
    
    data class UserPreferences(
        val preferredWorkHours: List<Pair<Int, Int>>,
        val preferredTaskTypes: List<TaskType>,
        val maxTasksPerDay: Int,
        val breakFrequency: Int,
        val energyAwareScheduling: Boolean
    )
    
    data class HistoricalData(
        val averageCompletionRate: Float,
        val averageTaskDuration: Map<TaskType, Int>,
        val peakProductivityHours: List<Int>,
        val taskTypeSuccessRates: Map<TaskType, Float>
    )
    
    data class TaskScore(
        val task: ShortTermTask,
        val priorityScore: Float,
        val urgencyScore: Float,
        val energyMatchScore: Float,
        val historicalSuccessScore: Float,
        val totalScore: Float
    )
    
    data class OptimizedPlan(
        val planItems: List<PlanItem>,
        val unscheduledTasks: List<ShortTermTask>,
        val optimizationMetrics: OptimizationMetrics,
        val adjustments: List<PlanAdjustment>
    )
    
    data class OptimizationMetrics(
        val totalUtilization: Float,
        val energyEfficiency: Float,
        val prioritySatisfaction: Float,
        val breakOptimization: Float
    )
    
    data class PlanAdjustment(
        val taskId: Long,
        val originalSlot: TimeSlot,
        val adjustedSlot: TimeSlot,
        val reason: String
    )
    
    suspend fun generateOptimizedPlan(
        date: Long,
        tasks: List<ShortTermTask>,
        userPreferences: UserPreferences = UserPreferences(),
        historicalData: HistoricalData = HistoricalData()
    ): OptimizedPlan = withContext(Dispatchers.IO) {
        
        LogManager.d(TAG, "开始生成优化计划，日期: $date, 任务数: ${tasks.size}")
        
        val context = createPlanningContext(date, userPreferences, historicalData)
        
        val scoredTasks = scoreTasks(tasks, context)
        
        val sortedTasks = sortTasksByScore(scoredTasks)
        
        val (plannedItems, unscheduledTasks) = allocateTasksOptimally(
            sortedTasks,
            context.availableSlots,
            context
        )
        
        val optimizationMetrics = calculateOptimizationMetrics(
            plannedItems,
            unscheduledTasks,
            context
        )
        
        val adjustments = analyzePlanAdjustments(plannedItems, context)
        
        LogManager.i(TAG, "计划生成完成: 已安排${plannedItems.size}个任务，未安排${unscheduledTasks.size}个任务")
        
        OptimizedPlan(
            planItems = plannedItems,
            unscheduledTasks = unscheduledTasks,
            optimizationMetrics = optimizationMetrics,
            adjustments = adjustments
        )
    }
    
    private suspend fun createPlanningContext(
        date: Long,
        userPreferences: UserPreferences,
        historicalData: HistoricalData
    ): PlanningContext {
        val dayOfWeek = getDayOfWeek(date)
        val fixedSchedules = repository.getFixedSchedulesByDay(dayOfWeek)
        
        val availableSlots = getAvailableTimeSlots(
            date,
            fixedSchedules,
            userPreferences.preferredWorkHours
        )
        
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val currentEnergyLevel = calculateCurrentEnergyLevel(currentHour)
        
        return PlanningContext(
            date = date,
            userPreferences = userPreferences,
            historicalData = historicalData,
            currentEnergyLevel = currentEnergyLevel,
            availableSlots = availableSlots
        )
    }
    
    private fun scoreTasks(tasks: List<ShortTermTask>, context: PlanningContext): List<TaskScore> {
        return tasks.map { task ->
            val priorityScore = calculatePriorityScore(task)
            val urgencyScore = calculateUrgencyScore(task, context.date)
            val energyMatchScore = calculateEnergyMatchScore(task, context.currentEnergyLevel)
            val historicalSuccessScore = calculateHistoricalSuccessScore(task, context.historicalData)
            
            val totalScore = priorityScore * 0.3f +
                            urgencyScore * 0.3f +
                            energyMatchScore * 0.2f +
                            historicalSuccessScore * 0.2f
            
            TaskScore(
                task = task,
                priorityScore = priorityScore,
                urgencyScore = urgencyScore,
                energyMatchScore = energyMatchScore,
                historicalSuccessScore = historicalSuccessScore,
                totalScore = totalScore
            )
        }
    }
    
    private fun calculatePriorityScore(task: ShortTermTask): Float {
        val priorityWeight = when (task.priority) {
            TaskPriority.URGENT -> 4.0f
            TaskPriority.HIGH -> 3.0f
            TaskPriority.MEDIUM -> 2.0f
            TaskPriority.LOW -> 1.0f
        }
        
        val taskTypeWeight = when (task.taskType) {
            TaskType.EMERGENCY -> 4.0f
            TaskType.MEETING -> 3.5f
            TaskType.LEARNING -> 3.0f
            TaskType.EXERCISE -> 2.5f
            TaskType.ROUTINE -> 2.0f
            TaskType.PERSONAL -> 1.5f
            TaskType.NORMAL -> 1.0f
            TaskType.SUBTASK -> 0.5f
        }
        
        return (priorityWeight + taskTypeWeight) / 2f
    }
    
    private fun calculateUrgencyScore(task: ShortTermTask, planDate: Long): Float {
        if (task.deadline == null) return 0f
        
        val calendar = Calendar.getInstance().apply {
            timeInMillis = planDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStart = calendar.timeInMillis
        val dayEnd = dayStart + (24 * 60 * 60 * 1000L)
        
        val deadline = task.deadline!!
        
        return when {
            deadline < dayStart -> 0f
            deadline in dayStart..dayEnd -> {
                val hoursUntilDeadline = (deadline - dayStart) / (60 * 60 * 1000L)
                when {
                    hoursUntilDeadline < 2 -> 1.0f
                    hoursUntilDeadline < 6 -> 0.8f
                    hoursUntilDeadline < 12 -> 0.6f
                    else -> 0.4f
                }
            }
            else -> {
                val daysUntilDeadline = (deadline - dayEnd) / (24 * 60 * 60 * 1000L)
                when {
                    daysUntilDeadline <= 1 -> 0.3f
                    daysUntilDeadline <= 3 -> 0.2f
                    daysUntilDeadline <= 7 -> 0.1f
                    else -> 0f
                }
            }
        }
    }
    
    private fun calculateEnergyMatchScore(task: ShortTermTask, currentEnergyLevel: EnergyLevel): Float {
        val taskEnergyLevel = task.energyLevel
        
        return when {
            taskEnergyLevel == currentEnergyLevel -> 1.0f
            abs(taskEnergyLevel.ordinal - currentEnergyLevel.ordinal) == 1 -> 0.8f
            abs(taskEnergyLevel.ordinal - currentEnergyLevel.ordinal) == 2 -> 0.5f
            else -> 0.2f
        }
    }
    
    private fun calculateHistoricalSuccessScore(task: ShortTermTask, historicalData: HistoricalData): Float {
        val successRate = historicalData.taskTypeSuccessRates[task.taskType] ?: 0.5f
        return successRate
    }
    
    private fun sortTasksByScore(scoredTasks: List<TaskScore>): List<ShortTermTask> {
        return scoredTasks.sortedByDescending { it.totalScore }.map { it.task }
    }
    
    private fun allocateTasksOptimally(
        tasks: List<ShortTermTask>,
        availableSlots: List<TimeSlot>,
        context: PlanningContext
    ): Pair<List<PlanItem>, List<ShortTermTask>> {
        
        val plannedItems = mutableListOf<PlanItem>()
        val unscheduledTasks = mutableListOf<ShortTermTask>()
        val remainingSlots = availableSlots.toMutableList()
        
        var continuousWorkMinutes = 0
        
        for (task in tasks) {
            val taskDurationMs = task.duration * 60 * 1000L
            var allocated = false
            
            for (i in remainingSlots.indices) {
                val slot = remainingSlots[i]
                
                if (slot.duration >= taskDurationMs) {
                    val taskStartTime = findOptimalStartTime(slot, task, context, continuousWorkMinutes)
                    val taskEndTime = taskStartTime + taskDurationMs
                    
                    plannedItems.add(
                        PlanItem(
                            taskId = task.id,
                            planDate = context.date,
                            startTime = taskStartTime,
                            endTime = taskEndTime
                        )
                    )
                    
                    val newSlotStart = taskEndTime + (TASK_INTERVAL_MINUTES * 60 * 1000L)
                    
                    if (context.userPreferences.energyAwareScheduling && continuousWorkMinutes >= MAX_CONTINUOUS_WORK) {
                        val breakSlot = TimeSlot(taskEndTime, taskEndTime + (BREAK_DURATION * 60 * 1000L))
                        remainingSlots.add(i + 1, breakSlot)
                        continuousWorkMinutes = 0
                    } else {
                        continuousWorkMinutes += task.duration
                    }
                    
                    if (newSlotStart < slot.endTime) {
                        remainingSlots[i] = TimeSlot(newSlotStart, slot.endTime)
                    } else {
                        remainingSlots.removeAt(i)
                    }
                    
                    allocated = true
                    break
                }
            }
            
            if (!allocated) {
                unscheduledTasks.add(task)
            }
        }
        
        return Pair(plannedItems, unscheduledTasks)
    }
    
    private fun findOptimalStartTime(
        slot: TimeSlot,
        task: ShortTermTask,
        context: PlanningContext,
        continuousWorkMinutes: Int
    ): Long {
        
        if (!context.userPreferences.energyAwareScheduling) {
            return slot.startTime
        }
        
        val slotHour = Calendar.getInstance().apply { timeInMillis = slot.startTime }.get(Calendar.HOUR_OF_DAY)
        
        val preferredEnergyHour = when (task.energyLevel) {
            EnergyLevel.HIGH -> listOf(MORNING_START_HOUR, MORNING_END_HOUR)
            EnergyLevel.MEDIUM -> listOf(AFTERNOON_START_HOUR, AFTERNOON_END_HOUR)
            EnergyLevel.LOW -> listOf(EVENING_START_HOUR, EVENING_END_HOUR)
        }
        
        val bestHour = preferredEnergyHour.minByOrNull { abs(it - slotHour) }
        
        return if (bestHour != null && bestHour in MORNING_START_HOUR..EVENING_END_HOUR) {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = slot.startTime
                set(Calendar.HOUR_OF_DAY, bestHour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            calendar.timeInMillis
        } else {
            slot.startTime
        }
    }
    
    private fun calculateOptimizationMetrics(
        plannedItems: List<PlanItem>,
        unscheduledTasks: List<ShortTermTask>,
        context: PlanningContext
    ): OptimizationMetrics {
        
        val totalAvailableTime = context.availableSlots.sumOf { it.duration }
        val totalPlannedTime = plannedItems.sumOf { it.endTime - it.startTime }
        
        val totalUtilization = if (totalAvailableTime > 0) {
            (totalPlannedTime.toFloat() / totalAvailableTime.toFloat()) * 100f
        } else {
            0f
        }
        
        val energyEfficiency = calculateEnergyEfficiency(plannedItems, context)
        
        val prioritySatisfaction = calculatePrioritySatisfaction(plannedItems, unscheduledTasks)
        
        val breakOptimization = calculateBreakOptimization(plannedItems, context)
        
        return OptimizationMetrics(
            totalUtilization = totalUtilization,
            energyEfficiency = energyEfficiency,
            prioritySatisfaction = prioritySatisfaction,
            breakOptimization = breakOptimization
        )
    }
    
    private suspend fun calculateEnergyEfficiency(
        plannedItems: List<PlanItem>,
        context: PlanningContext
    ): Float {
        
        val tasks = plannedItems.mapNotNull { item ->
            repository.getShortTermTaskById(item.taskId)
        }
        
        if (tasks.isEmpty()) return 0f
        
        val energyMatches = tasks.count { task ->
            val taskHour = Calendar.getInstance().apply { timeInMillis = plannedItems.find { it.taskId == task.id }?.startTime ?: 0L }
                .get(Calendar.HOUR_OF_DAY)
            
            val expectedEnergy = when {
                taskHour in MORNING_START_HOUR until MORNING_END_HOUR -> EnergyLevel.HIGH
                taskHour in AFTERNOON_START_HOUR until AFTERNOON_END_HOUR -> EnergyLevel.MEDIUM
                taskHour in EVENING_START_HOUR until EVENING_END_HOUR -> EnergyLevel.LOW
                else -> EnergyLevel.MEDIUM
            }
            
            task.energyLevel == expectedEnergy
        }
        
        return (energyMatches.toFloat() / tasks.size.toFloat()) * 100f
    }
    
    private fun calculatePrioritySatisfaction(
        plannedItems: List<PlanItem>,
        unscheduledTasks: List<ShortTermTask>
    ): Float {
        
        val allTasks = plannedItems.mapNotNull { item ->
            repository.getShortTermTaskById(item.taskId)
        } + unscheduledTasks
        
        if (allTasks.isEmpty()) return 0f
        
        val highPriorityTasks = allTasks.filter { it.priority == TaskPriority.HIGH || it.priority == TaskPriority.URGENT }
        val plannedHighPriorityTasks = plannedItems.count { item ->
            repository.getShortTermTaskById(item.taskId)?.let { task ->
                task.priority == TaskPriority.HIGH || task.priority == TaskPriority.URGENT
            } ?: false
        }
        
        return if (highPriorityTasks.isNotEmpty()) {
            (plannedHighPriorityTasks.toFloat() / highPriorityTasks.size.toFloat()) * 100f
        } else {
            100f
        }
    }
    
    private fun calculateBreakOptimization(
        plannedItems: List<PlanItem>,
        context: PlanningContext
    ): Float {
        
        if (plannedItems.size < 2) return 100f
        
        val totalBreakTime = plannedItems.zipWithNext { current, next ->
            val gap = next.startTime - current.endTime
            maxOf(0, gap - (TASK_INTERVAL_MINUTES * 60 * 1000L))
        }.sum()
        
        val expectedBreakTime = ((plannedItems.size - 1) * BREAK_DURATION * 60 * 1000L).toFloat()
        
        return if (expectedBreakTime > 0) {
            minOf(100f, (totalBreakTime.toFloat() / expectedBreakTime) * 100f)
        } else {
            100f
        }
    }
    
    private fun analyzePlanAdjustments(
        plannedItems: List<PlanItem>,
        context: PlanningContext
    ): List<PlanAdjustment> {
        
        val adjustments = mutableListOf<PlanAdjustment>()
        
        for (item in plannedItems) {
            val task = repository.getShortTermTaskById(item.taskId) ?: continue
            
            val itemHour = Calendar.getInstance().apply { timeInMillis = item.startTime }.get(Calendar.HOUR_OF_DAY)
            
            val preferredEnergyHour = when (task.energyLevel) {
                EnergyLevel.HIGH -> listOf(MORNING_START_HOUR, MORNING_END_HOUR)
                EnergyLevel.MEDIUM -> listOf(AFTERNOON_START_HOUR, AFTERNOON_END_HOUR)
                EnergyLevel.LOW -> listOf(EVENING_START_HOUR, EVENING_END_HOUR)
            }
            
            if (context.userPreferences.energyAwareScheduling) {
                val bestHour = preferredEnergyHour.minByOrNull { abs(it - itemHour) }
                
                if (bestHour != null && bestHour != itemHour) {
                    val originalSlot = TimeSlot(item.startTime, item.endTime)
                    
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = item.startTime
                        set(Calendar.HOUR_OF_DAY, bestHour)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    
                    val adjustedSlot = TimeSlot(
                        calendar.timeInMillis,
                        calendar.timeInMillis + (item.endTime - item.startTime)
                    )
                    
                    adjustments.add(
                        PlanAdjustment(
                            taskId = item.taskId,
                            originalSlot = originalSlot,
                            adjustedSlot = adjustedSlot,
                            reason = "能量水平优化：从${itemHour}点调整到${bestHour}点"
                        )
                    )
                }
            }
        }
        
        return adjustments
    }
    
    private fun getAvailableTimeSlots(
        date: Long,
        fixedSchedules: List<FixedSchedule>,
        userWorkingHours: List<Pair<Int, Int>>
    ): List<TimeSlot> {
        
        val calendar = Calendar.getInstance().apply {
            timeInMillis = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStart = calendar.timeInMillis
        
        val allSlots = mutableListOf<TimeSlot>()
        
        for ((startHour, endHour) in userWorkingHours) {
            val slotStart = dayStart + (startHour * 60 * 60 * 1000L)
            val slotEnd = dayStart + (endHour * 60 * 60 * 1000L)
            allSlots.add(TimeSlot(slotStart, slotEnd))
        }
        
        return subtractFixedSchedules(allSlots, fixedSchedules)
    }
    
    private fun subtractFixedSchedules(
        availableSlots: List<TimeSlot>,
        fixedSchedules: List<FixedSchedule>
    ): List<TimeSlot> {
        
        var resultSlots = availableSlots.toMutableList()
        
        for (schedule in fixedSchedules) {
            val newSlots = mutableListOf<TimeSlot>()
            
            for (slot in resultSlots) {
                val fragments = subtractTimeRange(slot, schedule.startTime, schedule.endTime)
                newSlots.addAll(fragments)
            }
            
            resultSlots = newSlots
        }
        
        return resultSlots.filter { it.duration >= MIN_TASK_DURATION * 60 * 1000L }
    }
    
    private fun subtractTimeRange(slot: TimeSlot, excludeStart: Long, excludeEnd: Long): List<TimeSlot> {
        val result = mutableListOf<TimeSlot>()
        
        if (excludeEnd <= slot.startTime || excludeStart >= slot.endTime) {
            result.add(slot)
            return result
        }
        
        if (slot.startTime < excludeStart) {
            result.add(TimeSlot(slot.startTime, min(excludeStart, slot.endTime)))
        }
        
        if (slot.endTime > excludeEnd) {
            result.add(TimeSlot(max(excludeEnd, slot.startTime), slot.endTime))
        }
        
        return result
    }
    
    private fun getDayOfWeek(date: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = date }
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }
    
    private fun calculateCurrentEnergyLevel(hour: Int): EnergyLevel {
        return when (hour) {
            in MORNING_START_HOUR until MORNING_END_HOUR -> EnergyLevel.HIGH
            in AFTERNOON_START_HOUR until AFTERNOON_END_HOUR -> EnergyLevel.MEDIUM
            in EVENING_START_HOUR until EVENING_END_HOUR -> EnergyLevel.LOW
            else -> EnergyLevel.MEDIUM
        }
    }
    
    private fun <T> List<T>.zipWithNext(transform: (T, T) -> Long): List<Long> {
        return this.zipWithNextOrNull().map { (current, next) ->
            if (next != null) {
                transform(current, next)
            } else {
                0L
            }
        }
    }
    
    private fun <T> List<T>.zipWithNextOrNull(): List<Pair<T, T?>> {
        return this.mapIndexed { index, item ->
            Pair(item, this.getOrNull(index + 1))
        }
    }
}