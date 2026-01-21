package com.jingran.taskmanager.ui.component

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.ViewGroup
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.NumberPicker
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.time.YearMonth

class IOSStyleDateTimePicker(
    context: Context,
    private val onDateTimeSelected: (LocalDateTime) -> Unit
) : Dialog(context) {
    
    private lateinit var yearPicker: NumberPicker
    private lateinit var monthPicker: NumberPicker
    private lateinit var dayPicker: NumberPicker
    private lateinit var hourPicker: NumberPicker
    private lateinit var minutePicker: NumberPicker
    private lateinit var titleText: TextView
    private lateinit var cancelButton: Button
    private lateinit var confirmButton: Button
    
    private val monthNames = arrayOf(
        "1月", "2月", "3月", "4月", "5月", "6月",
        "7月", "8月", "9月", "10月", "11月", "12月"
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 创建主容器
        val mainLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(0, 0, 0, 0)
        }
        
        // 创建顶部导航栏
        val headerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(54)
            )
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(16), 0, dpToPx(16), 0)
            setBackgroundColor(Color.parseColor("#F8F8F8"))
        }
        
        // 取消按钮
        cancelButton = Button(context).apply {
            text = "取消"
            setTextColor(Color.parseColor("#007AFF"))
            textSize = 17f
            background = null
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        
        // 标题
        titleText = TextView(context).apply {
            text = "选择时间"
            setTextColor(Color.BLACK)
            textSize = 17f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        
        // 确认按钮
        confirmButton = Button(context).apply {
            text = "确认"
            setTextColor(Color.parseColor("#007AFF"))
            textSize = 17f
            background = null
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        
        headerLayout.addView(cancelButton)
        headerLayout.addView(titleText)
        headerLayout.addView(confirmButton)
        
        // 创建分隔线
        val separator = View(context).apply {
            setBackgroundColor(Color.parseColor("#E5E5EA"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                1
            )
        }
        
        // 创建选择器容器
        val pickerContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(200)
            )
            gravity = Gravity.CENTER
            setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))
        }
        
        // 创建年份选择器
        yearPicker = createNumberPicker().apply {
            minValue = 2020
            maxValue = 2030
            value = LocalDateTime.now().year
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            )
        }
        
        // 创建月份选择器
        monthPicker = createNumberPicker().apply {
            minValue = 1
            maxValue = 12
            value = LocalDateTime.now().monthValue
            displayedValues = monthNames
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            )
        }
        
        // 创建日期选择器
        dayPicker = createNumberPicker().apply {
            minValue = 1
            maxValue = 31
            value = LocalDateTime.now().dayOfMonth
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            )
        }
        
        // 创建小时选择器
         hourPicker = createNumberPicker().apply {
             minValue = 0
             maxValue = 23
             value = LocalDateTime.now().hour
             setFormatter { value ->
                 String.format("%02d时", value)
             }
             layoutParams = LinearLayout.LayoutParams(
                 0,
                 ViewGroup.LayoutParams.MATCH_PARENT,
                 1f
             )
         }
        
        // 创建分钟选择器
         minutePicker = createNumberPicker().apply {
             minValue = 0
             maxValue = 59
             value = LocalDateTime.now().minute
             setFormatter { value ->
                 String.format("%02d分", value)
             }
             layoutParams = LinearLayout.LayoutParams(
                 0,
                 ViewGroup.LayoutParams.MATCH_PARENT,
                 1f
             )
         }
        
        // 设置年月日联动
        setupDatePickerListeners()
        
        // 添加选择器到容器
        pickerContainer.addView(yearPicker)
        pickerContainer.addView(monthPicker)
        pickerContainer.addView(dayPicker)
        pickerContainer.addView(hourPicker)
        pickerContainer.addView(minutePicker)
        
        // 组装布局
        mainLayout.addView(headerLayout)
        mainLayout.addView(separator)
        mainLayout.addView(pickerContainer)
        
        setContentView(mainLayout)
        
        // 设置对话框属性
        window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.BOTTOM)
            setBackgroundDrawableResource(android.R.color.transparent)
            // 添加圆角背景
            decorView.setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
        }
        
        setupClickListeners()
    }
    
    private fun createNumberPicker(): NumberPicker {
        return NumberPicker(context).apply {
            // 设置选择器样式
            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            wrapSelectorWheel = true
            
            // 尝试隐藏分割线（可能在某些设备上不生效）
            try {
                val fields = NumberPicker::class.java.declaredFields
                for (field in fields) {
                    if (field.name == "mSelectionDivider") {
                        field.isAccessible = true
                        field.set(this, null)
                        break
                    }
                }
            } catch (e: Exception) {
                // 忽略异常，使用默认样式
            }
        }
    }
    
    private fun setupDatePickerListeners() {
        val updateDayPicker = {
            val year = yearPicker.value
            val month = monthPicker.value
            val daysInMonth = YearMonth.of(year, month).lengthOfMonth()
            
            val currentDay = dayPicker.value
            dayPicker.maxValue = daysInMonth
            
            // 如果当前选中的日期超过了该月的最大天数，调整到最大天数
            if (currentDay > daysInMonth) {
                dayPicker.value = daysInMonth
            }
        }
        
        yearPicker.setOnValueChangedListener { _, _, _ -> updateDayPicker() }
        monthPicker.setOnValueChangedListener { _, _, _ -> updateDayPicker() }
    }
    
    private fun setupClickListeners() {
        cancelButton.setOnClickListener {
            dismiss()
        }
        
        confirmButton.setOnClickListener {
            val selectedDateTime = LocalDateTime.of(
                yearPicker.value,
                monthPicker.value,
                dayPicker.value,
                hourPicker.value,
                minutePicker.value
            )
            onDateTimeSelected(selectedDateTime)
            dismiss()
        }
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
    
    fun setTitle(title: String) {
        titleText.text = title
    }
    
    fun setInitialDateTime(dateTime: LocalDateTime?) {
        dateTime?.let {
            yearPicker.value = it.year
            monthPicker.value = it.monthValue
            dayPicker.value = it.dayOfMonth
            hourPicker.value = it.hour
            minutePicker.value = it.minute
        }
    }
    
    companion object {
        fun show(context: Context, onDateTimeSelected: (LocalDateTime) -> Unit) {
            val picker = IOSStyleDateTimePicker(context, onDateTimeSelected)
            picker.show()
        }
        
        fun show(context: Context, title: String, initialDateTime: LocalDateTime?, onDateTimeSelected: (LocalDateTime) -> Unit) {
            val picker = IOSStyleDateTimePicker(context, onDateTimeSelected)
            picker.setTitle(title)
            picker.setInitialDateTime(initialDateTime)
            picker.show()
        }
    }
}