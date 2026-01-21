package com.jingran.utils

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.google.android.material.snackbar.Snackbar
import com.jingran.utils.ErrorHandlerKt.ErrorInfo

/**
 * UI组件工具类
 * 提供常用UI组件的扩展函数
 */
object UiComponents {

    /**
     * 显示Toast消息
     */
    fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }

    /**
     * 显示Snackbar消息
     */
    fun View.showSnackbar(
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        val snackbar = Snackbar.make(this, message, duration)
        if (actionText != null && action != null) {
            snackbar.setAction(actionText) { action() }
        }
        snackbar.show()
    }

    /**
     * 显示错误信息
     */
    fun View.showError(
        errorInfo: ErrorInfo,
        actionText: String? = "重试",
        action: (() -> Unit)? = null
    ) {
        showSnackbar(errorInfo.userMessage, Snackbar.LENGTH_LONG, actionText, action)
    }

    /**
     * 显示加载状态
     */
    fun View.setVisibleOrGone(visible: Boolean) {
        visibility = if (visible) View.VISIBLE else View.GONE
    }

    /**
     * 显示加载状态
     */
    fun View.setVisibleOrInvisible(visible: Boolean) {
        visibility = if (visible) View.VISIBLE else View.INVISIBLE
    }

    /**
     * 观察UI状态
     */
    fun <T> Fragment.observeUiState(
        uiStateManager: UiStateManager<T>,
        loadingView: View? = null,
        emptyView: View? = null,
        errorView: View? = null,
        contentView: View? = null,
        onDataLoaded: ((T?) -> Unit)? = null,
        onError: ((ErrorInfo) -> Unit)? = null
    ) {
        // 观察加载状态
        uiStateManager.isLoading.observe(viewLifecycleOwner) { isLoading ->
            loadingView?.setVisibleOrGone(isLoading)
        }

        // 观察空数据状态
        uiStateManager.isEmpty.observe(viewLifecycleOwner) { isEmpty ->
            emptyView?.setVisibleOrGone(isEmpty && !uiStateManager.isLoading.value!!)
        }

        // 观察错误状态
        uiStateManager.error.observe(viewLifecycleOwner) { errorInfo ->
            val hasError = errorInfo != null
            errorView?.setVisibleOrGone(hasError)
            
            if (hasError && errorInfo != null) {
                onError?.invoke(errorInfo) ?: run {
                    view?.showError(errorInfo)
                }
            }
        }

        // 观察数据状态
        uiStateManager.data.observe(viewLifecycleOwner) { data ->
            contentView?.setVisibleOrGone(data != null)
            onDataLoaded?.invoke(data)
        }
    }

    /**
     * LiveData扩展函数，简化观察者模式
     */
    fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: (T) -> Unit) {
        observe(lifecycleOwner) { value ->
            value?.let {
                observer(it)
                if (lifecycleOwner is Fragment && lifecycleOwner.view == null) {
                    return@observe
                }
                removeObservers(lifecycleOwner)
            }
        }
    }
}