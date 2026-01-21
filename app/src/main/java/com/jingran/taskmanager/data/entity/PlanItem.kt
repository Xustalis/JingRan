package com.jingran.taskmanager.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * 计划项实体
 * 用于存储每日计划的时间轴信息
 */
@Entity(
    tableName = "plan_items",
    foreignKeys = [
        ForeignKey(
            entity = ShortTermTask::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlanItem(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    
    val taskId: Long, // 关联的任务ID
    
    val planDate: Long, // 计划日期（时间戳，精确到天）
    
    val startTime: Long, // 开始时间（时间戳）
    
    val endTime: Long, // 结束时间（时间戳）
    
    val isCompleted: Boolean = false
)