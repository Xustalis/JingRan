package com.jingran.taskmanager.data.repository

import com.jingran.utils.ErrorHandler
import com.jingran.utils.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 基础Repository类
 * 提供通用的数据访问方法和错误处理
 */
abstract class BaseRepository {
    
    protected val tag: String = this::class.java.simpleName
    
    /**
     * 在IO线程中执行数据库操作
     */
    protected suspend fun <T> ioCall(operation: String = "数据库操作", block: suspend () -> T): T {
        return withContext(Dispatchers.IO) {
            try {
                LogManager.d(tag, "开始执行: $operation")
                val startTime = System.currentTimeMillis()
                
                val result = block()
                
                val duration = System.currentTimeMillis() - startTime
                LogManager.performance(tag, operation, duration)
                LogManager.d(tag, "成功完成: $operation")
                
                result
            } catch (e: Exception) {
                LogManager.logException(tag, "执行失败: $operation", e)
                throw e
            }
        }
    }
    
    /**
     * 安全执行数据库操作，返回Result
     */
    protected suspend fun <T> safeIoCall(operation: String = "数据库操作", block: suspend () -> T): Result<T> {
        return try {
            LogManager.d(tag, "开始安全执行: $operation")
            val startTime = System.currentTimeMillis()
            
            val result = withContext(Dispatchers.IO) {
                block()
            }
            
            val duration = System.currentTimeMillis() - startTime
            LogManager.performance(tag, operation, duration)
            LogManager.d(tag, "安全执行成功: $operation")
            
            Result.success(result)
        } catch (e: Exception) {
            LogManager.logException(tag, "安全执行失败: $operation", e)
            Result.failure(e)
        }
    }
    
    /**
     * 记录数据库操作
     */
    protected fun logDbOperation(operation: String, table: String, details: String = "") {
        LogManager.dbOperation(tag, operation, table, details)
    }
    
    /**
     * 验证输入参数
     */
    protected fun validateInput(condition: Boolean, message: String) {
        ErrorHandler.validateInput(condition, message)
    }
    
    /**
     * 验证非空参数
     */
    protected fun validateNotNull(value: Any?, paramName: String) {
        ErrorHandler.validateNotNull(value, paramName)
    }
    
    /**
     * 验证字符串非空
     */
    protected fun validateNotEmpty(value: String?, paramName: String) {
        ErrorHandler.validateNotEmpty(value, paramName)
    }
}