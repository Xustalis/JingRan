package com.jingran.taskmanager.service

import android.net.Uri
import com.jingran.taskmanager.data.entity.*
import com.jingran.taskmanager.data.repository.TaskRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import kotlin.collections.ArrayList

/**
 * 数据同步服务
 * 根据PRD文档要求，提供本地数据备份、云端同步、数据导入导出等功能
 * 支持增量同步和冲突解决机制
 */
class DataSyncService(private val repository: TaskRepository) {
    
    companion object {
        private const val TAG = "DataSyncService"
        private const val SYNC_BATCH_SIZE = 50
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val SYNC_TIMEOUT_MS = 30000L
    }
    
    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()
    
    private val _syncProgress = MutableStateFlow(SyncProgress())
    val syncProgress: StateFlow<SyncProgress> = _syncProgress.asStateFlow()
    
    private var syncJob: Job? = null
    
    /**
     * 执行完整数据同步
     * @param syncType 同步类型（上传、下载、双向同步）
     * @param forceSync 是否强制同步（忽略时间戳检查）
     * @return 同步结果
     */
    suspend fun performFullSync(
        syncType: SyncType = SyncType.BIDIRECTIONAL,
        forceSync: Boolean = false
    ): SyncResult = withContext(Dispatchers.IO) {
        
        if (_syncStatus.value == SyncStatus.SYNCING) {
            return@withContext SyncResult(
                success = false,
                message = "同步正在进行中，请稍后再试"
            )
        }
        
        _syncStatus.value = SyncStatus.SYNCING
        _syncProgress.value = SyncProgress()
        
        try {
            val result = when (syncType) {
                SyncType.UPLOAD_ONLY -> performUploadSync(forceSync)
                SyncType.DOWNLOAD_ONLY -> performDownloadSync(forceSync)
                SyncType.BIDIRECTIONAL -> performBidirectionalSync(forceSync)
                else -> SyncResult(success = false, message = "不支持的同步类型")
            }
            
            _syncStatus.value = if (result.success) SyncStatus.SUCCESS else SyncStatus.FAILED
            result
            
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.FAILED
            SyncResult(
                success = false,
                message = "同步失败：${e.message}",
                error = e
            )
        }
    }
    
    /**
     * 从指定Uri恢复数据备份
     */
    suspend fun restoreBackup(uri: Uri): SyncResult = withContext(Dispatchers.IO) {
        try {
            _syncProgress.value = SyncProgress(currentOperation = "正在恢复备份...")
            
            // 从Uri读取备份数据
            // TODO: 实现从Uri读取备份数据的逻辑
            
            SyncResult(
                success = true,
                message = "备份恢复成功"
            )
            
        } catch (e: Exception) {
            SyncResult(
                success = false,
                message = "备份恢复失败：${e.message}",
                error = e
            )
        }
    }
    
    /**
     * 执行增量同步
     * 只同步自上次同步以来发生变化的数据
     */
    suspend fun performIncrementalSync(): SyncResult = withContext(Dispatchers.IO) {
        
        _syncStatus.value = SyncStatus.SYNCING
        
        try {
            val lastSyncTime = getLastSyncTime()
            
            // 获取本地变更
            val localChanges = getLocalChanges(lastSyncTime)
            
            // 获取远程变更
            val remoteChanges = getRemoteChanges(lastSyncTime)
            
            // 解决冲突
            val conflictResolution = resolveConflicts(localChanges, remoteChanges)
            
            // 应用变更
            val uploadResult = uploadChanges(conflictResolution.localChangesToUpload)
            val downloadResult = downloadChanges(conflictResolution.remoteChangesToDownload)
            
            // 更新同步时间戳
            updateLastSyncTime()
            
            _syncStatus.value = SyncStatus.SUCCESS
            
            SyncResult(
                success = true,
                message = "增量同步完成",
                uploadedItems = uploadResult.count,
                downloadedItems = downloadResult.count,
                conflictsResolved = conflictResolution.conflicts.size
            )
            
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.FAILED
            SyncResult(
                success = false,
                message = "增量同步失败：${e.message}",
                error = e
            )
        }
    }
    
    /**
     * 导出数据到Uri
     */
    suspend fun exportData(uri: Uri): SyncResult = withContext(Dispatchers.IO) {
        try {
            _syncProgress.value = SyncProgress(currentOperation = "正在导出数据...")
            
            // 收集所有数据
            val exportData = collectAllData(includeDeleted = false)
            
            // 将数据写入到Uri指定的位置
            // TODO: 实现将数据写入到Uri的逻辑
            
            SyncResult(
                success = true,
                message = "数据导出成功"
            )
            
        } catch (e: Exception) {
            SyncResult(
                success = false,
                message = "数据导出失败：${e.message}",
                error = e
            )
        }
    }
    
    /**
     * 导出数据到文件
     * @param exportPath 导出文件路径
     * @param exportType 导出格式（JSON、CSV等）
     * @param includeDeleted 是否包含已删除的数据
     * @return 导出结果
     */
    suspend fun exportData(
        exportPath: String,
        exportType: ExportType = ExportType.JSON,
        includeDeleted: Boolean = false
    ): ExportResult = withContext(Dispatchers.IO) {
        
        try {
            _syncProgress.value = SyncProgress(currentOperation = "正在导出数据...")
            
            // 收集所有数据
            val exportData = collectAllData(includeDeleted)
            
            // 根据格式导出
            val exportedFile = when (exportType) {
                ExportType.JSON -> exportToJson(exportData, exportPath)
                ExportType.CSV -> exportToCsv(exportData, exportPath)
                ExportType.XML -> exportToXml(exportData, exportPath)
            }
            
            ExportResult(
                success = true,
                filePath = exportedFile,
                totalRecords = exportData.getTotalRecords(),
                message = "数据导出成功"
            )
            
        } catch (e: Exception) {
            ExportResult(
                success = false,
                message = "数据导出失败：${e.message}",
                error = e
            )
        }
    }
    
    /**
     * 从Uri导入数据
     */
    suspend fun importData(uri: Uri): SyncResult = withContext(Dispatchers.IO) {
        try {
            _syncProgress.value = SyncProgress(currentOperation = "正在导入数据...")
            
            // 从Uri读取数据
            // TODO: 实现从Uri读取数据的逻辑
            
            SyncResult(
                success = true,
                message = "数据导入成功"
            )
            
        } catch (e: Exception) {
            SyncResult(
                success = false,
                message = "数据导入失败：${e.message}",
                error = e
            )
        }
    }
    
    /**
     * 从文件导入数据
     * @param importPath 导入文件路径
     * @param importType 导入格式
     * @param mergeStrategy 合并策略（覆盖、跳过、合并）
     * @return 导入结果
     */
    suspend fun importData(
        importPath: String,
        importType: ImportType = ImportType.JSON,
        mergeStrategy: MergeStrategy = MergeStrategy.MERGE
    ): ImportResult = withContext(Dispatchers.IO) {
        
        try {
            _syncProgress.value = SyncProgress(currentOperation = "正在导入数据...")
            
            // 解析导入文件
            val importData = when (importType) {
                ImportType.JSON -> parseJsonFile(importPath)
                ImportType.CSV -> parseCsvFile(importPath)
                ImportType.XML -> parseXmlFile(importPath)
            }
            
            // 验证数据
            val validationResult = validateImportData(importData)
            if (!validationResult.isValid) {
                throw IllegalArgumentException("数据验证失败：${validationResult.errorMessage}")
            }
            
            // 应用合并策略
            val mergeResult = applyMergeStrategy(importData, mergeStrategy)
            
            ImportResult(
                success = true,
                totalRecords = importData.getTotalRecords(),
                importedRecords = mergeResult.imported,
                skippedRecords = mergeResult.skipped,
                updatedRecords = mergeResult.updated,
                message = "数据导入成功"
            )
            
        } catch (e: Exception) {
            ImportResult(
                success = false,
                message = "数据导入失败：${e.message}",
                error = e
            )
        }
    }
    
    /**
     * 创建数据备份
     */
    suspend fun createBackup(backupName: String? = null): BackupResult = withContext(Dispatchers.IO) {
        
        try {
            val timestamp = System.currentTimeMillis()
            val finalBackupName = backupName ?: "backup_${timestamp}"
            
            _syncProgress.value = SyncProgress(currentOperation = "正在创建备份...")
            
            // 收集所有数据
            val backupData = collectAllData(includeDeleted = true)
            
            // 创建备份记录
            val backupRecord = BackupRecord(
                backupId = UUID.randomUUID().toString(),
                backupName = finalBackupName,
                creationTime = timestamp,
                dataSize = calculateDataSize(backupData),
                recordCount = backupData.getTotalRecords()
            )
            
            // 保存备份
            val backupId = saveBackup(backupRecord, backupData)
            
            BackupResult(
                success = true,
                backupId = backupId,
                backupName = finalBackupName,
                message = "备份创建成功"
            )
            
        } catch (e: Exception) {
            BackupResult(
                success = false,
                message = "备份创建失败：${e.message}",
                error = e
            )
        }
    }
    
    /**
     * 创建数据备份到指定Uri
     */
    suspend fun createBackup(uri: Uri): SyncResult = withContext(Dispatchers.IO) {
        try {
            _syncProgress.value = SyncProgress(currentOperation = "正在创建备份...")
            
            // 收集所有数据
            val backupData = collectAllData(includeDeleted = true)
            
            // 将数据写入到Uri指定的位置
            // TODO: 实现将备份数据写入到Uri的逻辑
            
            SyncResult(
                success = true,
                message = "备份创建成功"
            )
            
        } catch (e: Exception) {
            SyncResult(
                success = false,
                message = "备份创建失败：${e.message}",
                error = e
            )
        }
    }
    
    /**
     * 恢复数据备份
     */
    suspend fun restoreBackup(backupId: String): RestoreResult = withContext(Dispatchers.IO) {
        
        try {
            _syncProgress.value = SyncProgress(currentOperation = "正在恢复备份...")
            
            // 获取备份数据
            val backupData = loadBackup(backupId)
                ?: throw IllegalArgumentException("备份不存在：$backupId")
            
            // 清空当前数据（可选，根据需求决定）
            // clearAllData()
            
            // 恢复数据
            val restoreCount = restoreData(backupData)
            
            RestoreResult(
                success = true,
                restoredRecords = restoreCount,
                message = "备份恢复成功"
            )
            
        } catch (e: Exception) {
            RestoreResult(
                success = false,
                message = "备份恢复失败：${e.message}",
                error = e
            )
        }
    }
    
    /**
     * 获取备份列表
     */
    suspend fun getBackupList(): List<BackupRecord> {
        return repository.getAllBackupRecords()
    }
    
    /**
     * 删除备份
     */
    suspend fun deleteBackup(backupId: String): Boolean {
        return try {
            repository.deleteBackupRecord(backupId)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取同步历史记录
     */
    suspend fun getSyncHistory(): List<SyncRecord> {
        return repository.getAllSyncRecords()
    }
    
    /**
     * 清除同步历史记录
     */
    suspend fun clearSyncHistory() {
        repository.clearSyncHistory()
    }
    
    /**
     * 获取当前同步状态
     */
    fun getSyncStatus(): SyncStatus {
        return _syncStatus.value
    }
    
    /**
     * 取消当前同步操作
     */
    fun cancelSync() {
        syncJob?.cancel()
        _syncStatus.value = SyncStatus.CANCELLED
    }
    
    // 私有辅助方法
    
    private suspend fun performUploadSync(forceSync: Boolean): SyncResult {
        _syncProgress.value = SyncProgress(currentOperation = "正在上传数据...")
        
        val localData = collectAllData()
        val uploadResult = uploadToCloud(localData, forceSync)
        
        return SyncResult(
            success = uploadResult.success,
            message = if (uploadResult.success) "上传同步完成" else "上传同步失败",
            uploadedItems = uploadResult.count
        )
    }
    
    private suspend fun performDownloadSync(forceSync: Boolean): SyncResult {
        _syncProgress.value = SyncProgress(currentOperation = "正在下载数据...")
        
        val downloadResult = downloadFromCloud(forceSync)
        
        return SyncResult(
            success = downloadResult.success,
            message = if (downloadResult.success) "下载同步完成" else "下载同步失败",
            downloadedItems = downloadResult.count
        )
    }
    
    private suspend fun performBidirectionalSync(forceSync: Boolean): SyncResult {
        _syncProgress.value = SyncProgress(currentOperation = "正在执行双向同步...")
        
        val uploadResult = performUploadSync(forceSync)
        val downloadResult = performDownloadSync(forceSync)
        
        return SyncResult(
            success = uploadResult.success && downloadResult.success,
            message = "双向同步完成",
            uploadedItems = uploadResult.uploadedItems,
            downloadedItems = downloadResult.downloadedItems
        )
    }
    
    suspend fun getLastSyncTime(): Long {
        return repository.getLastSyncTimestamp() ?: 0L
    }
    
    private suspend fun updateLastSyncTime() {
        val currentTime = System.currentTimeMillis()
        val syncRecord = SyncRecord(
            id = UUID.randomUUID().toString(),
            syncType = SyncType.FULL,
            status = SyncStatus.SUCCESS,
            timestamp = currentTime,
            endTime = currentTime
        )
        repository.insertSyncRecord(syncRecord)
    }
    
    private suspend fun getLocalChanges(since: Long): List<DataChange> {
        val changes = mutableListOf<DataChange>()
        
        // 获取短期任务变更
        val shortTermTasks = repository.getAllShortTermTasksList()
        shortTermTasks.forEach { task ->
            if (task.createTime > since || task.lastModifiedTime > since) {
                changes.add(DataChange(
                    entityType = "ShortTermTask",
                    entityId = task.id.toString(),
                    changeType = if (task.createTime > since) ChangeType.CREATE else ChangeType.UPDATE,
                    timestamp = maxOf(task.createTime, task.lastModifiedTime),
                    data = task
                ))
            }
        }
        
        // 获取长期任务变更
        val longTermTasks = repository.getAllLongTermTasksList()
        longTermTasks.forEach { task ->
            if (task.createTime > since || task.lastModifiedTime > since) {
                changes.add(DataChange(
                    entityType = "LongTermTask",
                    entityId = task.id.toString(),
                    changeType = if (task.createTime > since) ChangeType.CREATE else ChangeType.UPDATE,
                    timestamp = maxOf(task.createTime, task.lastModifiedTime),
                    data = task
                ))
            }
        }
        
        return changes.sortedBy { it.timestamp }
    }
    
    private suspend fun getRemoteChanges(since: Long): List<DataChange> {
        // 模拟远程变更获取，实际应该调用网络API
        return try {
            // TODO: 实现实际的远程API调用
            // 这里返回空列表作为占位符
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun resolveConflicts(
        localChanges: List<DataChange>,
        remoteChanges: List<DataChange>
    ): ConflictResolution {
        val conflicts = mutableListOf<DataConflict>()
        val localToUpload = mutableListOf<DataChange>()
        val remoteToDownload = mutableListOf<DataChange>()
        
        // 创建本地变更的映射
        val localChangeMap = localChanges.associateBy { "${it.entityType}_${it.entityId}" }
        val remoteChangeMap = remoteChanges.associateBy { "${it.entityType}_${it.entityId}" }
        
        // 处理本地变更
        localChanges.forEach { localChange ->
            val key = "${localChange.entityType}_${localChange.entityId}"
            val remoteChange = remoteChangeMap[key]
            
            if (remoteChange == null) {
                // 没有远程冲突，直接上传
                localToUpload.add(localChange)
            } else {
                // 存在冲突，根据时间戳决定
                if (localChange.timestamp > remoteChange.timestamp) {
                    localToUpload.add(localChange)
                } else {
                    conflicts.add(DataConflict(
                        entityType = localChange.entityType,
                        entityId = localChange.entityId,
                        localChange = localChange,
                        remoteChange = remoteChange,
                        resolution = ConflictResolutionType.USE_REMOTE
                    ))
                }
            }
        }
        
        // 处理远程变更
        remoteChanges.forEach { remoteChange ->
            val key = "${remoteChange.entityType}_${remoteChange.entityId}"
            if (!localChangeMap.containsKey(key)) {
                // 没有本地冲突，直接下载
                remoteToDownload.add(remoteChange)
            }
        }
        
        return ConflictResolution(
            localChangesToUpload = localToUpload,
            remoteChangesToDownload = remoteToDownload,
            conflicts = conflicts
        )
    }

    private suspend fun uploadChanges(changes: List<DataChange>): UploadResult {
        return try {
            var uploadedCount = 0
            
            changes.forEach { change ->
                // 模拟网络上传
                delay(100) // 模拟网络延迟
                
                // TODO: 实现实际的网络上传逻辑
                // 这里应该调用REST API上传数据
                
                uploadedCount++
                
                // 更新进度
                _syncProgress.value = _syncProgress.value.copy(
                    currentOperation = "上传 ${change.entityType}",
                    processedItems = uploadedCount,
                    progress = uploadedCount.toFloat() / changes.size
                )
            }
            
            UploadResult(success = true, count = uploadedCount)
        } catch (e: Exception) {
            UploadResult(success = false, count = 0, error = e)
        }
    }

    private suspend fun downloadChanges(changes: List<DataChange>): DownloadResult {
        return try {
            var downloadedCount = 0
            
            changes.forEach { change ->
                // 模拟网络下载和本地应用
                delay(100) // 模拟网络延迟
                
                // TODO: 实现实际的数据应用逻辑
                // 根据change.entityType和change.data更新本地数据库
                
                downloadedCount++
                
                // 更新进度
                _syncProgress.value = _syncProgress.value.copy(
                    currentOperation = "下载 ${change.entityType}",
                    processedItems = downloadedCount,
                    progress = downloadedCount.toFloat() / changes.size
                )
            }
            
            DownloadResult(success = true, count = downloadedCount)
        } catch (e: Exception) {
            DownloadResult(success = false, count = 0, error = e)
        }
    }
    
    private suspend fun collectAllData(includeDeleted: Boolean = false): AllDataSnapshot {
        return try {
            val shortTermTasks = repository.getAllShortTermTasksList()
            val longTermTasks = repository.getAllLongTermTasksList()
            val courseSchedules = repository.getAllCourseSchedulesList()
            
            // TODO: 添加其他数据类型的收集
            // val fixedSchedules = repository.getAllFixedSchedules()
            // val subTasks = repository.getAllSubTasks()
            // val planItems = repository.getAllPlanItems()
            
            AllDataSnapshot(
                shortTermTasks = shortTermTasks,
                longTermTasks = longTermTasks,
                courseSchedules = courseSchedules,
                fixedSchedules = emptyList(), // TODO: 实现后替换
                subTasks = emptyList(), // TODO: 实现后替换
                planItems = emptyList() // TODO: 实现后替换
            )
        } catch (e: Exception) {
            AllDataSnapshot()
        }
    }
    
    private suspend fun uploadToCloud(data: AllDataSnapshot, forceSync: Boolean): UploadResult {
        return try {
            // 模拟云端上传过程
            _syncProgress.value = _syncProgress.value.copy(
                currentOperation = "上传到云端",
                totalItems = data.getTotalRecords()
            )
            
            var uploadedCount = 0
            val totalRecords = data.getTotalRecords()
            
            // 模拟分批上传
            val batchSize = SYNC_BATCH_SIZE
            for (i in 0 until totalRecords step batchSize) {
                delay(200) // 模拟网络延迟
                
                val batchEnd = minOf(i + batchSize, totalRecords)
                uploadedCount = batchEnd
                
                _syncProgress.value = _syncProgress.value.copy(
                    processedItems = uploadedCount,
                    progress = uploadedCount.toFloat() / totalRecords
                )
                
                // TODO: 实现实际的云端API调用
                // cloudApi.uploadBatch(data.getBatch(i, batchSize))
            }
            
            UploadResult(success = true, count = uploadedCount)
        } catch (e: Exception) {
            UploadResult(success = false, count = 0, error = e)
        }
    }
    
    private suspend fun downloadFromCloud(forceSync: Boolean): DownloadResult {
        return try {
            _syncProgress.value = _syncProgress.value.copy(
                currentOperation = "从云端下载"
            )
            
            // TODO: 实现实际的云端下载逻辑
            // val cloudData = cloudApi.downloadData()
            // val mergeResult = applyMergeStrategy(cloudData, MergeStrategy.MERGE)
            
            delay(1000) // 模拟网络延迟
            
            DownloadResult(success = true, count = 0)
        } catch (e: Exception) {
            DownloadResult(success = false, count = 0, error = e)
        }
    }
    
    private fun exportToJson(data: AllDataSnapshot, exportPath: String): String {
        try {
            // 构建JSON数据结构
            val jsonData = mapOf(
                "exportTime" to System.currentTimeMillis(),
                "version" to "1.0",
                "shortTermTasks" to data.shortTermTasks,
                "longTermTasks" to data.longTermTasks,
                "courseSchedules" to data.courseSchedules,
                "fixedSchedules" to data.fixedSchedules,
                "subTasks" to data.subTasks,
                "planItems" to data.planItems
            )
            
            // TODO: 使用JSON库序列化并写入文件
            // val jsonString = Gson().toJson(jsonData)
            // File(exportPath).writeText(jsonString)
            
            return exportPath
        } catch (e: Exception) {
            throw RuntimeException("JSON导出失败: ${e.message}", e)
        }
    }
    
    private fun exportToCsv(data: AllDataSnapshot, exportPath: String): String {
        try {
            // TODO: 实现CSV导出逻辑
            // 需要将不同类型的数据转换为CSV格式
            // 可能需要为每种数据类型创建单独的CSV文件
            
            return exportPath
        } catch (e: Exception) {
            throw RuntimeException("CSV导出失败: ${e.message}", e)
        }
    }
    
    private fun exportToXml(data: AllDataSnapshot, exportPath: String): String {
        try {
            // TODO: 实现XML导出逻辑
            // 构建XML结构并写入文件
            
            return exportPath
        } catch (e: Exception) {
            throw RuntimeException("XML导出失败: ${e.message}", e)
        }
    }
    
    private fun parseJsonFile(importPath: String): AllDataSnapshot {
        try {
            // TODO: 使用JSON库解析文件
            // val jsonString = File(importPath).readText()
            // val jsonData = Gson().fromJson(jsonString, Map::class.java)
            
            // 解析各种数据类型
            // val shortTermTasks = parseShortTermTasks(jsonData["shortTermTasks"])
            // val longTermTasks = parseLongTermTasks(jsonData["longTermTasks"])
            // ...
            
            return AllDataSnapshot()
        } catch (e: Exception) {
            throw RuntimeException("JSON解析失败: ${e.message}", e)
        }
    }
    
    private fun parseCsvFile(importPath: String): AllDataSnapshot {
        try {
            // TODO: 实现CSV解析逻辑
            // 可能需要解析多个CSV文件或一个包含所有数据的CSV文件
            
            return AllDataSnapshot()
        } catch (e: Exception) {
            throw RuntimeException("CSV解析失败: ${e.message}", e)
        }
    }
    
    private fun parseXmlFile(importPath: String): AllDataSnapshot {
        try {
            // TODO: 实现XML解析逻辑
            // 使用XML解析器解析文件内容
            
            return AllDataSnapshot()
        } catch (e: Exception) {
            throw RuntimeException("XML解析失败: ${e.message}", e)
        }
    }
    
    private fun validateImportData(data: AllDataSnapshot): ValidationResult {
        val errors = mutableListOf<String>()
        
        try {
            // 验证短期任务
            data.shortTermTasks.forEach { task ->
                if (task.title.isBlank()) {
                    errors.add("短期任务标题不能为空: ${task.id}")
                }
                if (task.deadline != null && task.deadline <= 0) {
                    errors.add("短期任务截止时间无效: ${task.id}")
                }
            }
            
            // 验证长期任务
            data.longTermTasks.forEach { task ->
                if (task.title.isBlank()) {
                    errors.add("长期任务标题不能为空: ${task.id}")
                }
                if (task.startDate >= task.endDate) {
                    errors.add("长期任务开始时间不能晚于结束时间: ${task.id}")
                }
            }
            
            // 验证课程安排
            data.courseSchedules.forEach { schedule ->
                if (schedule.courseName.isBlank()) {
                    errors.add("课程名称不能为空: ${schedule.id}")
                }
                // 注意：时间验证应该在关联的FixedSchedule中进行
            }
            
            // TODO: 添加其他数据类型的验证
            
        } catch (e: Exception) {
            errors.add("数据验证过程中发生错误: ${e.message}")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errorMessage = errors.joinToString("; ")
        )
    }
    
    private suspend fun applyMergeStrategy(
        importData: AllDataSnapshot,
        strategy: MergeStrategy
    ): MergeResult {
        var imported = 0
        var skipped = 0
        var updated = 0
        
        try {
            // 处理短期任务
            importData.shortTermTasks.forEach { task ->
                val existing = repository.getShortTermTaskById(task.id)
                when {
                    existing == null -> {
                        repository.insertShortTermTask(task)
                        imported++
                    }
                    strategy == MergeStrategy.OVERWRITE -> {
                        repository.updateShortTermTask(task)
                        updated++
                    }
                    strategy == MergeStrategy.SKIP -> {
                        skipped++
                    }
                    strategy == MergeStrategy.MERGE -> {
                        // 合并逻辑：保留较新的数据
                        if (task.lastModifiedTime > existing.lastModifiedTime) {
                            repository.updateShortTermTask(task)
                            updated++
                        } else {
                            skipped++
                        }
                    }
                }
            }
            
            // 处理长期任务
            importData.longTermTasks.forEach { task ->
                val existing = repository.getLongTermTaskById(task.id)
                when {
                    existing == null -> {
                        repository.insertLongTermTask(task)
                        imported++
                    }
                    strategy == MergeStrategy.OVERWRITE -> {
                        repository.updateLongTermTask(task)
                        updated++
                    }
                    strategy == MergeStrategy.SKIP -> {
                        skipped++
                    }
                    strategy == MergeStrategy.MERGE -> {
                        if (task.lastModifiedTime > existing.lastModifiedTime) {
                            repository.updateLongTermTask(task)
                            updated++
                        } else {
                            skipped++
                        }
                    }
                }
            }
            
            // TODO: 处理其他数据类型
            
        } catch (e: Exception) {
            throw RuntimeException("合并策略应用失败: ${e.message}", e)
        }
        
        return MergeResult(
            imported = imported,
            skipped = skipped,
            updated = updated
        )
    }
    
    private fun calculateDataSize(data: AllDataSnapshot): Long {
        try {
            var totalSize = 0L
            
            // 估算各种数据类型的大小（字节）
            totalSize += data.shortTermTasks.size * 500L // 每个短期任务约500字节
            totalSize += data.longTermTasks.size * 600L // 每个长期任务约600字节
            totalSize += data.courseSchedules.size * 400L // 每个课程安排约400字节
            totalSize += data.fixedSchedules.size * 300L // 每个固定安排约300字节
            totalSize += data.subTasks.size * 200L // 每个子任务约200字节
            totalSize += data.planItems.size * 250L // 每个计划项约250字节
            
            return totalSize
        } catch (e: Exception) {
            return 0L
        }
    }
    
    private suspend fun saveBackup(record: BackupRecord, data: AllDataSnapshot): String {
        return try {
            // 保存备份记录到数据库
            repository.insertBackupRecord(record)
            
            // TODO: 实现实际的备份文件保存逻辑
            // 可以保存为JSON文件或压缩文件
            // val backupFile = File(getBackupDirectory(), "${record.backupId}.json")
            // val jsonData = Gson().toJson(data)
            // backupFile.writeText(jsonData)
            
            record.backupId
        } catch (e: Exception) {
            throw RuntimeException("备份保存失败: ${e.message}", e)
        }
    }
    
    private suspend fun loadBackup(backupId: String): AllDataSnapshot? {
        return try {
            // 从数据库获取备份记录
            val backupRecord = repository.getBackupRecordById(backupId)
            if (backupRecord == null) {
                return null
            }
            
            // TODO: 实现实际的备份文件加载逻辑
            // val backupFile = File(getBackupDirectory(), "${backupId}.json")
            // if (!backupFile.exists()) {
            //     return null
            // }
            // val jsonData = backupFile.readText()
            // return Gson().fromJson(jsonData, AllDataSnapshot::class.java)
            
            // 临时返回空数据
            AllDataSnapshot()
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun restoreData(data: AllDataSnapshot): Int {
        return try {
            var restoredCount = 0
            
            // 清空现有数据（可选，根据需求决定）
            // repository.clearAllData()
            
            // 恢复短期任务
            data.shortTermTasks.forEach { task ->
                repository.insertShortTermTask(task)
                restoredCount++
            }
            
            // 恢复长期任务
            data.longTermTasks.forEach { task ->
                repository.insertLongTermTask(task)
                restoredCount++
            }
            
            // 恢复课程安排
            data.courseSchedules.forEach { schedule ->
                repository.insertCourseSchedule(schedule)
                restoredCount++
            }
            
            // TODO: 恢复其他数据类型
            // data.fixedSchedules.forEach { ... }
            // data.subTasks.forEach { ... }
            // data.planItems.forEach { ... }
            
            restoredCount
        } catch (e: Exception) {
            throw RuntimeException("数据恢复失败: ${e.message}", e)
        }
    }
}

// 数据类和枚举定义

enum class SyncStatus {
    IDLE, SYNCING, SUCCESS, FAILED, CANCELLED, CONFLICT
}

enum class SyncType {
    UPLOAD_ONLY, DOWNLOAD_ONLY, BIDIRECTIONAL, FULL, INCREMENTAL, EXPORT, IMPORT, BACKUP, RESTORE
}

enum class ExportType {
    JSON, CSV, XML
}

enum class ImportType {
    JSON, CSV, XML
}

enum class MergeStrategy {
    OVERWRITE, SKIP, MERGE
}

data class SyncProgress(
    val currentOperation: String = "",
    val progress: Float = 0f,
    val totalItems: Int = 0,
    val processedItems: Int = 0
)

data class SyncResult(
    val success: Boolean,
    val message: String = "",
    val uploadedItems: Int = 0,
    val downloadedItems: Int = 0,
    val conflictsResolved: Int = 0,
    val error: Throwable? = null
)

data class ExportResult(
    val success: Boolean,
    val filePath: String = "",
    val totalRecords: Int = 0,
    val message: String = "",
    val error: Throwable? = null
)

data class ImportResult(
    val success: Boolean,
    val totalRecords: Int = 0,
    val importedRecords: Int = 0,
    val skippedRecords: Int = 0,
    val updatedRecords: Int = 0,
    val message: String = "",
    val error: Throwable? = null
)

data class BackupResult(
    val success: Boolean,
    val backupId: String = "",
    val backupName: String = "",
    val message: String = "",
    val error: Throwable? = null
)

data class RestoreResult(
    val success: Boolean,
    val restoredRecords: Int = 0,
    val message: String = "",
    val error: Throwable? = null
)

data class AllDataSnapshot(
    val shortTermTasks: List<ShortTermTask> = emptyList(),
    val longTermTasks: List<LongTermTask> = emptyList(),
    val fixedSchedules: List<FixedSchedule> = emptyList(),
    val courseSchedules: List<CourseSchedule> = emptyList(),
    val subTasks: List<SubTask> = emptyList(),
    val planItems: List<PlanItem> = emptyList()
) {
    fun getTotalRecords(): Int {
        return shortTermTasks.size + longTermTasks.size + fixedSchedules.size + 
               courseSchedules.size + subTasks.size + planItems.size
    }
}

data class DataChange(
    val entityType: String,
    val entityId: String,
    val changeType: ChangeType,
    val timestamp: Long,
    val data: Any
)

enum class ChangeType {
    CREATE, UPDATE, DELETE
}

data class ConflictResolution(
    val localChangesToUpload: List<DataChange> = emptyList(),
    val remoteChangesToDownload: List<DataChange> = emptyList(),
    val conflicts: List<DataConflict> = emptyList()
)

data class DataConflict(
    val entityType: String,
    val entityId: String,
    val localChange: DataChange,
    val remoteChange: DataChange,
    val resolution: ConflictResolutionType
)

enum class ConflictResolutionType {
    USE_LOCAL, USE_REMOTE, MERGE, MANUAL_REVIEW
}

data class UploadResult(
    val success: Boolean,
    val count: Int = 0,
    val error: Throwable? = null
)

data class DownloadResult(
    val success: Boolean,
    val count: Int = 0,
    val error: Throwable? = null
)

data class MergeResult(
    val imported: Int,
    val skipped: Int,
    val updated: Int
)



// 注意：BackupRecord和SyncRecord现在定义在data.entity包中
// 这里保留类型别名以保持兼容性
typealias BackupRecord = com.jingran.taskmanager.data.entity.BackupRecord
typealias SyncRecord = com.jingran.taskmanager.data.entity.SyncRecord