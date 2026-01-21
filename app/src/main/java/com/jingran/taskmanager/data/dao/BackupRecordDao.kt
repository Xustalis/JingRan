package com.jingran.taskmanager.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.jingran.taskmanager.data.entity.BackupRecord

/**
 * 备份记录数据访问对象
 * 提供备份记录的数据库操作方法
 */
@Dao
interface BackupRecordDao {
    
    /**
     * 获取所有备份记录
     */
    @Query("SELECT * FROM backup_records ORDER BY creation_time DESC")
    suspend fun getAllBackupRecords(): List<BackupRecord>
    
    /**
     * 获取所有备份记录（LiveData）
     */
    @Query("SELECT * FROM backup_records ORDER BY creation_time DESC")
    fun getAllBackupRecordsLive(): LiveData<List<BackupRecord>>
    
    /**
     * 根据备份ID获取备份记录
     */
    @Query("SELECT * FROM backup_records WHERE backup_id = :backupId")
    suspend fun getBackupRecordById(backupId: String): BackupRecord?
    
    /**
     * 根据备份名称获取备份记录
     */
    @Query("SELECT * FROM backup_records WHERE backup_name = :backupName")
    suspend fun getBackupRecordByName(backupName: String): BackupRecord?
    
    /**
     * 根据备份名称搜索备份记录
     */
    @Query("SELECT * FROM backup_records WHERE backup_name LIKE '%' || :query || '%' ORDER BY creation_time DESC")
    suspend fun searchBackupRecords(query: String): List<BackupRecord>
    
    /**
     * 获取最近的备份记录
     */
    @Query("SELECT * FROM backup_records ORDER BY creation_time DESC LIMIT :limit")
    suspend fun getRecentBackupRecords(limit: Int): List<BackupRecord>
    
    /**
     * 获取最新的备份记录
     */
    @Query("SELECT * FROM backup_records ORDER BY creation_time DESC LIMIT 1")
    suspend fun getLatestBackupRecord(): BackupRecord?
    
    /**
     * 根据时间范围获取备份记录
     */
    @Query("SELECT * FROM backup_records WHERE creation_time BETWEEN :startTime AND :endTime ORDER BY creation_time DESC")
    suspend fun getBackupRecordsByTimeRange(startTime: Long, endTime: Long): List<BackupRecord>
    
    /**
     * 根据数据大小范围获取备份记录
     */
    @Query("SELECT * FROM backup_records WHERE data_size BETWEEN :minSize AND :maxSize ORDER BY creation_time DESC")
    suspend fun getBackupRecordsBySizeRange(minSize: Long, maxSize: Long): List<BackupRecord>
    
    /**
     * 插入备份记录
     */
    @Insert
    suspend fun insertBackupRecord(backupRecord: BackupRecord): Long
    
    /**
     * 批量插入备份记录
     */
    @Insert
    suspend fun insertBackupRecords(backupRecords: List<BackupRecord>)
    
    /**
     * 更新备份记录
     */
    @Update
    suspend fun updateBackupRecord(backupRecord: BackupRecord)
    
    /**
     * 删除备份记录
     */
    @Delete
    suspend fun deleteBackupRecord(backupRecord: BackupRecord)
    
    /**
     * 根据备份ID删除备份记录
     */
    @Query("DELETE FROM backup_records WHERE backup_id = :backupId")
    suspend fun deleteBackupRecordById(backupId: String)
    
    /**
     * 根据备份名称删除备份记录
     */
    @Query("DELETE FROM backup_records WHERE backup_name = :backupName")
    suspend fun deleteBackupRecordByName(backupName: String)
    
    /**
     * 清空所有备份记录
     */
    @Query("DELETE FROM backup_records")
    suspend fun deleteAllBackupRecords()
    
    /**
     * 删除指定时间之前的备份记录
     */
    @Query("DELETE FROM backup_records WHERE creation_time < :timestamp")
    suspend fun deleteBackupRecordsBefore(timestamp: Long)
    
    /**
     * 获取备份记录总数
     */
    @Query("SELECT COUNT(*) FROM backup_records")
    suspend fun getBackupRecordCount(): Int
    
    /**
     * 获取备份总大小
     */
    @Query("SELECT SUM(data_size) FROM backup_records")
    suspend fun getTotalBackupSize(): Long?
    
    /**
     * 获取平均备份大小
     */
    @Query("SELECT AVG(data_size) FROM backup_records")
    suspend fun getAverageBackupSize(): Double?
    
    /**
     * 检查备份名称是否存在
     */
    @Query("SELECT COUNT(*) > 0 FROM backup_records WHERE backup_name = :backupName")
    suspend fun isBackupNameExists(backupName: String): Boolean
    
    /**
     * 检查备份ID是否存在
     */
    @Query("SELECT COUNT(*) > 0 FROM backup_records WHERE backup_id = :backupId")
    suspend fun isBackupIdExists(backupId: String): Boolean
}