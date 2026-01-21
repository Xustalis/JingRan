package com.jingran.taskmanager.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.jingran.taskmanager.data.entity.ShortTermTask
import com.jingran.taskmanager.data.entity.LongTermTask
import com.jingran.taskmanager.data.entity.PlanItem
import com.jingran.taskmanager.data.entity.TaskPriority
import com.jingran.taskmanager.data.entity.TaskType
import com.jingran.taskmanager.data.entity.EnergyLevel
import com.jingran.taskmanager.data.repository.TaskRepository
import com.jingran.taskmanager.data.repository.ImportRepository
import com.jingran.taskmanager.service.PlanningService
import com.jingran.taskmanager.service.EnhancedPlanningService
import com.jingran.taskmanager.service.SubTaskManager
import com.jingran.utils.NotificationHelper
import com.jingran.taskmanager.viewmodel.TaskViewModelDependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.eq
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlinx.coroutines.runBlocking
import java.util.*

@ExperimentalCoroutinesApi
class TaskViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = UnconfinedTestDispatcher()
    
    @Mock
    private lateinit var repository: TaskRepository
    
    @Mock
    private lateinit var importRepository: ImportRepository
    
    @Mock
    private lateinit var planningService: PlanningService
    
    @Mock
    private lateinit var enhancedPlanningService: EnhancedPlanningService
    
    @Mock
    private lateinit var subTaskManager: SubTaskManager
    
    @Mock
    private lateinit var notificationHelper: NotificationHelper
    
    @Mock
    private lateinit var application: Application
    
    @Mock
    private lateinit var isLoadingObserver: Observer<Boolean>
    
    @Mock
    private lateinit var errorMessageObserver: Observer<String?>
    
    private lateinit var viewModel: TaskViewModel
    private lateinit var dependencies: TaskViewModelDependencies
    
    private val todayTimestamp = 1700000000000L
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        whenever(notificationHelper.setDefaultReminder(any())).thenAnswer { invocation ->
            invocation.arguments[0] as ShortTermTask
        }
        doNothing().whenever(notificationHelper).scheduleTaskReminder(any())
        doNothing().whenever(notificationHelper).cancelTaskReminder(any())
        
        whenever(planningService.getTodayStartTimestamp()).thenReturn(todayTimestamp)
        whenever(planningService.generateDailyPlan(eq(todayTimestamp), any())).thenReturn(emptyList())
        
        runBlocking {
            whenever(repository.getTasksForPlanning(any())).thenReturn(emptyList())
            whenever(repository.deletePlanItemsByDate(any())).thenReturn(Unit)
            whenever(repository.insertPlanItems(any())).thenReturn(Unit)
        }
        
        dependencies = TaskViewModelDependencies(
            repository = repository,
            importRepository = importRepository,
            planningService = planningService,
            enhancedPlanningService = enhancedPlanningService,
            subTaskManager = subTaskManager,
            notificationHelper = notificationHelper
        )
        
        viewModel = TaskViewModel(application, dependencies)
        
        viewModel.isLoading.observeForever(isLoadingObserver)
        viewModel.errorMessage.observeForever(errorMessageObserver)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        viewModel.isLoading.removeObserver(isLoadingObserver)
        viewModel.errorMessage.removeObserver(errorMessageObserver)
    }
    
    @Test
    fun `insertShortTermTask should set loading state and call repository`() = runTest {
        // Given
        val task = createTestShortTermTask()
        whenever(repository.insertShortTermTask(task)).thenReturn(task.id)
        
        // When
        viewModel.insertShortTermTask(task)
        advanceUntilIdle()
        
        // Then
        verify(isLoadingObserver, times(2)).onChanged(any()) // true, then false
        verify(repository).insertShortTermTask(task)
        verify(notificationHelper).scheduleTaskReminder(task)
    }
    
    @Test
    fun `insertShortTermTask should handle repository error`() = runTest {
        // Given
        val task = createTestShortTermTask()
        val errorMessage = "数据库错误"
        whenever(repository.insertShortTermTask(task)).thenThrow(RuntimeException(errorMessage))
        
        // When
        viewModel.insertShortTermTask(task)
        advanceUntilIdle()
        
        // Then
        verify(errorMessageObserver).onChanged(any())
        verify(isLoadingObserver, atLeast(1)).onChanged(false)
    }
    
    @Test
    fun `updateShortTermTask should call repository and notification helper`() = runTest {
        // Given
        val task = createTestShortTermTask()
        whenever(repository.updateShortTermTask(task)).thenReturn(Unit)
        
        // When
        viewModel.updateShortTermTask(task)
        advanceUntilIdle()
        
        // Then
        verify(repository).updateShortTermTask(task)
        verify(notificationHelper).cancelTaskReminder(task.id)
        verify(notificationHelper).scheduleTaskReminder(task)
    }
    
    @Test
    fun `deleteShortTermTask should call repository and cancel notification`() = runTest {
        // Given
        val task = createTestShortTermTask()
        whenever(repository.deleteShortTermTask(task)).thenReturn(Unit)
        
        // When
        viewModel.deleteShortTermTask(task)
        advanceUntilIdle()
        
        // Then
        verify(repository).deleteShortTermTask(task)
        verify(notificationHelper).cancelTaskReminder(task.id)
    }
    
    @Test
    fun `regenerateTodayPlan should use planning service result`() = runTest {
        val tasks = listOf(createTestShortTermTask())
        val planItems = listOf(
            PlanItem(
                id = 1L,
                taskId = tasks.first().id,
                planDate = todayTimestamp,
                startTime = todayTimestamp,
                endTime = todayTimestamp + 3_600_000
            )
        )
        whenever(repository.getTasksForPlanning(todayTimestamp)).thenReturn(tasks)
        whenever(planningService.generateDailyPlan(todayTimestamp, tasks)).thenReturn(planItems)
        whenever(repository.insertPlanItems(planItems)).thenReturn(Unit)
        
        viewModel.regenerateTodayPlan()
        advanceUntilIdle()
        
        verify(planningService).generateDailyPlan(todayTimestamp, tasks)
        verify(repository).deletePlanItemsByDate(todayTimestamp)
        verify(repository).insertPlanItems(planItems)
    }
    
    // 删除不存在的generateOptimalPlan测试方法
    
    @Test
    fun `loading state should be managed correctly during operations`() = runTest {
        // Given
        val task = createTestShortTermTask()
        whenever(repository.insertShortTermTask(task)).thenReturn(task.id)
        
        // When
        viewModel.insertShortTermTask(task)
        advanceUntilIdle()
        
        // Then
        val loadingCaptor = argumentCaptor<Boolean>()
        verify(isLoadingObserver, atLeast(2)).onChanged(loadingCaptor.capture())
        val loadingStates = loadingCaptor.allValues
        
        assertTrue("Loading should start with true", loadingStates.contains(true))
        assertTrue("Loading should end with false", loadingStates.contains(false))
    }
    
    private fun createTestShortTermTask(): ShortTermTask {
        return ShortTermTask(
            id = 1L,
            title = "测试任务",
            description = "测试描述",
            priority = TaskPriority.HIGH,
            taskType = TaskType.NORMAL,
            energyLevel = EnergyLevel.HIGH,
            duration = 60,
            deadline = System.currentTimeMillis() + 86400000, // 明天
            isCompleted = false
        )
    }
    
    private fun createTestLongTermTask(): LongTermTask {
        return LongTermTask(
            id = 1L,
            title = "长期测试任务",
            goal = "长期测试目标",
            description = "长期测试描述"
        )
    }
}
