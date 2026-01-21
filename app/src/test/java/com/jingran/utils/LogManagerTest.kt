package com.jingran.utils

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class LogManagerTest {

    private lateinit var testLogProvider: TestLogProvider

    private fun fullTag(tag: String): String = "TaskManager-$tag"
    
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
    fun `verbose should call Log_v with correct parameters`() {
        // Given
        val tag = "TestTag"
        val message = "测试详细日志"
        
        // When
        LogManager.v(tag, message)
        
        // Then
        val entry = testLogProvider.entries.last()
        assertEquals("V", entry.level)
        assertEquals(fullTag(tag), entry.tag)
        assertTrue(entry.message.contains(message))
    }
    
    @Test
    fun `debug should call Log_d with correct parameters`() {
        // Given
        val tag = "TestTag"
        val message = "测试调试日志"
        
        // When
        LogManager.d(tag, message)
        
        // Then
        val entry = testLogProvider.entries.last()
        assertEquals("D", entry.level)
        assertEquals(fullTag(tag), entry.tag)
        assertTrue(entry.message.contains(message))
    }
    
    @Test
    fun `info should call Log_i with correct parameters`() {
        // Given
        val tag = "TestTag"
        val message = "测试信息日志"
        
        // When
        LogManager.i(tag, message)
        
        // Then
        val entry = testLogProvider.entries.last()
        assertEquals("I", entry.level)
        assertEquals(fullTag(tag), entry.tag)
        assertTrue(entry.message.contains(message))
    }
    
    @Test
    fun `warn should call Log_w with correct parameters`() {
        // Given
        val tag = "TestTag"
        val message = "测试警告日志"
        
        // When
        LogManager.w(tag, message)
        
        // Then
        val entry = testLogProvider.entries.last()
        assertEquals("W", entry.level)
        assertEquals(fullTag(tag), entry.tag)
        assertTrue(entry.message.contains(message))
    }
    
    @Test
    fun `error should call Log_e with correct parameters`() {
        // Given
        val tag = "TestTag"
        val message = "测试错误日志"
        
        // When
        LogManager.e(tag, message)
        
        // Then
        val entry = testLogProvider.entries.last()
        assertEquals("E", entry.level)
        assertEquals(fullTag(tag), entry.tag)
        assertTrue(entry.message.contains(message))
    }
    
    @Test
    fun `error with exception should call Log_e with exception`() {
        // Given
        val tag = "TestTag"
        val message = "测试错误日志"
        val exception = RuntimeException("测试异常")
        
        // When
        LogManager.e(tag, message, exception)
        
        // Then
        val entry = testLogProvider.entries.last()
        assertEquals("E", entry.level)
        assertEquals(fullTag(tag), entry.tag)
        assertTrue(entry.message.contains(message))
        assertEquals(exception, entry.throwable)
    }
    
    @Test
    fun `enterMethod should log method entry`() {
        // Given
        val tag = "TestTag"
        val methodName = "testMethod"
        
        // When
        LogManager.enterMethod(tag, methodName)
        
        // Then
        val entry = testLogProvider.entries.last()
        assertEquals("D", entry.level)
        assertEquals(fullTag(tag), entry.tag)
    }
    
    @Test
    fun `exitMethod should log method exit`() {
        // Given
        val tag = "TestTag"
        val methodName = "testMethod"
        val result = "成功"
        
        // When
        LogManager.exitMethod(tag, methodName, result)
        
        // Then
        val entry = testLogProvider.entries.last()
        assertEquals("D", entry.level)
        assertEquals(fullTag(tag), entry.tag)
    }
    
    @Test
    fun `dbOperation should log database operation`() {
        // Given
        val tag = "TestTag"
        val operation = "INSERT"
        val table = "tasks"
        val details = "插入新任务"
        
        // When
        LogManager.dbOperation(tag, operation, table, details)
        
        // Then
        val entry = testLogProvider.entries.last()
        assertEquals("D", entry.level)
        assertEquals(fullTag(tag), entry.tag)
    }
    
    @Test
    fun `networkRequest should log network request`() {
        // Given
        val tag = "TestTag"
        val method = "GET"
        val url = "https://api.example.com/tasks"
        val statusCode = 200
        
        // When
        LogManager.networkRequest(tag, method, url, statusCode)
        
        // Then
        val entry = testLogProvider.entries.last()
        assertEquals("I", entry.level)
        assertEquals(fullTag(tag), entry.tag)
    }
    
    @Test
    fun `userAction should log user action`() {
        // Given
        val tag = "TestTag"
        val action = "点击按钮"
        val details = "保存任务"
        
        // When
        LogManager.userAction(tag, action, details)
        
        // Then
        val entry = testLogProvider.entries.last()
        assertEquals("I", entry.level)
        assertEquals(fullTag(tag), entry.tag)
    }
    
    @Test
    fun `performance should log performance metrics`() {
        // Given
        val tag = "PerformanceTest"
        val operation = "数据库查询"
        val duration = 150L
        
        // When
        LogManager.performance(tag, operation, duration)
        
        // Then
        val entry = testLogProvider.entries.last()
        assertEquals("I", entry.level)
        assertEquals(fullTag(tag), entry.tag)
    }
    
    @Test
    fun `getStackTrace should return formatted stack trace`() {
        // Given
        val exception = RuntimeException("测试异常")
        
        // When
        val stackTrace = LogManager.getStackTrace(exception)
        
        // Then
        assertNotNull("Stack trace should not be null", stackTrace)
        assertTrue("Stack trace should contain exception message", 
                  stackTrace.contains("测试异常"))
        assertTrue("Stack trace should contain class name", 
                  stackTrace.contains("RuntimeException"))
    }
    
    @Test
    fun `logException should log error with exception`() {
        // Given
        val tag = "TestTag"
        val message = "测试异常"
        val exception = RuntimeException("测试异常")
        
        // When
        LogManager.logException(tag, message, exception)
        
        // Then
        val entry = testLogProvider.entries.last()
        assertEquals("E", entry.level)
        assertEquals(fullTag(tag), entry.tag)
        assertTrue(entry.message.contains(message))
        assertEquals(exception, entry.throwable)
    }

    private class TestLogProvider : LogManager.LogProvider {
        val entries = mutableListOf<LogEntry>()

        override fun v(tag: String, message: String) {
            entries.add(LogEntry("V", tag, message, null))
        }

        override fun v(tag: String, message: String, throwable: Throwable) {
            entries.add(LogEntry("V", tag, message, throwable))
        }

        override fun d(tag: String, message: String) {
            entries.add(LogEntry("D", tag, message, null))
        }

        override fun d(tag: String, message: String, throwable: Throwable) {
            entries.add(LogEntry("D", tag, message, throwable))
        }

        override fun i(tag: String, message: String) {
            entries.add(LogEntry("I", tag, message, null))
        }

        override fun i(tag: String, message: String, throwable: Throwable) {
            entries.add(LogEntry("I", tag, message, throwable))
        }

        override fun w(tag: String, message: String) {
            entries.add(LogEntry("W", tag, message, null))
        }

        override fun w(tag: String, message: String, throwable: Throwable) {
            entries.add(LogEntry("W", tag, message, throwable))
        }

        override fun e(tag: String, message: String) {
            entries.add(LogEntry("E", tag, message, null))
        }

        override fun e(tag: String, message: String, throwable: Throwable) {
            entries.add(LogEntry("E", tag, message, throwable))
        }

        override fun getStackTraceString(throwable: Throwable): String {
            return "${throwable.javaClass.simpleName}: ${throwable.message}"
        }
    }

    private data class LogEntry(
        val level: String,
        val tag: String,
        val message: String,
        val throwable: Throwable?
    )
}
