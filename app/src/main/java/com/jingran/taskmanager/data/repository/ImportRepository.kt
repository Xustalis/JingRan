package com.jingran.taskmanager.data.repository

import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.Flow
import com.jingran.taskmanager.data.dao.ImportRecordDao
import com.jingran.taskmanager.data.entity.ImportRecord
import com.jingran.taskmanager.data.entity.ImportStatistics
import com.jingran.taskmanager.data.entity.ImportStatus
import com.jingran.taskmanager.data.entity.ImportSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 导入Repository
 * 负责数据导入记录相关的数据访问操作
 */
@Singleton
open class ImportRepository @Inject constructor(
    private val importRecordDao: ImportRecordDao
) : BaseRepository() {
    
    /**
     * 获取所有导入记录
     */
    fun getAllImportRecords(): Flow<List<ImportRecord>> = 
        importRecordDao.getAllImportRecords()
    
    /**
     * 获取最近的导入记录
     */
    suspend fun getRecentImportRecords(limit: Int): List<ImportRecord> = ioCall {
        importRecordDao.getRecentImportRecords(limit)
    }
    
    /**
     * 根据ID获取导入记录
     */
    suspend fun getImportRecordById(id: Long): ImportRecord? = ioCall {
        importRecordDao.getImportRecordById(id)
    }
    
    /**
     * 根据批次ID获取导入记录
     */
    suspend fun getImportRecordsByBatchId(batchId: String): List<ImportRecord> = ioCall {
        importRecordDao.getImportRecordsByBatchId(batchId)
    }
    
    /**
     * 根据状态获取导入记录
     */
    fun getImportRecordsByStatus(status: ImportStatus): Flow<List<ImportRecord>> = 
        importRecordDao.getImportRecordsByStatus(status)
    
    /**
     * 获取成功的导入记录
     */
    fun getSuccessfulImports(): Flow<List<ImportRecord>> = 
        importRecordDao.getSuccessfulImports()
    
    /**
     * 获取失败的导入记录
     */
    fun getFailedImports(): Flow<List<ImportRecord>> = 
        importRecordDao.getFailedImports()
    
    /**
     * 获取处理中的导入记录
     */
    suspend fun getProcessingImports(): List<ImportRecord> = ioCall {
        importRecordDao.getProcessingImports()
    }
    
    /**
     * 根据时间范围获取导入记录
     */
    suspend fun getImportRecordsByTimeRange(
        startTime: Long, 
        endTime: Long
    ): List<ImportRecord> = ioCall {
        importRecordDao.getImportRecordsByTimeRange(startTime, endTime)
    }
    
    /**
     * 根据文件名搜索导入记录
     */
    fun searchImportRecordsByFileName(fileName: String): Flow<List<ImportRecord>> = 
        importRecordDao.searchImportRecords(fileName)
    
    /**
     * 获取导入统计信息
     */
    suspend fun getImportStatistics(): ImportStatistics = ioCall {
        importRecordDao.getImportStatistics() ?: ImportStatistics(
            totalImports = 0,
            successfulImports = 0,
            failedImports = 0,
            processingImports = 0,
            totalRecordsProcessed = 0,
            totalSuccessRecords = 0,
            totalFailedRecords = 0
        )
    }
    
    /**
     * 插入导入记录
     */
    suspend fun insertImportRecord(record: ImportRecord): Long = ioCall {
        importRecordDao.insertImportRecord(record)
    }
    
    /**
     * 批量插入导入记录
     */
    suspend fun insertImportRecords(records: List<ImportRecord>) = ioCall {
        importRecordDao.insertImportRecords(records)
    }
    
    /**
     * 更新导入记录
     */
    suspend fun updateImportRecord(record: ImportRecord) = ioCall {
        importRecordDao.updateImportRecord(record)
    }
    
    /**
     * 更新导入状态
     */
    suspend fun updateImportStatus(id: Long, status: ImportStatus) = ioCall {
        importRecordDao.updateImportStatus(id, status, System.currentTimeMillis(), null)
    }
    
    /**
     * 更新导入进度
     */
    suspend fun updateImportProgress(id: Long, successRecords: Int, failedRecords: Int) = ioCall {
        importRecordDao.updateImportProgress(id, successRecords, failedRecords)
    }
    
    /**
     * 根据批次ID更新导入记录
     */
    suspend fun updateImportRecordsByBatchId(
        batchId: String,
        status: ImportStatus,
        errorMessage: String? = null
    ) = ioCall {
        importRecordDao.updateImportStatusByBatchId(batchId, status, System.currentTimeMillis(), errorMessage)
    }
    
    /**
     * 删除导入记录
     */
    suspend fun deleteImportRecord(record: ImportRecord) = ioCall {
        importRecordDao.deleteImportRecord(record)
    }
    
    /**
     * 根据ID删除导入记录
     */
    suspend fun deleteImportRecordById(id: Long) = ioCall {
        importRecordDao.deleteImportRecordById(id)
    }
    
    /**
     * 根据批次ID删除导入记录
     */
    suspend fun deleteImportRecordsByBatchId(batchId: String) = ioCall {
        importRecordDao.deleteImportRecordsByBatchId(batchId)
    }
    
    /**
     * 根据时间删除导入记录
     */
    suspend fun deleteImportRecordsByTime(beforeTime: Long) = ioCall {
        // 暂时不实现，避免编译错误
    }
    
    /**
     * 根据状态删除导入记录
     */
    suspend fun deleteImportRecordsByStatus(status: ImportStatus) = ioCall {
        // 暂时不实现，避免编译错误
    }
    
    /**
     * 清理旧的导入记录
     */
    suspend fun cleanupOldImportRecords(daysToKeep: Int) = ioCall {
        importRecordDao.cleanupOldImportRecords(daysToKeep)
    }
    
    /**
     * 创建新的导入批次记录
     */
    suspend fun createImportBatch(
        fileName: String,
        fileSize: Long,
        importType: String
    ): String = ioCall {
        val batchId = "batch_${System.currentTimeMillis()}"
        val record = ImportRecord(
            fileName = fileName,
            importType = ImportSource.valueOf(importType),
            batchId = batchId,
            importStatus = ImportStatus.PROCESSING
        )
        insertImportRecord(record)
        batchId
    }
    
    /**
     * 完成导入批次
     */
    suspend fun completeImportBatch(
        batchId: String,
        totalRecords: Int,
        successCount: Int,
        errorMessage: String? = null
    ) = ioCall {
        val status = if (errorMessage == null) "SUCCESS" else "FAILED"
        val records = importRecordDao.getImportRecordsByBatchId(batchId)
        
        records.forEach { record ->
            val updatedRecord = record.copy(
                importStatus = if (status == "SUCCESS") ImportStatus.SUCCESS else ImportStatus.FAILED,
                completionTime = System.currentTimeMillis(),
                totalRecords = totalRecords,
                successRecords = successCount,
                errorMessage = errorMessage
            )
            importRecordDao.updateImportRecord(updatedRecord)
        }
    }
    
    /**
     * 获取正在进行的导入批次
     */
    suspend fun getActiveImportBatches(): List<String> = ioCall {
        importRecordDao.getProcessingImports().map { it.batchId }.distinct()
    }
    
    /**
     * 取消导入批次
     */
    suspend fun cancelImportBatch(batchId: String) = ioCall {
        updateImportRecordsByBatchId(batchId, ImportStatus.FAILED, "用户取消导入")
    }
    
    /**
     * 重试失败的导入
     */
    suspend fun retryFailedImport(id: Long) = ioCall {
        updateImportStatus(id, ImportStatus.PROCESSING)
        updateImportProgress(id, 0, 0)
    }
    
    /**
     * 获取导入历史摘要
     */
    suspend fun getImportHistorySummary(days: Int): ImportHistorySummary = ioCall {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (days * 24 * 60 * 60 * 1000L)
        
        val records = importRecordDao.getImportRecordsByTimeRange(startTime, endTime)
        
        ImportHistorySummary(
            totalImports = records.size,
            successfulImports = records.count { it.importStatus.name == "SUCCESS" },
            failedImports = records.count { it.importStatus.name == "FAILED" },
            totalRecordsImported = records.sumOf { it.successRecords },
            averageImportTime = if (records.isNotEmpty()) {
                records.filter { it.completionTime != null }
                    .map { (it.completionTime!! - it.importTime) }
                    .average().toLong()
            } else 0L
        )
    }
}

/**
 * 导入历史摘要数据类
 */
data class ImportHistorySummary(
    val totalImports: Int = 0,
    val successfulImports: Int = 0,
    val failedImports: Int = 0,
    val totalRecordsImported: Int = 0,
    val averageImportTime: Long = 0L
) {
    val successRate: Float
        get() = if (totalImports > 0) successfulImports.toFloat() / totalImports else 0f
    
    val failureRate: Float
        get() = if (totalImports > 0) failedImports.toFloat() / totalImports else 0f
}
