package com.jingran.taskmanager.ui.activity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import com.jingran.utils.CoroutineErrorHandler
import com.jingran.utils.LogManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jingran.taskmanager.R
import com.jingran.taskmanager.data.entity.CourseSchedule
import com.jingran.taskmanager.service.CourseImportService
import com.jingran.taskmanager.ui.adapter.CourseScheduleAdapter
import com.jingran.taskmanager.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

class CourseScheduleActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "CourseScheduleActivity"
        private const val REQUEST_CODE_PICK_FILE = 1001
        private const val REQUEST_CODE_PICK_IMAGE = 1002
    }
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnImportExcel: MaterialButton
    private lateinit var btnImportImage: MaterialButton
    private lateinit var fabAdd: FloatingActionButton
    
    private val taskViewModel: TaskViewModel by viewModels()
    private lateinit var courseAdapter: CourseScheduleAdapter
    private lateinit var courseImportService: CourseImportService
    
    // 文件选择器
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                importFromExcel(uri)
            }
        }
    }
    
    // 图片选择器
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                importFromImage(uri)
            }
        }
    }
    
    // 权限请求
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(this, "需要存储权限才能导入文件", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_schedule)
        
        initViews()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        
        courseImportService = CourseImportService(taskViewModel.getRepository(), taskViewModel.getImportRepository())
        
        // 检查权限
        checkPermissions()
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewCourses)
        btnImportExcel = findViewById(R.id.btnImportExcel)
        btnImportImage = findViewById(R.id.btnImportImage)
        fabAdd = findViewById(R.id.fabAdd)
    }
    
    private fun setupRecyclerView() {
        courseAdapter = CourseScheduleAdapter(
            onItemClick = { course ->
                // 编辑课程
                editCourse(course)
            },
            onDeleteClick = { course ->
                // 删除课程
                deleteCourse(course)
            }
        )
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@CourseScheduleActivity)
            adapter = courseAdapter
        }
    }
    
    private fun setupClickListeners() {
        btnImportExcel.setOnClickListener {
            pickExcelFile()
        }
        
        btnImportImage.setOnClickListener {
            pickImageFile()
        }
        
        fabAdd.setOnClickListener {
            // 手动添加课程
            addCourse()
        }
    }
    
    private fun observeViewModel() {
        com.jingran.utils.CoroutineErrorHandler.safeLifecycleCoroutineExecute(
            lifecycleOwner = this,
            context = this@CourseScheduleActivity,
            onError = { errorInfo ->
                Toast.makeText(this@CourseScheduleActivity, "课程数据加载失败: ${errorInfo.userMessage}", Toast.LENGTH_SHORT).show()
            }
        ) {
            taskViewModel.allCourseSchedules.collect { courses ->
                courseAdapter.submitList(courses)
            }
        }
    }
    
    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }
    
    private fun pickExcelFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        filePickerLauncher.launch(intent)
    }
    
    private fun pickImageFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        imagePickerLauncher.launch(intent)
    }
    
    private fun importFromExcel(uri: Uri) {
        CoroutineErrorHandler.safeLifecycleCoroutineExecute(
            lifecycleOwner = this,
            context = this@CourseScheduleActivity,
            onError = { errorInfo ->
                Toast.makeText(this@CourseScheduleActivity, "Excel导入失败: ${errorInfo.userMessage}", Toast.LENGTH_SHORT).show()
            }
        ) {
            // 将Uri转换为文件路径，这里简化处理
            val filePath = uri.path ?: ""
            val currentSemester = "2024-1" // 可以从设置或用户输入获取
            
            val result = courseImportService.importFromExcel(filePath, currentSemester)
            if (result.success) {
                Toast.makeText(this@CourseScheduleActivity, 
                    "导入成功：${result.importedCourses}门课程", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@CourseScheduleActivity, 
                    "导入失败：${result.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun importFromImage(uri: Uri) {
        CoroutineErrorHandler.safeLifecycleCoroutineExecute(
            lifecycleOwner = this,
            context = this@CourseScheduleActivity,
            onError = { errorInfo ->
                Toast.makeText(this@CourseScheduleActivity, "图片导入失败: ${errorInfo.userMessage}", Toast.LENGTH_SHORT).show()
            }
        ) {
            // 将Uri转换为文件路径，这里简化处理
            val imagePath = uri.path ?: ""
            val currentSemester = "2024-1" // 可以从设置或用户输入获取
            
            val result = courseImportService.importFromImage(imagePath, currentSemester)
            if (result.success) {
                Toast.makeText(this@CourseScheduleActivity, 
                    "导入成功：${result.importedCourses}门课程", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@CourseScheduleActivity, 
                    "导入失败：${result.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun editCourse(course: CourseSchedule) {
        // TODO: 实现课程编辑功能
        Toast.makeText(this, "编辑课程功能待实现", Toast.LENGTH_SHORT).show()
    }
    
    private fun deleteCourse(course: CourseSchedule) {
        CoroutineErrorHandler.safeLifecycleCoroutineExecute(
            lifecycleOwner = this,
            context = this@CourseScheduleActivity,
            onError = { errorInfo ->
                Toast.makeText(this@CourseScheduleActivity, "删除课程失败: ${errorInfo.userMessage}", Toast.LENGTH_SHORT).show()
            }
        ) {
            taskViewModel.deleteCourseSchedule(course)
            Toast.makeText(this@CourseScheduleActivity, "删除成功", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun addCourse() {
        // TODO: 实现手动添加课程功能
        Toast.makeText(this, "手动添加课程功能待实现", Toast.LENGTH_SHORT).show()
    }
}