package com.jingran.taskmanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 课程表实体
 * 根据PRD文档要求，支持从Excel和图片导入课程表
 * 与FixedSchedule关联，提供更详细的课程信息
 */
@Entity(
    tableName = "course_schedules",
    foreignKeys = [
        ForeignKey(
            entity = FixedSchedule::class,
            parentColumns = ["id"],
            childColumns = ["fixed_schedule_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["fixed_schedule_id"])]
)
data class CourseSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "fixed_schedule_id")
    val fixedScheduleId: Long, // 关联的固定日程ID
    
    @ColumnInfo(name = "course_name")
    val courseName: String, // 课程名称
    
    @ColumnInfo(name = "course_code")
    val courseCode: String? = null, // 课程代码
    
    @ColumnInfo(name = "instructor")
    val instructor: String? = null, // 授课教师
    
    @ColumnInfo(name = "classroom")
    val classroom: String? = null, // 教室
    
    @ColumnInfo(name = "building")
    val building: String? = null, // 教学楼
    
    @ColumnInfo(name = "credits")
    val credits: Float? = null, // 学分
    
    @ColumnInfo(name = "course_type")
    val courseType: CourseType = CourseType.LECTURE, // 课程类型
    
    @ColumnInfo(name = "semester")
    val semester: String? = null, // 学期
    
    @ColumnInfo(name = "academic_year")
    val academicYear: String? = null, // 学年
    
    @ColumnInfo(name = "department")
    val department: String? = null, // 院系
    
    @ColumnInfo(name = "week_range")
    val weekRange: String? = null, // 上课周次（如"1-16周"）
    
    @ColumnInfo(name = "import_source")
    val importSource: ImportSource = ImportSource.MANUAL, // 导入来源
    
    @ColumnInfo(name = "import_batch_id")
    val importBatchId: String? = null, // 导入批次ID（用于批量操作）
    
    @ColumnInfo(name = "external_course_id")
    val externalCourseId: String? = null, // 外部系统课程ID
    
    @ColumnInfo(name = "syllabus_url")
    val syllabusUrl: String? = null, // 课程大纲链接
    
    @ColumnInfo(name = "notes")
    val notes: String? = null, // 备注
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true, // 是否激活
    
    @ColumnInfo(name = "create_time")
    val createTime: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "last_modified_time")
    val lastModifiedTime: Long = System.currentTimeMillis()
)

/**
 * 课程类型枚举
 * 区分不同类型的课程
 */
enum class CourseType(val value: String, val displayName: String) {
    LECTURE("lecture", "理论课"),
    LAB("lab", "实验课"),
    SEMINAR("seminar", "研讨课"),
    TUTORIAL("tutorial", "辅导课"),
    WORKSHOP("workshop", "工作坊"),
    FIELD_TRIP("field_trip", "实地考察"),
    EXAM("exam", "考试"),
    REVIEW("review", "复习课"),
    OTHER("other", "其他"),
    REQUIRED("required", "必修课"),
    ELECTIVE("elective", "选修课"),
    PUBLIC("public", "公共课"),
    PROFESSIONAL("professional", "专业课")
}

/**
 * 导入记录实体
 * 记录课程表导入的历史和状态
 */
@Entity(tableName = "import_records")
data class ImportRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "batch_id")
    val batchId: String, // 批次ID
    
    @ColumnInfo(name = "import_type")
    val importType: ImportSource, // 导入类型
    
    @ColumnInfo(name = "file_name")
    val fileName: String? = null, // 文件名
    
    @ColumnInfo(name = "file_path")
    val filePath: String? = null, // 文件路径
    
    @ColumnInfo(name = "total_records")
    val totalRecords: Int = 0, // 总记录数
    
    @ColumnInfo(name = "success_records")
    val successRecords: Int = 0, // 成功导入记录数
    
    @ColumnInfo(name = "failed_records")
    val failedRecords: Int = 0, // 失败记录数
    
    @ColumnInfo(name = "import_status")
    val importStatus: ImportStatus = ImportStatus.PENDING, // 导入状态
    
    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null, // 错误信息
    
    @ColumnInfo(name = "import_time")
    val importTime: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "completion_time")
    val completionTime: Long? = null // 完成时间
)

/**
 * 导入状态枚举
 */
enum class ImportStatus(val value: String, val description: String) {
    PENDING("pending", "等待中"),
    PROCESSING("processing", "处理中"),
    SUCCESS("success", "成功"),
    FAILED("failed", "失败"),
    PARTIAL_SUCCESS("partial_success", "部分成功")
}