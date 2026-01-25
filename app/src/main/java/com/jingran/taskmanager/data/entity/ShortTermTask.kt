package com.jingran.taskmanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index

@Entity(
    tableName = "short_term_tasks",
    indices = [
        Index(value = ["priority"]),
        Index(value = ["is_completed"]),
        Index(value = ["deadline"]),
        Index(value = ["task_type"]),
        Index(value = ["energy_level"])
    ]
)
data class ShortTermTask(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "description")
    val description: String? = null,
    
    @ColumnInfo(name = "deadline")
    val deadline: Long? = null,
    
    @ColumnInfo(name = "duration")
    val duration: Int,
    
    @ColumnInfo(name = "priority")
    val priority: TaskPriority = TaskPriority.MEDIUM,
    
    @ColumnInfo(name = "task_type")
    val taskType: TaskType = TaskType.NORMAL,
    
    @ColumnInfo(name = "is_flexible")
    val isFlexible: Boolean = true,
    
    @ColumnInfo(name = "actual_duration")
    val actualDuration: Int? = null,
    
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,
    
    @ColumnInfo(name = "completed_time")
    val completedTime: Long? = null,
    
    @ColumnInfo(name = "create_time")
    val createTime: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "reminder_time")
    val reminderTime: Long? = null,
    
    @ColumnInfo(name = "tags")
    val tags: String? = null,
    
    @ColumnInfo(name = "energy_level")
    val energyLevel: EnergyLevel = EnergyLevel.MEDIUM,
    
    @ColumnInfo(name = "location")
    val location: String? = null,
    
    @ColumnInfo(name = "context")
    val context: String? = null,
    
    @ColumnInfo(name = "estimated_start_time")
    val estimatedStartTime: Long? = null,
    
    @ColumnInfo(name = "last_modified_time")
    val lastModifiedTime: Long = System.currentTimeMillis()
)

enum class TaskPriority(val value: String, val weight: Int) {
    LOW("low", 1),
    MEDIUM("medium", 2),
    HIGH("high", 3),
    URGENT("urgent", 4)
}

enum class TaskType(val value: String) {
    NORMAL("normal"),
    EMERGENCY("emergency"),
    SUBTASK("subtask"),
    ROUTINE("routine"),
    LEARNING("learning"),
    MEETING("meeting"),
    EXERCISE("exercise"),
    PERSONAL("personal")
}

enum class EnergyLevel(val value: String, val weight: Int) {
    LOW("low", 1),
    MEDIUM("medium", 2),
    HIGH("high", 3)
}