package com.jingran.taskmanager.service

import com.jingran.taskmanager.data.entity.*
import com.jingran.taskmanager.data.repository.TaskRepository
import com.jingran.utils.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.kotlin.*
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AdvancedPlanningServiceTest {
    
    @Mock
    private lateinit var repository: TaskRepository
    
    private lateinit var planningService: AdvancedPlanningService
    
    private val testDate = System.currentTimeMillis()
    private val testTasks = createTestTasks()
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        planningService = AdvancedPlanningService(repository)
    }
    
    @After
    fun tearDown() {
        reset(repository)
    }
    
    @Test
    fun `test generateOptimizedPlan with valid tasks`() = runTest {
        val preferences = AdvancedPlanningService.UserPreferences(
            preferredWorkHours = listOf(Pair(9, 12), Pair(14, 18)),
            preferredTaskTypes = listOf(TaskType.LEARNING, TaskType.NORMAL),
            maxTasksPerDay = 8,
            breakFrequency = 2,
            energyAwareScheduling = true
        )
        
        val historicalData = AdvancedPlanningService.HistoricalData(
            averageCompletionRate = 0.85f,
            averageTaskDuration = mapOf(
                TaskType.LEARNING to 60,
                TaskType.NORMAL to 45
            ),
            peakProductivityHours = listOf(9, 10, 11, 14, 15, 16),
            taskTypeSuccessRates = mapOf(
                TaskType.LEARNING to 0.9f,
                TaskType.NORMAL to 0.8f
            )
        )
        
        whenever(repository.getShortTermTaskById(any())).thenReturn(testTasks[0])
        whenever(repository.getShortTermTaskById(any())).thenReturn(testTasks[1])
        
        val result = planningService.generateOptimizedPlan(
            date = testDate,
            tasks = testTasks,
            userPreferences = preferences,
            historicalData = historicalData
        )
        
        assertNotNull(result.planItems)
        assertTrue(result.planItems.size <= preferences.maxTasksPerDay)
        assertTrue(result.optimizationMetrics.totalUtilization > 0f)
        assertTrue(result.optimizationMetrics.totalUtilization <= 100f)
    }
    
    @Test
    fun `test generateOptimizedPlan with energy aware scheduling`() = runTest {
        val preferences = AdvancedPlanningService.UserPreferences(
            preferredWorkHours = listOf(Pair(9, 12), Pair(14, 18)),
            preferredTaskTypes = emptyList(),
            maxTasksPerDay = 8,
            breakFrequency = 2,
            energyAwareScheduling = true
        )
        
        val historicalData = AdvancedPlanningService.HistoricalData()
        
        val result = planningService.generateOptimizedPlan(
            date = testDate,
            tasks = testTasks,
            userPreferences = preferences,
            historicalData = historicalData
        )
        
        assertTrue(result.optimizationMetrics.energyEfficiency > 0f)
        assertTrue(result.optimizationMetrics.energyEfficiency <= 100f)
    }
    
    @Test
    fun `test generateOptimizedPlan with deadline driven tasks`() = runTest {
        val tasksWithDeadlines = testTasks.mapIndexed { index, task ->
            task.copy(deadline = testDate + (index + 1) * 60 * 60 * 1000L)
        }
        
        val preferences = AdvancedPlanningService.UserPreferences(
            preferredWorkHours = listOf(Pair(9, 12), Pair(14, 18)),
            preferredTaskTypes = emptyList(),
            maxTasksPerDay = 8,
            breakFrequency = 2,
            energyAwareScheduling = false
        )
        
        val historicalData = AdvancedPlanningService.HistoricalData()
        
        val result = planningService.generateOptimizedPlan(
            date = testDate,
            tasks = tasksWithDeadlines,
            userPreferences = preferences,
            historicalData = historicalData
        )
        
        assertTrue(result.optimizationMetrics.prioritySatisfaction > 0f)
        assertTrue(result.optimizationMetrics.prioritySatisfaction <= 100f)
    }
    
    @Test
    fun `test generateOptimizedPlan with overflow tasks`() = runTest {
        val preferences = AdvancedPlanningService.UserPreferences(
            preferredWorkHours = listOf(Pair(9, 12)),
            preferredTaskTypes = emptyList(),
            maxTasksPerDay = 3,
            breakFrequency = 1,
            energyAwareScheduling = false
        )
        
        val historicalData = AdvancedPlanningService.HistoricalData()
        
        val result = planningService.generateOptimizedPlan(
            date = testDate,
            tasks = testTasks,
            userPreferences = preferences,
            historicalData = historicalData
        )
        
        assertTrue(result.unscheduledTasks.size > 0)
        assertTrue(result.planItems.size <= preferences.maxTasksPerDay)
    }
    
    @Test
    fun `test calculateTaskScore with high priority task`() = runTest {
        val highPriorityTask = testTasks[0].copy(
            priority = TaskPriority.URGENT,
            energyLevel = EnergyLevel.HIGH,
            taskType = TaskType.LEARNING
        )
        
        val context = AdvancedPlanningService.PlanningContext(
            date = testDate,
            userPreferences = AdvancedPlanningService.UserPreferences(),
            historicalData = AdvancedPlanningService.HistoricalData(),
            currentEnergyLevel = EnergyLevel.HIGH,
            availableSlots = emptyList()
        )
        
        val score = planningService.calculateTaskScore(highPriorityTask, context)
        
        assertTrue(score.totalScore > 0.8f)
        assertTrue(score.priorityScore > 0.8f)
        assertTrue(score.urgencyScore > 0.8f)
    }
    
    @Test
    fun `test calculateTaskScore with low priority task`() = runTest {
        val lowPriorityTask = testTasks[0].copy(
            priority = TaskPriority.LOW,
            energyLevel = EnergyLevel.LOW,
            taskType = TaskType.PERSONAL
        )
        
        val context = AdvancedPlanningService.PlanningContext(
            date = testDate,
            userPreferences = AdvancedPlanningService.UserPreferences(),
            historicalData = AdvancedPlanningService.HistoricalData(),
            currentEnergyLevel = EnergyLevel.LOW,
            availableSlots = emptyList()
        )
        
        val score = planningService.calculateTaskScore(lowPriorityTask, context)
        
        assertTrue(score.totalScore < 0.5f)
        assertTrue(score.priorityScore < 0.5f)
    }
    
    @Test
    fun `test allocateTasksOptimally respects break frequency`() = runTest {
        val preferences = AdvancedPlanningService.UserPreferences(
            preferredWorkHours = listOf(Pair(9, 12)),
            preferredTaskTypes = emptyList(),
            maxTasksPerDay = 10,
            breakFrequency = 3,
            energyAwareScheduling = false
        )
        
        val context = AdvancedPlanningService.PlanningContext(
            date = testDate,
            userPreferences = preferences,
            historicalData = AdvancedPlanningService.HistoricalData(),
            currentEnergyLevel = EnergyLevel.MEDIUM,
            availableSlots = createTestTimeSlots()
        )
        
        val (plannedTasks, unscheduledTasks) = planningService.allocateTasksOptimally(
            testTasks,
            context.availableSlots,
            context
        )
        
        val breakCount = countBreaks(plannedTasks)
        assertTrue(breakCount >= plannedTasks.size / preferences.breakFrequency - 1)
    }
    
    @Test
    fun `test calculateOptimizationMetrics returns valid metrics`() = runTest {
        val plannedItems = createTestPlanItems(5)
        val unscheduledTasks = listOf(testTasks[0])
        
        val context = AdvancedPlanningService.PlanningContext(
            date = testDate,
            userPreferences = AdvancedPlanningService.UserPreferences(),
            historicalData = AdvancedPlanningService.HistoricalData(),
            currentEnergyLevel = EnergyLevel.MEDIUM,
            availableSlots = createTestTimeSlots()
        )
        
        val metrics = planningService.calculateOptimizationMetrics(
            plannedItems,
            unscheduledTasks,
            context
        )
        
        assertTrue(metrics.totalUtilization >= 0f)
        assertTrue(metrics.totalUtilization <= 100f)
        assertTrue(metrics.energyEfficiency >= 0f)
        assertTrue(metrics.energyEfficiency <= 100f)
        assertTrue(metrics.prioritySatisfaction >= 0f)
        assertTrue(metrics.prioritySatisfaction <= 100f)
    }
    
    @Test
    fun `test analyzePlanAdjustments generates valid adjustments`() = runTest {
        val plannedItems = createTestPlanItems(3)
        
        val context = AdvancedPlanningService.PlanningContext(
            date = testDate,
            userPreferences = AdvancedPlanningService.UserPreferences(
                energyAwareScheduling = true
            ),
            historicalData = AdvancedPlanningService.HistoricalData(),
            currentEnergyLevel = EnergyLevel.HIGH,
            availableSlots = createTestTimeSlots()
        )
        
        val adjustments = planningService.analyzePlanAdjustments(plannedItems, context)
        
        adjustments.forEach { adjustment ->
            assertNotNull(adjustment.taskId)
            assertNotNull(adjustment.originalSlot)
            assertNotNull(adjustment.adjustedSlot)
            assertNotNull(adjustment.reason)
            assertTrue(adjustment.impact.name.isNotEmpty())
        }
    }
    
    private fun createTestTasks(): List<ShortTermTask> {
        return listOf(
            ShortTermTask(
                id = 1,
                title = "完成数学作业",
                description = "完成第三章习题",
                duration = 60,
                priority = TaskPriority.URGENT,
                energyLevel = EnergyLevel.HIGH,
                taskType = TaskType.LEARNING,
                deadline = testDate + 2 * 60 * 60 * 1000L,
                createTime = testDate - 24 * 60 * 60 * 1000L,
                lastModifiedTime = testDate - 24 * 60 * 60 * 1000L
            ),
            ShortTermTask(
                id = 2,
                title = "准备英语演讲",
                description = "准备下周的英语演讲",
                duration = 45,
                priority = TaskPriority.HIGH,
                energyLevel = EnergyLevel.MEDIUM,
                taskType = TaskType.LEARNING,
                deadline = testDate + 24 * 60 * 60 * 1000L,
                createTime = testDate - 12 * 60 * 60 * 1000L,
                lastModifiedTime = testDate - 12 * 60 * 60 * 1000L
            ),
            ShortTermTask(
                id = 3,
                title = "锻炼身体",
                description = "跑步30分钟",
                duration = 30,
                priority = TaskPriority.MEDIUM,
                energyLevel = EnergyLevel.LOW,
                taskType = TaskType.EXERCISE,
                deadline = null,
                createTime = testDate - 6 * 60 * 60 * 1000L,
                lastModifiedTime = testDate - 6 * 60 * 60 * 1000L
            ),
            ShortTermTask(
                id = 4,
                title = "阅读书籍",
                description = "阅读技术书籍",
                duration = 90,
                priority = TaskPriority.LOW,
                energyLevel = EnergyLevel.MEDIUM,
                taskType = TaskType.PERSONAL,
                deadline = testDate + 48 * 60 * 60 * 1000L,
                createTime = testDate - 48 * 60 * 60 * 1000L,
                lastModifiedTime = testDate - 48 * 60 * 60 * 1000L
            ),
            ShortTermTask(
                id = 5,
                title = "整理房间",
                description = "整理书桌和房间",
                duration = 30,
                priority = TaskPriority.LOW,
                energyLevel = EnergyLevel.LOW,
                taskType = TaskType.ROUTINE,
                deadline = null,
                createTime = testDate - 24 * 60 * 60 * 1000L,
                lastModifiedTime = testDate - 24 * 60 * 60 * 1000L
            )
        )
    }
    
    private fun createTestTimeSlots(): List<AdvancedPlanningService.TimeSlot> {
        val dayStart = getDayStart(testDate)
        
        return listOf(
            AdvancedPlanningService.TimeSlot(dayStart, dayStart + 3 * 60 * 60 * 1000L),
            AdvancedPlanningService.TimeSlot(dayStart + 3 * 60 * 60 * 1000L, dayStart + 6 * 60 * 60 * 1000L),
            AdvancedPlanningService.TimeSlot(dayStart + 6 * 60 * 60 * 1000L, dayStart + 9 * 60 * 60 * 1000L),
            AdvancedPlanningService.TimeSlot(dayStart + 9 * 60 * 60 * 1000L, dayStart + 12 * 60 * 60 * 1000L),
            AdvancedPlanningService.TimeSlot(dayStart + 12 * 60 * 60 * 1000L, dayStart + 15 * 60 * 60 * 1000L),
            AdvancedPlanningService.TimeSlot(dayStart + 15 * 60 * 60 * 1000L, dayStart + 18 * 60 * 60 * 1000L),
            AdvancedPlanningService.TimeSlot(dayStart + 18 * 60 * 60 * 1000L, dayStart + 21 * 60 * 60 * 1000L),
            AdvancedPlanningService.TimeSlot(dayStart + 21 * 60 * 60 * 1000L, dayStart + 24 * 60 * 60 * 1000L)
        )
    }
    
    private fun createTestPlanItems(count: Int): List<PlanItem> {
        val dayStart = getDayStart(testDate)
        
        return (1..count).map { index ->
            val startTime = dayStart + (index * 2) * 60 * 60 * 1000L
            val endTime = startTime + 60 * 60 * 1000L
            
            PlanItem(
                taskId = index.toLong(),
                planDate = testDate,
                startTime = startTime,
                endTime = endTime
            )
        }
    }
    
    private fun countBreaks(planItems: List<PlanItem>): Int {
        var breakCount = 0
        
        for (i in 0 until planItems.size - 1) {
            val currentEnd = planItems[i].endTime
            val nextStart = planItems[i + 1].startTime
            val gap = nextStart - currentEnd
            
            if (gap > 15 * 60 * 1000L) {
                breakCount++
            }
        }
        
        return breakCount
    }
    
    private fun getDayStart(date: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}