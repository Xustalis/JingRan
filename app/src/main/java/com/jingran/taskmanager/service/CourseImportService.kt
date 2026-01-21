package com.jingran.taskmanager.service

import com.jingran.taskmanager.data.entity.*
import com.jingran.taskmanager.data.repository.TaskRepository
import com.jingran.taskmanager.data.repository.ImportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.*
import kotlin.collections.ArrayList

/**
 * 课程表导入服务
 * 根据PRD文档要求，支持从Excel文件和图片导入课程表
 * 提供数据验证、冲突检测和批量处理功能
 */
class CourseImportService(
    private val repository: TaskRepository,
    private val importRepository: ImportRepository
) {
    
    companion object {
        private const val TAG = "CourseImportService"
        private const val MAX_IMPORT_BATCH_SIZE = 100
        private const val SUPPORTED_EXCEL_EXTENSIONS = ".xlsx,.xls"
        private const val SUPPORTED_IMAGE_EXTENSIONS = ".jpg,.jpeg,.png,.bmp"
    }
    
    /**
     * 从Excel文件导入课程表
     * @param filePath Excel文件路径
     * @param semester 学期信息
     * @param replaceExisting 是否替换现有课程
     * @return 导入结果
     */
    suspend fun importFromExcel(
        filePath: String,
        semester: String,
        replaceExisting: Boolean = false
    ): CourseImportResult = withContext(Dispatchers.IO) {
        
        val batchId = generateBatchId()
        val importRecord = createImportRecord(batchId, ImportSource.EXCEL, File(filePath).name)
        
        try {
            // 1. 验证文件格式
            if (!isValidExcelFile(filePath)) {
                throw IllegalArgumentException("不支持的文件格式，请使用.xlsx或.xls文件")
            }
            
            // 2. 解析Excel文件
            val courses = parseExcelFile(filePath, semester, batchId)
            
            // 3. 数据验证
            val validationResult = validateCourses(courses)
            if (!validationResult.isValid) {
                throw IllegalArgumentException("数据验证失败：${validationResult.errorMessage}")
            }
            
            // 4. 冲突检测
            val conflictResult = detectConflicts(courses, replaceExisting)
            
            // 5. 保存数据
            val savedCourses = saveCourses(courses, conflictResult, replaceExisting)
            
            // 6. 更新导入记录
            updateImportRecord(
                importRecord.copy(
                    totalRecords = courses.size,
                    successRecords = savedCourses.size,
                    failedRecords = courses.size - savedCourses.size,
                    importStatus = ImportStatus.SUCCESS,
                    completionTime = System.currentTimeMillis()
                )
            )
            
            CourseImportResult(
                success = true,
                batchId = batchId,
                totalCourses = courses.size,
                importedCourses = savedCourses.size,
                skippedCourses = courses.size - savedCourses.size,
                conflicts = conflictResult.conflicts,
                message = "成功导入${savedCourses.size}门课程"
            )
            
        } catch (e: Exception) {
            // 更新失败记录
            updateImportRecord(
                importRecord.copy(
                    importStatus = ImportStatus.FAILED,
                    errorMessage = e.message,
                    completionTime = System.currentTimeMillis()
                )
            )
            
            CourseImportResult(
                success = false,
                batchId = batchId,
                message = "导入失败：${e.message}"
            )
        }
    }
    
    /**
     * 从图片导入课程表（OCR识别）
     * @param imagePath 图片文件路径
     * @param semester 学期信息
     * @return 导入结果
     */
    suspend fun importFromImage(
        imagePath: String,
        semester: String
    ): CourseImportResult = withContext(Dispatchers.IO) {
        
        val batchId = generateBatchId()
        val importRecord = createImportRecord(batchId, ImportSource.IMAGE_OCR, File(imagePath).name)
        
        try {
            // 1. 验证图片格式
            if (!isValidImageFile(imagePath)) {
                throw IllegalArgumentException("不支持的图片格式，请使用jpg、png或bmp格式")
            }
            
            // 2. OCR识别课程表
            val courses = performOCRRecognition(imagePath, semester, batchId)
            
            // 3. 数据验证和清理
            val cleanedCourses = cleanOCRData(courses)
            val validationResult = validateCourses(cleanedCourses)
            
            if (!validationResult.isValid) {
                throw IllegalArgumentException("OCR识别数据验证失败：${validationResult.errorMessage}")
            }
            
            // 4. 保存数据
            val savedCourses = saveCourses(cleanedCourses, ConflictDetectionResult(), false)
            
            // 5. 更新导入记录
            updateImportRecord(
                importRecord.copy(
                    totalRecords = cleanedCourses.size,
                    successRecords = savedCourses.size,
                    failedRecords = cleanedCourses.size - savedCourses.size,
                    importStatus = ImportStatus.SUCCESS,
                    completionTime = System.currentTimeMillis()
                )
            )
            
            CourseImportResult(
                success = true,
                batchId = batchId,
                totalCourses = cleanedCourses.size,
                importedCourses = savedCourses.size,
                skippedCourses = cleanedCourses.size - savedCourses.size,
                message = "OCR识别并导入${savedCourses.size}门课程",
                requiresManualReview = true // OCR结果建议人工审核
            )
            
        } catch (e: Exception) {
            updateImportRecord(
                importRecord.copy(
                    importStatus = ImportStatus.FAILED,
                    errorMessage = e.message,
                    completionTime = System.currentTimeMillis()
                )
            )
            
            CourseImportResult(
                success = false,
                batchId = batchId,
                message = "OCR导入失败：${e.message}"
            )
        }
    }
    
    /**
     * 获取导入历史记录
     */
    suspend fun getImportHistory(limit: Int = 20): List<ImportRecord> {
        return importRepository.getRecentImportRecords(limit)
    }
    
    /**
     * 删除导入批次
     */
    suspend fun deleteImportBatch(batchId: String): Boolean {
        return try {
            repository.deleteCoursesByImportBatch(batchId)
            importRepository.deleteImportRecordsByBatchId(batchId)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // 私有辅助方法
    
    private fun generateBatchId(): String {
        return "IMPORT_${System.currentTimeMillis()}_${UUID.randomUUID().toString().substring(0, 8)}"
    }
    
    private suspend fun createImportRecord(
        batchId: String,
        importType: ImportSource,
        fileName: String
    ): ImportRecord {
        val record = ImportRecord(
            batchId = batchId,
            importType = importType,
            fileName = fileName,
            importStatus = ImportStatus.PROCESSING
        )
        importRepository.insertImportRecord(record)
        return record
    }
    
    private suspend fun updateImportRecord(record: ImportRecord) {
        importRepository.updateImportRecord(record)
    }
    
    private fun isValidExcelFile(filePath: String): Boolean {
        val extension = File(filePath).extension.lowercase()
        return extension in listOf("xlsx", "xls")
    }
    
    private fun isValidImageFile(imagePath: String): Boolean {
        val extension = File(imagePath).extension.lowercase()
        return extension in listOf("jpg", "jpeg", "png", "bmp")
    }
    
    /**
     * 解析Excel文件（简化实现，实际需要使用Apache POI等库）
     */
    private fun parseExcelFile(
        filePath: String,
        semester: String,
        batchId: String
    ): List<CourseSchedule> {
        // TODO: 实现Excel解析逻辑
        // 这里返回示例数据，实际实现需要使用Apache POI库
        return listOf(
            CourseSchedule(
                fixedScheduleId = 0, // 临时值，后续会更新
                courseName = "示例课程",
                courseCode = "CS101",
                instructor = "张教授",
                classroom = "A101",
                semester = semester,
                importSource = ImportSource.EXCEL,
                importBatchId = batchId
            )
        )
    }
    
    /**
     * OCR识别课程表（简化实现，实际需要集成OCR服务）
     */
    private fun performOCRRecognition(
        imagePath: String,
        semester: String,
        batchId: String
    ): List<CourseSchedule> {
        // TODO: 实现OCR识别逻辑
        // 这里返回示例数据，实际实现需要集成OCR服务如百度OCR、腾讯OCR等
        return listOf(
            CourseSchedule(
                fixedScheduleId = 0,
                courseName = "OCR识别课程",
                courseCode = "OCR101",
                instructor = "AI识别",
                classroom = "待确认",
                semester = semester,
                importSource = ImportSource.IMAGE_OCR,
                importBatchId = batchId
            )
        )
    }
    
    private fun cleanOCRData(courses: List<CourseSchedule>): List<CourseSchedule> {
        return courses.map { course ->
            course.copy(
                courseName = course.courseName.trim(),
                instructor = course.instructor?.trim(),
                classroom = course.classroom?.trim()
            )
        }.filter { it.courseName.isNotBlank() }
    }
    
    private fun validateCourses(courses: List<CourseSchedule>): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (courses.isEmpty()) {
            errors.add("没有找到有效的课程数据")
        }
        
        courses.forEachIndexed { index, course ->
            if (course.courseName.isBlank()) {
                errors.add("第${index + 1}行：课程名称不能为空")
            }
            if (course.courseName.length > 100) {
                errors.add("第${index + 1}行：课程名称过长")
            }
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errorMessage = errors.joinToString("; ")
        )
    }
    
    private suspend fun detectConflicts(
        courses: List<CourseSchedule>,
        replaceExisting: Boolean
    ): ConflictDetectionResult {
        if (replaceExisting) {
            return ConflictDetectionResult()
        }
        
        val conflicts = mutableListOf<CourseConflict>()
        
        // TODO: 实现冲突检测逻辑
        // 检查时间冲突、教室冲突等
        
        return ConflictDetectionResult(conflicts = conflicts)
    }
    
    private suspend fun saveCourses(
        courses: List<CourseSchedule>,
        conflictResult: ConflictDetectionResult,
        replaceExisting: Boolean
    ): List<CourseSchedule> {
        val savedCourses = mutableListOf<CourseSchedule>()
        
        for (course in courses) {
            try {
                // 首先创建对应的FixedSchedule
                val fixedSchedule = FixedSchedule(
                    title = course.courseName,
                    location = course.classroom,
                    startTime = System.currentTimeMillis(), // TODO: 从课程数据中解析实际时间
                    endTime = System.currentTimeMillis() + (90 * 60 * 1000), // 默认90分钟
                    dayOfWeek = 1, // TODO: 从课程数据中解析
                    scheduleType = ScheduleType.COURSE,
                    importSource = course.importSource
                )
                
                val fixedScheduleId = repository.insertFixedSchedule(fixedSchedule)
                
                // 然后创建CourseSchedule
                val courseWithFixedId = course.copy(fixedScheduleId = fixedScheduleId)
                repository.insertCourseSchedule(courseWithFixedId)
                
                savedCourses.add(courseWithFixedId)
            } catch (e: Exception) {
                // 记录失败的课程，继续处理其他课程
                continue
            }
        }
        
        return savedCourses
    }
}

// 数据类定义

data class CourseImportResult(
    val success: Boolean,
    val batchId: String = "",
    val totalCourses: Int = 0,
    val importedCourses: Int = 0,
    val skippedCourses: Int = 0,
    val conflicts: List<CourseConflict> = emptyList(),
    val message: String = "",
    val requiresManualReview: Boolean = false
)

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String = ""
)

data class ConflictDetectionResult(
    val hasConflicts: Boolean = false,
    val conflicts: List<CourseConflict> = emptyList()
)

data class CourseConflict(
    val type: ConflictType,
    val existingCourse: CourseSchedule,
    val newCourse: CourseSchedule,
    val description: String
)

enum class ConflictType {
    TIME_OVERLAP,
    CLASSROOM_CONFLICT,
    INSTRUCTOR_CONFLICT,
    DUPLICATE_COURSE
}