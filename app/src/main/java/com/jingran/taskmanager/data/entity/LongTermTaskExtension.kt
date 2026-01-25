package com.jingran.taskmanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "long_term_task_extensions",
    foreignKeys = [
        ForeignKey(
            entity = LongTermTask::class,
            parentColumns = ["id"],
            childColumns = ["long_term_task_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["long_term_task_id"])]
)
data class LongTermTaskExtension(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "long_term_task_id")
    val longTermTaskId: Long,
    
    @ColumnInfo(name = "estimated_total_hours")
    val estimatedTotalHours: Int? = null,
    
    @ColumnInfo(name = "actual_total_hours")
    val actualTotalHours: Int = 0,
    
    @ColumnInfo(name = "milestone_count")
    val milestoneCount: Int = 0,
    
    @ColumnInfo(name = "completed_milestones")
    val completedMilestones: Int = 0,
    
    @ColumnInfo(name = "auto_generate_subtasks")
    val autoGenerateSubtasks: Boolean = true,
    
    @ColumnInfo(name = "next_review_date")
    val nextReviewDate: Long? = null,
    
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    
    @ColumnInfo(name = "attachments")
    val attachments: String? = null,
    
    @ColumnInfo(name = "tags")
    val tags: String? = null,
    
    @ColumnInfo(name = "reminder_settings")
    val reminderSettings: String? = null,
    
    @ColumnInfo(name = "custom_fields")
    val customFields: String? = null,
    
    @ColumnInfo(name = "create_time")
    val createTime: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "last_modified_time")
    val lastModifiedTime: Long = System.currentTimeMillis()
)