package com.jingran.taskmanager.service

import com.jingran.taskmanager.data.entity.ShortTermTask
import com.jingran.taskmanager.data.entity.TaskPriority
import com.jingran.taskmanager.data.entity.TaskType
import com.jingran.taskmanager.data.entity.EnergyLevel
import java.time.LocalDateTime
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * 智能规划服务
 * 提供基础的任务规划算法
 */
class IntelligentPlanningService {
    
    /**
     * 生成今日计划
     */
    fun generateTodayPlan(tasks: List<ShortTermTask>): List<ShortTermTask> {
        if (tasks.isEmpty()) return emptyList()
        
        return tasks.sortedWith { task1, task2 ->
            // 按优先级排序
            val priorityComparison = task2.priority.weight.compareTo(task1.priority.weight)
            if (priorityComparison != 0) {
                priorityComparison
            } else {
                // 优先级相同时按截止时间排序
                compareDeadlines(task1.deadline, task2.deadline)
            }
        }
    }
    
    /**
     * 生成最优计划
     */
    fun generateOptimalPlan(tasks: List<ShortTermTask>): List<ShortTermTask> {
        if (tasks.isEmpty()) return emptyList()
        
        return tasks.sortedWith { task1, task2 ->
            // 综合考虑优先级、精力水平和时长
            val score1 = calculateTaskScore(task1)
            val score2 = calculateTaskScore(task2)
            score2.compareTo(score1)
        }
    }
    
    /**
     * 计算任务分数
     */
    fun calculateTaskScore(task: ShortTermTask): Float {
        var score = 0f
        
        // 优先级权重
        score += task.priority.weight * 10f
        
        // 截止时间紧急度
        task.deadline?.let { deadline ->
            val now = System.currentTimeMillis()
            val timeLeft = deadline - now
            val hoursLeft = timeLeft / (1000 * 60 * 60)
            
            when {
                hoursLeft <= 1 -> score += 50f  // 1小时内
                hoursLeft <= 24 -> score += 20f // 24小时内
                hoursLeft <= 72 -> score += 10f // 3天内
            }
        }
        
        // 精力水平权重
        score += task.energyLevel.weight * 5f
        
        return score
    }
    
    /**
     * 优化任务顺序
     */
    fun optimizeTaskOrder(tasks: List<ShortTermTask>): List<ShortTermTask> {
        return generateOptimalPlan(tasks)
    }
    
    /**
     * 生成时间槽
     */
    fun generateTimeSlots(tasks: List<ShortTermTask>): List<TimeSlot> {
        val timeSlots = mutableListOf<TimeSlot>()
        var currentTime = LocalDateTime.now().withHour(9).withMinute(0).withSecond(0)
        
        for (task in tasks) {
            val timeSlot = TimeSlot(
                startTime = currentTime,
                durationMinutes = task.duration,
                energyLevel = getTimeSlotEnergyLevel(currentTime)
            )
            timeSlots.add(timeSlot)
            currentTime = currentTime.plusMinutes(task.duration.toLong() + 5) // 5分钟间隔
        }
        
        return timeSlots
    }
    
    /**
     * 检查任务是否适合时间槽
     */
    fun isTaskSuitableForTimeSlot(task: ShortTermTask, timeSlot: TimeSlot): Boolean {
        // 检查时长是否匹配
        if (task.duration > timeSlot.durationMinutes) {
            return false
        }
        
        // 检查精力水平是否匹配
        return when (task.energyLevel) {
            EnergyLevel.HIGH -> timeSlot.energyLevel == EnergyLevel.HIGH
            EnergyLevel.MEDIUM -> timeSlot.energyLevel in listOf(EnergyLevel.HIGH, EnergyLevel.MEDIUM)
            EnergyLevel.LOW -> true // 低精力任务可以在任何时间完成
        }
    }
    
    private fun compareDeadlines(deadline1: Long?, deadline2: Long?): Int {
        return when {
            deadline1 == null && deadline2 == null -> 0
            deadline1 == null -> 1
            deadline2 == null -> -1
            else -> deadline1.compareTo(deadline2)
        }
    }
    
    private fun getTimeSlotEnergyLevel(time: LocalDateTime): EnergyLevel {
        return when (time.hour) {
            in 9..11 -> EnergyLevel.HIGH    // 上午精力充沛
            in 14..16 -> EnergyLevel.MEDIUM // 下午中等精力
            else -> EnergyLevel.LOW         // 其他时间低精力
        }
    }
    
    /**
     * 时间槽数据类
     */
    data class TimeSlot(
        val startTime: LocalDateTime,
        val durationMinutes: Int,
        val energyLevel: EnergyLevel
    )
}