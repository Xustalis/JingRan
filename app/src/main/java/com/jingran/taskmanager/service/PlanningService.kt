package com.jingran.taskmanager.service

import com.jingran.taskmanager.data.entity.PlanItem
import com.jingran.taskmanager.data.entity.ShortTermTask
import com.jingran.taskmanager.data.entity.TaskPriority
import java.util.*

/**
 * 排期算法服务类
 * 实现自动排期的核心逻辑
 */
open class PlanningService {
    
    companion object {
        // 每日可用时段：9:00-12:00、14:00-18:00
        private const val MORNING_START_HOUR = 9
        private const val MORNING_END_HOUR = 12
        private const val AFTERNOON_START_HOUR = 14
        private const val AFTERNOON_END_HOUR = 18
        
        // 任务间隔时间（分钟）
        private const val TASK_INTERVAL_MINUTES = 5
    }
    
    /**
     * 为指定日期生成任务计划
     * @param date 计划日期（时间戳，精确到天）
     * @param tasks 待安排的任务列表
     * @return 生成的计划项列表
     */
    fun generateDailyPlan(date: Long, tasks: List<ShortTermTask>): List<PlanItem> {
        val planItems = mutableListOf<PlanItem>()
        
        // 获取当日的可用时间段
        val availableSlots = getAvailableTimeSlots(date)
        
        // 按优先级和规则排序任务
        val sortedTasks = sortTasksByPriority(tasks, date)
        
        // 为每个任务分配时间段
        var currentSlotIndex = 0
        var currentSlotStartTime = availableSlots.getOrNull(currentSlotIndex)?.first ?: return planItems
        
        for (task in sortedTasks) {
            val taskDurationMs = task.duration * 60 * 1000L // 转换为毫秒
            
            // 寻找合适的时间段
            while (currentSlotIndex < availableSlots.size) {
                val (slotStart, slotEnd) = availableSlots[currentSlotIndex]
                val availableTime = slotEnd - currentSlotStartTime
                
                if (availableTime >= taskDurationMs) {
                    // 当前时间段可以容纳这个任务
                    val taskStartTime = currentSlotStartTime
                    val taskEndTime = taskStartTime + taskDurationMs
                    
                    planItems.add(
                        PlanItem(
                            taskId = task.id,
                            planDate = date,
                            startTime = taskStartTime,
                            endTime = taskEndTime
                        )
                    )
                    
                    // 更新下一个任务的开始时间（加上间隔）
                    currentSlotStartTime = taskEndTime + (TASK_INTERVAL_MINUTES * 60 * 1000L)
                    
                    // 如果当前时间段剩余时间不足，移动到下一个时间段
                    if (currentSlotStartTime >= slotEnd) {
                        currentSlotIndex++
                        currentSlotStartTime = availableSlots.getOrNull(currentSlotIndex)?.first ?: break
                    }
                    
                    break
                } else {
                    // 当前时间段不够，移动到下一个时间段
                    currentSlotIndex++
                    currentSlotStartTime = availableSlots.getOrNull(currentSlotIndex)?.first ?: break
                }
            }
        }
        
        return planItems
    }
    
    /**
     * 获取指定日期的可用时间段
     * @param date 日期时间戳
     * @return 时间段列表，每个元素为 Pair(开始时间, 结束时间)
     */
    private fun getAvailableTimeSlots(date: Long): List<Pair<Long, Long>> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val dayStart = calendar.timeInMillis
        
        // 上午时段：9:00-12:00
        val morningStart = dayStart + (MORNING_START_HOUR * 60 * 60 * 1000L)
        val morningEnd = dayStart + (MORNING_END_HOUR * 60 * 60 * 1000L)
        
        // 下午时段：14:00-18:00
        val afternoonStart = dayStart + (AFTERNOON_START_HOUR * 60 * 60 * 1000L)
        val afternoonEnd = dayStart + (AFTERNOON_END_HOUR * 60 * 60 * 1000L)
        
        return listOf(
            Pair(morningStart, morningEnd),
            Pair(afternoonStart, afternoonEnd)
        )
    }
    
    /**
     * 按优先级和规则排序任务
     * 排序规则：
     * 1. 优先安排「高优先级 + 当日截止」任务
     * 2. 剩余时段按优先级从高到低、时长从短到长填充
     */
    private fun sortTasksByPriority(tasks: List<ShortTermTask>, planDate: Long): List<ShortTermTask> {
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
            // 检查是否为当日截止的高优先级任务
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
                    // 按优先级排序
                    val priorityComparison = getPriorityValue(task1.priority.value).compareTo(getPriorityValue(task2.priority.value))
                    if (priorityComparison != 0) {
                        priorityComparison
                    } else {
                        // 优先级相同时，按时长从短到长排序
                        task1.duration.compareTo(task2.duration)
                    }
                }
            }
        }
    }
    
    /**
     * 获取优先级数值（用于排序）
     */
    private fun getPriorityValue(priority: String): Int {
        return when (priority) {
            "high" -> 1
            "medium" -> 2
            "low" -> 3
            else -> 4
        }
    }
    
    /**
     * 获取今日开始时间戳（精确到天）
     */
    fun getTodayStartTimestamp(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
    
    /**
     * 获取今日结束时间戳
     */
    fun getTodayEndTimestamp(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }
}
