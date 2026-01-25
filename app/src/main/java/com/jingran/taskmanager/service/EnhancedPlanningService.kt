package com.jingran.taskmanager.service

import com.jingran.taskmanager.data.entity.*
import com.jingran.taskmanager.data.repository.TaskRepository
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.math.min
import kotlin.math.max

open class EnhancedPlanningService(private val repository: TaskRepository) {
    
    companion object {
        private const val MORNING_START_HOUR = 9
        private const val MORNING_END_HOUR = 12
        private const val AFTERNOON_START_HOUR = 14
        private const val AFTERNOON_END_HOUR = 18
        private const val EVENING_START_HOUR = 19
        private const val EVENING_END_HOUR = 22
        
        private const val TASK_INTERVAL_MINUTES = 5
        private const val MIN_TASK_DURATION = 15
    }
    
    suspend fun generateIntelligentPlan(
        date: Long, 
        tasks: List<ShortTermTask>,
        userWorkingHours: List<Pair<Int, Int>>? = null
    ): PlanningResult {
        
        val dayOfWeek = getDayOfWeek(date)
        val fixedSchedules = repository.getFixedSchedulesByDay(dayOfWeek)
        
        val availableSlots = getAvailableTimeSlots(date, fixedSchedules, userWorkingHours)
        
        val sortedTasks = sortTasksIntelligently(tasks, date)
        
        val (plannedItems, unscheduledTasks) = allocateTasksToSlots(sortedTasks, availableSlots, date)
        
        val stats = generatePlanningStats(tasks, plannedItems, unscheduledTasks)
        
        return PlanningResult(
            planItems = plannedItems,
            unscheduledTasks = unscheduledTasks,
            fixedSchedules = fixedSchedules,
            stats = stats
        )
    }
    
    suspend fun insertEmergencyTask(
        emergencyTask: ShortTermTask,
        currentPlan: List<PlanItem>,
        date: Long
    ): EmergencyInsertionResult {
        
        val dayOfWeek = getDayOfWeek(date)
        val fixedSchedules = repository.getFixedSchedulesByDay(dayOfWeek)
        val availableSlots = getAvailableTimeSlots(date, fixedSchedules)
        
        val insertionOptions = findInsertionOptions(emergencyTask, currentPlan, availableSlots)
        
        if (insertionOptions.isEmpty()) {
            return EmergencyInsertionResult(
                success = false,
                adjustedPlan = currentPlan,
                postponedTasks = emptyList(),
                message = "无法找到合适的时间段插入紧急任务"
            )
        }
        
        val bestOption = selectBestInsertionOption(insertionOptions)
        
        return EmergencyInsertionResult(
            success = true,
            adjustedPlan = bestOption.adjustedPlan,
            postponedTasks = bestOption.postponedTasks,
            message = "紧急任务已成功插入，${bestOption.postponedTasks.size}个任务被推迟"
        )
    }
    
    private fun sortTasksIntelligently(tasks: List<ShortTermTask>, planDate: Long): List<ShortTermTask> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = planDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStart = calendar.timeInMillis
        val dayEnd = dayStart + (24 * 60 * 60 * 1000L)
        
        return tasks.sortedWith { task1, task2 ->
            val task1IsEmergency = task1.taskType == TaskType.EMERGENCY
            val task2IsEmergency = task2.taskType == TaskType.EMERGENCY
            
            when {
                task1IsEmergency && !task2IsEmergency -> -1
                !task1IsEmergency && task2IsEmergency -> 1
                else -> {
                    val task1IsInflexible = !task1.isFlexible
                    val task2IsInflexible = !task2.isFlexible
                    
                    when {
                        task1IsInflexible && !task2IsInflexible -> -1
                        !task1IsInflexible && task2IsInflexible -> 1
                        else -> {
                            val task1IsUrgent = task1.priority == TaskPriority.HIGH && 
                                    task1.deadline != null && 
                                    task1.deadline >= dayStart && 
                                    task1.deadline < dayEnd
                                    
                            val task2IsUrgent = task2.priority == TaskPriority.HIGH && 
                                    task2.deadline != null && 
                                    task2.deadline >= dayStart && 
                                    task2.deadline < dayEnd
                            
                            when {
                                task1IsUrgent && !task2IsUrgent -> -1
                                !task1IsUrgent && task2IsUrgent -> 1
                                else -> {
                                    val priorityComparison = task1.priority.ordinal.compareTo(task2.priority.ordinal)
                                    if (priorityComparison != 0) {
                                        priorityComparison
                                    } else {
                                        val deadlineComparison = compareDeadlines(task1.deadline, task2.deadline)
                                        if (deadlineComparison != 0) {
                                            deadlineComparison
                                        } else {
                                            val energyComparison = compareEnergyLevels(task1.energyLevel, task2.energyLevel)
                                            if (energyComparison != 0) {
                                                energyComparison
                                            } else {
                                                task1.duration.compareTo(task2.duration)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun getAvailableTimeSlots(
        date: Long, 
        fixedSchedules: List<FixedSchedule>,
        userWorkingHours: List<Pair<Int, Int>>? = null
    ): List<TimeSlot> {
        
        val calendar = Calendar.getInstance().apply {
            timeInMillis = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStart = calendar.timeInMillis
        
        val workingHours = userWorkingHours ?: listOf(
            Pair(MORNING_START_HOUR, MORNING_END_HOUR),
            Pair(AFTERNOON_START_HOUR, AFTERNOON_END_HOUR),
            Pair(EVENING_START_HOUR, EVENING_END_HOUR)
        )
        
        val allSlots = mutableListOf<TimeSlot>()
        
        for ((startHour, endHour) in workingHours) {
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
    
    private fun allocateTasksToSlots(
        tasks: List<ShortTermTask>,
        availableSlots: List<TimeSlot>,
        date: Long
    ): Pair<List<PlanItem>, List<ShortTermTask>> {
        
        val plannedItems = mutableListOf<PlanItem>()
        val unscheduledTasks = mutableListOf<ShortTermTask>()
        val remainingSlots = availableSlots.toMutableList()
        
        for (task in tasks) {
            val taskDurationMs = task.duration * 60 * 1000L
            var allocated = false
            
            for (i in remainingSlots.indices) {
                val slot = remainingSlots[i]
                
                if (slot.duration >= taskDurationMs) {
                    val taskStartTime = slot.startTime
                    val taskEndTime = taskStartTime + taskDurationMs
                    
                    plannedItems.add(
                        PlanItem(
                            taskId = task.id,
                            planDate = date,
                            startTime = taskStartTime,
                            endTime = taskEndTime
                        )
                    )
                    
                    val newSlotStart = taskEndTime + (TASK_INTERVAL_MINUTES * 60 * 1000L)
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
    
    private fun compareEnergyLevels(energy1: EnergyLevel, energy2: EnergyLevel): Int {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        
        val preferredEnergy = when {
            currentHour in MORNING_START_HOUR until MORNING_END_HOUR -> EnergyLevel.HIGH
            currentHour in AFTERNOON_START_HOUR until AFTERNOON_END_HOUR -> EnergyLevel.MEDIUM
            currentHour in EVENING_START_HOUR until EVENING_END_HOUR -> EnergyLevel.LOW
            else -> EnergyLevel.MEDIUM
        }
        
        val energy1Distance = kotlin.math.abs(energy1.ordinal - preferredEnergy.ordinal)
        val energy2Distance = kotlin.math.abs(energy2.ordinal - preferredEnergy.ordinal)
        
        return energy1Distance.compareTo(energy2Distance)
    }
    
    private fun compareDeadlines(deadline1: Long?, deadline2: Long?): Int {
        return when {
            deadline1 == null && deadline2 == null -> 0
            deadline1 == null -> 1
            deadline2 == null -> -1
            else -> deadline1.compareTo(deadline2)
        }
    }
    
    private fun generatePlanningStats(
        allTasks: List<ShortTermTask>,
        plannedItems: List<PlanItem>,
        unscheduledTasks: List<ShortTermTask>
    ): PlanningStats {
        
        val totalTasks = allTasks.size
        val scheduledTasks = plannedItems.size
        val totalDuration = allTasks.sumOf { it.duration }
        val scheduledDuration = plannedItems.sumOf { item ->
            allTasks.find { it.id == item.taskId }?.duration ?: 0
        }
        
        return PlanningStats(
            totalTasks = totalTasks,
            scheduledTasks = scheduledTasks,
            unscheduledTasks = unscheduledTasks.size,
            totalDuration = totalDuration,
            scheduledDuration = scheduledDuration,
            schedulingRate = if (totalTasks > 0) scheduledTasks.toFloat() / totalTasks else 0f
        )
    }
    
    private fun findInsertionOptions(
        emergencyTask: ShortTermTask,
        currentPlan: List<PlanItem>,
        availableSlots: List<TimeSlot>
    ): List<InsertionOption> {
        val options = mutableListOf<InsertionOption>()
        val emergencyDuration = emergencyTask.duration * 60 * 1000L
        
        for (slot in availableSlots) {
            if (slot.duration >= emergencyDuration) {
                val newPlanItem = PlanItem(
                    taskId = emergencyTask.id,
                    planDate = currentPlan.firstOrNull()?.planDate ?: System.currentTimeMillis(),
                    startTime = slot.startTime,
                    endTime = slot.startTime + emergencyDuration
                )
                
                val adjustedPlan = currentPlan.toMutableList()
                adjustedPlan.add(newPlanItem)
                adjustedPlan.sortBy { it.startTime }
                
                options.add(
                    InsertionOption(
                        adjustedPlan = adjustedPlan,
                        postponedTasks = emptyList(),
                        impactScore = 0.0f
                    )
                )
            }
        }
        
        if (options.isEmpty()) {
            val sortedPlan = currentPlan.sortedBy { it.startTime }
            
            for (i in sortedPlan.indices) {
                val planItem = sortedPlan[i]
                val task = runBlocking { repository.getShortTermTaskById(planItem.taskId) }
                
                if (task != null && (task.priority == TaskPriority.LOW || task.priority == TaskPriority.MEDIUM)) {
                    val adjustedPlan = sortedPlan.toMutableList()
                    val postponedTasks = mutableListOf<ShortTermTask>()
                    
                    adjustedPlan.removeAt(i)
                    postponedTasks.add(task)
                    
                    val newPlanItem = PlanItem(
                        taskId = emergencyTask.id,
                        planDate = planItem.planDate,
                        startTime = planItem.startTime,
                        endTime = planItem.startTime + emergencyDuration
                    )
                    adjustedPlan.add(i, newPlanItem)
                    
                    val impactScore = calculateImpactScore(postponedTasks)
                    
                    options.add(
                        InsertionOption(
                            adjustedPlan = adjustedPlan,
                            postponedTasks = postponedTasks,
                            impactScore = impactScore
                        )
                    )
                }
            }
        }
        
        if (options.isEmpty()) {
            val dayStart = getDayStartTime(currentPlan.firstOrNull()?.planDate ?: System.currentTimeMillis())
            val dayEnd = dayStart + (24 * 60 * 60 * 1000L)
            
            val earliestPlan = currentPlan.minByOrNull { it.startTime }
            if (earliestPlan != null && (earliestPlan.startTime - dayStart) >= emergencyDuration) {
                val newPlanItem = PlanItem(
                    taskId = emergencyTask.id,
                    planDate = earliestPlan.planDate,
                    startTime = dayStart + (MORNING_START_HOUR * 60 * 60 * 1000L),
                    endTime = dayStart + (MORNING_START_HOUR * 60 * 60 * 1000L) + emergencyDuration
                )
                
                val adjustedPlan = currentPlan.toMutableList()
                adjustedPlan.add(newPlanItem)
                adjustedPlan.sortBy { it.startTime }
                
                options.add(
                    InsertionOption(
                        adjustedPlan = adjustedPlan,
                        postponedTasks = emptyList(),
                        impactScore = 1.0f
                    )
                )
            }
        }
        
        return options
    }
    
    private fun calculateImpactScore(postponedTasks: List<ShortTermTask>): Float {
        var score = 0.0f
        for (task in postponedTasks) {
            score += when (task.priority) {
                TaskPriority.HIGH -> 3.0f
                TaskPriority.MEDIUM -> 2.0f
                TaskPriority.LOW -> 1.0f
                else -> 1.5f
            }
        }
        return score
    }
    
    private fun getDayStartTime(date: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
    
    private fun selectBestInsertionOption(options: List<InsertionOption>): InsertionOption {
        return options.minByOrNull { it.postponedTasks.size } ?: options.first()
    }
}

data class TimeSlot(
    val startTime: Long,
    val endTime: Long
) {
    val duration: Long get() = endTime - startTime
}

data class PlanningResult(
    val planItems: List<PlanItem>,
    val unscheduledTasks: List<ShortTermTask>,
    val fixedSchedules: List<FixedSchedule>,
    val stats: PlanningStats
)

data class PlanningStats(
    val totalTasks: Int,
    val scheduledTasks: Int,
    val unscheduledTasks: Int,
    val totalDuration: Int,
    val scheduledDuration: Int,
    val schedulingRate: Float
)

data class EmergencyInsertionResult(
    val success: Boolean,
    val adjustedPlan: List<PlanItem>,
    val postponedTasks: List<ShortTermTask>,
    val message: String
)

data class InsertionOption(
    val adjustedPlan: List<PlanItem>,
    val postponedTasks: List<ShortTermTask>,
    val impactScore: Float
)