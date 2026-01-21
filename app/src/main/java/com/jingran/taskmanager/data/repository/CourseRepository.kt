package com.jingran.taskmanager.data.repository

import androidx.lifecycle.LiveData
import com.jingran.taskmanager.data.dao.CourseScheduleDao
import com.jingran.taskmanager.data.entity.CourseSchedule
import com.jingran.taskmanager.data.entity.CourseStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 课程Repository
 * 负责课程表相关的数据访问操作
 */
@Singleton
class CourseRepository @Inject constructor(
    private val courseScheduleDao: CourseScheduleDao
) : BaseRepository() {
    
    /**
     * 获取所有课程
     */
    fun getAllCourses(): Flow<List<CourseSchedule>> = 
        courseScheduleDao.getAllCourses()
    
    /**
     * 根据学期获取课程
     */
    fun getCoursesBySemester(semester: String): Flow<List<CourseSchedule>> = 
        courseScheduleDao.getCoursesBySemester(semester)
    
    /**
     * 根据星期几获取课程（通过冲突检查实现）
     */
    suspend fun getCoursesByDayOfWeek(dayOfWeek: Int): List<CourseSchedule> = ioCall {
        // 使用冲突检查方法，传入全天时间范围
        courseScheduleDao.getConflictingCourses(dayOfWeek, 0, 24 * 60, "")
    }
    
    /**
     * 根据学期和星期几获取课程（通过冲突检查实现）
     */
    suspend fun getCoursesBySemesterAndDay(
        semester: String, 
        dayOfWeek: Int
    ): List<CourseSchedule> = ioCall {
        // 使用冲突检查方法，传入全天时间范围
        courseScheduleDao.getConflictingCourses(dayOfWeek, 0, 24 * 60, semester)
    }
    
    /**
     * 根据ID获取课程
     */
    suspend fun getCourseById(id: Long): CourseSchedule? = ioCall {
        courseScheduleDao.getCourseById(id)
    }
    
    /**
     * 根据课程名称搜索课程
     */
    fun searchCoursesByName(courseName: String): Flow<List<CourseSchedule>> {
        return courseScheduleDao.searchCourses(courseName)
    }
    
    /**
     * 根据教师搜索课程
     */
    fun searchCoursesByTeacher(teacher: String): Flow<List<CourseSchedule>> {
        return courseScheduleDao.searchCourses(teacher)
    }
    
    /**
     * 根据地点搜索课程
     */
    fun searchCoursesByLocation(location: String): Flow<List<CourseSchedule>> {
        return courseScheduleDao.searchCourses(location)
    }
    
    /**
     * 获取冲突的课程
     */
    suspend fun getConflictingCourses(
        dayOfWeek: Int,
        startTime: Long,
        endTime: Long,
        semester: String
    ): List<CourseSchedule> = ioCall {
        courseScheduleDao.getConflictingCourses(dayOfWeek, startTime, endTime, semester)
    }
    
    /**
     * 检查时间段是否有冲突
     */
    suspend fun hasTimeConflict(
        dayOfWeek: Int,
        startTime: Long,
        endTime: Long,
        semester: String,
        excludeId: Long? = null
    ): Boolean = ioCall {
        val conflicts = courseScheduleDao.getConflictingCourses(dayOfWeek, startTime, endTime, semester)
        if (excludeId != null) {
            conflicts.any { it.id != excludeId }
        } else {
            conflicts.isNotEmpty()
        }
    }
    
    /**
     * 根据固定日程ID获取课程
     */
    suspend fun getCoursesByFixedScheduleId(fixedScheduleId: Long): List<CourseSchedule> = ioCall {
        courseScheduleDao.getCoursesByFixedScheduleId(fixedScheduleId)
    }
    
    /**
     * 获取所有不同的学期
     */
    suspend fun getDistinctSemesters(): List<String> = ioCall {
        courseScheduleDao.getDistinctSemesters()
    }
    
    /**
     * 获取课程统计信息
     */
    suspend fun getCourseStatsBySemester(semester: String): CourseStats = ioCall {
        courseScheduleDao.getCourseStatsBySemester(semester) ?: CourseStats(0, 0, 0.0f)
    }
    
    /**
     * 插入课程
     */
    suspend fun insertCourse(course: CourseSchedule): Long = ioCall {
        courseScheduleDao.insertCourseSchedule(course)
    }
    
    /**
     * 批量插入课程
     */
    suspend fun insertCourses(courses: List<CourseSchedule>): List<Long> = ioCall {
        courseScheduleDao.insertCourses(courses)
    }
    
    /**
     * 更新课程
     */
    suspend fun updateCourse(course: CourseSchedule) = ioCall {
        courseScheduleDao.updateCourse(course)
    }
    
    /**
     * 删除课程
     */
    suspend fun deleteCourse(course: CourseSchedule) = ioCall {
        courseScheduleDao.deleteCourse(course)
    }
    
    /**
     * 根据ID删除课程
     */
    suspend fun deleteCourseById(id: Long) = ioCall {
        courseScheduleDao.deleteCourseById(id)
    }
    
    /**
     * 软删除课程（设置为非激活状态）
     */
    suspend fun softDeleteCourse(id: Long) = ioCall {
        courseScheduleDao.softDeleteCourse(id)
    }
    
    /**
     * 根据学期删除课程
     */
    suspend fun deleteCoursesBySemester(semester: String) = ioCall {
        courseScheduleDao.deleteCoursesBySemester(semester)
    }
    
    /**
     * 根据导入批次ID删除课程
     */
    suspend fun deleteCoursesByImportBatch(importBatchId: String) = ioCall {
        courseScheduleDao.deleteCoursesByImportBatch(importBatchId)
    }
    
    /**
     * 激活课程
     */
    suspend fun activateCourse(id: Long) = ioCall {
        courseScheduleDao.updateCourseActiveStatus(id, true)
    }
    
    /**
     * 停用课程
     */
    suspend fun deactivateCourse(id: Long) = ioCall {
        courseScheduleDao.updateCourseActiveStatus(id, false)
    }
    
    /**
     * 更新课程最后修改时间
     */
    suspend fun updateLastModified(id: Long, lastModified: Long) = ioCall {
        courseScheduleDao.updateLastModifiedTime(id, lastModified)
    }
    
    /**
     * 批量更新课程状态
     */
    suspend fun updateCoursesStatus(ids: List<Long>, isActive: Boolean) = ioCall {
        ids.forEach { id ->
            courseScheduleDao.updateCourseActiveStatus(id, isActive)
        }
    }
    
    /**
     * 获取当前学期的课程
     */
    fun getCurrentSemesterCourses(): Flow<List<CourseSchedule>> {
        return flow {
            val semesters = courseScheduleDao.getDistinctSemesters()
            if (semesters.isNotEmpty()) {
                // 假设最新的学期是当前学期
                courseScheduleDao.getCoursesBySemester(semesters.first()).collect { courses ->
                    emit(courses)
                }
            } else {
                emit(emptyList())
            }
        }
    }
    
    /**
     * 清空所有课程
     */
    suspend fun deleteAllCourses() = ioCall {
        // 使用统计查询获取总数，然后删除所有学期的课程
        val semesters = courseScheduleDao.getDistinctSemesters()
        semesters.forEach { semester ->
            courseScheduleDao.deleteCoursesBySemester(semester)
        }
    }
    
    /**
     * 获取课程总数
     */
    suspend fun getCourseCount(): Int = ioCall {
        // 使用统计查询而不是获取所有数据
        val semesters = courseScheduleDao.getDistinctSemesters()
        var totalCount = 0
        semesters.forEach { semester ->
            val stats = courseScheduleDao.getCourseStatsBySemester(semester)
            totalCount += stats?.totalCourses ?: 0
        }
        totalCount
    }
    
    /**
     * 获取指定学期的课程总数
     */
    suspend fun getCourseCountBySemester(semester: String): Int = ioCall {
        // 使用统计查询而不是获取所有数据
        val stats = courseScheduleDao.getCourseStatsBySemester(semester)
        stats?.totalCourses ?: 0
    }
}