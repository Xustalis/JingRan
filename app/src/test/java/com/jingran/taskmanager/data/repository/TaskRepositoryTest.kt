package com.jingran.taskmanager.data.repository

import com.jingran.taskmanager.data.dao.*
import com.jingran.taskmanager.data.database.TaskDatabase
import com.jingran.taskmanager.data.entity.ShortTermTask
import com.jingran.taskmanager.data.entity.LongTermTask
import com.jingran.taskmanager.data.entity.TaskPriority
import com.jingran.taskmanager.data.entity.TaskType
import com.jingran.taskmanager.data.entity.EnergyLevel
import kotlinx.coroutines.test.runTest
import androidx.lifecycle.MutableLiveData
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.*

class TaskRepositoryTest {

    @Mock
    private lateinit var database: TaskDatabase
    
    @Mock
    private lateinit var shortTermTaskDao: ShortTermTaskDao
    
    @Mock
    private lateinit var longTermTaskDao: LongTermTaskDao
    
    @Mock
    private lateinit var subTaskDao: SubTaskDao
    
    @Mock
    private lateinit var planItemDao: PlanItemDao
    
    @Mock
    private lateinit var fixedScheduleDao: FixedScheduleDao
    
    @Mock
    private lateinit var dailyStatsDao: DailyStatsDao
    
    @Mock
    private lateinit var courseScheduleDao: CourseScheduleDao
    
    @Mock
    private lateinit var importRecordDao: ImportRecordDao
    
    @Mock
    private lateinit var syncRecordDao: SyncRecordDao
    
    @Mock
    private lateinit var backupRecordDao: BackupRecordDao
    
    private lateinit var repository: TaskRepository
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = TaskRepository(
            database,
            shortTermTaskDao,
            longTermTaskDao,
            subTaskDao,
            planItemDao,
            fixedScheduleDao,
            dailyStatsDao,
            courseScheduleDao,
            importRecordDao,
            syncRecordDao,
            backupRecordDao
        )
    }
    
    @Test
    fun `insertShortTermTask should call dao insert method`() = runTest {
        // Given
        val task = ShortTermTask(
            id = 1L,
            title = "测试任务",
            description = "测试描述",
            priority = TaskPriority.HIGH,
            taskType = TaskType.NORMAL,
            energyLevel = EnergyLevel.HIGH,
            duration = 60,
            deadline = System.currentTimeMillis() + 86400000,
            isCompleted = false
        )
        
        // When
        repository.insertShortTermTask(task)
        
        // Then
        verify(shortTermTaskDao).insertTask(task)
    }
    
    @Test
    fun `updateShortTermTask should call dao update method`() = runTest {
        // Given
        val task = ShortTermTask(
            id = 1L,
            title = "更新任务",
            description = "更新描述",
            priority = TaskPriority.MEDIUM,
            taskType = TaskType.PERSONAL,
            energyLevel = EnergyLevel.MEDIUM,
            duration = 30,
            deadline = System.currentTimeMillis() + 86400000,
            isCompleted = true
        )
        
        // When
        repository.updateShortTermTask(task)
        
        // Then
        verify(shortTermTaskDao).updateTask(task)
    }
    
    @Test
    fun `deleteShortTermTask should call dao delete method`() = runTest {
        // Given
        val task = ShortTermTask(
            id = 1L,
            title = "删除任务",
            description = "删除描述",
            priority = TaskPriority.LOW,
            taskType = TaskType.LEARNING,
            energyLevel = EnergyLevel.LOW,
            duration = 15,
            deadline = System.currentTimeMillis() + 86400000,
            isCompleted = false
        )
        
        // When
        repository.deleteShortTermTask(task)
        
        // Then
        verify(shortTermTaskDao).deleteTask(task)
    }
    
    @Test
    fun `getAllShortTermTasks should return dao result`() = runTest {
        // Given
        val expectedTasks = listOf(
            ShortTermTask(
                id = 1L,
                title = "任务1",
                description = "描述1",
                priority = TaskPriority.HIGH,
                taskType = TaskType.NORMAL,
                energyLevel = EnergyLevel.HIGH,
                duration = 60,
                deadline = System.currentTimeMillis() + 86400000,
                isCompleted = false
            )
        )
        val liveData = MutableLiveData(expectedTasks)
        whenever(shortTermTaskDao.getAllTasks()).thenReturn(liveData)
        
        // When
        val result = repository.getAllShortTermTasks()
        
        // Then
        assertEquals(liveData, result)
        verify(shortTermTaskDao).getAllTasks()
    }
    
    @Test
    fun `insertLongTermTask should call dao insert method`() = runTest {
        // Given
        val task = LongTermTask(
            id = 1L,
            title = "长期任务",
            goal = "长期目标",
            description = "长期描述",
            targetDeadline = System.currentTimeMillis() + 86400000,
            isCompleted = false
        )
        
        // When
        repository.insertLongTermTask(task)
        
        // Then
        verify(longTermTaskDao).insertTask(task)
    }
    

}
