package com.jingran.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.sql.SQLException

/**
 * 统一异常处理工具类
 * 提供标准化的错误处理和用户友好的错误提示
 */
object ErrorHandlerKt {
    
    private const val TAG = "ErrorHandlerKt"
    
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
        Log.e(TAG, "处理异常", throwable)
        
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
     * 创建协程异常处理器
     */
    fun createCoroutineExceptionHandler(context: Context): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, throwable ->
            val errorInfo = handleException(throwable)
            Log.e(TAG, "协程执行异常: ${errorInfo.message}", throwable)
            Toast.makeText(context, errorInfo.userMessage, Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 安全执行协程
     */
    fun safeCoroutineExecute(
        context: Context,
        scope: CoroutineScope,
        onError: ((ErrorInfo) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit
    ) {
        scope.launch(createCoroutineExceptionHandler(context)) {
            try {
                block()
            } catch (e: Exception) {
                val errorInfo = handleException(e)
                onError?.invoke(errorInfo) ?: run {
                    Toast.makeText(context, errorInfo.userMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * 安全执行普通函数
     */
    fun <T> safeExecute(
        context: Context,
        onError: ((ErrorInfo) -> Unit)? = null,
        block: () -> T
    ): T? {
        return try {
            block()
        } catch (e: Exception) {
            val errorInfo = handleException(e)
            Log.e(TAG, "执行异常: ${errorInfo.message}", e)
            onError?.invoke(errorInfo) ?: run {
                Toast.makeText(context, errorInfo.userMessage, Toast.LENGTH_SHORT).show()
            }
            null
        }
    }
    
    /**
     * 用于ViewModel的错误处理
     */
    class ViewModelErrorHandler {
        private val _error = MutableLiveData<ErrorInfo?>()
        val error: LiveData<ErrorInfo?> = _error
        
        private val _isLoading = MutableLiveData<Boolean>()
        val isLoading: LiveData<Boolean> = _isLoading
        
        fun setLoading(isLoading: Boolean) {
            _isLoading.postValue(isLoading)
        }
        
        fun setError(errorInfo: ErrorInfo?) {
            _error.postValue(errorInfo)
        }
        
        fun clearError() {
            _error.postValue(null)
        }
        
        fun <T> executeWithLoading(
            scope: CoroutineScope,
            block: suspend () -> T,
            onSuccess: (T) -> Unit = {}
        ) {
            setLoading(true)
            clearError()
            
            scope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
                val errorInfo = handleException(throwable)
                setError(errorInfo)
                setLoading(false)
            }) {
                try {
                    val result = block()
                    withContext(Dispatchers.Main) {
                        onSuccess(result)
                        setLoading(false)
                    }
                } catch (e: Exception) {
                    val errorInfo = handleException(e)
                    setError(errorInfo)
                    setLoading(false)
                }
            }
        }
    }
}