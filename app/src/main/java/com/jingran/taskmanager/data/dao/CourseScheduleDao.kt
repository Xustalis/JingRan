package com.jingran.taskmanager.data.dao

import androidx.room.*
import com.jingran.taskmanager.data.entity.CourseSchedule
import com.jingran.taskmanager.data.entity.CourseStats
import com.jingran.taskmanager.data.entity.ImportSource
import kotlinx.coroutines.flow.Flow

/**
 * 课程表数据访问对象
 * 提供课程表相关的数据库操作
 */
@Dao
interface CourseScheduleDao {
    
    /**
     * 获取所有课程
     */
    @Query("SELECT * FROM course_schedules WHERE is_active = 1 ORDER BY course_name")
    fun getAllCourses(): Flow<List<CourseSchedule>>
    
    /**
     * 根据ID获取课程
     */
    @Query("SELECT * FROM course_schedules WHERE id = :id")
    suspend fun getCourseById(id: Long): CourseSchedule?
    
    /**
     * 根据固定日程ID获取课程
     */
    @Query("SELECT * FROM course_schedules WHERE fixed_schedule_id = :fixedScheduleId")
    suspend fun getCourseByFixedScheduleId(fixedScheduleId: Long): CourseSchedule?
    
    /**
     * 根据固定日程ID获取所有相关课程（用于Repository）
     */
    @Query("SELECT * FROM course_schedules WHERE fixed_schedule_id = :fixedScheduleId AND is_active = 1")
    suspend fun getCoursesByFixedScheduleId(fixedScheduleId: Long): List<CourseSchedule>
    
    /**
     * 根据学期获取课程
     */
    @Query("SELECT * FROM course_schedules WHERE semester = :semester AND is_active = 1 ORDER BY course_name")
    fun getCoursesBySemester(semester: String): Flow<List<CourseSchedule>>
    
    /**
     * 根据院系获取课程
     */
    @Query("SELECT * FROM course_schedules WHERE department = :department AND is_active = 1 ORDER BY course_name")
    fun getCoursesByDepartment(department: String): Flow<List<CourseSchedule>>
    
    /**
     * 根据导入批次获取课程
     */
    @Query("SELECT * FROM course_schedules WHERE import_batch_id = :batchId ORDER BY course_name")
    suspend fun getCoursesByImportBatch(batchId: String): List<CourseSchedule>
    
    /**
     * 根据导入来源获取课程
     */
    @Query("SELECT * FROM course_schedules WHERE import_source = :importSource AND is_active = 1 ORDER BY course_name")
    fun getCoursesByImportSource(importSource: ImportSource): Flow<List<CourseSchedule>>
    
    /**
     * 搜索课程（按课程名称或课程代码）
     */
    @Query("""
        SELECT * FROM course_schedules 
        WHERE (course_name LIKE '%' || :query || '%' OR course_code LIKE '%' || :query || '%') 
        AND is_active = 1 
        ORDER BY course_name
    """)
    fun searchCourses(query: String): Flow<List<CourseSchedule>>
    
    /**
     * 获取所有学期
     */
    @Query("SELECT DISTINCT semester FROM course_schedules WHERE semester IS NOT NULL AND is_active = 1 ORDER BY semester")
    suspend fun getAllSemesters(): List<String>
    
    /**
     * 获取所有不同的学期（用于Repository中的getDistinctSemesters方法）
     */
    @Query("SELECT DISTINCT semester FROM course_schedules WHERE semester IS NOT NULL AND is_active = 1 ORDER BY semester")
    suspend fun getDistinctSemesters(): List<String>
    
    /**
     * 获取所有院系
     */
    @Query("SELECT DISTINCT department FROM course_schedules WHERE department IS NOT NULL AND is_active = 1 ORDER BY department")
    suspend fun getAllDepartments(): List<String>
    
    /**
     * 获取总学分
     */
    @Query("SELECT SUM(credits) FROM course_schedules WHERE semester = :semester AND is_active = 1")
    suspend fun getTotalCreditsBySemester(semester: String): Float?
    
    /**
     * 检查时间冲突的课程
     */
    @Query("""
        SELECT cs.* FROM course_schedules cs
        INNER JOIN fixed_schedules fs ON cs.fixed_schedule_id = fs.id
        WHERE fs.day_of_week = :dayOfWeek 
        AND cs.semester = :semester 
        AND cs.is_active = 1
        AND fs.is_active = 1
        AND (
            (fs.start_time <= :startTime AND fs.end_time > :startTime) OR
            (fs.start_time < :endTime AND fs.end_time >= :endTime) OR
            (fs.start_time >= :startTime AND fs.end_time <= :endTime)
        )
    """)
    suspend fun getConflictingCourses(dayOfWeek: Int, startTime: Long, endTime: Long, semester: String): List<CourseSchedule>
    
    /**
     * 插入课程
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseSchedule): Long
    
    /**
     * 插入课程（别名方法，用于Repository兼容）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseSchedule(course: CourseSchedule): Long
    
    /**
     * 批量插入课程
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<CourseSchedule>): List<Long>
    
    /**
     * 更新课程
     */
    @Update
    suspend fun updateCourse(course: CourseSchedule)
    
    /**
     * 批量更新课程
     */
    @Update
    suspend fun updateCourses(courses: List<CourseSchedule>)
    
    /**
     * 删除课程
     */
    @Delete
    suspend fun deleteCourse(course: CourseSchedule)
    
    /**
     * 根据ID删除课程
     */
    @Query("DELETE FROM course_schedules WHERE id = :id")
    suspend fun deleteCourseById(id: Long)
    
    /**
     * 软删除课程（设置为非激活状态）
     */
    @Query("UPDATE course_schedules SET is_active = 0, last_modified_time = :modifiedTime WHERE id = :id")
    suspend fun softDeleteCourse(id: Long, modifiedTime: Long = System.currentTimeMillis())
    
    /**
     * 根据导入批次删除课程
     */
    @Query("DELETE FROM course_schedules WHERE import_batch_id = :batchId")
    suspend fun deleteCoursesByImportBatch(batchId: String)
    
    /**
     * 根据学期删除课程
     */
    @Query("DELETE FROM course_schedules WHERE semester = :semester")
    suspend fun deleteCoursesBySemester(semester: String)
    
    /**
     * 激活/停用课程
     */
    @Query("UPDATE course_schedules SET is_active = :isActive, last_modified_time = :modifiedTime WHERE id = :id")
    suspend fun updateCourseActiveStatus(id: Long, isActive: Boolean, modifiedTime: Long = System.currentTimeMillis())
    
    /**
     * 更新课程的最后修改时间
     */
    @Query("UPDATE course_schedules SET last_modified_time = :modifiedTime WHERE id = :id")
    suspend fun updateLastModifiedTime(id: Long, modifiedTime: Long = System.currentTimeMillis())
    
    /**
     * 获取课程统计信息
     */
    @Query("""
        SELECT 
            COUNT(*) as totalCourses,
            SUM(CASE WHEN is_active = 1 THEN 1 ELSE 0 END) as activeCourses,
            SUM(CASE WHEN is_active = 1 THEN credits ELSE 0 END) as totalCredits
        FROM course_schedules 
        WHERE semester = :semester
    """)
    suspend fun getCourseStatsBySemester(semester: String): CourseStats?
}