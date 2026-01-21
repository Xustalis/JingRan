package com.jingran.taskmanager.ui.activity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.jingran.taskmanager.R
import com.jingran.taskmanager.data.entity.EnergyLevel
import com.jingran.taskmanager.data.entity.LongTermTask
import com.jingran.taskmanager.data.entity.ShortTermTask
import com.jingran.taskmanager.data.entity.TaskPriority
import com.jingran.taskmanager.data.entity.TaskType
import com.jingran.taskmanager.ui.component.IOSStyleDateTimePicker
import com.jingran.taskmanager.ui.component.IOSStylePriorityPicker
import com.jingran.taskmanager.viewmodel.LongTermTaskViewModel
import com.jingran.taskmanager.viewmodel.ShortTermTaskViewModel
import com.jingran.utils.ErrorHandlerKt
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TaskEditActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "TaskEditActivity"
    }

    private lateinit var editTitle: EditText
    private lateinit var editGoal: EditText
    private lateinit var editStartTime: TextView
    private lateinit var editDuration: EditText
    private lateinit var editDeadline: TextView
    private lateinit var editReminderTime: TextView
    private lateinit var tvPriority: TextView
    private lateinit var editDescription: EditText

    private lateinit var shortTermTaskViewModel: ShortTermTaskViewModel
    private lateinit var longTermTaskViewModel: LongTermTaskViewModel

    private var selectedStartTime: LocalDateTime? = null
    private var selectedDeadline: LocalDateTime? = null
    private var selectedReminderTime: LocalDateTime? = null
    private var currentPriority: TaskPriority = TaskPriority.MEDIUM

    private var taskType: String = "short" // 默认为短期任务
    private var taskId: Long = -1 // 默认为新建任务
    private var isEditMode: Boolean = false
    private var loadedShortTermTask: ShortTermTask? = null
    private var loadedLongTermTask: LongTermTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_edit)

        // 初始化ViewModel
        shortTermTaskViewModel = ViewModelProvider(this)[ShortTermTaskViewModel::class.java]
        longTermTaskViewModel = ViewModelProvider(this)[LongTermTaskViewModel::class.java]

        // 初始化视图
        initViews()

        // 获取传递的参数
        taskType = intent.getStringExtra("task_type") ?: "short"
        taskId = intent.getLongExtra("task_id", -1)
        isEditMode = taskId != -1L

        // 设置标题
        val titleText = if (isEditMode) {
            if (taskType == "short") "编辑短期任务" else "编辑长期任务"
        } else {
            if (taskType == "short") "新建短期任务" else "新建长期任务"
        }
        title = titleText

        // 根据任务类型调整UI
        adjustUIByTaskType()

        // 如果是编辑模式，加载任务数据
        if (isEditMode) {
            loadTaskData()
        }
    }

    private fun initViews() {
        editTitle = findViewById(R.id.editTitle)
        editGoal = findViewById(R.id.editGoal)
        editStartTime = findViewById(R.id.editStartTime)
        editDuration = findViewById(R.id.editDuration)
        editDeadline = findViewById(R.id.editDeadline)
        editReminderTime = findViewById(R.id.editReminderTime)
        tvPriority = findViewById(R.id.tvPriority)
        editDescription = findViewById(R.id.editDescription)

        // 设置时间选择器点击事件
        editStartTime.setOnClickListener { showDateTimePicker(it as TextView, true) }
        editDeadline.setOnClickListener { showDateTimePicker(it as TextView, false) }
        editReminderTime.setOnClickListener { showDateTimePicker(it as TextView, false) }

        // 设置优先级选择器点击事件
        tvPriority.setOnClickListener { showPriorityPicker() }

        // 设置保存按钮点击事件
        findViewById<View>(R.id.btnSave).setOnClickListener { saveTask() }
    }

    private fun adjustUIByTaskType() {
        if (taskType == "short") {
            // 短期任务显示开始时间和结束时间
            findViewById<View>(R.id.layoutStartTime).visibility = View.VISIBLE
            findViewById<View>(R.id.layoutDuration).visibility = View.VISIBLE
            findViewById<View>(R.id.layoutDeadline).visibility = View.VISIBLE
            findViewById<View>(R.id.layoutGoal).visibility = View.GONE
        } else {
            // 长期任务显示截止日期
            findViewById<View>(R.id.layoutStartTime).visibility = View.GONE
            findViewById<View>(R.id.layoutDuration).visibility = View.GONE
            findViewById<View>(R.id.layoutDeadline).visibility = View.VISIBLE
            findViewById<View>(R.id.layoutGoal).visibility = View.VISIBLE
        }
    }

    private fun showDateTimePicker(textView: TextView, isStartTime: Boolean) {
        val now = LocalDateTime.now()
        
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val timePickerDialog = TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        val selectedDateTime = LocalDateTime.of(year, month + 1, dayOfMonth, hourOfDay, minute)
                        val formattedDateTime = selectedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        textView.text = formattedDateTime

                        when (textView.id) {
                            R.id.editStartTime -> selectedStartTime = selectedDateTime
                            R.id.editDeadline -> selectedDeadline = selectedDateTime
                            R.id.editReminderTime -> selectedReminderTime = selectedDateTime
                        }

                        // 如果是开始时间，自动设置结束时间为开始时间后1小时
                        if (isStartTime && selectedDeadline == null) {
                            selectedDeadline = selectedDateTime.plusHours(1)
                            editDeadline.text = selectedDeadline?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        }
                    },
                    now.hour,
                    now.minute,
                    true
                )
                timePickerDialog.show()
            },
            now.year,
            now.monthValue - 1,
            now.dayOfMonth
        )
        datePickerDialog.show()
    }

    private fun showPriorityPicker() {
        val priorityPicker = IOSStylePriorityPicker(this, toPriorityValue(currentPriority)) { value, _ ->
            currentPriority = fromPriorityValue(value)
            updatePriorityDisplay()
        }
        priorityPicker.show()
    }

    private fun loadTaskData() {
        lifecycleScope.launch {
            try {
                if (taskType == "short") {
                    val liveData = shortTermTaskViewModel.getShortTermTaskById(taskId)
                    val observer = object : Observer<ShortTermTask?> {
                        override fun onChanged(task: ShortTermTask?) {
                            if (task == null) {
                                return
                            }
                            liveData.removeObserver(this)
                            loadedShortTermTask = task
                            editTitle.setText(task.title)
                        editDescription.setText(task.description ?: "")
                        editDuration.setText(task.duration.toString())
                            
                            fromEpochMillis(task.estimatedStartTime)?.let { time ->
                                selectedStartTime = time
                                editStartTime.text = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                            }
                            
                            fromEpochMillis(task.deadline)?.let { time ->
                            selectedDeadline = time
                            editDeadline.text = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                            }
                            
                            fromEpochMillis(task.reminderTime)?.let { time ->
                                selectedReminderTime = time
                                editReminderTime.text = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                            }
                            
                            currentPriority = task.priority
                            updatePriorityDisplay()
                        }
                    }
                    liveData.observe(this@TaskEditActivity, observer)
                } else {
                    val liveData = longTermTaskViewModel.getLongTermTaskById(taskId)
                    val observer = object : Observer<LongTermTask?> {
                        override fun onChanged(task: LongTermTask?) {
                            if (task == null) {
                                return
                            }
                            liveData.removeObserver(this)
                            loadedLongTermTask = task
                            editTitle.setText(task.title)
                            editGoal.setText(task.goal)
                        editDescription.setText(task.description)
                            
                            fromEpochMillis(task.targetDeadline)?.let { time ->
                                selectedDeadline = time
                                editDeadline.text = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                            }
                            
                            fromEpochMillis(task.nextReviewDate)?.let { time ->
                                selectedReminderTime = time
                                editReminderTime.text = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                            }
                            
                            currentPriority = task.priority
                            updatePriorityDisplay()
                        }
                    }
                    liveData.observe(this@TaskEditActivity, observer)
                }
            } catch (e: Exception) {
                Toast.makeText(this@TaskEditActivity, "加载任务数据失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun saveTask() {
        val title = editTitle.text.toString().trim()
        
        // 输入验证
        if (title.isEmpty()) {
            editTitle.error = "请输入任务标题"
            Toast.makeText(this, "请输入任务标题", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 显示保存中提示
        val loadingToast = Toast.makeText(this, "正在保存...", Toast.LENGTH_SHORT)
        loadingToast.show()
        
        lifecycleScope.launch {
            try {
                if (taskType == "short") {
                    saveShortTermTask(title)
                } else {
                    saveLongTermTask(title)
                }
                
                runOnUiThread {
                    loadingToast.cancel()
                    
                    // 显示成功提示
                    val successMessage = if (isEditMode) "任务更新成功" else "任务创建成功"
                    Toast.makeText(this@TaskEditActivity, successMessage, Toast.LENGTH_SHORT).show()
                    
                    // 设置结果并返回
                    setResult(RESULT_OK)
                    finish()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "保存任务时发生异常", e)
                runOnUiThread {
                    loadingToast.cancel()
                    Toast.makeText(this@TaskEditActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private suspend fun saveShortTermTask(title: String) {
        // 验证起始时间必须输入
        val startTime = selectedStartTime
        if (startTime == null) {
            runOnUiThread {
                editStartTime.error = "请选择起始时间"
                Toast.makeText(this@TaskEditActivity, "起始时间为必填项", Toast.LENGTH_SHORT).show()
            }
            return
        }
        
        val durationText = editDuration.text.toString().trim()
        val durationMinutes = durationText.toIntOrNull()
        if (durationMinutes == null || durationMinutes <= 0) {
            runOnUiThread {
                editDuration.error = "请输入有效时长"
                Toast.makeText(this@TaskEditActivity, "任务时长为必填项", Toast.LENGTH_SHORT).show()
            }
            return
        }
        
        val description = editDescription.text.toString().trim()
        val startMillis = toEpochMillis(startTime)
        val endMillis = toEpochMillis(selectedDeadline)
        val reminderMillis = toEpochMillis(selectedReminderTime)
        
        val existingTask = loadedShortTermTask
        val task = if (isEditMode && existingTask != null) {
            existingTask.copy(
                title = title,
                description = description,
                deadline = endMillis,
                duration = durationMinutes,
                priority = currentPriority,
                reminderTime = reminderMillis,
                estimatedStartTime = startMillis,
                lastModifiedTime = System.currentTimeMillis()
            )
        } else {
            ShortTermTask(
                id = if (isEditMode) taskId else 0,
                title = title,
                description = description,
                deadline = endMillis,
                duration = durationMinutes,
                priority = currentPriority,
                taskType = TaskType.NORMAL,
                isFlexible = true,
                reminderTime = reminderMillis,
                energyLevel = EnergyLevel.MEDIUM,
                estimatedStartTime = startMillis,
                lastModifiedTime = System.currentTimeMillis()
            )
        }
        
        if (isEditMode) {
            shortTermTaskViewModel.updateShortTermTask(task)
        } else {
            shortTermTaskViewModel.insertShortTermTask(task)
        }
    }
    
    private suspend fun saveLongTermTask(title: String) {
        val goal = editGoal.text.toString().trim()
        val description = editDescription.text.toString().trim()
        val deadlineMillis = toEpochMillis(selectedDeadline)
        val reminderMillis = toEpochMillis(selectedReminderTime)
        
        val existingTask = loadedLongTermTask
        val task = if (isEditMode && existingTask != null) {
            existingTask.copy(
                title = title,
                goal = goal,
                description = description,
                targetDeadline = deadlineMillis,
                nextReviewDate = reminderMillis,
                priority = currentPriority,
                lastModifiedTime = System.currentTimeMillis()
            )
        } else {
            LongTermTask(
                id = if (isEditMode) taskId else 0,
                title = title,
                goal = goal,
                description = description,
                targetDeadline = deadlineMillis,
                nextReviewDate = reminderMillis,
                priority = currentPriority,
                lastModifiedTime = System.currentTimeMillis()
            )
        }
        
        if (isEditMode) {
            longTermTaskViewModel.updateLongTermTask(task)
        } else {
            longTermTaskViewModel.insertLongTermTask(task)
        }
    }
    
    private fun updateTimeDisplay() {
        selectedStartTime?.let {
            editStartTime.text = it.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        }
        
        selectedDeadline?.let {
            editDeadline.text = it.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        }
        
        selectedReminderTime?.let {
            editReminderTime.text = it.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        }
    }
    
    private fun updatePriorityDisplay() {
        val priorityText = when (currentPriority) {
            TaskPriority.HIGH -> "高"
            TaskPriority.MEDIUM -> "中"
            TaskPriority.LOW -> "低"
            TaskPriority.URGENT -> "紧急"
        }
        tvPriority.text = priorityText
    }

    private fun toEpochMillis(dateTime: LocalDateTime?): Long? {
        return dateTime?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    }

    private fun fromEpochMillis(millis: Long?): LocalDateTime? {
        return millis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }
    }

    private fun toPriorityValue(priority: TaskPriority): String {
        return when (priority) {
            TaskPriority.HIGH, TaskPriority.URGENT -> "high"
            TaskPriority.MEDIUM -> "medium"
            TaskPriority.LOW -> "low"
        }
    }

    private fun fromPriorityValue(value: String): TaskPriority {
        return when (value) {
            "high" -> TaskPriority.HIGH
            "low" -> TaskPriority.LOW
            else -> TaskPriority.MEDIUM
        }
    }
}
