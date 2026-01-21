package com.jingran.utils

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicInteger

/**
 * 增强的协程异常处理工具类
 * 提供更完善的协程异常处理和生命周期管理
 */
object CoroutineErrorHandler {
    
    private const val TAG = "CoroutineErrorHandler"
    
    /**
     * 创建带有超时和重试机制的协程异常处理器
     */
    fun createAdvancedExceptionHandler(
        context: Context? = null,
        onError: ((ErrorHandler.ErrorInfo) -> Unit)? = null,
        onTimeout: (() -> Unit)? = null,
        onRetry: ((Int) -> Unit)? = null
    ): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, throwable ->
            when (throwable) {
                is CancellationException -> {
                    LogManager.d(TAG, "协程被取消: ${throwable.message}")
                    // 协程取消是正常行为，不需要特殊处理
                }
                is TimeoutCancellationException -> {
                    LogManager.w(TAG, "协程执行超时: ${throwable.message}")
                    onTimeout?.invoke()
                    context?.let {
                        ErrorHandler.showError(it, ErrorHandler.ErrorInfo(
                            ErrorHandler.ErrorType.UNKNOWN_ERROR,
                            "操作超时",
                            "操作超时，请检查网络连接或稍后重试",
                            throwable
                        ))
                    }
                }
                else -> {
                    val errorInfo = ErrorHandler.handleException(throwable)
                    LogManager.e(TAG, "协程执行异常: ${errorInfo.message}", throwable)
                    onError?.invoke(errorInfo)
                    context?.let { ErrorHandler.showError(it, errorInfo) }
                }
            }
        }
    }
    
    /**
     * 安全执行协程代码块，带有超时控制
     */
    fun safeCoroutineExecuteWithTimeout(
        scope: CoroutineScope,
        timeoutMs: Long = 30000L, // 默认30秒超时
        context: Context? = null,
        showErrorToUser: Boolean = true,
        onError: ((ErrorHandler.ErrorInfo) -> Unit)? = null,
        onTimeout: (() -> Unit)? = null,
        block: suspend () -> Unit
    ): Job {
        val exceptionHandler = createAdvancedExceptionHandler(
            context = context,
            onError = { errorInfo ->
                onError?.invoke(errorInfo)
                if (showErrorToUser && context != null) {
                    ErrorHandler.showError(context, errorInfo)
                }
            },
            onTimeout = onTimeout
        )
        
        return scope.launch(exceptionHandler) {
            withTimeout(timeoutMs) {
                block()
            }
        }
    }
    
    /**
     * 带有重试机制的协程执行
     */
    fun safeCoroutineExecuteWithRetry(
        scope: CoroutineScope,
        maxRetries: Int = 3,
        retryDelayMs: Long = 1000L,
        context: Context? = null,
        showErrorToUser: Boolean = true,
        onError: ((ErrorHandler.ErrorInfo, Int) -> Unit)? = null,
        onRetryExhausted: ((ErrorHandler.ErrorInfo) -> Unit)? = null,
        block: suspend () -> Unit
    ): Job {
        return scope.launch {
            var lastError: ErrorHandler.ErrorInfo? = null
            
            repeat(maxRetries + 1) { attempt ->
                try {
                    block()
                    return@launch // 成功执行，退出重试循环
                } catch (e: CancellationException) {
                    throw e // 重新抛出取消异常
                } catch (e: Exception) {
                    lastError = ErrorHandler.handleException(e)
                    LogManager.w(TAG, "协程执行失败，尝试次数: ${attempt + 1}/${maxRetries + 1}", e)
                    
                    onError?.invoke(lastError!!, attempt)
                    
                    if (attempt < maxRetries) {
                        delay(retryDelayMs * (attempt + 1)) // 递增延迟
                    }
                }
            }
            
            // 重试次数用尽
            lastError?.let { error ->
                LogManager.e(TAG, "协程执行失败，重试次数已用尽: ${error.message}")
                onRetryExhausted?.invoke(error)
                if (showErrorToUser && context != null) {
                    ErrorHandler.showError(context, error)
                }
            }
        }
    }
    
    /**
     * 生命周期感知的协程执行
     */
    fun safeLifecycleCoroutineExecute(
        lifecycleOwner: LifecycleOwner,
        context: Context? = null,
        showErrorToUser: Boolean = true,
        onError: ((ErrorHandler.ErrorInfo) -> Unit)? = null,
        block: suspend () -> Unit
    ): Job {
        val exceptionHandler = createAdvancedExceptionHandler(
            context = context,
            onError = { errorInfo ->
                onError?.invoke(errorInfo)
                if (showErrorToUser && context != null) {
                    ErrorHandler.showError(context, errorInfo)
                }
            }
        )
        
        return lifecycleOwner.lifecycleScope.launch(exceptionHandler) {
            block()
        }
    }
    
    /**
     * ViewModel专用的协程执行方法
     */
    fun safeViewModelExecute(
        scope: CoroutineScope,
        loadingState: MutableLiveData<Boolean>? = null,
        errorState: MutableLiveData<String?>? = null,
        onError: ((ErrorHandler.ErrorInfo) -> Unit)? = null,
        onFinally: (() -> Unit)? = null,
        block: suspend () -> Unit
    ): Job {
        return scope.launch {
            try {
                loadingState?.value = true
                block()
            } catch (e: CancellationException) {
                throw e // 重新抛出取消异常
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleException(e)
                LogManager.e(TAG, "ViewModel协程执行异常: ${errorInfo.message}", e)
                
                onError?.invoke(errorInfo)
                errorState?.value = errorInfo.userMessage
            } finally {
                loadingState?.value = false
                onFinally?.invoke()
            }
        }
    }
    
    /**
     * 批量执行协程任务
     */
    fun safeBatchExecute(
        scope: CoroutineScope,
        tasks: List<suspend () -> Unit>,
        concurrency: Int = 3, // 并发数量
        @Suppress("UNUSED_PARAMETER") context: Context? = null,
        onProgress: ((Int, Int) -> Unit)? = null,
        onError: ((ErrorHandler.ErrorInfo, Int) -> Unit)? = null,
        onComplete: ((Int, Int) -> Unit)? = null // 成功数量，失败数量
    ): Job {
        return scope.launch {
            val successCount = AtomicInteger(0)
            val failureCount = AtomicInteger(0)
            
            val jobs = tasks.mapIndexed { index, task ->
                async {
                    try {
                        task()
                        val currentSuccess = successCount.incrementAndGet()
                        val currentFailure = failureCount.get()
                        onProgress?.invoke(currentSuccess + currentFailure, tasks.size)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        val errorInfo = ErrorHandler.handleException(e)
                        LogManager.e(TAG, "批量任务执行失败，任务索引: $index", e)
                        
                        val currentFailure = failureCount.incrementAndGet()
                        val currentSuccess = successCount.get()
                        onError?.invoke(errorInfo, index)
                        onProgress?.invoke(currentSuccess + currentFailure, tasks.size)
                    }
                }
            }
            
            // 等待所有任务完成
            jobs.awaitAll()
            
            onComplete?.invoke(successCount.get(), failureCount.get())
            LogManager.i(TAG, "批量任务执行完成: 成功 ${successCount.get()}, 失败 ${failureCount.get()}")
        }
    }
    
    /**
     * 创建可取消的协程作业管理器
     */
    class JobManager {
        private val jobs = mutableSetOf<Job>()
        
        fun launch(
            scope: CoroutineScope,
            context: Context? = null,
            onError: ((ErrorHandler.ErrorInfo) -> Unit)? = null,
            block: suspend () -> Unit
        ): Job {
            val job = safeCoroutineExecuteWithTimeout(
                scope = scope,
                context = context,
                onError = onError,
                block = block
            )
            
            jobs.add(job)
            job.invokeOnCompletion { jobs.remove(job) }
            
            return job
        }
        
        fun cancelAll() {
            jobs.forEach { it.cancel() }
            jobs.clear()
            LogManager.d(TAG, "所有协程作业已取消")
        }
        
        fun getActiveJobCount(): Int = jobs.count { it.isActive }
    }
}