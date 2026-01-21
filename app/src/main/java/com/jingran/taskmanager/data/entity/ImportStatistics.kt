package com.jingran.taskmanager.data.entity

/**
 * 导入统计信息数据类
 */
data class ImportStatistics(
    val totalImports: Int,
    val successfulImports: Int,
    val failedImports: Int,
    val processingImports: Int,
    val totalRecordsProcessed: Int,
    val totalSuccessRecords: Int,
    val totalFailedRecords: Int
) {
    val successRate: Float
        get() = if (totalImports > 0) successfulImports.toFloat() / totalImports else 0f
    
    val recordSuccessRate: Float
        get() = if (totalRecordsProcessed > 0) totalSuccessRecords.toFloat() / totalRecordsProcessed else 0f
}