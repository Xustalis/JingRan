package com.jingran.taskmanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * 固定日程实体
 * 根据PRD文档要求，支持课程表、会议等固定时间安排的管理
 * 支持从Excel和图片导入课程表功能
 */
@Entity(tableName = "fixed_schedules")
data class FixedSchedule(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    
    @ColumnInfo(name = "title")
    val title: String, // 日程标题（如课程名、会议名）
    
    @ColumnInfo(name = "location")
    val location: String? = null, // 地点（可选）
    
    @ColumnInfo(name = "start_time")
    val startTime: Long, // 开始时间（时间戳）
    
    @ColumnInfo(name = "end_time")
    val endTime: Long, // 结束时间（时间戳）
    
    @ColumnInfo(name = "day_of_week")
    val dayOfWeek: Int, // 星期几（1-7，1为周一）
    
    @ColumnInfo(name = "recurrence_type")
    val recurrenceType: RecurrenceType = RecurrenceType.WEEKLY, // 重复类型
    
    @ColumnInfo(name = "recurrence_end_date")
    val recurrenceEndDate: Long? = null, // 重复结束日期（可选）
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true, // 是否激活
    
    @ColumnInfo(name = "create_time")
    val createTime: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "description")
    val description: String? = null, // 描述（可选）
    
    @ColumnInfo(name = "schedule_type")
    val scheduleType: ScheduleType = ScheduleType.COURSE, // 日程类型
    
    @ColumnInfo(name = "instructor_or_organizer")
    val instructorOrOrganizer: String? = null, // 教师或组织者
    
    @ColumnInfo(name = "course_code")
    val courseCode: String? = null, // 课程代码
    
    @ColumnInfo(name = "semester")
    val semester: String? = null, // 学期
    
    @ColumnInfo(name = "credits")
    val credits: Float? = null, // 学分
    
    @ColumnInfo(name = "color")
    val color: String? = null, // 显示颜色（十六进制）
    
    @ColumnInfo(name = "reminder_minutes")
    val reminderMinutes: Int = 15, // 提前提醒分钟数
    
    @ColumnInfo(name = "is_mandatory")
    val isMandatory: Boolean = true, // 是否必须参加
    
    @ColumnInfo(name = "import_source")
    val importSource: ImportSource = ImportSource.MANUAL, // 导入来源
    
    @ColumnInfo(name = "external_id")
    val externalId: String? = null, // 外部系统ID（用于同步）
    
    @ColumnInfo(name = "last_modified_time")
    val lastModifiedTime: Long = System.currentTimeMillis() // 最后修改时间
)

/**
 * 重复类型枚举
 * 支持多种重复模式的固定日程
 */
enum class RecurrenceType(val value: String, val description: String) {
    NONE("none", "不重复"),
    DAILY("daily", "每日"),
    WEEKLY("weekly", "每周"),
    BIWEEKLY("biweekly", "双周"),
    MONTHLY("monthly", "每月"),
    CUSTOM("custom", "自定义")
}

/**
 * 日程类型枚举
 * 区分不同类型的固定日程
 */
enum class ScheduleType(val value: String, val displayName: String) {
    COURSE("course", "课程"),
    MEETING("meeting", "会议"),
    APPOINTMENT("appointment", "预约"),
    EVENT("event", "活动"),
    BREAK("break", "休息"),
    EXAM("exam", "考试"),
    OFFICE_HOURS("office_hours", "办公时间"),
    OTHER("other", "其他"),
    WEEKLY("weekly", "每周"),
    SINGLE("single", "单次"),
    IRREGULAR("irregular", "不定期")
}

/**
 * 导入来源枚举
 * 记录固定日程的创建方式
 */
enum class ImportSource(val value: String, val description: String) {
    MANUAL("manual", "手动创建"),
    EXCEL("excel", "Excel导入"),
    IMAGE_OCR("image_ocr", "图片识别"),
    EXTERNAL_SYNC("external_sync", "外部同步")
}