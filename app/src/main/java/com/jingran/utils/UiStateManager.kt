package com.jingran.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jingran.utils.ErrorHandler.ErrorInfo

/**
 * UI状态管理工具类
 * 用于管理界面加载状态、错误状态和数据状态
 */
class UiStateManager<T> {
    // 加载状态
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // 错误状态
    private val _error = MutableLiveData<ErrorInfo?>()
    val error: LiveData<ErrorInfo?> = _error

    // 数据状态
    private val _data = MutableLiveData<T?>()
    val data: LiveData<T?> = _data

    // 空数据状态
    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    /**
     * 设置加载状态
     */
    fun setLoading(isLoading: Boolean) {
        _isLoading.postValue(isLoading)
    }

    /**
     * 设置错误状态
     */
    fun setError(errorInfo: ErrorInfo?) {
        _error.postValue(errorInfo)
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        _error.postValue(null)
    }

    /**
     * 设置数据
     */
    fun setData(data: T?) {
        _data.postValue(data)
        _isEmpty.postValue(isDataEmpty(data))
    }

    /**
     * 判断数据是否为空
     */
    private fun isDataEmpty(data: T?): Boolean {
        return when (data) {
            null -> true
            is Collection<*> -> data.isEmpty()
            is Array<*> -> data.isEmpty()
            is Map<*, *> -> data.isEmpty()
            is String -> data.isEmpty()
            else -> false
        }
    }

    /**
     * 重置所有状态
     */
    fun reset() {
        _isLoading.postValue(false)
        _error.postValue(null)
        _data.postValue(null)
        _isEmpty.postValue(true)
    }

    /**
     * 数据加载成功
     */
    fun onSuccess(data: T?) {
        setLoading(false)
        clearError()
        setData(data)
    }

    /**
     * 数据加载失败
     */
    fun onError(errorInfo: ErrorInfo) {
        setLoading(false)
        setError(errorInfo)
    }

    /**
     * 开始加载数据
     */
    fun onLoading() {
        setLoading(true)
        clearError()
    }
}