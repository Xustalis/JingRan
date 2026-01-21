package com.jingran.taskmanager.service

import com.jingran.taskmanager.data.entity.*
import com.jingran.taskmanager.data.repository.TaskRepository
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.math.min
import kotlin.math.max

/**
 * 增强的智能规划服务
 * 根据PRD文档要求，实现智能任务规划算法
 * 支持固定日程、时间冲突检测、能量水平匹配和自动调整
 */
open class EnhancedPlanningService(private val repository: TaskRepository) {
    
    companion object {
        // 默认工作时段：9:00-12:00、14:00-18:00、19:00-22:00
        private const val MORNING_START_HOUR = 9
        private const val MORNING_END_HOUR = 12
        private const val AFTERNOON_START_HOUR = 14
        private const val AFTERNOON_END_HOUR = 18
        private const val EVENING_START_HOUR = 19
        private const val EVENING_END_HOUR = 22
        
        // 任务间隔时间（分钟）
        private const val TASK_INTERVAL_MINUTES = 5
        
        // 最小任务时长（分钟）
        private const val MIN_TASK_DURATION = 15
    }
    
    /**
     * 生成智能日程规划
     * @param date 计划日期
     * @param tasks 待安排的任务列表
     * @param userWorkingHours 用户自定义工作时段（可选）
     * @return 生成的计划项列表
     */
    suspend fun generateIntelligentPlan(
        date: Long, 
        tasks: List<ShortTermTask>,
        userWorkingHours: List<Pair<Int, Int>>? = null
    ): PlanningResult {
        
        // 1. 获取当日固定日程
        val dayOfWeek = getDayOfWeek(date)
        val fixedSchedules = repository.getFixedSchedulesByDay(dayOfWeek)
        
        // 2. 获取可用时间段
        val availableSlots = getAvailableTimeSlots(date, fixedSchedules, userWorkingHours)
        
        // 3. 按智能规则排序任务
        val sortedTasks = sortTasksIntelligently(tasks, date)
        
        // 4. 分配任务到时间段
        val (plannedItems, unscheduledTasks) = allocateTasksToSlots(sortedTasks, availableSlots, date)
        
        // 5. 生成统计信息
        val stats = generatePlanningStats(tasks, plannedItems, unscheduledTasks)
        
        return PlanningResult(
            planItems = plannedItems,
            unscheduledTasks = unscheduledTasks,
            fixedSchedules = fixedSchedules,
            stats = stats
        )
    }
    
    /**
     * 插入紧急任务并自动调整现有计划
     * @param emergencyTask 紧急任务
     * @param currentPlan 当前计划
     * @param date 日期
     * @return 调整后的计划结果
     */
    suspend fun insertEmergencyTask(
        emergencyTask: ShortTermTask,
        currentPlan: List<PlanItem>,
        date: Long
    ): EmergencyInsertionResult {
        
        val dayOfWeek = getDayOfWeek(date)
        val fixedSchedules = repository.getFixedSchedulesByDay(dayOfWeek)
        val availableSlots = getAvailableTimeSlots(date, fixedSchedules)
        
        // 寻找最佳插入位置
        val insertionOptions = findInsertionOptions(emergencyTask, currentPlan, availableSlots)
        
        if (insertionOptions.isEmpty()) {
            return EmergencyInsertionResult(
                success = false,
                adjustedPlan = currentPlan,
                postponedTasks = emptyList(),
                message = "无法找到合适的时间段插入紧急任务"
            )
        }
        
        // 选择最佳插入方案
        val bestOption = selectBestInsertionOption(insertionOptions)
        
        return EmergencyInsertionResult(
            success = true,
            adjustedPlan = bestOption.adjustedPlan,
            postponedTasks = bestOption.postponedTasks,
            message = "紧急任务已成功插入，${bestOption.postponedTasks.size}个任务被推迟"
        )
    }
    
    /**
     * 智能任务排序
     * 排序规则：
     * 1. 紧急且当日截止的高优先级任务
     * 2. 不可调整时间的任务
     * 3. 按优先级、截止日期、时长综合排序
     */
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
            // 1. 紧急任务优先
            val task1IsEmergency = task1.taskType == TaskType.EMERGENCY
            val task2IsEmergency = task2.taskType == TaskType.EMERGENCY
            
            when {
                task1IsEmergency && !task2IsEmergency -> -1
                !task1IsEmergency && task2IsEmergency -> 1
                else -> {
                    // 2. 不可调整时间的任务优先
                    val task1IsInflexible = !task1.isFlexible
                    val task2IsInflexible = !task2.isFlexible
                    
                    when {
                        task1IsInflexible && !task2IsInflexible -> -1
                        !task1IsInflexible && task2IsInflexible -> 1
                        else -> {
                            // 3. 当日截止的高优先级任务
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
                                    // 4. 按优先级排序
                                    val priorityComparison = task1.priority.ordinal.compareTo(task2.priority.ordinal)
                                    if (priorityComparison != 0) {
                                        priorityComparison
                                    } else {
                                        // 5. 按截止日期排序
                                        val deadlineComparison = compareDeadlines(task1.deadline, task2.deadline)
                                        if (deadlineComparison != 0) {
                                            deadlineComparison
                                        } else {
                                            // 6. 按能量水平匹配当前时间段
                                            val energyComparison = compareEnergyLevels(task1.energyLevel, task2.energyLevel)
                                            if (energyComparison != 0) {
                                                energyComparison
                                            } else {
                                                // 7. 按时长排序（短任务优先）
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
    
    /**
     * 获取可用时间段（排除固定日程）
     */
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
        
        // 使用用户自定义工作时段或默认时段
        val workingHours = userWorkingHours ?: listOf(
            Pair(MORNING_START_HOUR, MORNING_END_HOUR),
            Pair(AFTERNOON_START_HOUR, AFTERNOON_END_HOUR),
            Pair(EVENING_START_HOUR, EVENING_END_HOUR)
        )
        
        val allSlots = mutableListOf<TimeSlot>()
        
        // 生成基础时间段
        for ((startHour, endHour) in workingHours) {
            val slotStart = dayStart + (startHour * 60 * 60 * 1000L)
            val slotEnd = dayStart + (endHour * 60 * 60 * 1000L)
            allSlots.add(TimeSlot(slotStart, slotEnd))
        }
        
        // 排除固定日程占用的时间
        return subtractFixedSchedules(allSlots, fixedSchedules)
    }
    
    /**
     * 从可用时间段中减去固定日程
     */
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
    
    /**
     * 从时间段中减去指定时间范围
     */
    private fun subtractTimeRange(slot: TimeSlot, excludeStart: Long, excludeEnd: Long): List<TimeSlot> {
        val result = mutableListOf<TimeSlot>()
        
        // 没有重叠
        if (excludeEnd <= slot.startTime || excludeStart >= slot.endTime) {
            result.add(slot)
            return result
        }
        
        // 前半部分
        if (slot.startTime < excludeStart) {
            result.add(TimeSlot(slot.startTime, min(excludeStart, slot.endTime)))
        }
        
        // 后半部分
        if (slot.endTime > excludeEnd) {
            result.add(TimeSlot(kotlin.math.max(excludeEnd, slot.startTime), slot.endTime))
        }
        
        return result
    }
    
    /**
     * 将任务分配到时间段
     */
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
                    // 分配任务到这个时间段
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
                    
                    // 更新剩余时间段
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
    
    // 辅助方法
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
    
    /**
     * 比较能量水平，根据当前时间段匹配最适合的任务
     * 上午：高能量任务优先
     * 下午：中等能量任务优先
     * 晚上：低能量任务优先
     */
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
        
        // 选项1：在现有空闲时间段中插入
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
                        impactScore = 0.0f // 无影响，最佳选择
                    )
                )
            }
        }
        
        // 选项2：推迟低优先级任务来腾出空间
        if (options.isEmpty()) {
            val sortedPlan = currentPlan.sortedBy { it.startTime }
            
            for (i in sortedPlan.indices) {
                val planItem = sortedPlan[i]
                val task = runBlocking { repository.getShortTermTaskById(planItem.taskId) }
                
                // 只推迟低优先级或中等优先级的任务
                if (task != null && (task.priority == TaskPriority.LOW || task.priority == TaskPriority.MEDIUM)) {
                    val adjustedPlan = sortedPlan.toMutableList()
                    val postponedTasks = mutableListOf<ShortTermTask>()
                    
                    // 移除当前任务
                    adjustedPlan.removeAt(i)
                    postponedTasks.add(task)
                    
                    // 插入紧急任务到这个位置
                    val newPlanItem = PlanItem(
                        taskId = emergencyTask.id,
                        planDate = planItem.planDate,
                        startTime = planItem.startTime,
                        endTime = planItem.startTime + emergencyDuration
                    )
                    adjustedPlan.add(i, newPlanItem)
                    
                    // 计算影响分数（推迟的任务数量和优先级）
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
        
        // 选项3：在一天的开始或结束插入（如果有时间）
        if (options.isEmpty()) {
            val dayStart = getDayStartTime(currentPlan.firstOrNull()?.planDate ?: System.currentTimeMillis())
            val dayEnd = dayStart + (24 * 60 * 60 * 1000L)
            
            // 尝试在一天开始插入
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
                        impactScore = 1.0f // 轻微影响，改变了一天的开始时间
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
        // 选择最佳插入方案（影响最小的方案）
        return options.minByOrNull { it.postponedTasks.size } ?: options.first()
    }
}

// 数据类定义
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
