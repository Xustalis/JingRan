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
import android.widget.RadioButton
import android.widget.RadioGroup

class IOSStylePriorityPicker(
    context: Context,
    private val currentPriority: String,
    private val onPrioritySelected: (String, String) -> Unit
) : Dialog(context) {
    
    private lateinit var titleText: TextView
    private lateinit var cancelButton: Button
    private lateinit var confirmButton: Button
    private lateinit var radioGroup: RadioGroup
    
    private val priorities = listOf(
        "high" to "高",
        "medium" to "中", 
        "low" to "低"
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
                dpToPx(44)
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
            text = "选择优先级"
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
        val separator = android.view.View(context).apply {
            setBackgroundColor(Color.parseColor("#E5E5EA"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                1
            )
        }
        
        // 创建选项容器
        val optionsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, dpToPx(20), 0, dpToPx(20))
        }
        
        // 创建RadioGroup
        radioGroup = RadioGroup(context).apply {
            orientation = RadioGroup.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        
        // 添加优先级选项
        priorities.forEachIndexed { index, (value, displayText) ->
            val radioButton = RadioButton(context).apply {
                text = displayText
                textSize = 17f
                setTextColor(Color.BLACK)
                setPadding(dpToPx(20), dpToPx(12), dpToPx(20), dpToPx(12))
                layoutParams = RadioGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                id = index
                isChecked = (value == currentPriority)
            }
            
            radioGroup.addView(radioButton)
            
            // 添加分隔线（除了最后一个）
            if (index < priorities.size - 1) {
                val divider = android.view.View(context).apply {
                    setBackgroundColor(Color.parseColor("#E5E5EA"))
                    layoutParams = RadioGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1
                    ).apply {
                        setMargins(dpToPx(20), 0, dpToPx(20), 0)
                    }
                }
                radioGroup.addView(divider)
            }
        }
        
        optionsLayout.addView(radioGroup)
        
        // 组装布局
        mainLayout.addView(headerLayout)
        mainLayout.addView(separator)
        mainLayout.addView(optionsLayout)
        
        setContentView(mainLayout)
        
        // 设置对话框属性
        window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.BOTTOM)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
        cancelButton.setOnClickListener {
            dismiss()
        }
        
        confirmButton.setOnClickListener {
            val selectedIndex = radioGroup.checkedRadioButtonId
            if (selectedIndex != -1 && selectedIndex < priorities.size) {
                val (value, displayText) = priorities[selectedIndex]
                onPrioritySelected(value, displayText)
            }
            dismiss()
        }
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
    
    companion object {
        fun show(context: Context, currentPriority: String, onPrioritySelected: (String) -> Unit) {
            val picker = IOSStylePriorityPicker(context, currentPriority) { value, _ ->
                onPrioritySelected(value)
            }
            picker.show()
        }
    }
}