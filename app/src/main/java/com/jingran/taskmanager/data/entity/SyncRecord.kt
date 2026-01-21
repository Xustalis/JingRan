package com.jingran.taskmanager.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jingran.taskmanager.service.SyncStatus
import com.jingran.taskmanager.service.SyncType

/**
 * 同步记录实体类
 * 记录数据同步的历史和状态
 */
@Entity(tableName = "sync_records")
data class SyncRecord(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    @ColumnInfo(name = "sync_type")
    val syncType: SyncType,
    
    @ColumnInfo(name = "sync_status")
    val status: SyncStatus,
    
    @ColumnInfo(name = "sync_time")
    val timestamp: Long,
    
    @ColumnInfo(name = "end_time")
    val endTime: Long = 0L,
    
    @ColumnInfo(name = "records_processed")
    val recordsProcessed: Int = 0,
    
    @ColumnInfo(name = "records_updated")
    val recordsUpdated: Int = 0,
    
    @ColumnInfo(name = "records_deleted")
    val recordsDeleted: Int = 0,
    
    @ColumnInfo(name = "conflicts_resolved")
    val conflictsResolved: Int = 0,
    
    @ColumnInfo(name = "error_message")
    val errorMessage: String = "",
    
    @ColumnInfo(name = "details")
    val details: String = ""
)