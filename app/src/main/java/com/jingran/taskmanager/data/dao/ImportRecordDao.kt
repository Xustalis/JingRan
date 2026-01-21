package com.jingran.taskmanager.data.dao

import androidx.room.*
import com.jingran.taskmanager.data.entity.ImportRecord
import com.jingran.taskmanager.data.entity.ImportSource
import com.jingran.taskmanager.data.entity.ImportStatus
import com.jingran.taskmanager.data.entity.ImportStatistics
import kotlinx.coroutines.flow.Flow

/**
 * 导入记录数据访问对象
 * 提供导入记录相关的数据库操作
 */
@Dao
interface ImportRecordDao {
    
    /**
     * 获取所有导入记录
     */
    @Query("SELECT * FROM import_records ORDER BY import_time DESC")
    fun getAllImportRecords(): Flow<List<ImportRecord>>
    
    /**
     * 根据ID获取导入记录
     */
    @Query("SELECT * FROM import_records WHERE id = :id")
    suspend fun getImportRecordById(id: Long): ImportRecord?
    
    /**
     * 根据批次ID获取导入记录
     */
    @Query("SELECT * FROM import_records WHERE batch_id = :batchId")
    suspend fun getImportRecordByBatchId(batchId: String): ImportRecord?
    
    /**
     * 根据批次ID获取所有导入记录（用于Repository）
     */
    @Query("SELECT * FROM import_records WHERE batch_id = :batchId ORDER BY import_time DESC")
    suspend fun getImportRecordsByBatchId(batchId: String): List<ImportRecord>
    
    /**
     * 根据导入类型获取记录
     */
    @Query("SELECT * FROM import_records WHERE import_type = :importType ORDER BY import_time DESC")
    fun getImportRecordsByType(importType: ImportSource): Flow<List<ImportRecord>>
    
    /**
     * 根据导入状态获取记录
     */
    @Query("SELECT * FROM import_records WHERE import_status = :status ORDER BY import_time DESC")
    fun getImportRecordsByStatus(status: ImportStatus): Flow<List<ImportRecord>>
    
    /**
     * 获取最近的导入记录
     */
    @Query("SELECT * FROM import_records ORDER BY import_time DESC LIMIT :limit")
    suspend fun getRecentImportRecords(limit: Int = 10): List<ImportRecord>
    
    /**
     * 获取成功的导入记录
     */
    @Query("SELECT * FROM import_records WHERE import_status = 'success' ORDER BY import_time DESC")
    fun getSuccessfulImports(): Flow<List<ImportRecord>>
    
    /**
     * 获取失败的导入记录
     */
    @Query("SELECT * FROM import_records WHERE import_status = 'failed' ORDER BY import_time DESC")
    fun getFailedImports(): Flow<List<ImportRecord>>
    
    /**
     * 获取正在处理的导入记录
     */
    @Query("SELECT * FROM import_records WHERE import_status = 'processing' ORDER BY import_time DESC")
    suspend fun getProcessingImports(): List<ImportRecord>
    
    /**
     * 根据时间范围获取导入记录
     */
    @Query("SELECT * FROM import_records WHERE import_time BETWEEN :startTime AND :endTime ORDER BY import_time DESC")
    suspend fun getImportRecordsByTimeRange(startTime: Long, endTime: Long): List<ImportRecord>
    
    /**
     * 搜索导入记录（按文件名）
     */
    @Query("SELECT * FROM import_records WHERE file_name LIKE '%' || :query || '%' ORDER BY import_time DESC")
    fun searchImportRecords(query: String): Flow<List<ImportRecord>>
    
    /**
     * 获取导入统计信息
     */
    @Query("""
        SELECT 
            COUNT(*) as totalImports,
            SUM(CASE WHEN import_status = 'success' THEN 1 ELSE 0 END) as successfulImports,
            SUM(CASE WHEN import_status = 'failed' THEN 1 ELSE 0 END) as failedImports,
            SUM(CASE WHEN import_status = 'processing' THEN 1 ELSE 0 END) as processingImports,
            SUM(total_records) as totalRecordsProcessed,
            SUM(success_records) as totalSuccessRecords,
            SUM(failed_records) as totalFailedRecords
        FROM import_records
    """)
    suspend fun getImportStatistics(): ImportStatistics?
    
    /**
     * 根据导入类型获取统计信息
     */
    @Query("""
        SELECT 
            COUNT(*) as totalImports,
            SUM(CASE WHEN import_status = 'success' THEN 1 ELSE 0 END) as successfulImports,
            SUM(CASE WHEN import_status = 'failed' THEN 1 ELSE 0 END) as failedImports,
            0 as processingImports,
            SUM(total_records) as totalRecordsProcessed,
            SUM(success_records) as totalSuccessRecords,
            SUM(failed_records) as totalFailedRecords
        FROM import_records 
        WHERE import_type = :importType
    """)
    suspend fun getImportStatisticsByType(importType: ImportSource): ImportStatistics?
    
    /**
     * 插入导入记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportRecord(record: ImportRecord): Long
    
    /**
     * 批量插入导入记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportRecords(records: List<ImportRecord>): List<Long>
    
    /**
     * 更新导入记录
     */
    @Update
    suspend fun updateImportRecord(record: ImportRecord)
    
    /**
     * 更新导入状态
     */
    @Query("""
        UPDATE import_records 
        SET import_status = :status, 
            completion_time = :completionTime,
            error_message = :errorMessage
        WHERE id = :id
    """)
    suspend fun updateImportStatus(
        id: Long, 
        status: ImportStatus, 
        completionTime: Long? = null,
        errorMessage: String? = null
    )
    
    /**
     * 更新导入进度
     */
    @Query("""
        UPDATE import_records 
        SET success_records = :successRecords,
            failed_records = :failedRecords
        WHERE id = :id
    """)
    suspend fun updateImportProgress(id: Long, successRecords: Int, failedRecords: Int)
    
    /**
     * 根据批次ID更新状态
     */
    @Query("""
        UPDATE import_records 
        SET import_status = :status,
            completion_time = :completionTime,
            error_message = :errorMessage
        WHERE batch_id = :batchId
    """)
    suspend fun updateImportStatusByBatchId(
        batchId: String,
        status: ImportStatus,
        completionTime: Long? = null,
        errorMessage: String? = null
    )
    
    /**
     * 删除导入记录
     */
    @Delete
    suspend fun deleteImportRecord(record: ImportRecord)
    
    /**
     * 根据ID删除导入记录
     */
    @Query("DELETE FROM import_records WHERE id = :id")
    suspend fun deleteImportRecordById(id: Long)
    
    /**
     * 根据批次ID删除导入记录
     */
    @Query("DELETE FROM import_records WHERE batch_id = :batchId")
    suspend fun deleteImportRecordByBatchId(batchId: String)
    
    /**
     * 根据批次ID删除多个导入记录（用于Repository）
     */
    @Query("DELETE FROM import_records WHERE batch_id = :batchId")
    suspend fun deleteImportRecordsByBatchId(batchId: String)
    
    /**
     * 删除指定时间之前的导入记录
     */
    @Query("DELETE FROM import_records WHERE import_time < :beforeTime")
    suspend fun deleteImportRecordsBefore(beforeTime: Long)
    
    /**
     * 删除失败的导入记录
     */
    @Query("DELETE FROM import_records WHERE import_status = 'failed'")
    suspend fun deleteFailedImportRecords()
    
    /**
     * 清理旧的导入记录（保留最近N条）
     */
    @Query("""
        DELETE FROM import_records 
        WHERE id NOT IN (
            SELECT id FROM import_records 
            ORDER BY import_time DESC 
            LIMIT :keepCount
        )
    """)
    suspend fun cleanupOldImportRecords(keepCount: Int = 100)
}