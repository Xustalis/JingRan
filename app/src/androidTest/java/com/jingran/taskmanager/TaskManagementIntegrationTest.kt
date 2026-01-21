package com.jingran.taskmanager

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jingran.taskmanager.data.database.TaskDatabase
import com.jingran.taskmanager.data.entity.ShortTermTask
import com.jingran.taskmanager.data.entity.LongTermTask
import com.jingran.taskmanager.data.entity.TaskPriority
import com.jingran.taskmanager.data.entity.TaskType
import com.jingran.taskmanager.data.entity.EnergyLevel
import com.jingran.taskmanager.data.repository.TaskRepository
import com.jingran.taskmanager.service.IntelligentPlanningService
import com.jingran.taskmanager.service.SubTaskManager
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class TaskManagementIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var database: TaskDatabase
    private lateinit var repository: TaskRepository
    private lateinit var planningService: IntelligentPlanningService
    private lateinit var subTaskManager: SubTaskManager
    
    @Before
    fun setup() {
        // 创建内存数据库用于测试
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TaskDatabase::class.java
        ).allowMainThreadQueries().build()
        
        // 初始化组件
        repository = TaskRepository(
            database,
            database.shortTermTaskDao(),
            database.longTermTaskDao(),
            database.subTaskDao(),
            database.planItemDao(),
            database.fixedScheduleDao(),
            database.dailyStatsDao(),
            database.courseScheduleDao(),
            database.importRecordDao(),
            database.syncRecordDao(),
            database.backupRecordDao()
        )
        
        planningService = IntelligentPlanningService()
        subTaskManager = SubTaskManager(repository)
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun testCompleteTaskManagementFlow() = runTest {
        // 1. 创建短期任务
        val shortTermTask = ShortTermTask(
            title = "完成项目报告",
            description = "撰写季度项目总结报告",
            priority = TaskPriority.HIGH,
            taskType = TaskType.MEETING,
            energyLevel = EnergyLevel.HIGH,
            duration = 120,
            deadline = System.currentTimeMillis() + 86400000
        )
        
        val shortTermTaskId = repository.insertShortTermTask(shortTermTask)
        
        val savedTasks = database.shortTermTaskDao().getAllTasksSync()
        assertEquals("应该有一个任务", 1, savedTasks.size)
        assertEquals("任务标题应该匹配", shortTermTask.title, savedTasks[0].title)
        assertEquals("任务ID应该匹配", shortTermTaskId, savedTasks[0].id)
        
        // 4. 创建长期任务
        val longTermTask = LongTermTask(
            title = "学习新技术",
            goal = "掌握协程与Compose",
            description = "深入学习Kotlin协程和Jetpack Compose",
            priority = TaskPriority.MEDIUM
        )
        
        repository.insertLongTermTask(longTermTask)
        
        val savedLongTermTasks = database.longTermTaskDao().getAllTasksSync()
        assertEquals("应该有一个长期任务", 1, savedLongTermTasks.size)
        
        // 6. 生成智能规划
        val allShortTermTasks = database.shortTermTaskDao().getAllTasksSync()
        val todayPlan = planningService.generateTodayPlan(allShortTermTasks)
        assertFalse("今日计划不应为空", todayPlan.isEmpty())
        
        // 7. 更新任务状态
        val taskToUpdate = savedTasks[0].copy(isCompleted = true)
        repository.updateShortTermTask(taskToUpdate)
        
        // 8. 验证任务状态已更新
        val updatedTasks = database.shortTermTaskDao().getAllTasksSync()
        assertTrue("任务应该已完成", updatedTasks[0].isCompleted)
        
        // 9. 删除任务
        repository.deleteShortTermTask(updatedTasks[0])
        
        // 10. 验证任务已删除
        val finalTasks = database.shortTermTaskDao().getAllTasksSync()
        assertTrue("任务列表应该为空", finalTasks.isEmpty())
    }
    
    @Test
    fun testIntelligentPlanningIntegration() = runTest {
        // 创建多个不同优先级和类型的任务
        val tasks = listOf(
            createTestShortTermTask(
                title = "紧急会议",
                priority = TaskPriority.HIGH,
                energyLevel = EnergyLevel.HIGH,
                duration = 60,
                deadline = System.currentTimeMillis() + 3600000
            ),
            createTestShortTermTask(
                title = "代码审查",
                priority = TaskPriority.MEDIUM,
                energyLevel = EnergyLevel.MEDIUM,
                duration = 90,
                deadline = System.currentTimeMillis() + 86400000
            ),
            createTestShortTermTask(
                title = "整理文档",
                priority = TaskPriority.LOW,
                energyLevel = EnergyLevel.LOW,
                duration = 30,
                deadline = System.currentTimeMillis() + 2 * 86400000
            )
        )
        
        // 保存所有任务
        tasks.forEach { task ->
            repository.insertShortTermTask(task)
        }
        
        // 获取保存的任务
        val savedTasks = database.shortTermTaskDao().getAllTasksSync()
        assertEquals("应该有3个任务", 3, savedTasks.size)
        
        // 生成今日计划
        val todayPlan = planningService.generateTodayPlan(savedTasks)
        assertEquals("今日计划应包含所有任务", 3, todayPlan.size)
        
        // 验证高优先级任务排在前面
        val highPriorityTask = todayPlan.find { it.priority == TaskPriority.HIGH }
        val lowPriorityTask = todayPlan.find { it.priority == TaskPriority.LOW }
        assertNotNull("应该找到高优先级任务", highPriorityTask)
        assertNotNull("应该找到低优先级任务", lowPriorityTask)
        
        val highPriorityIndex = todayPlan.indexOf(highPriorityTask)
        val lowPriorityIndex = todayPlan.indexOf(lowPriorityTask)
        assertTrue("高优先级任务应该排在低优先级任务前面", 
                  highPriorityIndex < lowPriorityIndex)
        
        // 生成最优计划
        val optimalPlan = planningService.generateOptimalPlan(savedTasks)
        assertEquals("最优计划应包含所有任务", 3, optimalPlan.size)
    }
    
    @Test
    fun testSubTaskManagementIntegration() = runTest {
        // 创建主任务
        val mainTask = createTestLongTermTask(
            title = "开发新功能",
            goal = "交付用户管理模块",
            description = "开发用户管理模块"
        )
        
        val mainTaskId = repository.insertLongTermTask(mainTask)
        val savedMainTask = database.longTermTaskDao().getAllTasksSync().first { it.id == mainTaskId }
        
        val subTaskId = subTaskManager.createSubTask(
            longTermTaskId = savedMainTask.id,
            subTaskTitle = "完成用户管理接口",
            estimatedDuration = 45,
            priority = TaskPriority.HIGH
        )
        
        val savedSubTask = repository.getShortTermTaskById(subTaskId)
        val subTaskRelation = repository.getSubTaskByShortTaskId(subTaskId)
        
        assertNotNull("子任务应被创建", savedSubTask)
        assertNotNull("子任务关系应存在", subTaskRelation)
        assertEquals("子任务应关联到主任务", savedMainTask.id, subTaskRelation?.longTaskId)
        assertEquals("子任务时长应匹配", 45, savedSubTask?.duration)
        assertEquals("子任务类型应匹配", TaskType.SUBTASK, savedSubTask?.taskType)
    }
    
    @Test
    fun testErrorHandlingIntegration() = runTest {
        // 测试无效输入的处理
        val invalidTask = ShortTermTask(
            title = "",
            description = "测试描述",
            priority = TaskPriority.MEDIUM,
            taskType = TaskType.NORMAL,
            energyLevel = EnergyLevel.MEDIUM,
            duration = 60,
            deadline = System.currentTimeMillis() + 60000
        )
        
        try {
            repository.insertShortTermTask(invalidTask)
            fail("应该抛出验证异常")
        } catch (e: IllegalArgumentException) {
            assertTrue("异常消息应该包含字段名", e.message!!.contains("task.title"))
        }
    }
    
    private fun createTestShortTermTask(
        title: String = "测试任务",
        description: String = "测试描述",
        priority: TaskPriority = TaskPriority.MEDIUM,
        taskType: TaskType = TaskType.NORMAL,
        energyLevel: EnergyLevel = EnergyLevel.MEDIUM,
        duration: Int = 60,
        deadline: Long = System.currentTimeMillis() + 86400000
    ): ShortTermTask {
        return ShortTermTask(
            title = title,
            description = description,
            priority = priority,
            taskType = taskType,
            energyLevel = energyLevel,
            duration = duration,
            deadline = deadline
        )
    }
    
    private fun createTestLongTermTask(
        title: String = "长期测试任务",
        goal: String = "达成长期目标",
        description: String = "长期测试描述"
    ): LongTermTask {
        return LongTermTask(
            title = title,
            goal = goal,
            description = description,
            priority = TaskPriority.MEDIUM
        )
    }
}
