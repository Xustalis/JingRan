package com.jingran.utils

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

object UtilsManager {
    
    private const val TAG = "UtilsManager"
    
    val errorHandler = ErrorHandler
    
    val dateUtils = DateUtils
    
    val logManager = LogManager
    
    val notificationHelper = NotificationHelper
    
    val uiComponents = UiComponents
    
    val coroutineErrorHandler = CoroutineErrorHandler
    
    fun createUiStateManager(): UiStateManager<*> {
        return UiStateManager<Any>()
    }
    
    fun <T> createUiStateManagerTyped(): UiStateManager<T> {
        return UiStateManager()
    }
    
    fun safeExecute(
        context: Context? = null,
        showErrorToUser: Boolean = true,
        onError: ((ErrorHandler.ErrorInfo) -> Unit)? = null,
        block: () -> Unit
    ) {
        errorHandler.safeExecute(context, showErrorToUser, onError, block)
    }
    
    fun <T> safeExecuteWithResult(
        context: Context? = null,
        showErrorToUser: Boolean = true,
        onError: ((ErrorHandler.ErrorInfo) -> Unit)? = null,
        block: () -> T
    ): T? {
        return errorHandler.safeExecute(context, showErrorToUser, onError, block)
    }
    
    fun safeCoroutineExecute(
        scope: CoroutineScope,
        context: Context? = null,
        showErrorToUser: Boolean = true,
        onError: ((ErrorHandler.ErrorInfo) -> Unit)? = null,
        block: suspend () -> Unit
    ): Job {
        return coroutineErrorHandler.safeCoroutineExecuteWithTimeout(
            scope = scope,
            context = context,
            showErrorToUser = showErrorToUser,
            onError = onError,
            block = block
        )
    }
    
    fun safeLifecycleCoroutineExecute(
        lifecycleOwner: LifecycleOwner,
        context: Context? = null,
        showErrorToUser: Boolean = true,
        onError: ((ErrorHandler.ErrorInfo) -> Unit)? = null,
        block: suspend () -> Unit
    ): Job {
        return coroutineErrorHandler.safeLifecycleCoroutineExecute(
            lifecycleOwner = lifecycleOwner,
            context = context,
            showErrorToUser = showErrorToUser,
            onError = onError,
            block = block
        )
    }
    
    fun safeViewModelExecute(
        scope: CoroutineScope,
        loadingState: MutableLiveData<Boolean>? = null,
        errorState: MutableLiveData<String?>? = null,
        onError: ((ErrorHandler.ErrorInfo) -> Unit)? = null,
        onFinally: (() -> Unit)? = null,
        block: suspend () -> Unit
    ): Job {
        return coroutineErrorHandler.safeViewModelExecute(
            scope = scope,
            loadingState = loadingState,
            errorState = errorState,
            onError = onError,
            onFinally = onFinally,
            block = block
        )
    }
    
    fun showToast(context: Context, message: String, duration: Int = android.widget.Toast.LENGTH_SHORT) {
        context.showToast(message, duration)
    }
    
    fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        return dateUtils.isSameDay(timestamp1, timestamp2)
    }
    
    fun logD(tag: String, message: String) {
        logManager.d(tag, message)
    }
    
    fun logI(tag: String, message: String) {
        logManager.i(tag, message)
    }
    
    fun logW(tag: String, message: String) {
        logManager.w(tag, message)
    }
    
    fun logE(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            logManager.e(tag, message, throwable)
        } else {
            logManager.e(tag, message)
        }
    }
    
    fun logException(tag: String, message: String, throwable: Throwable) {
        logManager.logException(tag, message, throwable)
    }
}