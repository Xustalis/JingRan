package com.jingran.taskmanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 每日效率统计实体
 * 用于存储每日任务完成情况和效率数据
 */
@Entity(tableName = "daily_stats")
data class DailyStats(
    @PrimaryKey 
    val date: Long, // 日期（时间戳，精确到天）
    
    val totalPlannedTasks: Int = 0, // 当日计划任务总数
    
    val completedTasks: Int = 0, // 已完成任务数
    
    val totalPlannedDuration: Int = 0, // 计划总时长（分钟）
    
    val actualCompletedDuration: Int = 0, // 实际完成时长（分钟）
    
    val completionRate: Float = 0f, // 完成率（0-1）
    
    val efficiencyRate: Float = 0f, // 效率率（实际完成时长/计划时长）
    
    val emergencyTasksAdded: Int = 0, // 当日添加的紧急任务数
    
    val tasksPostponed: Int = 0, // 被推迟的任务数
    
    val updateTime: Long = System.currentTimeMillis() // 最后更新时间
)