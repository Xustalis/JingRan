package com.jingran.taskmanager.data.repository

import com.jingran.taskmanager.data.database.TaskDatabase
import com.jingran.taskmanager.data.dao.*

data class Repositories(
    val database: TaskDatabase,
    val shortTermTaskDao: ShortTermTaskDao,
    val longTermTaskDao: LongTermTaskDao,
    val subTaskDao: SubTaskDao,
    val planItemDao: PlanItemDao,
    val fixedScheduleDao: FixedScheduleDao,
    val dailyStatsDao: DailyStatsDao,
    val courseScheduleDao: CourseScheduleDao,
    val importRecordDao: ImportRecordDao,
    val syncRecordDao: SyncRecordDao,
    val backupRecordDao: BackupRecordDao
) {
    val taskRepository: TaskRepository by lazy {
        TaskRepository(
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
    
    val shortTermTaskRepository: ShortTermTaskRepository by lazy {
        ShortTermTaskRepository(shortTermTaskDao)
    }
    
    val longTermTaskRepository: LongTermTaskRepository by lazy {
        LongTermTaskRepository(longTermTaskDao)
    }
    
    val subTaskRepository: SubTaskRepository by lazy {
        SubTaskRepository(subTaskDao, planItemDao)
    }
    
    val planRepository: PlanRepository by lazy {
        PlanRepository(planItemDao)
    }
    
    val scheduleRepository: ScheduleRepository by lazy {
        ScheduleRepository(fixedScheduleDao)
    }
    
    val courseRepository: CourseRepository by lazy {
        CourseRepository(courseScheduleDao)
    }
    
    val statsRepository: StatsRepository by lazy {
        StatsRepository(dailyStatsDao)
    }
    
    val importRepository: ImportRepository by lazy {
        ImportRepository(importRecordDao)
    }
}