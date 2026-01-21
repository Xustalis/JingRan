package com.jingran.taskmanager.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.tabs.TabLayout
import com.jingran.taskmanager.R
import com.jingran.taskmanager.ui.activity.TaskEditActivity
import com.jingran.taskmanager.ui.adapter.ShortTermTaskAdapter
import com.jingran.taskmanager.ui.adapter.LongTermTaskAdapter
import com.jingran.utils.ErrorHandler
import com.jingran.utils.LogManager
import com.jingran.taskmanager.viewmodel.ShortTermTaskViewModel
import com.jingran.taskmanager.viewmodel.LongTermTaskViewModel
import com.jingran.utils.SwipeToDeleteCallback
import kotlinx.coroutines.launch

class TaskListFragment : Fragment() {
    
    companion object {
        private const val TAG = "TaskListFragment"
        private const val KEY_CURRENT_TAB = "current_tab"
        private const val KEY_SCROLL_POSITION = "scroll_position"
    }
    
    private val shortTermTaskViewModel: ShortTermTaskViewModel by viewModels()
    private val longTermTaskViewModel: LongTermTaskViewModel by viewModels()
    
    private lateinit var toggleGroup: MaterialButtonToggleGroup
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var shortTermAdapter: ShortTermTaskAdapter
    private lateinit var longTermAdapter: LongTermTaskAdapter
    
    private var currentTab = 0 // 0: 短期任务, 1: 长期任务
    private var savedScrollPosition = 0
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_task_list, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        LogManager.enterMethod(TAG, "onViewCreated")
        
        // 恢复保存的状态
        restoreInstanceState(savedInstanceState)
        
        ErrorHandler.safeExecute(
            context = requireContext(),
            showErrorToUser = true,
            onError = { errorInfo ->
                LogManager.e(TAG, "Fragment初始化失败: ${errorInfo.message}")
            }
        ) {
            initViews(view)
            setupRecyclerView()
            setupTabLayout()
            observeViewModel()
            
            // 恢复标签页状态
            restoreTabState()
            setupToggleGroup()
            
            LogManager.exitMethod(TAG, "onViewCreated", "成功")
        }
    }
    
    private fun initViews(view: View) {
        toggleGroup = view.findViewById(R.id.toggleGroup)
        tabLayout = view.findViewById(R.id.tabLayout)
        recyclerView = view.findViewById(R.id.recyclerViewTasks)
    }
    
    private fun setupRecyclerView() {
        // 短期任务适配器
        shortTermAdapter = ShortTermTaskAdapter(
            onItemClick = { task: com.jingran.taskmanager.data.entity.ShortTermTask ->
                // 编辑任务
                val intent = Intent(requireContext(), TaskEditActivity::class.java)
                intent.putExtra("task_id", task.id)
                intent.putExtra("task_type", "short")
                startActivity(intent)
            },
            onCompleteClick = { task: com.jingran.taskmanager.data.entity.ShortTermTask ->
                // 切换完成状态
                com.jingran.utils.CoroutineErrorHandler.safeLifecycleCoroutineExecute(
                    lifecycleOwner = this@TaskListFragment,
                    context = requireContext(),
                    onError = { errorInfo ->
                        Toast.makeText(requireContext(), "任务状态更新失败: ${errorInfo.userMessage}", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    val updatedTask = task.copy(isCompleted = !task.isCompleted)
                    shortTermTaskViewModel.updateShortTermTask(updatedTask)
                }
            },
            onDeleteClick = { task: com.jingran.taskmanager.data.entity.ShortTermTask ->
                // 删除任务
                com.jingran.utils.CoroutineErrorHandler.safeLifecycleCoroutineExecute(
                    lifecycleOwner = this@TaskListFragment,
                    context = requireContext(),
                    onError = { errorInfo ->
                        Toast.makeText(requireContext(), "任务删除失败: ${errorInfo.userMessage}", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    shortTermTaskViewModel.deleteShortTermTask(task)
                }
            }
        )
        
        // 长期任务适配器
        longTermAdapter = LongTermTaskAdapter(
            onItemClick = { task: com.jingran.taskmanager.data.entity.LongTermTask ->
                // 编辑任务
                val intent = Intent(requireContext(), TaskEditActivity::class.java)
                intent.putExtra("task_id", task.id)
                intent.putExtra("task_type", "long")
                startActivity(intent)
            },
            onCompleteClick = { task: com.jingran.taskmanager.data.entity.LongTermTask ->
                // 切换完成状态
                com.jingran.utils.CoroutineErrorHandler.safeLifecycleCoroutineExecute(
                    lifecycleOwner = this@TaskListFragment,
                    context = requireContext(),
                    onError = { errorInfo ->
                        Toast.makeText(requireContext(), "任务状态更新失败: ${errorInfo.userMessage}", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    val updatedTask = task.copy(isCompleted = !task.isCompleted)
                    longTermTaskViewModel.updateLongTermTask(updatedTask)
                }
            },
            onDeleteClick = { task: com.jingran.taskmanager.data.entity.LongTermTask ->
                // 删除任务
                com.jingran.utils.CoroutineErrorHandler.safeLifecycleCoroutineExecute(
                    lifecycleOwner = this@TaskListFragment,
                    context = requireContext(),
                    onError = { errorInfo ->
                        Toast.makeText(requireContext(), "任务删除失败: ${errorInfo.userMessage}", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    longTermTaskViewModel.deleteLongTermTask(task)
                }
            }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        setupSwipeToDelete()
    }

    private fun setupSwipeToDelete() {
        val swipeHandler = object : SwipeToDeleteCallback() {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                when (currentTab) {
                    0 -> {
                        val task = shortTermAdapter.currentList[position]
                        shortTermTaskViewModel.deleteShortTermTask(task)
                        Toast.makeText(context, "短期任务已删除", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        val task = longTermAdapter.currentList[position]
                        longTermTaskViewModel.deleteLongTermTask(task)
                        Toast.makeText(context, "长期任务已删除", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }
    
    private fun setupToggleGroup() {
        toggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                currentTab = when (checkedId) {
                    R.id.btnShortTerm -> 0
                    R.id.btnLongTerm -> 1
                    else -> 0
                }
                savedScrollPosition = 0
                when (currentTab) {
                    0 -> loadShortTermTasks()
                    1 -> loadLongTermTasks()
                }
            }
        }
    }

    private fun setupTabLayout() {
        // Obsolete, functionality moved to setupToggleGroup()
    }
    
    private fun observeViewModel() {
        LogManager.d(TAG, "开始观察ViewModel数据")
        
        // 观察短期任务
        shortTermTaskViewModel.allShortTermTasks.observe(viewLifecycleOwner) { tasks: List<com.jingran.taskmanager.data.entity.ShortTermTask>? ->
            LogManager.d(TAG, "收到短期任务数据: ${tasks?.size ?: 0}个任务")
            ErrorHandler.safeExecute(
                context = requireContext(),
                showErrorToUser = false,
                onError = { errorInfo ->
                    LogManager.e(TAG, "更新短期任务列表失败: ${errorInfo.message}")
                }
            ) {
                shortTermAdapter.submitList(tasks)
                // 如果当前显示的是短期任务标签页，恢复滚动位置
                if (currentTab == 0) {
                    restoreScrollPosition()
                }
            }
        }
        
        // 观察长期任务
        longTermTaskViewModel.allLongTermTasks.observe(viewLifecycleOwner) { tasks: List<com.jingran.taskmanager.data.entity.LongTermTask>? ->
            LogManager.d(TAG, "收到长期任务数据: ${tasks?.size ?: 0}个任务")
            ErrorHandler.safeExecute(
                context = requireContext(),
                showErrorToUser = false,
                onError = { errorInfo ->
                    LogManager.e(TAG, "更新长期任务列表失败: ${errorInfo.message}")
                }
            ) {
                longTermAdapter.submitList(tasks)
                // 如果当前显示的是长期任务标签页，恢复滚动位置
                if (currentTab == 1) {
                    restoreScrollPosition()
                }
            }
        }
    }
    
    private fun loadShortTermTasks() {
        LogManager.d(TAG, "加载短期任务列表")
        recyclerView.adapter = shortTermAdapter
        // 不直接使用value，而是依赖Observer来更新数据
        // Observer已经在observeViewModel中设置，会自动更新UI
    }
    
    private fun loadLongTermTasks() {
        LogManager.d(TAG, "加载长期任务列表")
        recyclerView.adapter = longTermAdapter
        // 不直接使用value，而是依赖Observer来更新数据
        // Observer已经在observeViewModel中设置，会自动更新UI
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CURRENT_TAB, currentTab)
        
        // 保存当前滚动位置
        saveCurrentScrollPosition()
        outState.putInt(KEY_SCROLL_POSITION, savedScrollPosition)
        
        LogManager.d(TAG, "状态已保存: currentTab=$currentTab, scrollPosition=$savedScrollPosition")
    }
    
    private fun restoreInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            currentTab = it.getInt(KEY_CURRENT_TAB, 0)
            savedScrollPosition = it.getInt(KEY_SCROLL_POSITION, 0)
            LogManager.d(TAG, "状态已恢复: currentTab=$currentTab, scrollPosition=$savedScrollPosition")
        }
    }
    
    private fun restoreTabState() {
        // 恢复 ToggleGroup 选中状态
        val checkedId = when (currentTab) {
            0 -> R.id.btnShortTerm
            1 -> R.id.btnLongTerm
            else -> R.id.btnShortTerm
        }
        toggleGroup.check(checkedId)

        // 保持兼容性恢复标签页选中状态（如果 TabLayout 还在发挥作用）
        tabLayout.getTabAt(currentTab)?.select()
        
        // 加载对应的任务列表
        when (currentTab) {
            0 -> loadShortTermTasks()
            1 -> loadLongTermTasks()
        }
    }
    
    private fun saveCurrentScrollPosition() {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
        layoutManager?.let {
            savedScrollPosition = it.findFirstVisibleItemPosition()
        }
    }
    
    private fun restoreScrollPosition() {
        if (savedScrollPosition > 0) {
            recyclerView.post {
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                layoutManager?.scrollToPosition(savedScrollPosition)
                savedScrollPosition = 0 // 重置，避免重复恢复
            }
        }
    }
    
    /**
     * 刷新数据 - 供外部调用
     */
    fun refreshData() {
        if (isAdded) {
            // 重新加载当前标签页的数据
            when (currentTab) {
                0 -> loadShortTermTasks()
                1 -> loadLongTermTasks()
            }
            LogManager.d(TAG, "外部调用刷新数据，当前标签页: $currentTab")
        }
    }
}