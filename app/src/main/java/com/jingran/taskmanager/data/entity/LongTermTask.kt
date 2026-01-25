package com.jingran.taskmanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index

@Entity(
    tableName = "long_term_tasks",
    indices = [
        Index(value = ["category"]),
        Index(value = ["priority"]),
        Index(value = ["is_completed"]),
        Index(value = ["target_deadline"])
    ]
)
data class LongTermTask(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "goal")
    val goal: String,
    
    @ColumnInfo(name = "description")
    val description: String = "",
    
    @ColumnInfo(name = "start_date")
    val startDate: String = "",
    
    @ColumnInfo(name = "end_date")
    val endDate: String = "",
    
    @ColumnInfo(name = "total_amount")
    val totalAmount: Double = 0.0,
    
    @ColumnInfo(name = "allocation_times")
    val allocationTimes: Int = 1,
    
    @ColumnInfo(name = "execution_frequency")
    val executionFrequency: ExecutionFrequency = ExecutionFrequency.DAILY,
    
    @ColumnInfo(name = "frequency_value")
    val frequencyValue: Int = 1,
    
    @ColumnInfo(name = "target_deadline")
    val targetDeadline: Long? = null,
    
    @ColumnInfo(name = "progress")
    val progress: Float = 0f,
    
    @ColumnInfo(name = "total_sub_tasks")
    val totalSubTasks: Int = 0,
    
    @ColumnInfo(name = "completed_sub_tasks")
    val completedSubTasks: Int = 0,
    
    @ColumnInfo(name = "create_time")
    val createTime: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,
    
    @ColumnInfo(name = "category")
    val category: LongTermCategory = LongTermCategory.PERSONAL,
    
    @ColumnInfo(name = "priority")
    val priority: TaskPriority = TaskPriority.MEDIUM,
    
    @ColumnInfo(name = "last_activity_time")
    val lastActivityTime: Long? = null,
    
    @ColumnInfo(name = "last_modified_time")
    val lastModifiedTime: Long = System.currentTimeMillis()
)

enum class ExecutionFrequency(val value: String, val description: String) {
    DAILY("daily", "每日"),
    WEEKLY("weekly", "每周"),
    MONTHLY("monthly", "每月"),
    CUSTOM("custom", "自定义"),
    FLEXIBLE("flexible", "灵活安排")
}

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