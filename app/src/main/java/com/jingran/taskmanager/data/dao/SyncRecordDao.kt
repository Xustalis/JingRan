package com.jingran.taskmanager.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.jingran.taskmanager.data.entity.SyncRecord

/**
 * 同步记录数据访问对象
 * 提供同步记录的数据库操作方法
 */
@Dao
interface SyncRecordDao {
    
    /**
     * 获取所有同步记录
     */
    @Query("SELECT * FROM sync_records ORDER BY sync_time DESC")
    suspend fun getAllSyncRecords(): List<SyncRecord>
    
    /**
     * 获取所有同步记录（LiveData）
     */
    @Query("SELECT * FROM sync_records ORDER BY sync_time DESC")
    fun getAllSyncRecordsLive(): LiveData<List<SyncRecord>>
    
    /**
     * 根据ID获取同步记录
     */
    @Query("SELECT * FROM sync_records WHERE id = :id")
    suspend fun getSyncRecordById(id: Long): SyncRecord?
    
    /**
     * 根据同步类型获取记录
     */
    @Query("SELECT * FROM sync_records WHERE sync_type = :syncType ORDER BY sync_time DESC")
    suspend fun getSyncRecordsByType(syncType: String): List<SyncRecord>
    
    /**
     * 根据同步状态获取记录
     */
    @Query("SELECT * FROM sync_records WHERE sync_status = :status ORDER BY sync_time DESC")
    suspend fun getSyncRecordsByStatus(status: String): List<SyncRecord>
    
    /**
     * 获取最近的同步记录
     */
    @Query("SELECT * FROM sync_records ORDER BY sync_time DESC LIMIT :limit")
    suspend fun getRecentSyncRecords(limit: Int): List<SyncRecord>
    
    /**
     * 获取成功的同步记录
     */
    @Query("SELECT * FROM sync_records WHERE sync_status = 'SUCCESS' ORDER BY sync_time DESC")
    suspend fun getSuccessfulSyncRecords(): List<SyncRecord>
    
    /**
     * 获取失败的同步记录
     */
    @Query("SELECT * FROM sync_records WHERE sync_status = 'FAILED' ORDER BY sync_time DESC")
    suspend fun getFailedSyncRecords(): List<SyncRecord>
    
    /**
     * 获取最后一次同步记录
     */
    @Query("SELECT * FROM sync_records ORDER BY sync_time DESC LIMIT 1")
    suspend fun getLastSyncRecord(): SyncRecord?
    
    /**
     * 获取最后一次成功同步的时间戳
     */
    @Query("SELECT sync_time FROM sync_records WHERE sync_status = 'SUCCESS' ORDER BY sync_time DESC LIMIT 1")
    suspend fun getLastSuccessfulSyncTime(): Long?
    
    /**
     * 插入同步记录
     */
    @Insert
    suspend fun insertSyncRecord(syncRecord: SyncRecord): Long
    
    /**
     * 批量插入同步记录
     */
    @Insert
    suspend fun insertSyncRecords(syncRecords: List<SyncRecord>)
    
    /**
     * 更新同步记录
     */
    @Update
    suspend fun updateSyncRecord(syncRecord: SyncRecord)
    
    /**
     * 删除同步记录
     */
    @Delete
    suspend fun deleteSyncRecord(syncRecord: SyncRecord)
    
    /**
     * 根据ID删除同步记录
     */
    @Query("DELETE FROM sync_records WHERE id = :id")
    suspend fun deleteSyncRecordById(id: Long)
    
    /**
     * 清空所有同步记录
     */
    @Query("DELETE FROM sync_records")
    suspend fun deleteAllSyncRecords()
    
    /**
     * 删除指定时间之前的同步记录
     */
    @Query("DELETE FROM sync_records WHERE sync_time < :timestamp")
    suspend fun deleteSyncRecordsBefore(timestamp: Long)
    
    /**
     * 获取同步记录总数
     */
    @Query("SELECT COUNT(*) FROM sync_records")
    suspend fun getSyncRecordCount(): Int
    
    /**
     * 根据时间范围获取同步记录
     */
    @Query("SELECT * FROM sync_records WHERE sync_time BETWEEN :startTime AND :endTime ORDER BY sync_time DESC")
    suspend fun getSyncRecordsByTimeRange(startTime: Long, endTime: Long): List<SyncRecord>
}