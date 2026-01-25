package com.jingran.taskmanager.di

import android.app.Application
import com.jingran.taskmanager.data.database.TaskDatabase
import com.jingran.taskmanager.data.dao.*
import com.jingran.taskmanager.data.repository.*

object DependencyInjectionModule {
    
    private var database: TaskDatabase? = null
    private var repositories: Repositories? = null
    
    fun initialize(application: Application) {
        database = TaskDatabase.getDatabase(application)
        repositories = Repositories(
            database!!,
            database!!.shortTermTaskDao(),
            database!!.longTermTaskDao(),
            database!!.subTaskDao(),
            database!!.planItemDao(),
            database!!.fixedScheduleDao(),
            database!!.dailyStatsDao(),
            database!!.courseScheduleDao(),
            database!!.importRecordDao(),
            database!!.syncRecordDao(),
            database!!.backupRecordDao()
        )
    }
    
    fun getDatabase(): TaskDatabase {
        return database ?: throw IllegalStateException("DependencyInjectionModule not initialized")
    }
    
    fun getRepositories(): Repositories {
        return repositories ?: throw IllegalStateException("DependencyInjectionModule not initialized")
    }
    
    fun getTaskRepository(): TaskRepository {
        return getRepositories().taskRepository
    }
    
    fun getShortTermTaskRepository(): ShortTermTaskRepository {
        return getRepositories().shortTermTaskRepository
    }
    
    fun getLongTermTaskRepository(): LongTermTaskRepository {
        return getRepositories().longTermTaskRepository
    }
    
    fun getPlanRepository(): PlanRepository {
        return getRepositories().planRepository
    }
    
    fun getScheduleRepository(): ScheduleRepository {
        return getRepositories().scheduleRepository
    }
    
    fun getCourseRepository(): CourseRepository {
        return getRepositories().courseRepository
    }
    
    fun getStatsRepository(): StatsRepository {
        return getRepositories().statsRepository
    }
    
    fun getImportRepository(): ImportRepository {
        return getRepositories().importRepository
    }
    
    fun getSubTaskRepository(): SubTaskRepository {
        return getRepositories().subTaskRepository
    }
}