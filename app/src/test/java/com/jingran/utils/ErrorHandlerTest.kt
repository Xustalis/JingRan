package com.jingran.utils

import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.IOException
import java.net.SocketTimeoutException

class ErrorHandlerTest {

    private lateinit var testLogProvider: TestLogProvider

    @Before
    fun setup() {
        testLogProvider = TestLogProvider()
        LogManager.logProvider = testLogProvider
    }

    @After
    fun tearDown() {
        LogManager.logProvider = LogManager.defaultLogProvider
    }
    @Test
    fun `handleException should return correct ErrorInfo for IOException`() {
        // Given
        val exception = IOException("网络连接失败")
        
        // When
        val errorInfo = ErrorHandler.handleException(exception)
        
        // Then
        assertEquals(ErrorHandler.ErrorType.NETWORK_ERROR, errorInfo.type)
        assertEquals("网络IO异常: 网络连接失败", errorInfo.message)
        assertEquals(exception, errorInfo.throwable)
    }
    
    @Test
    fun `handleException should return correct ErrorInfo for SocketTimeoutException`() {
        // Given
        val exception = SocketTimeoutException("连接超时")
        
        // When
        val errorInfo = ErrorHandler.handleException(exception)
        
        // Then
        assertEquals(ErrorHandler.ErrorType.NETWORK_ERROR, errorInfo.type)
        assertEquals("网络连接超时", errorInfo.message)
    }
    
    @Test
    fun `handleException should return correct ErrorInfo for IllegalArgumentException`() {
        // Given
        val exception = IllegalArgumentException("参数无效")
        
        // When
        val errorInfo = ErrorHandler.handleException(exception)
        
        // Then
        assertEquals(ErrorHandler.ErrorType.VALIDATION_ERROR, errorInfo.type)
        assertEquals("参数验证失败: 参数无效", errorInfo.message)
    }
    
    @Test
    fun `handleException should return correct ErrorInfo for unknown exception`() {
        // Given
        val exception = RuntimeException("未知错误")
        
        // When
        val errorInfo = ErrorHandler.handleException(exception)
        
        // Then
        assertEquals(ErrorHandler.ErrorType.UNKNOWN_ERROR, errorInfo.type)
        assertEquals("未知异常: 未知错误", errorInfo.message)
    }
    
    @Test
    fun `safeExecute should execute block successfully`() {
        // Given
        var executed = false
        val block = { executed = true }
        
        // When
        val result = ErrorHandler.safeExecute(
            context = null,
            showErrorToUser = false,
            block = block
        )
        
        // Then
        assertTrue(executed)
        assertNotNull(result)
    }
    
    @Test
    fun `safeExecute should handle exception and return null`() {
        // Given
        val exception = RuntimeException("测试异常")
        val block = { throw exception }
        var errorHandled = false
        
        // When
        val result = ErrorHandler.safeExecute(
            context = null,
            showErrorToUser = false,
            onError = { errorHandled = true },
            block = block
        )
        
        // Then
        assertNull(result)
        assertTrue(errorHandled)
    }
    
    @Test
    fun `validateNotEmpty should not throw for valid input`() {
        // Given
        val validInput = "有效输入"
        val fieldName = "测试字段"
        
        // When & Then
        try {
            ErrorHandler.validateNotEmpty(validInput, fieldName)
            // If no exception is thrown, the test passes
        } catch (e: Exception) {
            fail("Expected no exception, but got: ${e.message}")
        }
    }
    
    @Test
    fun `validateNotEmpty should throw for empty input`() {
        // Given
        val emptyInput = ""
        val fieldName = "测试字段"
        
        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            ErrorHandler.validateNotEmpty(emptyInput, fieldName)
        }
        assertTrue(exception.message!!.contains(fieldName))
    }
    
    @Test
    fun `validateNotEmpty should throw for blank input`() {
        // Given
        val blankInput = "   "
        val fieldName = "测试字段"
        
        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            ErrorHandler.validateNotEmpty(blankInput, fieldName)
        }
        assertTrue(exception.message!!.contains(fieldName))
    }

    private class TestLogProvider : LogManager.LogProvider {
        override fun v(tag: String, message: String) {}

        override fun v(tag: String, message: String, throwable: Throwable) {}

        override fun d(tag: String, message: String) {}

        override fun d(tag: String, message: String, throwable: Throwable) {}

        override fun i(tag: String, message: String) {}

        override fun i(tag: String, message: String, throwable: Throwable) {}

        override fun w(tag: String, message: String) {}

        override fun w(tag: String, message: String, throwable: Throwable) {}

        override fun e(tag: String, message: String) {}

        override fun e(tag: String, message: String, throwable: Throwable) {}

        override fun getStackTraceString(throwable: Throwable): String {
            return "stack trace"
        }
    }
    
    

    
    @Test
    fun `ErrorInfo should return correct user message for different error types`() {
        // Test NETWORK error
        val networkError = ErrorHandler.ErrorInfo(
            type = ErrorHandler.ErrorType.NETWORK_ERROR,
            message = "网络错误",
            userMessage = "网络连接失败，请检查网络设置",
            throwable = IOException()
        )
        val networkMessage = networkError.userMessage
        assertTrue(networkMessage.contains("网络"))
        
        // Test DATABASE error
        val databaseError = ErrorHandler.ErrorInfo(
            type = ErrorHandler.ErrorType.DATABASE_ERROR,
            message = "数据库错误",
            userMessage = "数据保存失败，请稍后重试",
            throwable = RuntimeException()
        )
        val databaseMessage = databaseError.userMessage
        assertTrue(databaseMessage.contains("数据"))
        
        // Test VALIDATION error
        val validationError = ErrorHandler.ErrorInfo(
            type = ErrorHandler.ErrorType.VALIDATION_ERROR,
            message = "验证错误",
            userMessage = "输入信息有误，请检查后重试",
            throwable = IllegalArgumentException()
        )
        val validationMessage = validationError.userMessage
        assertTrue(validationMessage.contains("输入"))
    }
    
    @Test
    fun `safeCoroutineExecute should execute block in coroutine`() = runTest {
        // Given
        var executed = false
        val block: suspend () -> Unit = { executed = true }
        
        // When
        ErrorHandler.safeCoroutineExecute(
            scope = this,
            block = block
        )
        advanceUntilIdle()
        
        // Then
        assertTrue(executed)
    }
}
