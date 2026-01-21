package com.jingran.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

/**
 * 统一日志管理工具类
 * 提供标准化的日志记录功能，支持不同级别的日志输出
 */
object LogManager {
    
    private const val TAG_PREFIX = "TaskManager"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    interface LogProvider {
        fun v(tag: String, message: String)
        fun v(tag: String, message: String, throwable: Throwable)
        fun d(tag: String, message: String)
        fun d(tag: String, message: String, throwable: Throwable)
        fun i(tag: String, message: String)
        fun i(tag: String, message: String, throwable: Throwable)
        fun w(tag: String, message: String)
        fun w(tag: String, message: String, throwable: Throwable)
        fun e(tag: String, message: String)
        fun e(tag: String, message: String, throwable: Throwable)
        fun getStackTraceString(throwable: Throwable): String
    }

    private object AndroidLogProvider : LogProvider {
        override fun v(tag: String, message: String) {
            Log.v(tag, message)
        }

        override fun v(tag: String, message: String, throwable: Throwable) {
            Log.v(tag, message, throwable)
        }

        override fun d(tag: String, message: String) {
            Log.d(tag, message)
        }

        override fun d(tag: String, message: String, throwable: Throwable) {
            Log.d(tag, message, throwable)
        }

        override fun i(tag: String, message: String) {
            Log.i(tag, message)
        }

        override fun i(tag: String, message: String, throwable: Throwable) {
            Log.i(tag, message, throwable)
        }

        override fun w(tag: String, message: String) {
            Log.w(tag, message)
        }

        override fun w(tag: String, message: String, throwable: Throwable) {
            Log.w(tag, message, throwable)
        }

        override fun e(tag: String, message: String) {
            Log.e(tag, message)
        }

        override fun e(tag: String, message: String, throwable: Throwable) {
            Log.e(tag, message, throwable)
        }

        override fun getStackTraceString(throwable: Throwable): String {
            return Log.getStackTraceString(throwable)
        }
    }

    val defaultLogProvider: LogProvider = AndroidLogProvider
    var logProvider: LogProvider = defaultLogProvider
    
    /**
     * Initialize LogManager (placeholder for future logging setup)
     */
    fun init(context: android.content.Context) {
        // Future: Initialize file logger or crash reporting
    }
    
    // 日志级别枚举
    enum class LogLevel {
        VERBOSE, DEBUG, INFO, WARN, ERROR
    }
    
    /**
     * 记录详细日志
     */
    fun v(tag: String, message: String, throwable: Throwable? = null) {
        val fullTag = "$TAG_PREFIX-$tag"
        val logMessage = formatMessage(message)
        if (throwable != null) {
            logProvider.v(fullTag, logMessage, throwable)
        } else {
            logProvider.v(fullTag, logMessage)
        }
    }
    
    /**
     * 记录调试日志
     */
    fun d(tag: String, message: String, throwable: Throwable? = null) {
        val fullTag = "$TAG_PREFIX-$tag"
        val logMessage = formatMessage(message)
        if (throwable != null) {
            logProvider.d(fullTag, logMessage, throwable)
        } else {
            logProvider.d(fullTag, logMessage)
        }
    }
    
    /**
     * 记录信息日志
     */
    fun i(tag: String, message: String, throwable: Throwable? = null) {
        val fullTag = "$TAG_PREFIX-$tag"
        val logMessage = formatMessage(message)
        if (throwable != null) {
            logProvider.i(fullTag, logMessage, throwable)
        } else {
            logProvider.i(fullTag, logMessage)
        }
    }
    
    /**
     * 记录警告日志
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        val fullTag = "$TAG_PREFIX-$tag"
        val logMessage = formatMessage(message)
        if (throwable != null) {
            logProvider.w(fullTag, logMessage, throwable)
        } else {
            logProvider.w(fullTag, logMessage)
        }
    }
    
    /**
     * 记录错误日志
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val fullTag = "$TAG_PREFIX-$tag"
        val logMessage = formatMessage(message)
        if (throwable != null) {
            logProvider.e(fullTag, logMessage, throwable)
        } else {
            logProvider.e(fullTag, logMessage)
        }
    }
    
    /**
     * 记录方法进入日志
     */
    fun enterMethod(tag: String, methodName: String, vararg params: Any?) {
        val paramString = if (params.isNotEmpty()) {
            params.joinToString(", ") { it?.toString() ?: "null" }
        } else {
            "no params"
        }
        d(tag, "→ $methodName($paramString)")
    }
    
    /**
     * 记录方法退出日志
     */
    fun exitMethod(tag: String, methodName: String, result: Any? = null) {
        val resultString = result?.toString() ?: "void"
        d(tag, "← $methodName returns: $resultString")
    }
    
    /**
     * 记录数据库操作日志
     */
    fun dbOperation(tag: String, operation: String, table: String, details: String = "") {
        val message = "DB[$operation] $table${if (details.isNotEmpty()) " - $details" else ""}"
        d(tag, message)
    }
    
    /**
     * 记录网络请求日志
     */
    fun networkRequest(tag: String, method: String, url: String, statusCode: Int? = null) {
        val status = statusCode?.let { " [$it]" } ?: ""
        i(tag, "HTTP[$method] $url$status")
    }
    
    /**
     * 记录用户操作日志
     */
    fun userAction(tag: String, action: String, details: String = "") {
        val message = "USER[$action]${if (details.isNotEmpty()) " - $details" else ""}"
        i(tag, message)
    }
    
    /**
     * 记录性能监控日志
     */
    fun performance(tag: String, operation: String, duration: Long) {
        i(tag, "PERF[$operation] took ${duration}ms")
    }
    
    /**
     * 格式化日志消息
     */
    private fun formatMessage(message: String): String {
        val timestamp = dateFormat.format(Date())
        val threadName = Thread.currentThread().name
        return "[$timestamp][$threadName] $message"
    }
    
    /**
     * 获取调用栈信息
     */
    fun getStackTrace(throwable: Throwable): String {
        return logProvider.getStackTraceString(throwable)
    }
    
    /**
     * 记录异常详细信息
     */
    fun logException(tag: String, message: String, throwable: Throwable) {
        e(tag, "$message\nException: ${throwable.javaClass.simpleName}\nMessage: ${throwable.message}\nStackTrace: ${getStackTrace(throwable)}")
    }
    
    /**
     * 条件日志记录
     */
    inline fun logIf(condition: Boolean, level: LogLevel, tag: String, message: () -> String) {
        if (condition) {
            when (level) {
                LogLevel.VERBOSE -> v(tag, message())
                LogLevel.DEBUG -> d(tag, message())
                LogLevel.INFO -> i(tag, message())
                LogLevel.WARN -> w(tag, message())
                LogLevel.ERROR -> e(tag, message())
            }
        }
    }
}
