package com.jingran.taskmanager.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jingran.taskmanager.R
import com.jingran.utils.LogManager
import android.widget.LinearLayout
import android.widget.TextView
import android.view.View

class SettingsActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "SettingsActivity"
        private const val KEY_LAST_CLICKED_ITEM = "last_clicked_item"
    }
    
    // 状态变量
    private var lastClickedItem: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogManager.enterMethod(TAG, "onCreate")
        
        // 恢复状态
        restoreInstanceState(savedInstanceState)
        
        setContentView(R.layout.activity_settings)
        
        initViews()
        setupClickListeners()
        
        LogManager.exitMethod(TAG, "onCreate", "成功")
    }
    
    private fun initViews() {
        // 设置标题
        findViewById<TextView>(R.id.tvTitle).text = getString(R.string.settings)
    }
    
    private fun setupClickListeners() {
        // 固定日程管理
        findViewById<LinearLayout>(R.id.layoutFixedSchedule).setOnClickListener {
            lastClickedItem = "fixed_schedule"
            // TODO: 跳转到固定日程管理页面
            LogManager.d(TAG, "点击固定日程管理")
        }
        
        // 系统设置
        findViewById<LinearLayout>(R.id.layoutSystemSettings).setOnClickListener {
            lastClickedItem = "system_settings"
            // TODO: 跳转到系统设置页面
            LogManager.d(TAG, "点击系统设置")
        }
        
        // 返回按钮
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        lastClickedItem?.let {
            outState.putString(KEY_LAST_CLICKED_ITEM, it)
        }
        LogManager.d(TAG, "状态已保存: lastClickedItem=$lastClickedItem")
    }
    
    private fun restoreInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            lastClickedItem = it.getString(KEY_LAST_CLICKED_ITEM)
            LogManager.d(TAG, "状态已恢复: lastClickedItem=$lastClickedItem")
        }
    }
}