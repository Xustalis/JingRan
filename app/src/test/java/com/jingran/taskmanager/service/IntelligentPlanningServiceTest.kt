package com.jingran.taskmanager.service

import com.jingran.taskmanager.data.entity.ShortTermTask
import com.jingran.taskmanager.data.entity.TaskPriority
import com.jingran.taskmanager.data.entity.TaskType
import com.jingran.taskmanager.data.entity.EnergyLevel
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.*
import java.time.LocalDateTime
import java.time.ZoneId

class IntelligentPlanningServiceTest {

    private lateinit var planningService: IntelligentPlanningService
    
    @Before
    fun setup() {
        planningService = IntelligentPlanningService()
    }
    
    @Test
    fun `generateTodayPlan should return empty list for empty input`() {
        // Given
        val emptyTasks = emptyList<ShortTermTask>()
        
        // When
        val result = planningService.generateTodayPlan(emptyTasks)
        
        // Then
        assertTrue("Result should be empty for empty input", result.isEmpty())
    }
    
    @Test
    fun `generateTodayPlan should prioritize high priority tasks`() {
        // Given
        val highPriorityTask = createTestTask(
            id = 1L,
            title = "高优先级任务",
            priority = TaskPriority.HIGH,
            duration = 60
        )
        val lowPriorityTask = createTestTask(
            id = 2L,
            title = "低优先级任务",
            priority = TaskPriority.LOW,
            duration = 30
        )
        val tasks = listOf(lowPriorityTask, highPriorityTask)
        
        // When
        val result = planningService.generateTodayPlan(tasks)
        
        // Then
        assertTrue("High priority task should come first", 
                  result.indexOf(highPriorityTask) < result.indexOf(lowPriorityTask))
    }
    
    @Test
    fun `generateTodayPlan should consider deadline urgency`() {
        // Given
        val urgentTask = createTestTask(
            id = 1L,
            title = "紧急任务",
            priority = TaskPriority.MEDIUM,
            deadline = System.currentTimeMillis() + 3600000 // 1小时后
        )
        val normalTask = createTestTask(
            id = 2L,
            title = "普通任务",
            priority = TaskPriority.MEDIUM,
            deadline = System.currentTimeMillis() + 86400000 // 1天后
        )
        val tasks = listOf(normalTask, urgentTask)
        
        // When
        val result = planningService.generateTodayPlan(tasks)
        
        // Then
        assertTrue("Urgent task should come first", 
                  result.indexOf(urgentTask) < result.indexOf(normalTask))
    }
    
    @Test
    fun `generateOptimalPlan should balance workload throughout day`() {
        // Given
        val longTask = createTestTask(
            id = 1L,
            title = "长任务",
            duration = 180, // 3小时
            energyLevel = EnergyLevel.HIGH
        )
        val shortTask1 = createTestTask(
            id = 2L,
            title = "短任务1",
            duration = 30,
            energyLevel = EnergyLevel.LOW
        )
        val shortTask2 = createTestTask(
            id = 3L,
            title = "短任务2",
            duration = 45,
            energyLevel = EnergyLevel.MEDIUM
        )
        val tasks = listOf(longTask, shortTask1, shortTask2)
        
        // When
        val result = planningService.generateOptimalPlan(tasks)
        
        // Then
        assertFalse("Result should not be empty", result.isEmpty())
        assertEquals("All tasks should be included", tasks.size, result.size)
    }
    
    @Test
    fun `generateOptimalPlan should consider energy levels`() {
        // Given
        val highEnergyTask = createTestTask(
            id = 1L,
            title = "高能量任务",
            energyLevel = EnergyLevel.HIGH,
            duration = 120
        )
        val lowEnergyTask = createTestTask(
            id = 2L,
            title = "低能量任务",
            energyLevel = EnergyLevel.LOW,
            duration = 30
        )
        val tasks = listOf(lowEnergyTask, highEnergyTask)
        
        // When
        val result = planningService.generateOptimalPlan(tasks)
        
        // Then
        // 高能量任务应该安排在一天的早些时候（假设用户早上精力充沛）
        assertTrue("High energy task should be scheduled earlier", 
                  result.indexOf(highEnergyTask) <= result.indexOf(lowEnergyTask))
    }
    
    @Test
    fun `calculateTaskScore should return higher score for high priority tasks`() {
        // Given
        val highPriorityTask = createTestTask(
            priority = TaskPriority.HIGH,
            deadline = System.currentTimeMillis() + 86400000
        )
        val lowPriorityTask = createTestTask(
            priority = TaskPriority.LOW,
            deadline = System.currentTimeMillis() + 86400000
        )
        
        // When
        val highScore = planningService.calculateTaskScore(highPriorityTask)
        val lowScore = planningService.calculateTaskScore(lowPriorityTask)
        
        // Then
        assertTrue("High priority task should have higher score", 
                  highScore > lowScore)
    }
    
    @Test
    fun `calculateTaskScore should consider deadline urgency`() {
        // Given
        val urgentTask = createTestTask(
            priority = TaskPriority.MEDIUM,
            deadline = System.currentTimeMillis() + 3600000 // 1小时后
        )
        val normalTask = createTestTask(
            priority = TaskPriority.MEDIUM,
            deadline = System.currentTimeMillis() + 86400000 // 1天后
        )
        
        // When
        val urgentScore = planningService.calculateTaskScore(urgentTask)
        val normalScore = planningService.calculateTaskScore(normalTask)
        
        // Then
        assertTrue("Urgent task should have higher score", 
                  urgentScore > normalScore)
    }
    
    @Test
    fun `optimizeTaskOrder should not lose any tasks`() {
        // Given
        val tasks = listOf(
            createTestTask(id = 1L, title = "任务1"),
            createTestTask(id = 2L, title = "任务2"),
            createTestTask(id = 3L, title = "任务3")
        )
        
        // When
        val result = planningService.optimizeTaskOrder(tasks)
        
        // Then
        assertEquals("Should not lose any tasks", tasks.size, result.size)
        tasks.forEach { task ->
            assertTrue("Task ${task.title} should be in result", 
                      result.contains(task))
        }
    }
    
    @Test
    fun `generateTimeSlots should create appropriate time slots`() {
        // Given
        val tasks = listOf(
            createTestTask(id = 1L, duration = 60),
            createTestTask(id = 2L, duration = 90),
            createTestTask(id = 3L, duration = 30)
        )
        
        // When
        val timeSlots = planningService.generateTimeSlots(tasks)
        
        // Then
        assertFalse("Time slots should not be empty", timeSlots.isEmpty())
        assertEquals("Should have time slot for each task", tasks.size, timeSlots.size)
        
        // 验证时间槽的总时长
        val totalDuration = timeSlots.sumOf { it.durationMinutes }
        val expectedDuration = tasks.sumOf { it.duration }
        assertEquals("Total duration should match", expectedDuration, totalDuration)
    }
    
    @Test
    fun `isTaskSuitableForTimeSlot should consider energy levels`() {
        // Given
        val morningSlot = IntelligentPlanningService.TimeSlot(
            startTime = LocalDateTime.now().withHour(9).withMinute(0),
            durationMinutes = 60,
            energyLevel = EnergyLevel.HIGH
        )
        val eveningSlot = IntelligentPlanningService.TimeSlot(
            startTime = LocalDateTime.now().withHour(18).withMinute(0),
            durationMinutes = 60,
            energyLevel = EnergyLevel.LOW
        )
        val highEnergyTask = createTestTask(energyLevel = EnergyLevel.HIGH)
        val lowEnergyTask = createTestTask(energyLevel = EnergyLevel.LOW)
        
        // When & Then
        assertTrue("High energy task should be suitable for morning", 
                  planningService.isTaskSuitableForTimeSlot(highEnergyTask, morningSlot))
        assertTrue("Low energy task should be suitable for evening", 
                  planningService.isTaskSuitableForTimeSlot(lowEnergyTask, eveningSlot))
    }
    
    private fun createTestTask(
        id: Long = 1L,
        title: String = "测试任务",
        priority: TaskPriority = TaskPriority.MEDIUM,
        taskType: TaskType = TaskType.NORMAL,
        energyLevel: EnergyLevel = EnergyLevel.MEDIUM,
        duration: Int = 60,
        deadline: Long? = System.currentTimeMillis() + 86400000,
        isCompleted: Boolean = false
    ): ShortTermTask {
        return ShortTermTask(
            id = id,
            title = title,
            description = "测试描述",
            deadline = deadline,
            duration = duration,
            priority = priority,
            taskType = taskType,
            isFlexible = true,
            actualDuration = null,
            isCompleted = isCompleted,
            completedTime = null,
            createTime = System.currentTimeMillis(),
            reminderTime = null,
            tags = null,
            energyLevel = energyLevel,
            location = null,
            context = null,
            estimatedStartTime = null,
            parentLongTermTaskId = null,
            lastModifiedTime = System.currentTimeMillis()
        )
    }
}