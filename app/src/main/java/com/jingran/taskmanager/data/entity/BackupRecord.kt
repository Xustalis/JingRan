package com.jingran.taskmanager.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 备份记录实体类
 * 记录数据备份的历史和状态
 */
@Entity(tableName = "backup_records")
data class BackupRecord(
    @PrimaryKey
    @ColumnInfo(name = "backup_id")
    val backupId: String,
    
    @ColumnInfo(name = "backup_name")
    val backupName: String,
    
    @ColumnInfo(name = "creation_time")
    val creationTime: Long,
    
    @ColumnInfo(name = "data_size")
    val dataSize: Long,
    
    @ColumnInfo(name = "record_count")
    val recordCount: Int,
    
    @ColumnInfo(name = "description")
    val description: String = ""
)