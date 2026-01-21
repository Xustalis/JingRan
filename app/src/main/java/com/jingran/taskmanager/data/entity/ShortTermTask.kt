package com.jingran.taskmanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * 短期任务实体
 * 根据PRD文档要求，支持任务的完整生命周期管理
 * 包括创建、规划、执行、完成等各个阶段的数据记录
 */
@Entity(tableName = "short_term_tasks")
data class ShortTermTask(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    
    @ColumnInfo(name = "title")
    val title: String, // 任务标题（必填）
    
    @ColumnInfo(name = "description")
    val description: String? = null, // 任务描述（可选）
    
    @ColumnInfo(name = "deadline")
    val deadline: Long? = null, // 截止时间戳（毫秒），可选
    
    @ColumnInfo(name = "duration")
    val duration: Int, // 预估时长（分钟），必填
    
    @ColumnInfo(name = "priority")
    val priority: TaskPriority = TaskPriority.MEDIUM, // 任务优先级
    
    @ColumnInfo(name = "task_type")
    val taskType: TaskType = TaskType.NORMAL, // 任务类型
    
    @ColumnInfo(name = "is_flexible")
    val isFlexible: Boolean = true, // 是否可调整时间（紧急任务为false）
    
    @ColumnInfo(name = "actual_duration")
    val actualDuration: Int? = null, // 实际完成时长（分钟，完成后记录）
    
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,
    
    @ColumnInfo(name = "completed_time")
    val completedTime: Long? = null, // 完成时间（时间戳）
    
    @ColumnInfo(name = "create_time")
    val createTime: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "reminder_time")
    val reminderTime: Long? = null, // 提醒时间（时间戳），可选
    
    @ColumnInfo(name = "tags")
    val tags: String? = null, // 标签，用逗号分隔
    
    @ColumnInfo(name = "energy_level")
    val energyLevel: EnergyLevel = EnergyLevel.MEDIUM, // 所需精力水平
    
    @ColumnInfo(name = "location")
    val location: String? = null, // 执行地点（可选）
    
    @ColumnInfo(name = "context")
    val context: String? = null, // 执行上下文（如@电脑、@外出等）
    
    @ColumnInfo(name = "estimated_start_time")
    val estimatedStartTime: Long? = null, // 预计开始时间
    
    @ColumnInfo(name = "parent_long_term_task_id")
    val parentLongTermTaskId: Long? = null, // 关联的长期任务ID
    
    @ColumnInfo(name = "last_modified_time")
    val lastModifiedTime: Long = System.currentTimeMillis() // 最后修改时间
)

/**
 * 任务优先级枚举
 * 根据PRD文档中的智能规划需求定义
 */
enum class TaskPriority(val value: String, val weight: Int) {
    LOW("low", 1),
    MEDIUM("medium", 2),
    HIGH("high", 3),
    URGENT("urgent", 4) // 紧急任务，最高优先级
}

/**
 * 任务类型枚举
 * 支持不同类型任务的差异化处理
 */
enum class TaskType(val value: String) {
    NORMAL("normal"),           // 普通任务
    EMERGENCY("emergency"),     // 紧急任务
    SUBTASK("subtask"),         // 子任务
    ROUTINE("routine"),         // 例行任务
    LEARNING("learning"),       // 学习任务
    MEETING("meeting"),         // 会议任务
    EXERCISE("exercise"),       // 运动任务
    PERSONAL("personal")        // 个人事务
}

/**
 * 精力水平枚举
 * 用于智能规划时考虑任务对精力的要求
 */
enum class EnergyLevel(val value: String, val weight: Int) {
    LOW("low", 1),       // 低精力要求
    MEDIUM("medium", 2), // 中等精力要求
    HIGH("high", 3)      // 高精力要求
}