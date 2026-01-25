package com.jingran.taskmanager.service

import com.jingran.taskmanager.data.entity.*
import com.jingran.taskmanager.data.repository.TaskRepository
import com.jingran.utils.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

open class PriorityAdjustmentService(private val repository: TaskRepository) {
    
    companion object {
        private const val TAG = "PriorityAdjustmentService"
        private const val DEFAULT_PRIORITY_BOOST = 1
        private const val MAX_PRIORITY_BOOST = 2
        private const val PRIORITY_DECAY_HOURS = 24
        private const val COMPLETION_RATE_THRESHOLD = 0.8f
        private const val TASK_COUNT_THRESHOLD = 5
    }
    
    data class PriorityAdjustment(
        val taskId: Long,
        val originalPriority: TaskPriority,
        val adjustedPriority: TaskPriority,
        val reason: String,
        val confidence: Float
    )
    
    data class PriorityAnalysis(
        val taskId: Long,
        val currentPriority: TaskPriority,
        val suggestedPriority: TaskPriority,
        val factors: List<PriorityFactor>,
        val adjustmentScore: Float
    )
    
    data class PriorityFactor(
        val name: String,
        val weight: Float,
        val value: Float,
        val impact: String
    )
    
    suspend fun analyzeAndAdjustPriorities(
        tasks: List<ShortTermTask>,
        historicalData: HistoricalPriorityData = HistoricalPriorityData()
    ): List<PriorityAdjustment> = withContext(Dispatchers.IO) {
        
        LogManager.d(TAG, "开始分析并调整任务优先级，任务数: ${tasks.size}")
        
        val adjustments = mutableListOf<PriorityAdjustment>()
        
        for (task in tasks) {
            val analysis = analyzeTaskPriority(task, historicalData)
            
            if (analysis.suggestedPriority != task.priority) {
                val adjustment = PriorityAdjustment(
                    taskId = task.id,
                    originalPriority = task.priority,
                    adjustedPriority = analysis.suggestedPriority,
                    reason = generateAdjustmentReason(analysis),
                    confidence = analysis.adjustmentScore
                )
                
                adjustments.add(adjustment)
                
                LogManager.d(TAG, "任务 ${task.id} 优先级调整: ${task.priority} -> ${analysis.suggestedPriority}")
            }
        }
        
        LogManager.i(TAG, "优先级调整完成，共调整 ${adjustments.size} 个任务")
        return adjustments
    }
    
    suspend fun applyPriorityAdjustments(
        adjustments: List<PriorityAdjustment>
    ): Result<Int> = withContext(Dispatchers.IO) {
        
        LogManager.d(TAG, "应用优先级调整，数量: ${adjustments.size}")
        
        var successCount = 0
        val failures = mutableListOf<Exception>()
        
        for (adjustment in adjustments) {
            try {
                val task = repository.getShortTermTaskById(adjustment.taskId)
                
                if (task != null) {
                    val updatedTask = task.copy(
                        priority = adjustment.adjustedPriority
                    )
                    
                    repository.updateShortTermTask(updatedTask)
                    successCount++
                }
            } catch (e: Exception) {
                LogManager.e(TAG, "调整任务优先级失败: ${adjustment.taskId}", e)
                failures.add(e)
            }
        }
        
        if (failures.isNotEmpty()) {
            LogManager.w(TAG, "优先级调整完成，成功: $successCount, 失败: ${failures.size}")
            return Result.failure(Exception("部分优先级调整失败"))
        }
        
        LogManager.i(TAG, "优先级调整全部成功，共调整 $successCount 个任务")
        return Result.success(successCount)
    }
    
    private fun analyzeTaskPriority(
        task: ShortTermTask,
        historicalData: HistoricalPriorityData
    ): PriorityAnalysis {
        
        val factors = mutableListOf<PriorityFactor>()
        var adjustmentScore = 0f
        
        val deadlineUrgencyFactor = calculateDeadlineUrgency(task)
        factors.add(deadlineUrgencyFactor)
        adjustmentScore += deadlineUrgencyFactor.value * 0.3f
        
        val completionRateFactor = calculateCompletionRateFactor(task, historicalData)
        factors.add(completionRateFactor)
        adjustmentScore += completionRateFactor.value * 0.2f
        
        val taskAgeFactor = calculateTaskAgeFactor(task)
        factors.add(taskAgeFactor)
        adjustmentScore += taskAgeFactor.value * 0.15f
        
        val energyLevelFactor = calculateEnergyLevelFactor(task)
        factors.add(energyLevelFactor)
        adjustmentScore += energyLevelFactor.value * 0.15f
        
        val taskTypeFactor = calculateTaskTypeFactor(task)
        factors.add(taskTypeFactor)
        adjustmentScore += taskTypeFactor.value * 0.1f
        
        val durationFactor = calculateDurationFactor(task)
        factors.add(durationFactor)
        adjustmentScore += durationFactor.value * 0.1f
        
        val suggestedPriority = calculateSuggestedPriority(
            task.priority,
            adjustmentScore
        )
        
        return PriorityAnalysis(
            taskId = task.id,
            currentPriority = task.priority,
            suggestedPriority = suggestedPriority,
            factors = factors,
            adjustmentScore = adjustmentScore
        )
    }
    
    private fun calculateDeadlineUrgency(task: ShortTermTask): PriorityFactor {
        if (task.deadline == null) {
            return PriorityFactor(
                name = "截止时间",
                weight = 0.3f,
                value = 0f,
                impact = "无截止时间"
            )
        }
        
        val currentTime = System.currentTimeMillis()
        val hoursUntilDeadline = (task.deadline!! - currentTime) / (60 * 60 * 1000L)
        
        val (value, impact) = when {
            hoursUntilDeadline < 0 -> Pair(1.0f, "已过期")
            hoursUntilDeadline < 2 -> Pair(1.0f, "2小时内到期")
            hoursUntilDeadline < 6 -> Pair(0.8f, "6小时内到期")
            hoursUntilDeadline < 12 -> Pair(0.6f, "12小时内到期")
            hoursUntilDeadline < 24 -> Pair(0.4f, "24小时内到期")
            hoursUntilDeadline < 48 -> Pair(0.2f, "48小时内到期")
            else -> Pair(0f, "48小时后到期")
        }
        
        return PriorityFactor(
            name = "截止时间",
            weight = 0.3f,
            value = value,
            impact = impact
        )
    }
    
    private fun calculateCompletionRateFactor(
        task: ShortTermTask,
        historicalData: HistoricalPriorityData
    ): PriorityFactor {
        
        val completionRate = historicalData.taskCompletionRates[task.taskType] ?: 0.5f
        
        val (value, impact) = if (completionRate < COMPLETION_RATE_THRESHOLD) {
            Pair(0.8f, "历史完成率较低: ${(completionRate * 100).toInt()}%")
        } else {
            Pair(0.2f, "历史完成率良好: ${(completionRate * 100).toInt()}%")
        }
        
        return PriorityFactor(
            name = "完成率",
            weight = 0.2f,
            value = value,
            impact = impact
        )
    }
    
    private fun calculateTaskAgeFactor(task: ShortTermTask): PriorityFactor {
        val currentTime = System.currentTimeMillis()
        val hoursSinceCreation = (currentTime - task.createTime) / (60 * 60 * 1000L)
        
        val (value, impact) = when {
            hoursSinceCreation > 168 -> Pair(1.0f, "创建超过7天")
            hoursSinceCreation > 72 -> Pair(0.8f, "创建超过3天")
            hoursSinceCreation > 48 -> Pair(0.6f, "创建超过2天")
            hoursSinceCreation > 24 -> Pair(0.4f, "创建超过1天")
            else -> Pair(0.2f, "创建不到1天")
        }
        
        return PriorityFactor(
            name = "任务年龄",
            weight = 0.15f,
            value = value,
            impact = impact
        )
    }
    
    private fun calculateEnergyLevelFactor(task: ShortTermTask): PriorityFactor {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        
        val isHighEnergyHour = currentHour in 9..12 || currentHour in 14..17
        
        val (value, impact) = if (task.energyLevel == EnergyLevel.HIGH && isHighEnergyHour) {
            Pair(0.2f, "高能量任务在高能量时段")
        } else if (task.energyLevel == EnergyLevel.HIGH && !isHighEnergyHour) {
            Pair(0.8f, "高能量任务在低能量时段")
        } else {
            Pair(0.4f, "能量水平匹配")
        }
        
        return PriorityFactor(
            name = "能量水平",
            weight = 0.15f,
            value = value,
            impact = impact
        )
    }
    
    private fun calculateTaskTypeFactor(task: ShortTermTask): PriorityFactor {
        val (value, impact) = when (task.taskType) {
            TaskType.EMERGENCY -> Pair(1.0f, "紧急任务")
            TaskType.MEETING -> Pair(0.8f, "会议任务")
            TaskType.LEARNING -> Pair(0.6f, "学习任务")
            TaskType.EXERCISE -> Pair(0.4f, "运动任务")
            TaskType.ROUTINE -> Pair(0.2f, "日常任务")
            TaskType.PERSONAL -> Pair(0.2f, "个人任务")
            TaskType.NORMAL -> Pair(0.4f, "普通任务")
            TaskType.SUBTASK -> Pair(0.2f, "子任务")
        }
        
        return PriorityFactor(
            name = "任务类型",
            weight = 0.1f,
            value = value,
            impact = impact
        )
    }
    
    private fun calculateDurationFactor(task: ShortTermTask): PriorityFactor {
        val (value, impact) = when {
            task.duration < 30 -> Pair(0.2f, "短时任务(<30分钟)")
            task.duration < 60 -> Pair(0.4f, "中等时长任务(30-60分钟)")
            task.duration < 120 -> Pair(0.6f, "长时任务(60-120分钟)")
            else -> Pair(0.8f, "超长时任务(>120分钟)")
        }
        
        return PriorityFactor(
            name = "任务时长",
            weight = 0.1f,
            value = value,
            impact = impact
        )
    }
    
    private fun calculateSuggestedPriority(
        currentPriority: TaskPriority,
        adjustmentScore: Float
    ): TaskPriority {
        
        val priorityBoost = (adjustmentScore * MAX_PRIORITY_BOOST).toInt()
        
        val currentPriorityIndex = currentPriority.ordinal
        
        val newPriorityIndex = maxOf(
            0,
            minOf(
                TaskPriority.values().size - 1,
                currentPriorityIndex - priorityBoost
            )
        )
        
        return TaskPriority.values()[newPriorityIndex]
    }
    
    private fun generateAdjustmentReason(analysis: PriorityAnalysis): String {
        val significantFactors = analysis.factors.filter { it.value > 0.6f }
        
        return if (significantFactors.isNotEmpty()) {
            val factorDescriptions = significantFactors.joinToString(", ") { "${it.name}: ${it.impact}" }
            "基于以下因素调整: $factorDescriptions"
        } else {
            "综合评分调整"
        }
    }
    
    data class HistoricalPriorityData(
        val taskCompletionRates: Map<TaskType, Float> = emptyMap(),
        val averageTaskAge: Float = 0f,
        val peakCompletionHours: List<Int> = emptyList()
    )
    
    suspend fun getPriorityStatistics(
        tasks: List<ShortTermTask>
    ): PriorityStatistics = withContext(Dispatchers.IO) {
        
        val priorityDistribution = tasks.groupingBy { it.priority }.mapValues { it.value.size }
        val averagePriority = tasks.map { it.priority.ordinal }.average()
        
        val urgentTasks = tasks.filter { it.priority == TaskPriority.URGENT }
        val highPriorityTasks = tasks.filter { it.priority == TaskPriority.HIGH }
        val overdueTasks = tasks.filter { 
            it.deadline != null && it.deadline < System.currentTimeMillis() 
        }
        
        return PriorityStatistics(
            totalTasks = tasks.size,
            priorityDistribution = priorityDistribution,
            averagePriority = averagePriority,
            urgentTaskCount = urgentTasks.size,
            highPriorityTaskCount = highPriorityTasks.size,
            overdueTaskCount = overdueTasks.size
        )
    }
    
    data class PriorityStatistics(
        val totalTasks: Int,
        val priorityDistribution: Map<TaskPriority, Int>,
        val averagePriority: Double,
        val urgentTaskCount: Int,
        val highPriorityTaskCount: Int,
        val overdueTaskCount: Int
    )
}