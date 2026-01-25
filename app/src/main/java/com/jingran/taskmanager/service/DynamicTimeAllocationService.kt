package com.jingran.taskmanager.service

import com.jingran.taskmanager.data.entity.*
import com.jingran.taskmanager.data.repository.TaskRepository
import com.jingran.utils.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

open class DynamicTimeAllocationService(private val repository: TaskRepository) {
    
    companion object {
        private const val TAG = "DynamicTimeAllocationService"
        private const val DEFAULT_WORK_HOURS_PER_DAY = 8
        private const val MIN_TASK_DURATION = 15
        private const val MAX_TASK_DURATION = 180
        private const val BREAK_DURATION = 15
        private const val MIN_BREAK_DURATION = 5
        private const val TASK_PADDING = 5
    }
    
    data class AllocationResult(
        val allocatedTasks: List<AllocatedTask>,
        val unallocatedTasks: List<ShortTermTask>,
        val timeUtilization: Float,
        val allocationStrategy: AllocationStrategy
    )
    
    data class AllocatedTask(
        val task: ShortTermTask,
        val startTime: Long,
        val endTime: Long,
        val confidence: Float,
        val allocationReason: String
    )
    
    enum class AllocationStrategy {
        BALANCED,
        PRIORITY_FOCUSED,
        ENERGY_AWARE,
        DEADLINE_DRIVEN,
        FLEXIBLE
    }
    
    data class AllocationContext(
        val date: Long,
        val availableTimeSlots: List<TimeSlot>,
        val userEnergyProfile: EnergyProfile,
        val taskPriorities: Map<Long, TaskPriority>,
        val historicalData: HistoricalAllocationData
    )
    
    data class EnergyProfile(
        val highEnergyHours: List<Int>,
        val mediumEnergyHours: List<Int>,
        val lowEnergyHours: List<Int>,
        val peakProductivityHours: List<Int>
    )
    
    data class HistoricalAllocationData(
        val averageTaskDuration: Int,
        val averageTasksPerDay: Int,
        val commonTaskPatterns: Map<TaskType, List<Int>>,
        val successRates: Map<TaskType, Float>
    )
    
    suspend fun allocateTimeDynamically(
        date: Long,
        tasks: List<ShortTermTask>,
        strategy: AllocationStrategy = AllocationStrategy.BALANCED,
        userPreferences: AllocationPreferences = AllocationPreferences()
    ): AllocationResult = withContext(Dispatchers.IO) {
        
        LogManager.d(TAG, "开始动态时间分配，日期: $date, 策略: $strategy, 任务数: ${tasks.size}")
        
        val context = createAllocationContext(date, tasks, userPreferences)
        
        val (allocatedTasks, unallocatedTasks) = when (strategy) {
            AllocationStrategy.BALANCED -> allocateBalanced(tasks, context)
            AllocationStrategy.PRIORITY_FOCUSED -> allocatePriorityFocused(tasks, context)
            AllocationStrategy.ENERGY_AWARE -> allocateEnergyAware(tasks, context)
            AllocationStrategy.DEADLINE_DRIVEN -> allocateDeadlineDriven(tasks, context)
            AllocationStrategy.FLEXIBLE -> allocateFlexible(tasks, context)
        }
        
        val timeUtilization = calculateTimeUtilization(allocatedTasks, context)
        
        LogManager.i(TAG, "时间分配完成: 已分配${allocatedTasks.size}个任务，未分配${unallocatedTasks.size}个任务，时间利用率: ${timeUtilization}%")
        
        AllocationResult(
            allocatedTasks = allocatedTasks,
            unallocatedTasks = unallocatedTasks,
            timeUtilization = timeUtilization,
            allocationStrategy = strategy
        )
    }
    
    private fun createAllocationContext(
        date: Long,
        tasks: List<ShortTermTask>,
        preferences: AllocationPreferences
    ): AllocationContext {
        
        val availableTimeSlots = getAvailableTimeSlots(date, preferences.workHours)
        
        val userEnergyProfile = EnergyProfile(
            highEnergyHours = preferences.highEnergyHours,
            mediumEnergyHours = preferences.mediumEnergyHours,
            lowEnergyHours = preferences.lowEnergyHours,
            peakProductivityHours = preferences.peakProductivityHours
        )
        
        val taskPriorities = tasks.associate { it.id to it.priority }
        
        val historicalData = HistoricalAllocationData(
            averageTaskDuration = preferences.averageTaskDuration,
            averageTasksPerDay = preferences.averageTasksPerDay,
            commonTaskPatterns = emptyMap(),
            successRates = emptyMap()
        )
        
        return AllocationContext(
            date = date,
            availableTimeSlots = availableTimeSlots,
            userEnergyProfile = userEnergyProfile,
            taskPriorities = taskPriorities,
            historicalData = historicalData
        )
    }
    
    private fun allocateBalanced(
        tasks: List<ShortTermTask>,
        context: AllocationContext
    ): Pair<List<AllocatedTask>, List<ShortTermTask>> {
        
        val allocatedTasks = mutableListOf<AllocatedTask>()
        val unallocatedTasks = mutableListOf<ShortTermTask>()
        val remainingSlots = context.availableTimeSlots.toMutableList()
        
        val sortedTasks = tasks.sortedBy { it.priority.ordinal }
        
        for (task in sortedTasks) {
            val taskDurationMs = task.duration * 60 * 1000L
            var allocated = false
            
            for (i in remainingSlots.indices) {
                val slot = remainingSlots[i]
                
                if (slot.duration >= taskDurationMs + (TASK_PADDING * 60 * 1000L)) {
                    val taskStartTime = findBalancedStartTime(slot, task, context)
                    val taskEndTime = taskStartTime + taskDurationMs
                    
                    allocatedTasks.add(
                        AllocatedTask(
                            task = task,
                            startTime = taskStartTime,
                            endTime = taskEndTime,
                            confidence = 0.8f,
                            allocationReason = "平衡分配策略"
                        )
                    )
                    
                    val newSlotStart = taskEndTime + (TASK_PADDING * 60 * 1000L)
                    
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
                unallocatedTasks.add(task)
            }
        }
        
        return Pair(allocatedTasks, unallocatedTasks)
    }
    
    private fun allocatePriorityFocused(
        tasks: List<ShortTermTask>,
        context: AllocationContext
    ): Pair<List<AllocatedTask>, List<ShortTermTask>> {
        
        val allocatedTasks = mutableListOf<AllocatedTask>()
        val unallocatedTasks = mutableListOf<ShortTermTask>()
        val remainingSlots = context.availableTimeSlots.toMutableList()
        
        val sortedTasks = tasks.sortedWith(compareBy<ShortTermTask> { task1, task2 ->
            val priorityComparison = task1.priority.compareTo(task2.priority)
            if (priorityComparison != 0) return@sortedWith priorityComparison
            
            val deadlineComparison = compareDeadlines(task1.deadline, task2.deadline)
            deadlineComparison
        })
        
        for (task in sortedTasks) {
            val taskDurationMs = task.duration * 60 * 1000L
            var allocated = false
            
            for (i in remainingSlots.indices) {
                val slot = remainingSlots[i]
                
                if (slot.duration >= taskDurationMs) {
                    val taskStartTime = slot.startTime
                    val taskEndTime = taskStartTime + taskDurationMs
                    
                    val confidence = calculatePriorityConfidence(task, context)
                    
                    allocatedTasks.add(
                        AllocatedTask(
                            task = task,
                            startTime = taskStartTime,
                            endTime = taskEndTime,
                            confidence = confidence,
                            allocationReason = "优先级优先策略"
                        )
                    )
                    
                    val newSlotStart = taskEndTime + (TASK_PADDING * 60 * 1000L)
                    
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
                unallocatedTasks.add(task)
            }
        }
        
        return Pair(allocatedTasks, unallocatedTasks)
    }
    
    private fun allocateEnergyAware(
        tasks: List<ShortTermTask>,
        context: AllocationContext
    ): Pair<List<AllocatedTask>, List<ShortTermTask>> {
        
        val allocatedTasks = mutableListOf<AllocatedTask>()
        val unallocatedTasks = mutableListOf<ShortTermTask>()
        val remainingSlots = context.availableTimeSlots.toMutableList()
        
        val tasksByEnergyLevel = tasks.groupBy { it.energyLevel }
        
        val energyOrder = listOf(EnergyLevel.HIGH, EnergyLevel.MEDIUM, EnergyLevel.LOW)
        
        for (energyLevel in energyOrder) {
            val levelTasks = tasksByEnergyLevel[energyLevel] ?: continue
            
            for (task in levelTasks) {
                val taskDurationMs = task.duration * 60 * 1000L
                var allocated = false
                
                val preferredHours = when (energyLevel) {
                    EnergyLevel.HIGH -> context.userEnergyProfile.highEnergyHours
                    EnergyLevel.MEDIUM -> context.userEnergyProfile.mediumEnergyHours
                    EnergyLevel.LOW -> context.userEnergyProfile.lowEnergyHours
                }
                
                for (i in remainingSlots.indices) {
                    val slot = remainingSlots[i]
                    val slotHour = getHourFromTimestamp(slot.startTime)
                    
                    if (slot.duration >= taskDurationMs && slotHour in preferredHours) {
                        val taskStartTime = slot.startTime
                        val taskEndTime = taskStartTime + taskDurationMs
                        
                        allocatedTasks.add(
                            AllocatedTask(
                                task = task,
                                startTime = taskStartTime,
                                endTime = taskEndTime,
                                confidence = 0.9f,
                                allocationReason = "能量感知策略：匹配${energyLevel.name}能量时段"
                            )
                        )
                        
                        val newSlotStart = taskEndTime + (TASK_PADDING * 60 * 1000L)
                        
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
                    for (i in remainingSlots.indices) {
                        val slot = remainingSlots[i]
                        
                        if (slot.duration >= taskDurationMs) {
                            val taskStartTime = slot.startTime
                            val taskEndTime = taskStartTime + taskDurationMs
                            
                            allocatedTasks.add(
                                AllocatedTask(
                                    task = task,
                                    startTime = taskStartTime,
                                    endTime = taskEndTime,
                                    confidence = 0.7f,
                                    allocationReason = "能量感知策略：备用时段"
                                )
                            )
                            
                            val newSlotStart = taskEndTime + (TASK_PADDING * 60 * 1000L)
                            
                            if (newSlotStart < slot.endTime) {
                                remainingSlots[i] = TimeSlot(newSlotStart, slot.endTime)
                            } else {
                                remainingSlots.removeAt(i)
                            }
                            
                            allocated = true
                            break
                        }
                    }
                }
                
                if (!allocated) {
                    unallocatedTasks.add(task)
                }
            }
        }
        
        return Pair(allocatedTasks, unallocatedTasks)
    }
    
    private fun allocateDeadlineDriven(
        tasks: List<ShortTermTask>,
        context: AllocationContext
    ): Pair<List<AllocatedTask>, List<ShortTermTask>> {
        
        val allocatedTasks = mutableListOf<AllocatedTask>()
        val unallocatedTasks = mutableListOf<ShortTermTask>()
        val remainingSlots = context.availableTimeSlots.toMutableList()
        
        val tasksWithDeadlines = tasks.filter { it.deadline != null }
        val tasksWithoutDeadlines = tasks.filter { it.deadline == null }
        
        val sortedDeadlineTasks = tasksWithDeadlines.sortedBy { it.deadline!! }
        
        for (task in sortedDeadlineTasks) {
            val taskDurationMs = task.duration * 60 * 1000L
            val deadline = task.deadline!!
            
            var allocated = false
            
            for (i in remainingSlots.indices) {
                val slot = remainingSlots[i]
                
                if (slot.duration >= taskDurationMs && slot.endTime <= deadline) {
                    val taskStartTime = maxOf(slot.startTime, deadline - taskDurationMs)
                    val taskEndTime = taskStartTime + taskDurationMs
                    
                    val confidence = calculateDeadlineConfidence(taskStartTime, deadline)
                    
                    allocatedTasks.add(
                        AllocatedTask(
                            task = task,
                            startTime = taskStartTime,
                            endTime = taskEndTime,
                            confidence = confidence,
                            allocationReason = "截止时间驱动策略"
                        )
                    )
                    
                    val newSlotStart = taskEndTime + (TASK_PADDING * 60 * 1000L)
                    
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
                unallocatedTasks.add(task)
            }
        }
        
        for (task in tasksWithoutDeadlines) {
            val taskDurationMs = task.duration * 60 * 1000L
            var allocated = false
            
            for (i in remainingSlots.indices) {
                val slot = remainingSlots[i]
                
                if (slot.duration >= taskDurationMs) {
                    val taskStartTime = slot.startTime
                    val taskEndTime = taskStartTime + taskDurationMs
                    
                    allocatedTasks.add(
                        AllocatedTask(
                            task = task,
                            startTime = taskStartTime,
                            endTime = taskEndTime,
                            confidence = 0.6f,
                            allocationReason = "截止时间驱动策略：无截止时间"
                        )
                    )
                    
                    val newSlotStart = taskEndTime + (TASK_PADDING * 60 * 1000L)
                    
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
                unallocatedTasks.add(task)
            }
        }
        
        return Pair(allocatedTasks, unallocatedTasks)
    }
    
    private fun allocateFlexible(
        tasks: List<ShortTermTask>,
        context: AllocationContext
    ): Pair<List<AllocatedTask>, List<ShortTermTask>> {
        
        val allocatedTasks = mutableListOf<AllocatedTask>()
        val unallocatedTasks = mutableListOf<ShortTermTask>()
        val remainingSlots = context.availableTimeSlots.toMutableList()
        
        val sortedTasks = tasks.sortedBy { it.priority.ordinal }
        
        var currentSlotIndex = 0
        
        for (task in sortedTasks) {
            val taskDurationMs = task.duration * 60 * 1000L
            var allocated = false
            
            while (currentSlotIndex < remainingSlots.size && !allocated) {
                val slot = remainingSlots[currentSlotIndex]
                
                if (slot.duration >= taskDurationMs) {
                    val taskStartTime = slot.startTime
                    val taskEndTime = taskStartTime + taskDurationMs
                    
                    val confidence = 0.75f
                    
                    allocatedTasks.add(
                        AllocatedTask(
                            task = task,
                            startTime = taskStartTime,
                            endTime = taskEndTime,
                            confidence = confidence,
                            allocationReason = "灵活分配策略"
                        )
                    )
                    
                    val newSlotStart = taskEndTime + (TASK_PADDING * 60 * 1000L)
                    
                    if (newSlotStart < slot.endTime) {
                        remainingSlots[currentSlotIndex] = TimeSlot(newSlotStart, slot.endTime)
                    } else {
                        remainingSlots.removeAt(currentSlotIndex)
                    }
                    
                    allocated = true
                    break
                }
                
                currentSlotIndex++
            }
            
            if (!allocated) {
                unallocatedTasks.add(task)
            }
        }
        
        return Pair(allocatedTasks, unallocatedTasks)
    }
    
    private fun findBalancedStartTime(
        slot: TimeSlot,
        task: ShortTermTask,
        context: AllocationContext
    ): Long {
        
        val taskHour = getHourFromTimestamp(slot.startTime)
        val isHighEnergyHour = taskHour in context.userEnergyProfile.highEnergyHours
        
        return if (task.energyLevel == EnergyLevel.HIGH && isHighEnergyHour) {
            slot.startTime
        } else if (task.energyLevel == EnergyLevel.HIGH && !isHighEnergyHour) {
            findNearestHighEnergyHour(slot, context.userEnergyProfile.highEnergyHours)
        } else {
            slot.startTime
        }
    }
    
    private fun findNearestHighEnergyHour(
        slot: TimeSlot,
        highEnergyHours: List<Int>
    ): Long {
        
        val slotHour = getHourFromTimestamp(slot.startTime)
        val calendar = Calendar.getInstance().apply { timeInMillis = slot.startTime }
        
        val nearestHour = highEnergyHours.minByOrNull { abs(it - slotHour) }
        
        if (nearestHour != null) {
            calendar.set(Calendar.HOUR_OF_DAY, nearestHour)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            
            val proposedTime = calendar.timeInMillis
            
            if (proposedTime < slot.endTime) {
                return proposedTime
            }
        }
        
        return slot.startTime
    }
    
    private fun calculatePriorityConfidence(
        task: ShortTermTask,
        context: AllocationContext
    ): Float {
        
        var confidence = 0.7f
        
        if (task.priority == TaskPriority.URGENT) {
            confidence += 0.2f
        } else if (task.priority == TaskPriority.HIGH) {
            confidence += 0.1f
        }
        
        if (task.deadline != null) {
            val hoursUntilDeadline = (task.deadline!! - context.date) / (60 * 60 * 1000L)
            
            if (hoursUntilDeadline < 24) {
                confidence += 0.1f
            } else if (hoursUntilDeadline < 48) {
                confidence += 0.05f
            }
        }
        
        return minOf(1.0f, confidence)
    }
    
    private fun calculateDeadlineConfidence(
        startTime: Long,
        deadline: Long
    ): Float {
        
        val timeUntilDeadline = deadline - startTime
        val hoursUntilDeadline = timeUntilDeadline / (60 * 60 * 1000L)
        
        return when {
            hoursUntilDeadline < 0 -> 0.0f
            hoursUntilDeadline < 1 -> 1.0f
            hoursUntilDeadline < 2 -> 0.9f
            hoursUntilDeadline < 4 -> 0.8f
            hoursUntilDeadline < 8 -> 0.7f
            hoursUntilDeadline < 12 -> 0.6f
            hoursUntilDeadline < 24 -> 0.5f
            else -> 0.4f
        }
    }
    
    private fun calculateTimeUtilization(
        allocatedTasks: List<AllocatedTask>,
        context: AllocationContext
    ): Float {
        
        val totalAvailableTime = context.availableTimeSlots.sumOf { it.duration }
        val totalAllocatedTime = allocatedTasks.sumOf { it.endTime - it.startTime }
        
        return if (totalAvailableTime > 0) {
            (totalAllocatedTime.toFloat() / totalAvailableTime.toFloat()) * 100f
        } else {
            0f
        }
    }
    
    private fun getAvailableTimeSlots(
        date: Long,
        workHours: List<Pair<Int, Int>>
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
        
        for ((startHour, endHour) in workHours) {
            val slotStart = dayStart + (startHour * 60 * 60 * 1000L)
            val slotEnd = dayStart + (endHour * 60 * 60 * 1000L)
            allSlots.add(TimeSlot(slotStart, slotEnd))
        }
        
        return allSlots
    }
    
    private fun getHourFromTimestamp(timestamp: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return calendar.get(Calendar.HOUR_OF_DAY)
    }
    
    private fun compareDeadlines(deadline1: Long?, deadline2: Long?): Int {
        return when {
            deadline1 == null && deadline2 == null -> 0
            deadline1 == null -> 1
            deadline2 == null -> -1
            else -> deadline1.compareTo(deadline2)
        }
    }
    
    data class AllocationPreferences(
        val workHours: List<Pair<Int, Int>> = listOf(Pair(9, 12), Pair(14, 18), Pair(19, 22)),
        val highEnergyHours: List<Int> = listOf(9, 10, 11),
        val mediumEnergyHours: List<Int> = listOf(14, 15, 16, 17),
        val lowEnergyHours: List<Int> = listOf(19, 20, 21),
        val peakProductivityHours: List<Int> = listOf(9, 10, 11, 14, 15, 16),
        val averageTaskDuration: Int = 60,
        val averageTasksPerDay: Int = 5,
        val allowTaskSplitting: Boolean = false,
        val minimumTaskDuration: Int = MIN_TASK_DURATION
    )
}