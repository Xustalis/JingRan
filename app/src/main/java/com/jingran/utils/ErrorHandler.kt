package com.jingran.utils

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.sql.SQLException

/**
 * 统一异常处理工具类
 * 提供标准化的错误处理和用户友好的错误提示
 */
object ErrorHandler {
    
    private const val TAG = "ErrorHandler"
    
    /**
 * 异常类型枚举
     */
    enum class ErrorType {
        NETWORK_ERROR,
        DATABASE_ERROR,
        VALIDATION_ERROR,
        PERMISSION_ERROR,
        UNKNOWN_ERROR,
        BUSINESS_ERROR
    }
    
    /**
     * 错误信息数据类
     */
    data class ErrorInfo(
        val type: ErrorType,
        val message: String,
        val userMessage: String,
        val throwable: Throwable? = null
    )
    
    /**
     * 处理异常并返回错误信息
     */
    fun handleException(throwable: Throwable): ErrorInfo {
        LogManager.logException(TAG, "处理异常", throwable)
        
        return when (throwable) {
            is SocketTimeoutException -> ErrorInfo(
                ErrorType.NETWORK_ERROR,
                "网络连接超时",
                "网络连接超时，请检查网络设置后重试",
                throwable
            )
            
            is UnknownHostException -> ErrorInfo(
                ErrorType.NETWORK_ERROR,
                "无法连接到服务器",
                "无法连接到服务器，请检查网络连接",
                throwable
            )
            
            is IOException -> ErrorInfo(
                ErrorType.NETWORK_ERROR,
                "网络IO异常: ${throwable.message}",
                "网络连接异常，请稍后重试",
                throwable
            )
            
            is SQLException -> ErrorInfo(
                ErrorType.DATABASE_ERROR,
                "数据库操作异常: ${throwable.message}",
                "数据保存失败，请稍后重试",
                throwable
            )
            
            is IllegalArgumentException -> ErrorInfo(
                ErrorType.VALIDATION_ERROR,
                "参数验证失败: ${throwable.message}",
                "输入的数据格式不正确，请检查后重试",
                throwable
            )
            
            is SecurityException -> ErrorInfo(
                ErrorType.PERMISSION_ERROR,
                "权限不足: ${throwable.message}",
                "应用权限不足，请检查权限设置",
                throwable
            )
            
            else -> ErrorInfo(
                ErrorType.UNKNOWN_ERROR,
                "未知异常: ${throwable.message}",
                "操作失败，请稍后重试",
                throwable
            )
        }
    }
    
    /**
     * 显示错误提示给用户
     */
    fun showError(context: Context, errorInfo: ErrorInfo) {
        LogManager.w(TAG, "显示错误提示: ${errorInfo.userMessage}")
        Toast.makeText(context, errorInfo.userMessage, Toast.LENGTH_LONG).show()
    }
    
    /**
     * 处理异常并显示错误提示
     */
    fun handleAndShowError(context: Context, throwable: Throwable) {
        val errorInfo = handleException(throwable)
        showError(context, errorInfo)
    }
    
    /**
     * 创建协程异常处理器
     */
    fun createCoroutineExceptionHandler(
        context: Context? = null,
        onError: ((ErrorInfo) -> Unit)? = null
    ): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, throwable ->
            val errorInfo = handleException(throwable)
            
            // 如果提供了自定义错误处理回调，则调用它
            onError?.invoke(errorInfo)
            
            // 如果提供了Context，则显示错误提示
            context?.let { showError(it, errorInfo) }
        }
    }
    
    /**
     * 安全执行代码块，捕获并处理异常
     */
    inline fun <T> safeExecute(
        context: Context? = null,
        showErrorToUser: Boolean = true,
        noinline onError: ((ErrorInfo) -> Unit)? = null,
        block: () -> T
    ): T? {
        return try {
            block()
        } catch (throwable: Throwable) {
            val errorInfo = handleException(throwable)
            
            onError?.invoke(errorInfo)
            
            if (showErrorToUser && context != null) {
                showError(context, errorInfo)
            }
            
            null
        }
    }
    
    /**
     * 安全执行协程代码块
     */
    fun safeCoroutineExecute(
        scope: CoroutineScope,
        context: Context? = null,
        showErrorToUser: Boolean = true,
        onError: ((ErrorInfo) -> Unit)? = null,
        block: suspend () -> Unit
    ) {
        val exceptionHandler = createCoroutineExceptionHandler(context) { errorInfo ->
            onError?.invoke(errorInfo)
            if (showErrorToUser && context != null) {
                showError(context, errorInfo)
            }
        }
        
        scope.launch(exceptionHandler) {
            block()
        }
    }
    
    /**
     * 验证输入参数
     */
    fun validateInput(condition: Boolean, message: String) {
        if (!condition) {
            throw IllegalArgumentException(message)
        }
    }
    
    /**
     * 验证非空参数
     */
    fun validateNotNull(value: Any?, paramName: String) {
        if (value == null) {
            throw IllegalArgumentException("参数 $paramName 不能为空")
        }
    }
    
    /**
     * 验证字符串非空
     */
    fun validateNotEmpty(value: String?, paramName: String) {
        if (value.isNullOrEmpty()) {
            throw IllegalArgumentException("参数 $paramName 不能为空")
        }
    }
    
    /**
     * 验证集合非空
     */
    fun validateNotEmpty(collection: Collection<*>?, paramName: String) {
        if (collection.isNullOrEmpty()) {
            throw IllegalArgumentException("参数 $paramName 不能为空")
        }
    }
    
    /**
     * 创建业务异常
     */
    fun createBusinessError(message: String): ErrorInfo {
        return ErrorInfo(
            ErrorType.BUSINESS_ERROR,
            message,
            message
        )
    }
    
    /**
     * 记录并返回错误结果
     */
    fun <T> logAndReturnError(tag: String, message: String, throwable: Throwable? = null): Result<T> {
        if (throwable != null) {
            LogManager.logException(tag, message, throwable)
        } else {
            LogManager.e(tag, message)
        }
        
        val errorInfo = throwable?.let { handleException(it) } ?: createBusinessError(message)
        return Result.failure(Exception(errorInfo.userMessage))
    }
    
    /**
     * 记录成功结果
     */
    fun <T> logAndReturnSuccess(tag: String, message: String, result: T): Result<T> {
        LogManager.i(tag, message)
        return Result.success(result)
    }
}