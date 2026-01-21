package com.jingran.taskmanager.service

import com.jingran.taskmanager.data.entity.ExecutionFrequency
import com.jingran.taskmanager.data.entity.LongTermTask
import com.jingran.taskmanager.data.entity.ShortTermTask
import com.jingran.taskmanager.data.entity.SubTask
import com.jingran.taskmanager.data.entity.TaskPriority
import com.jingran.taskmanager.data.entity.TaskType
import com.jingran.taskmanager.data.repository.TaskRepository
import kotlinx.coroutines.flow.first

/**
 * 子任务管理服务
 * 负责长期任务的子任务创建、完成和进度跟踪
 */
open class SubTaskManager(private val repository: TaskRepository) {
    
    /**
     * 为长期任务创建子任务
     * @param longTermTaskId 长期任务ID
     * @param subTaskTitle 子任务标题
     * @param description 子任务描述
     * @param estimatedDuration 预估时长（分钟）
     * @param priority 优先级
     * @return 创建的子任务ID
     */
    suspend fun createSubTask(
        longTermTaskId: Long,
        subTaskTitle: String,
        description: String? = null,
        estimatedDuration: Int = 30,
        priority: TaskPriority = TaskPriority.MEDIUM
    ): Long {
        
        // 先创建短期任务
        val shortTermTask = ShortTermTask(
            title = subTaskTitle,
            description = description,
            duration = estimatedDuration,
            priority = priority,
            taskType = TaskType.SUBTASK,
            isCompleted = false,
            createTime = System.currentTimeMillis()
        )
        
        val shortTaskId = repository.insertShortTermTask(shortTermTask)
        
        // 然后创建关联
        repository.addSubTask(longTermTaskId, shortTaskId)
        
        // 更新长期任务的子任务总数
        updateLongTermTaskProgress(longTermTaskId)
        
        return shortTaskId
    }
    
    /**
     * 完成子任务
     * @param shortTaskId 短期任务ID（子任务实际上是ShortTermTask）
     */
    suspend fun completeSubTask(shortTaskId: Long) {
        val shortTermTask = repository.getShortTermTaskById(shortTaskId)
        val subTask = repository.getSubTaskByShortTaskId(shortTaskId)
        
        if (shortTermTask != null && subTask != null && !shortTermTask.isCompleted) {
            val completedTask = shortTermTask.copy(
                isCompleted = true,
                completedTime = System.currentTimeMillis()
            )
            
            repository.updateShortTermTask(completedTask)
            
            // 更新长期任务进度
            updateLongTermTaskProgress(subTask.longTaskId)
            
            // 检查是否所有子任务都已完成
            checkAndCompleteLongTermTask(subTask.longTaskId)
        }
    }
    
    /**
     * 取消完成子任务
     * @param shortTaskId 短期任务ID（子任务实际上是ShortTermTask）
     */
    suspend fun uncompleteSubTask(shortTaskId: Long) {
        val shortTermTask = repository.getShortTermTaskById(shortTaskId)
        val subTask = repository.getSubTaskByShortTaskId(shortTaskId)
        
        if (shortTermTask != null && subTask != null && shortTermTask.isCompleted) {
            val uncompletedTask = shortTermTask.copy(
                isCompleted = false,
                completedTime = null
            )
            
            repository.updateShortTermTask(uncompletedTask)
            
            // 更新长期任务进度
            updateLongTermTaskProgress(subTask.longTaskId)
            
            // 如果长期任务已完成，需要取消完成状态
            val longTermTask = repository.getLongTermTaskById(subTask.longTaskId)
            if (longTermTask?.isCompleted == true) {
                repository.updateLongTermTask(
                    longTermTask.copy(isCompleted = false)
                )
            }
        }
    }
    
    /**
     * 删除子任务
     * @param shortTaskId 短期任务ID（子任务实际上是ShortTermTask）
     */
    suspend fun deleteSubTask(shortTaskId: Long) {
        // 获取关联的长期任务ID和短期任务对象
        val subTask = repository.getSubTaskByShortTaskId(shortTaskId)
        val shortTermTask = repository.getShortTermTaskById(shortTaskId)
        
        if (subTask != null && shortTermTask != null) {
            val longTermTaskId = subTask.longTaskId
            
            // 删除短期任务
            repository.deleteShortTermTask(shortTermTask)
            
            // 移除关联关系
            repository.removeSubTask(longTermTaskId, shortTaskId)
            
            // 更新长期任务进度
            updateLongTermTaskProgress(longTermTaskId)
        }
    }
    
    /**
     * 批量创建子任务
     * @param longTermTaskId 长期任务ID
     * @param subTaskTitles 子任务标题列表
     * @return 创建的子任务ID列表
     */
    suspend fun createSubTasksBatch(
        longTermTaskId: Long,
        subTaskTitles: List<String>
    ): List<Long> {
        
        val subTaskIds = mutableListOf<Long>()
        
        for (title in subTaskTitles) {
            val subTaskId = createSubTask(longTermTaskId, title)
            subTaskIds.add(subTaskId)
        }
        
        return subTaskIds
    }
    
    /**
     * 获取长期任务的进度信息
     * @param longTermTaskId 长期任务ID
     * @return 进度信息
     */
    suspend fun getTaskProgress(longTermTaskId: Long): TaskProgress {
        val allSubTasks = repository.getShortTasksByLongTaskId(longTermTaskId).value ?: emptyList()
        val totalSubTasks = allSubTasks.size
        val completedSubTasks = allSubTasks.count { it.isCompleted }
        val progress = if (totalSubTasks > 0) {
            (completedSubTasks.toFloat() / totalSubTasks * 100).toInt()
        } else {
            0
        }
        
        return TaskProgress(
            totalSubTasks = totalSubTasks,
            completedSubTasks = completedSubTasks,
            progress = progress,
            isCompleted = totalSubTasks > 0 && completedSubTasks == totalSubTasks
        )
    }
    
    /**
     * 根据执行频率生成子任务计划
     * @param longTermTask 长期任务
     * @return 建议的子任务执行计划
     */
    suspend fun generateSubTaskSchedule(longTermTask: LongTermTask): SubTaskSchedule {
        val incompleteSubTasks = repository.getIncompleteSubTasksByLongTaskId(longTermTask.id)
        
        if (incompleteSubTasks.isEmpty()) {
            return SubTaskSchedule(
                recommendedSubTasks = emptyList(),
                message = "所有子任务已完成"
            )
        }
        
        // 根据执行频率计算每次应该执行的子任务数量
        val tasksPerSession = when (longTermTask.executionFrequency) {
            ExecutionFrequency.DAILY -> min(2, incompleteSubTasks.size)
            ExecutionFrequency.WEEKLY -> min(1, incompleteSubTasks.size)
            ExecutionFrequency.MONTHLY -> min(1, incompleteSubTasks.size)
            else -> 1
        }
        
        // 按优先级和创建时间排序
        val sortedSubTasks = incompleteSubTasks.sortedWith { task1: ShortTermTask, task2: ShortTermTask ->
            val priorityComparison = getPriorityValue(task1.priority.value).compareTo(getPriorityValue(task2.priority.value))
            if (priorityComparison != 0) {
                priorityComparison
            } else {
                task1.createTime.compareTo(task2.createTime)
            }
        }
        
        val recommendedSubTasks = sortedSubTasks.take(tasksPerSession)
        
        return SubTaskSchedule(
            recommendedSubTasks = recommendedSubTasks,
            message = "建议今日完成 ${recommendedSubTasks.size} 个子任务"
        )
    }
    
    /**
     * 更新长期任务的进度
     */
    private suspend fun updateLongTermTaskProgress(longTermTaskId: Long) {
        val progress = getTaskProgress(longTermTaskId)
        val longTermTask = repository.getLongTermTaskById(longTermTaskId)
        
        if (longTermTask != null) {
            val updatedTask = longTermTask.copy(
                totalSubTasks = progress.totalSubTasks,
                completedSubTasks = progress.completedSubTasks,
                progress = progress.progress.toFloat() / 100f
            )
            
            repository.updateLongTermTask(updatedTask)
        }
    }
    
    /**
     * 检查并完成长期任务（如果所有子任务都已完成）
     */
    private suspend fun checkAndCompleteLongTermTask(longTermTaskId: Long) {
        val progress = getTaskProgress(longTermTaskId)
        
        if (progress.isCompleted) {
            val longTermTask = repository.getLongTermTaskById(longTermTaskId)
            if (longTermTask != null && !longTermTask.isCompleted) {
                val completedTask = longTermTask.copy(
                    isCompleted = true
                )
                
                repository.updateLongTermTask(completedTask)
            }
        }
    }
    
    private fun getPriorityValue(priority: String): Int {
        return when (priority) {
            "high" -> 1
            "medium" -> 2
            "low" -> 3
            else -> 4
        }
    }
    
    private fun min(a: Int, b: Int): Int = if (a < b) a else b
}

/**
 * 任务进度数据类
 */
data class TaskProgress(
    val totalSubTasks: Int,
    val completedSubTasks: Int,
    val progress: Int, // 百分比
    val isCompleted: Boolean
)

/**
 * 子任务执行计划数据类
 */
data class SubTaskSchedule(
    val recommendedSubTasks: List<ShortTermTask>,
    val message: String
)
