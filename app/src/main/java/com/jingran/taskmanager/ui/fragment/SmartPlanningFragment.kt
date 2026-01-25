package com.jingran.taskmanager.ui.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jingran.taskmanager.R
import com.jingran.taskmanager.databinding.FragmentSmartPlanningBinding
import com.jingran.taskmanager.ui.adapter.PlanItemAdapter
import com.jingran.taskmanager.ui.adapter.FixedScheduleAdapter
import com.jingran.taskmanager.viewmodel.PlanningViewModel
import com.jingran.utils.CoroutineErrorHandler
import com.jingran.utils.LogManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 智能规划界面
 * 显示每日时间规划表和效率统计
 */
class SmartPlanningFragment : Fragment() {
    
    companion object {
        private const val TAG = "SmartPlanningFragment"
        private const val KEY_CURRENT_DATE = "current_date"
        private const val KEY_SCROLL_POSITION = "scroll_position"
    }
    
    private var _binding: FragmentSmartPlanningBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: PlanningViewModel by viewModels()
    private lateinit var planItemAdapter: PlanItemAdapter
    private lateinit var fixedScheduleAdapter: FixedScheduleAdapter
    
    private val dateFormat = SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    private var currentDate: Long = System.currentTimeMillis()
    private var savedScrollPosition = 0
    
    // 文件选择器
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleSelectedFile(uri)
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSmartPlanningBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 恢复保存的状态
        restoreInstanceState(savedInstanceState)
        
        setupUI()
        setupObservers()
        loadTodayPlan()
    }
    
    override fun onResume() {
        super.onResume()
        // 每次页面恢复时重新加载数据，确保数据实时更新
        loadTodayPlan()
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_CURRENT_DATE, currentDate)
        
        // 保存滚动位置
        if (::planItemAdapter.isInitialized && _binding != null) {
            val layoutManager = binding.recyclerViewPlanItems.layoutManager as? LinearLayoutManager
            layoutManager?.let {
                savedScrollPosition = it.findFirstVisibleItemPosition()
                outState.putInt(KEY_SCROLL_POSITION, savedScrollPosition)
            }
        }
        
        LogManager.d(TAG, "状态已保存: currentDate=$currentDate, scrollPosition=$savedScrollPosition")
    }
    
    private fun restoreInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            currentDate = it.getLong(KEY_CURRENT_DATE, System.currentTimeMillis())
            savedScrollPosition = it.getInt(KEY_SCROLL_POSITION, 0)
            LogManager.d(TAG, "状态已恢复: currentDate=$currentDate, scrollPosition=$savedScrollPosition")
        }
    }
    
    private fun setupUI() {
        // 设置日期显示
        updateDateDisplay()
        
        // 设置计划项列表
        planItemAdapter = PlanItemAdapter { planItem ->
            // 处理计划项点击事件
            onPlanItemClick(planItem)
        }
        
        binding.recyclerViewPlanItems.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = planItemAdapter
        }
        
        // 设置固定日程列表
        fixedScheduleAdapter = FixedScheduleAdapter { schedule ->
            // 处理固定日程点击事件
            onFixedScheduleClick(schedule)
        }
        
        binding.recyclerViewFixedSchedules.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = fixedScheduleAdapter
        }
        
        // 设置按钮点击事件
        binding.buttonPreviousDay.setOnClickListener {
            changeDate(-1)
        }
        
        binding.buttonNextDay.setOnClickListener {
            changeDate(1)
        }
        
        binding.buttonGeneratePlan.setOnClickListener {
            generateIntelligentPlan()
        }
        
        binding.buttonAddEmergencyTask.setOnClickListener {
            showAddEmergencyTaskDialog()
        }

        binding.buttonImportSchedule.setOnClickListener {
            showImportScheduleDialog()
        }


    }
    
    private fun setupObservers() {
        // 观察计划项数据
        viewModel.todayPlanItems.observe(viewLifecycleOwner) { planItems ->
            planItemAdapter.submitList(planItems)
            updateBasicStatistics(planItems)
            
            // 恢复滚动位置
            if (savedScrollPosition > 0 && planItems.isNotEmpty()) {
                binding.recyclerViewPlanItems.post {
                    val layoutManager = binding.recyclerViewPlanItems.layoutManager as? LinearLayoutManager
                    layoutManager?.scrollToPosition(savedScrollPosition)
                    savedScrollPosition = 0 // 重置，避免重复恢复
                }
            }
        }
        
        // 观察固定日程数据
        viewModel.fixedSchedules.observe(viewLifecycleOwner) { schedules ->
            fixedScheduleAdapter.submitList(schedules)
        }
        
        // 观察每日统计数据
        viewModel.getDailyStatsLive(currentDate).observe(viewLifecycleOwner) { dailyStats ->
            updateDailyStatistics(dailyStats)
        }
        
        // 观察加载状态
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.buttonGeneratePlan.isEnabled = !isLoading
        }
        
        // 观察错误信息
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null) {
                // 显示错误信息
                showErrorMessage(errorMessage)
                viewModel.clearErrorMessage()
            }
        }
    }
    
    private fun loadTodayPlan() {
        if (!isAdded || context == null) {
            return
        }
        
        CoroutineErrorHandler.safeLifecycleCoroutineExecute(
            lifecycleOwner = this,
            context = requireContext(),
            onError = { errorInfo ->
                LogManager.e("SmartPlanningFragment", "加载今日计划失败: ${errorInfo.message}", errorInfo.throwable)
                if (isAdded) {
                    showErrorMessage("加载今日计划失败，请重试")
                }
            }
        ) {
            // 加载当日计划项
            viewModel.getPlanItemsByDate(currentDate).observe(viewLifecycleOwner) { items ->
                // Update UI with the plan items
            }
            
            // 加载固定日程
            loadFixedSchedules()
            
            // 加载每日统计数据
            // viewModel.loadDailyStats(currentDate)
        }
    }
    
    private fun loadFixedSchedules() {
        val dayOfWeek = getDayOfWeek(currentDate)
        viewModel.loadFixedSchedulesByDay(dayOfWeek)
    }
    
    private fun generateIntelligentPlan() {
        // 使用默认工作时段生成智能计划
        val defaultWorkingHours = listOf(
            Pair(9, 12),   // 上午 9:00-12:00
            Pair(14, 18),  // 下午 14:00-18:00
            Pair(19, 22)   // 晚上 19:00-22:00
        )
        
        // viewModel.generatePlan(currentDate, emptyList())
    }
    
    private fun showAddEmergencyTaskDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_emergency_task, null)
        
        val editTitle = dialogView.findViewById<android.widget.EditText>(R.id.editEmergencyTitle)
        val editDescription = dialogView.findViewById<android.widget.EditText>(R.id.editEmergencyDescription)
        val editDuration = dialogView.findViewById<android.widget.EditText>(R.id.editEmergencyDuration)
        
        // 设置默认值
        editDuration.setText("60") // 默认1小时
        
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("添加紧急任务")
            .setView(dialogView)
            .setPositiveButton("添加") { _, _ ->
                val title = editTitle.text.toString().trim()
                val description = editDescription.text.toString().trim()
                val durationText = editDuration.text.toString().trim()
                
                if (title.isEmpty()) {
                    showErrorMessage("请输入任务标题")
                    return@setPositiveButton
                }
                
                val duration = try {
                    durationText.toInt()
                } catch (e: NumberFormatException) {
                    60 // 默认1小时
                }
                
                if (duration <= 0 || duration > 480) { // 最多8小时
                    showErrorMessage("任务时长应在1-480分钟之间")
                    return@setPositiveButton
                }
                
                val emergencyTask = com.jingran.taskmanager.data.entity.ShortTermTask(
                    title = title,
                    description = description,
                    deadline = currentDate + (4 * 60 * 60 * 1000L), // 4小时后截止
                    duration = duration,
                    priority = com.jingran.taskmanager.data.entity.TaskPriority.HIGH,
                    taskType = com.jingran.taskmanager.data.entity.TaskType.EMERGENCY,
                    isFlexible = false,
                    isCompleted = false,
                    createTime = System.currentTimeMillis()
                )
                
                viewModel.insertEmergencyTask(emergencyTask, currentDate)
            }
            .setNegativeButton("取消", null)
            .create()
            
        dialog.show()
    }
    
    private fun changeDate(dayOffset: Int) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentDate
            add(Calendar.DAY_OF_MONTH, dayOffset)
        }
        
        currentDate = calendar.timeInMillis
        updateDateDisplay()
        loadTodayPlan()
    }
    
    private fun updateDateDisplay() {
        binding.textViewCurrentDate.text = dateFormat.format(Date(currentDate))
    }
    
    private fun refreshPlan() {
        loadTodayPlan()
    }
    
    /**
     * 刷新数据 - 供外部调用
     */
    fun refreshData() {
        if (isAdded && _binding != null) {
            refreshPlan()
            LogManager.d(TAG, "外部调用刷新数据")
        }
    }
    
    private fun updateBasicStatistics(planItems: List<com.jingran.taskmanager.data.entity.PlanItem>) {
        if (!isAdded || _binding == null) {
            return
        }
        
        CoroutineErrorHandler.safeLifecycleCoroutineExecute(
            lifecycleOwner = this,
            context = requireContext(),
            onError = { errorInfo ->
                LogManager.e(TAG, "更新基本统计信息失败: ${errorInfo.message}", errorInfo.throwable)
                if (isAdded) {
                    showErrorMessage("统计信息更新失败，请刷新页面")
                }
            }
        ) {
            lifecycleScope.launch {
                // 计算基本统计信息
                val totalTasks = planItems.size
                var completedTasks = 0
                for (planItem in planItems) {
                    if (viewModel.isTaskCompleted(planItem.taskId)) {
                        completedTasks++
                    }
                }

                val totalDuration = planItems.sumOf { planItem ->
                    (planItem.endTime - planItem.startTime) / (60 * 1000L) // 转换为分钟
                }

                // 如果没有数据库统计数据，使用基本计算
                val completionRate = if (totalTasks > 0) {
                    (completedTasks.toFloat() / totalTasks * 100).toInt()
                } else {
                    0
                }

                // 确保Fragment仍然附加到Activity再更新UI
                if (isAdded && _binding != null) {
                    binding.textViewTotalTasks.text = "计划任务: $totalTasks"
                    binding.textViewCompletedTasks.text = "已完成: $completedTasks"
                    binding.textViewCompletionRate.text = "完成率: $completionRate%"
                    binding.textViewTotalDuration.text = "总时长: ${totalDuration}分钟"
                    binding.progressBarCompletion.progress = completionRate
                }
            }
        }
    }
    
    private fun updateDailyStatistics(dailyStats: com.jingran.taskmanager.data.entity.DailyStats?) {
        try {
            if (dailyStats != null) {
                // 使用数据库中的统计数据
                binding.textViewTotalTasks.text = "计划任务: ${dailyStats.totalPlannedTasks}"
                binding.textViewCompletedTasks.text = "已完成: ${dailyStats.completedTasks}"
                
                val completionRatePercent = (dailyStats.completionRate * 100).toInt()
                binding.textViewCompletionRate.text = "完成率: $completionRatePercent%"
                
                val efficiencyRatePercent = (dailyStats.efficiencyRate * 100).toInt()
                binding.textViewTotalDuration.text = "计划: ${dailyStats.totalPlannedDuration}分钟 | 实际: ${dailyStats.actualCompletedDuration}分钟"
                
                // 更新进度条
                binding.progressBarCompletion.progress = completionRatePercent
                
                // 显示额外统计信息
                if (dailyStats.emergencyTasksAdded > 0) {
                    showToast("今日新增紧急任务: ${dailyStats.emergencyTasksAdded}个")
                }
                
                if (dailyStats.tasksPostponed > 0) {
                    showToast("推迟任务: ${dailyStats.tasksPostponed}个")
                }
                
            }
        } catch (e: Exception) {
            showErrorMessage("更新每日统计失败: ${e.message}")
        }
    }
    
    private fun onPlanItemClick(planItem: com.jingran.taskmanager.data.entity.PlanItem) {
        // 处理计划项点击事件，可以显示任务详情或编辑
        // 暂时显示时间信息
        val startTime = timeFormat.format(Date(planItem.startTime))
        val endTime = timeFormat.format(Date(planItem.endTime))
        showErrorMessage("计划时间: $startTime - $endTime")
    }
    
    private fun onFixedScheduleClick(schedule: com.jingran.taskmanager.data.entity.FixedSchedule) {
        // 处理固定日程点击事件
        val startTime = timeFormat.format(Date(schedule.startTime))
        val endTime = timeFormat.format(Date(schedule.endTime))
        showMessage("${schedule.title}: $startTime - $endTime")
    }
    
    private fun showMessage(message: String) {
        // 显示一般信息消息
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }
    
    private fun showErrorMessage(message: String) {
        // 显示错误消息，使用更明显的提示方式
        if (isAdded && context != null) {
            try {
                val snackbar = com.google.android.material.snackbar.Snackbar.make(
                    binding.root, 
                    message, 
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                )
                snackbar.setAction("重试") {
                    loadTodayPlan()
                }
                snackbar.show()
            } catch (e: Exception) {
                // 如果Snackbar显示失败，回退到Toast
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    private fun getDayOfWeek(date: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = date }
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }
    
    private fun showImportScheduleDialog() {
        val options = arrayOf("导入Excel文件", "拍照识别课程表")
        
        AlertDialog.Builder(requireContext())
            .setTitle("选择导入方式")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openFilePicker()
                    1 -> openCameraForOCR()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        filePickerLauncher.launch(intent)
    }
    
    private fun openCameraForOCR() {
        // TODO: 实现OCR功能
        Toast.makeText(requireContext(), "OCR功能开发中，敬请期待", Toast.LENGTH_SHORT).show()
    }
    
    private fun handleSelectedFile(uri: Uri) {
        CoroutineErrorHandler.safeLifecycleCoroutineExecute(
            lifecycleOwner = this,
            context = requireContext(),
            onError = { errorInfo ->
                LogManager.e(TAG, "文件导入失败: ${errorInfo.message}", errorInfo.throwable)
                if (isAdded) {
                    Toast.makeText(requireContext(), "导入失败: ${errorInfo.userMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            val schedules = parseExcelFile(uri)
            if (schedules.isNotEmpty()) {
                viewModel.importFixedSchedules(schedules)
                Toast.makeText(requireContext(), "成功导入${schedules.size}个课程", Toast.LENGTH_SHORT).show()
                loadFixedSchedules() // 刷新显示
            } else {
                Toast.makeText(requireContext(), "未找到有效的课程数据", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private suspend fun parseExcelFile(uri: Uri): List<com.jingran.taskmanager.data.entity.FixedSchedule> {
        val schedules = mutableListOf<com.jingran.taskmanager.data.entity.FixedSchedule>()
        
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                // 简化的Excel解析逻辑
                // 假设Excel格式：课程名称 | 地点 | 星期几 | 开始时间 | 结束时间
                val reader = stream.bufferedReader()
                var lineNumber = 0
                
                reader.forEachLine { line ->
                    lineNumber++
                    if (lineNumber == 1) return@forEachLine // 跳过标题行
                    
                    val parts = line.split(",", "\t", "|").map { it.trim() }
                    if (parts.size >= 5) {
                        try {
                            val courseName = parts[0]
                            val location = parts[1].ifEmpty { null }
                            val dayOfWeek = parseDayOfWeek(parts[2])
                            val startTime = parseTimeToMillis(parts[3])
                            val endTime = parseTimeToMillis(parts[4])
                            
                            if (dayOfWeek > 0 && startTime > 0 && endTime > startTime) {
                                schedules.add(
                                    com.jingran.taskmanager.data.entity.FixedSchedule(
                                        title = courseName,
                                        location = location,
                                        startTime = startTime,
                                        endTime = endTime,
                                        dayOfWeek = dayOfWeek,
                                        description = "从Excel导入的课程"
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            // 跳过解析失败的行
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Excel文件解析失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        
        return schedules
    }
    
    private fun parseDayOfWeek(dayStr: String): Int {
        return when (dayStr.lowercase()) {
            "周一", "星期一", "monday", "mon", "1" -> 1
            "周二", "星期二", "tuesday", "tue", "2" -> 2
            "周三", "星期三", "wednesday", "wed", "3" -> 3
            "周四", "星期四", "thursday", "thu", "4" -> 4
            "周五", "星期五", "friday", "fri", "5" -> 5
            "周六", "星期六", "saturday", "sat", "6" -> 6
            "周日", "星期日", "sunday", "sun", "0", "7" -> 7
            else -> 0
        }
    }
    
    private fun parseTimeToMillis(timeStr: String): Long {
        try {
            val parts = timeStr.split(":")
            if (parts.size >= 2) {
                val hour = parts[0].toInt()
                val minute = parts[1].toInt()
                return (hour * 60 + minute) * 60 * 1000L
            }
        } catch (e: Exception) {
            // 解析失败
        }
        return 0L
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}