package com.jingran.taskmanager.data.entity

import androidx.room.TypeConverter
import com.jingran.taskmanager.data.entity.TaskPriority
import com.jingran.taskmanager.data.entity.TaskType
import com.jingran.taskmanager.data.entity.EnergyLevel
import com.jingran.taskmanager.data.entity.ExecutionFrequency
import com.jingran.taskmanager.data.entity.LongTermCategory
import com.jingran.taskmanager.data.entity.RecurrenceType
import com.jingran.taskmanager.data.entity.ScheduleType
import com.jingran.taskmanager.data.entity.ImportSource
import com.jingran.taskmanager.data.entity.CourseType
import com.jingran.taskmanager.data.entity.ImportStatus

/**
 * Room数据库类型转换器
 * 用于将枚举类型转换为数据库可存储的基本类型
 */
class TaskTypeConverters {
    
    @TypeConverter
    fun fromTaskPriority(priority: TaskPriority): String {
        return priority.value
    }
    
    @TypeConverter
    fun toTaskPriority(value: String): TaskPriority {
        return TaskPriority.values().find { it.value == value } ?: TaskPriority.MEDIUM
    }
    
    @TypeConverter
    fun fromTaskType(taskType: TaskType): String {
        return taskType.value
    }
    
    @TypeConverter
    fun toTaskType(value: String): TaskType {
        return TaskType.values().find { it.value == value } ?: TaskType.NORMAL
    }
    
    @TypeConverter
    fun fromEnergyLevel(energyLevel: EnergyLevel): String {
        return energyLevel.value
    }
    
    @TypeConverter
    fun toEnergyLevel(value: String): EnergyLevel {
        return EnergyLevel.values().find { it.value == value } ?: EnergyLevel.MEDIUM
    }
    
    @TypeConverter
    fun fromExecutionFrequency(frequency: ExecutionFrequency): String {
        return frequency.value
    }
    
    @TypeConverter
    fun toExecutionFrequency(value: String): ExecutionFrequency {
        return ExecutionFrequency.values().find { it.value == value } ?: ExecutionFrequency.DAILY
    }
    
    @TypeConverter
    fun fromLongTermCategory(category: LongTermCategory): String {
        return category.value
    }
    
    @TypeConverter
    fun toLongTermCategory(value: String): LongTermCategory {
        return LongTermCategory.values().find { it.value == value } ?: LongTermCategory.PERSONAL
    }
    
    @TypeConverter
    fun fromRecurrenceType(recurrenceType: RecurrenceType): String {
        return recurrenceType.value
    }
    
    @TypeConverter
    fun toRecurrenceType(value: String): RecurrenceType {
        return RecurrenceType.values().find { it.value == value } ?: RecurrenceType.WEEKLY
    }
    
    @TypeConverter
    fun fromScheduleType(scheduleType: ScheduleType): String {
        return scheduleType.value
    }
    
    @TypeConverter
    fun toScheduleType(value: String): ScheduleType {
        return ScheduleType.values().find { it.value == value } ?: ScheduleType.OTHER
    }
    
    @TypeConverter
    fun fromImportSource(importSource: ImportSource): String {
        return importSource.value
    }
    
    @TypeConverter
    fun toImportSource(value: String): ImportSource {
        return ImportSource.values().find { it.value == value } ?: ImportSource.MANUAL
    }
    
    @TypeConverter
    fun fromCourseType(courseType: CourseType): String {
        return courseType.value
    }
    
    @TypeConverter
    fun toCourseType(value: String): CourseType {
        return CourseType.values().find { it.value == value } ?: CourseType.OTHER
    }
    
    @TypeConverter
    fun fromImportStatus(importStatus: ImportStatus): String {
        return importStatus.value
    }
    
    @TypeConverter
    fun toImportStatus(value: String): ImportStatus {
        return ImportStatus.values().find { it.value == value } ?: ImportStatus.PENDING
    }
}