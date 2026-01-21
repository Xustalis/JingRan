package com.jingran.taskmanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * 长期任务实体
 * 根据PRD文档要求，支持长期目标的分解和进度跟踪
 * 实现长期目标与短期任务的关联管理
 */
@Entity(tableName = "long_term_tasks")
data class LongTermTask(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    
    @ColumnInfo(name = "title")
    val title: String, // 长期任务标题（必填）
    
    @ColumnInfo(name = "goal")
    val goal: String, // 目标描述
    
    @ColumnInfo(name = "description")
    val description: String = "", // 任务描述
    
    @ColumnInfo(name = "start_date")
    val startDate: String = "", // 任务起始日期
    
    @ColumnInfo(name = "end_date")
    val endDate: String = "", // 任务结束日期
    
    @ColumnInfo(name = "total_amount")
    val totalAmount: Double = 0.0, // 任务总量
    
    @ColumnInfo(name = "allocation_times")
    val allocationTimes: Int = 1, // 分配次数
    
    @ColumnInfo(name = "execution_frequency")
    val executionFrequency: ExecutionFrequency = ExecutionFrequency.DAILY, // 执行频率
    
    @ColumnInfo(name = "frequency_value")
    val frequencyValue: Int = 1, // 频率数值（如每周3次，则为3）
    
    @ColumnInfo(name = "target_deadline")
    val targetDeadline: Long? = null, // 目标完成期限（可选）
    
    @ColumnInfo(name = "progress")
    val progress: Float = 0f, // 进度百分比（0-1）
    
    @ColumnInfo(name = "total_sub_tasks")
    val totalSubTasks: Int = 0, // 子任务总数
    
    @ColumnInfo(name = "completed_sub_tasks")
    val completedSubTasks: Int = 0, // 已完成子任务数
    
    @ColumnInfo(name = "create_time")
    val createTime: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false, // 当所有子任务完成后自动标记为完成
    
    @ColumnInfo(name = "category")
    val category: LongTermCategory = LongTermCategory.PERSONAL, // 长期任务分类
    
    @ColumnInfo(name = "priority")
    val priority: TaskPriority = TaskPriority.MEDIUM, // 长期任务优先级
    
    @ColumnInfo(name = "estimated_total_hours")
    val estimatedTotalHours: Int? = null, // 预估总时长（小时）
    
    @ColumnInfo(name = "actual_total_hours")
    val actualTotalHours: Int = 0, // 实际投入总时长（小时）
    
    @ColumnInfo(name = "milestone_count")
    val milestoneCount: Int = 0, // 里程碑数量
    
    @ColumnInfo(name = "completed_milestones")
    val completedMilestones: Int = 0, // 已完成里程碑数
    
    @ColumnInfo(name = "last_activity_time")
    val lastActivityTime: Long? = null, // 最后活动时间
    
    @ColumnInfo(name = "auto_generate_subtasks")
    val autoGenerateSubtasks: Boolean = true, // 是否自动生成子任务
    
    @ColumnInfo(name = "next_review_date")
    val nextReviewDate: Long? = null, // 下次回顾日期
    
    @ColumnInfo(name = "last_modified_time")
    val lastModifiedTime: Long = System.currentTimeMillis() // 最后修改时间
)

/**
 * 执行频率枚举
 * 支持多种长期任务的执行模式
 */
enum class ExecutionFrequency(val value: String, val description: String) {
    DAILY("daily", "每日"),
    WEEKLY("weekly", "每周"),
    MONTHLY("monthly", "每月"),
    CUSTOM("custom", "自定义"),
    FLEXIBLE("flexible", "灵活安排")
}

/**
 * 长期任务分类枚举
 * 帮助用户更好地组织和管理长期目标
 */
enum class LongTermCategory(val value: String, val displayName: String) {
    ACADEMIC("academic", "学术学习"),
    CAREER("career", "职业发展"),
    HEALTH("health", "健康管理"),
    PERSONAL("personal", "个人成长"),
    SKILL("skill", "技能提升"),
    HOBBY("hobby", "兴趣爱好"),
    FINANCE("finance", "财务管理"),
    RELATIONSHIP("relationship", "人际关系"),
    PROJECT("project", "项目管理")
}