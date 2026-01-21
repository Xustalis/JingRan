package com.jingran.taskmanager.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * 子任务关联表（长期任务-短期任务）
 * 用于建立长期任务和短期任务之间的关联关系
 */
@Entity(
    tableName = "sub_tasks",
    foreignKeys = [
        ForeignKey(
            entity = LongTermTask::class,
            parentColumns = ["id"],
            childColumns = ["longTaskId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ShortTermTask::class,
            parentColumns = ["id"],
            childColumns = ["shortTaskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["longTaskId"]),
        androidx.room.Index(value = ["shortTaskId"])
    ]
)
data class SubTask(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    
    val longTaskId: Long,
    
    val shortTaskId: Long
)