package com.jingran.taskmanager.service

import com.jingran.taskmanager.data.repository.TaskRepository
import com.jingran.taskmanager.data.repository.DataConsistencyReport
import com.jingran.taskmanager.data.repository.DataFixResult
import com.jingran.utils.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DataConsistencyService constructor(
    private val taskRepository: TaskRepository
) {
    
    private val tag = "DataConsistencyService"
    
    /**
     * 执行完整的数据一致性检查
     */
    suspend fun performConsistencyCheck(): DataConsistencyReport = withContext(Dispatchers.IO) {
        LogManager.d(tag, "开始执行数据一致性检查")
        
        try {
            val report = taskRepository.checkDataConsistency()
            
            if (report.isConsistent) {
                LogManager.i(tag, "数据一致性检查通过，未发现问题")
            } else {
                LogManager.w(tag, "数据一致性检查发现${report.issues.size}个问题: ${report.issues.joinToString("; ")}")
            }
            
            report
        } catch (e: Exception) {
            LogManager.e(tag, "数据一致性检查失败", e)
            DataConsistencyReport(
                isConsistent = false,
                issues = listOf("检查过程中发生错误: ${e.message}"),
                checkedAt = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * 自动修复数据一致性问题
     */
    suspend fun autoFixConsistencyIssues(): DataFixResult = withContext(Dispatchers.IO) {
        LogManager.d(tag, "开始自动修复数据一致性问题")
        
        try {
            // 先检查问题
            val report = taskRepository.checkDataConsistency()
            
            if (report.isConsistent) {
                LogManager.i(tag, "数据一致性良好，无需修复")
                return@withContext DataFixResult(
                    success = true,
                    fixedCount = 0,
                    errors = emptyList()
                )
            }
            
            // 执行修复
            val fixResult = taskRepository.fixDataConsistencyIssues()
            
            if (fixResult.success) {
                LogManager.i(tag, "数据一致性修复成功，修复了${fixResult.fixedCount}个问题")
            } else {
                LogManager.w(tag, "数据一致性修复部分失败，修复了${fixResult.fixedCount}个问题，错误: ${fixResult.errors.joinToString("; ")}")
            }
            
            fixResult
        } catch (e: Exception) {
            LogManager.e(tag, "数据一致性修复失败", e)
            DataFixResult(
                success = false,
                fixedCount = 0,
                errors = listOf("修复过程中发生错误: ${e.message}")
            )
        }
    }
    
    /**
     * 定期数据一致性检查
     * 建议在应用启动时或定期执行
     */
    suspend fun scheduleConsistencyCheck(): Boolean = withContext(Dispatchers.IO) {
        try {
            LogManager.d(tag, "执行定期数据一致性检查")
            
            val report = performConsistencyCheck()
            
            if (!report.isConsistent) {
                LogManager.w(tag, "定期检查发现数据一致性问题，尝试自动修复")
                
                val fixResult = autoFixConsistencyIssues()
                
                if (fixResult.success) {
                    LogManager.i(tag, "定期检查：数据一致性问题已自动修复")
                    true
                } else {
                    LogManager.e(tag, "定期检查：数据一致性问题修复失败")
                    false
                }
            } else {
                LogManager.d(tag, "定期检查：数据一致性良好")
                true
            }
        } catch (e: Exception) {
            LogManager.e(tag, "定期数据一致性检查失败", e)
            false
        }
    }
    
    /**
     * 获取数据一致性状态摘要
     */
    suspend fun getConsistencyStatus(): ConsistencyStatus = withContext(Dispatchers.IO) {
        try {
            val report = taskRepository.checkDataConsistency()
            
            ConsistencyStatus(
                isHealthy = report.isConsistent,
                issueCount = report.issues.size,
                lastChecked = report.checkedAt,
                summary = if (report.isConsistent) {
                    "数据一致性良好"
                } else {
                    "发现${report.issues.size}个数据一致性问题"
                }
            )
        } catch (e: Exception) {
            LogManager.e(tag, "获取数据一致性状态失败", e)
            ConsistencyStatus(
                isHealthy = false,
                issueCount = -1,
                lastChecked = System.currentTimeMillis(),
                summary = "状态检查失败: ${e.message}"
            )
        }
    }
}

/**
 * 数据一致性状态
 */
data class ConsistencyStatus(
    val isHealthy: Boolean,
    val issueCount: Int,
    val lastChecked: Long,
    val summary: String
)