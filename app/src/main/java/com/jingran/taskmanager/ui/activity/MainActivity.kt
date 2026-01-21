package com.jingran.taskmanager.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.jingran.taskmanager.R
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.jingran.taskmanager.ui.fragment.SmartPlanningFragment
import com.jingran.taskmanager.ui.fragment.TaskListFragment
import com.jingran.taskmanager.viewmodel.ShortTermTaskViewModel
import com.jingran.taskmanager.viewmodel.LongTermTaskViewModel
import com.jingran.taskmanager.viewmodel.PlanningViewModel
import com.jingran.taskmanager.ui.activity.CourseScheduleActivity
import com.jingran.taskmanager.ui.activity.DataSyncActivity
import com.jingran.utils.ErrorHandler
import com.jingran.utils.LogManager
import android.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    
    private lateinit var toolbar: MaterialToolbar
    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigation: BottomNavigationView

    
    private val shortTermTaskViewModel: ShortTermTaskViewModel by viewModels()
    private val longTermTaskViewModel: LongTermTaskViewModel by viewModels()
    private val planningViewModel: PlanningViewModel by viewModels()
    
    // 状态变量
    private var currentPage = 0
    private var lastSelectedTab = R.id.nav_today_plan
    
    // Activity Result Launcher for TaskEditActivity
    private val taskEditLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            LogManager.d(TAG, "任务编辑完成，刷新数据")
            refreshCurrentFragment()
        }
    }
    
    companion object {
        private const val TAG = "MainActivity"
        private const val KEY_CURRENT_PAGE = "current_page"
        private const val KEY_LAST_SELECTED_TAB = "last_selected_tab"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogManager.enterMethod(TAG, "onCreate")
        
        try {
            // 设置布局
            setContentView(R.layout.activity_main)
            LogManager.d(TAG, "布局设置成功")
            
            // 初始化视图组件
            initViews()
            LogManager.d(TAG, "视图初始化完成")
            
            // 恢复保存的状态
            restoreInstanceState(savedInstanceState)
            
            // 设置工具栏
            setupToolbar()
            
            // 设置ViewPager
            setupViewPager()
            
            // 设置底部导航
            setupBottomNavigation()
            

            
            // 恢复页面状态
            restorePageState()
            
            LogManager.exitMethod(TAG, "onCreate", "成功")
            
        } catch (e: Exception) {
            LogManager.logException(TAG, "MainActivity初始化异常", e)
            showInitializationError("应用启动失败: ${e.message}")
        }
    }
    
    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        viewPager = findViewById(R.id.viewPager)
        bottomNavigation = findViewById(R.id.bottomNavigation)
    }
    
    private fun showInitializationError(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
    
    private fun setupViewPager() {
        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter
        
        // 禁用滑动切换
        viewPager.isUserInputEnabled = false
        
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPage = position
                when (position) {
                    0 -> {
                        bottomNavigation.selectedItemId = R.id.nav_today_plan
                        lastSelectedTab = R.id.nav_today_plan
                    }
                    1 -> {
                        bottomNavigation.selectedItemId = R.id.nav_task_list
                        lastSelectedTab = R.id.nav_task_list
                    }
                    // 设置页面不在ViewPager中，所以不需要处理
                }
                updateToolbarTitle(position)
            }
        })
    }
    
    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_today_plan -> {
                    currentPage = 0
                    lastSelectedTab = R.id.nav_today_plan
                    viewPager.currentItem = 0
                    updateToolbarTitle(0)
                    true
                }
                R.id.nav_task_list -> {
                    currentPage = 1
                    lastSelectedTab = R.id.nav_task_list
                    viewPager.currentItem = 1
                    updateToolbarTitle(1)
                    true
                }
                R.id.nav_settings -> {
                    // 打开设置页面
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
    

    
    private fun showTaskTypeSelectionDialog() {
        val dialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_task_type_selection)
            .create()
        
        dialog.show()
        
        // 设置对话框宽度
        val window = dialog.window
        window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        // 设置点击事件
        dialog.findViewById<View>(R.id.layoutShortTermTask)?.setOnClickListener {
            dialog.dismiss()
            // 跳转到短期任务编辑界面
            val intent = Intent(this, TaskEditActivity::class.java)
            intent.putExtra("task_type", "short")
            taskEditLauncher.launch(intent)
        }
        
        dialog.findViewById<View>(R.id.layoutLongTermTask)?.setOnClickListener {
            dialog.dismiss()
            // 跳转到长期任务编辑界面
            val intent = Intent(this, TaskEditActivity::class.java)
            intent.putExtra("task_type", "long")
            taskEditLauncher.launch(intent)
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        // 设置初始标题
        toolbar.title = getString(R.string.today_plan)
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_course_schedule -> {
                startActivity(Intent(this, CourseScheduleActivity::class.java))
                true
            }
            R.id.menu_data_sync -> {
                startActivity(Intent(this, DataSyncActivity::class.java))
                true
            }
            R.id.menu_add_task -> {
                showTaskTypeSelectionDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CURRENT_PAGE, currentPage)
        outState.putInt(KEY_LAST_SELECTED_TAB, lastSelectedTab)
        LogManager.d(TAG, "状态已保存: currentPage=$currentPage, lastSelectedTab=$lastSelectedTab")
    }
    
    private fun restoreInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            currentPage = it.getInt(KEY_CURRENT_PAGE, 0)
            lastSelectedTab = it.getInt(KEY_LAST_SELECTED_TAB, R.id.nav_today_plan)
            LogManager.d(TAG, "状态已恢复: currentPage=$currentPage, lastSelectedTab=$lastSelectedTab")
        }
    }
    
    private fun restorePageState() {
        // 恢复ViewPager页面
        viewPager.setCurrentItem(currentPage, false)
        
        // 恢复底部导航选中状态
        bottomNavigation.selectedItemId = lastSelectedTab
        
        // 恢复工具栏标题
        updateToolbarTitle(currentPage)
    }
    
    private fun updateToolbarTitle(position: Int) {
        toolbar.title = when (position) {
            0 -> getString(R.string.today_plan)
            1 -> getString(R.string.task_list)
            else -> getString(R.string.app_name)
        }
    }
    
    private fun refreshCurrentFragment() {
        try {
            val adapter = viewPager.adapter as? ViewPagerAdapter
            adapter?.let {
                when (currentPage) {
                    0 -> {
                        // 刷新今日计划页面
                        val fragment = supportFragmentManager.findFragmentByTag("f0") as? SmartPlanningFragment
                        fragment?.refreshData()
                        LogManager.d(TAG, "刷新今日计划页面")
                    }
                    1 -> {
                        // 刷新任务列表页面
                        val fragment = supportFragmentManager.findFragmentByTag("f1") as? TaskListFragment
                        fragment?.refreshData()
                        LogManager.d(TAG, "刷新任务列表页面")
                    }
                }
            }
        } catch (e: Exception) {
            LogManager.logException(TAG, "刷新页面时发生异常", e)
        }
    }
    
    private inner class ViewPagerAdapter(fragmentActivity: FragmentActivity) : 
        FragmentStateAdapter(fragmentActivity) {
        
        override fun getItemCount(): Int = 2
        
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> SmartPlanningFragment()
                1 -> TaskListFragment()
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }
    }
}