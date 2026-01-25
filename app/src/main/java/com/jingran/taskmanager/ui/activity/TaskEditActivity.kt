package com.jingran.taskmanager.ui.activity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.ChipGroup
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
    private lateinit var editDuration: EditText
    private lateinit var editDeadline: TextView
    private lateinit var editReminder: TextView
    private lateinit var editDescription: EditText
    private lateinit var chipGroupPriority: ChipGroup

    private lateinit var shortTermTaskViewModel: ShortTermTaskViewModel
    private lateinit var longTermTaskViewModel: LongTermTaskViewModel

    private var selectedDeadline: LocalDateTime? = null
    private var selectedReminder: LocalDateTime? = null
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
        editDuration = findViewById(R.id.editDuration)
        editDeadline = findViewById(R.id.editDeadline)
        editReminder = findViewById(R.id.editReminder)
        editDescription = findViewById(R.id.editDescription)
        chipGroupPriority = findViewById(R.id.chipGroupPriority)

        // 设置时间选择器点击事件
        editDeadline.setOnClickListener { showDateTimePicker(it as TextView, false) }
        editReminder.setOnClickListener { showDateTimePicker(it as TextView, false) }

        // 设置优先级选择器点击事件
        chipGroupPriority.setOnCheckedChangeListener { group, checkedId ->
            currentPriority = when (checkedId) {
                R.id.chipPriorityLow -> TaskPriority.LOW
                R.id.chipPriorityMedium -> TaskPriority.MEDIUM
                R.id.chipPriorityHigh -> TaskPriority.HIGH
                R.id.chipPriorityUrgent -> TaskPriority.URGENT
                else -> TaskPriority.MEDIUM
            }
        }

        // 设置保存按钮点击事件
        findViewById<View>(R.id.btnSaveText).setOnClickListener { saveTask() }

        // 取消按钮
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        // 删除按钮
        findViewById<View>(R.id.btnDelete).setOnClickListener { deleteTask() }
    }

    private fun deleteTask() {
        AlertDialog.Builder(this)
            .setTitle("删除任务")
            .setMessage("确定要删除这个任务吗？此操作无法撤销。")
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    if (taskType == "short") {
                        loadedShortTermTask?.let { shortTermTaskViewModel.deleteShortTermTask(it) }
                    } else {
                        loadedLongTermTask?.let { longTermTaskViewModel.deleteLongTermTask(it) }
                    }
                    finish()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun adjustUIByTaskType() {
        if (taskType == "short") {
            // 短期任务
            findViewById<View>(R.id.textInputLayoutDuration).visibility = View.VISIBLE
            findViewById<View>(R.id.textInputLayoutDeadline).visibility = View.VISIBLE
            findViewById<View>(R.id.textInputLayoutReminder).visibility = View.VISIBLE
            findViewById<View>(R.id.switchFlexible).visibility = View.VISIBLE
        } else {
            // 长期任务
            findViewById<View>(R.id.textInputLayoutDuration).visibility = View.GONE
            findViewById<View>(R.id.textInputLayoutDeadline).visibility = View.VISIBLE
            findViewById<View>(R.id.textInputLayoutReminder).visibility = View.VISIBLE
            findViewById<View>(R.id.switchFlexible).visibility = View.GONE
        }
        
        // 编辑模式显示删除按钮
        findViewById<View>(R.id.btnDelete).visibility = if (isEditMode) View.VISIBLE else View.GONE
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
                            R.id.editDeadline -> selectedDeadline = selectedDateTime
                            R.id.editReminder -> selectedReminder = selectedDateTime
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
                                selectedReminder = time
                                editReminder.text = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
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
        val deadlineMillis = toEpochMillis(selectedDeadline)
        val reminderMillis = toEpochMillis(selectedReminder)
        
        val existingTask = loadedShortTermTask
        val task = if (isEditMode && existingTask != null) {
            existingTask.copy(
                title = title,
                description = description,
                deadline = deadlineMillis,
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
