package com.jingran.taskmanager.ui.fragment

import android.os.Bundle
import android.widget.Toast
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.jingran.taskmanager.R
import com.jingran.taskmanager.ui.adapter.PlanItemAdapter
import com.jingran.taskmanager.viewmodel.TaskViewModel
import com.jingran.utils.LogManager
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PlanFragment : Fragment() {
    
    companion object {
        private const val TAG = "PlanFragment"
        private const val KEY_SCROLL_POSITION = "scroll_position"
        private const val KEY_PLAN_DATE = "plan_date"
    }
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnGeneratePlan: MaterialButton
    private lateinit var progressIndicator: CircularProgressIndicator
    
    private val taskViewModel: TaskViewModel by activityViewModels()
    private lateinit var planAdapter: PlanItemAdapter
    
    // 状态变量
    private var savedScrollPosition: Int = 0
    private var currentPlanDate: LocalDate = LocalDate.now()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_plan, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 恢复状态
        restoreInstanceState(savedInstanceState)
        
        initViews(view)
        setupRecyclerView()
        observeViewModel()
        
        // 加载今日计划
        loadTodayPlan()
        
        LogManager.d(TAG, "PlanFragment视图已创建")
    }
    
    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewPlan)
        btnGeneratePlan = view.findViewById(R.id.btnGeneratePlan)
        progressIndicator = view.findViewById(R.id.progressIndicator)
        
        btnGeneratePlan.setOnClickListener {
            generateTodayPlan()
        }
    }
    
    private fun setupRecyclerView() {
        planAdapter = PlanItemAdapter { planItem: com.jingran.taskmanager.data.entity.PlanItem ->
            // 切换完成状态
            com.jingran.utils.CoroutineErrorHandler.safeLifecycleCoroutineExecute(
                lifecycleOwner = this@PlanFragment,
                context = requireContext(),
                onError = { errorInfo ->
                    Toast.makeText(requireContext(), "计划项状态更新失败: ${errorInfo.userMessage}", Toast.LENGTH_SHORT).show()
                }
            ) {
                taskViewModel.updatePlanItemCompletion(planItem.id, !planItem.isCompleted)
            }
        }
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = planAdapter
        }
    }
    
    private fun observeViewModel() {
        taskViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        taskViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                // 显示错误信息
                // 可以使用 Snackbar 或 Toast
            }
        }
    }
    
    private fun loadTodayPlan() {
        taskViewModel.todayPlanItems.observe(viewLifecycleOwner) { planItems: List<com.jingran.taskmanager.data.entity.PlanItem>? ->
            planAdapter.submitList(planItems)
            // 恢复滚动位置
            restoreScrollPosition()
        }
    }
    
    private fun generateTodayPlan() {
        com.jingran.utils.CoroutineErrorHandler.safeLifecycleCoroutineExecute(
            lifecycleOwner = this@PlanFragment,
            context = requireContext(),
            onError = { errorInfo ->
                Toast.makeText(requireContext(), "今日计划生成失败: ${errorInfo.userMessage}", Toast.LENGTH_SHORT).show()
            }
        ) {
            taskViewModel.regenerateTodayPlan()
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        
        // 保存当前滚动位置
        saveCurrentScrollPosition()
        outState.putInt(KEY_SCROLL_POSITION, savedScrollPosition)
        
        // 保存当前计划日期
        outState.putString(KEY_PLAN_DATE, currentPlanDate.toString())
        
        LogManager.d(TAG, "状态已保存: scrollPosition=$savedScrollPosition, planDate=$currentPlanDate")
    }
    
    private fun restoreInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            savedScrollPosition = it.getInt(KEY_SCROLL_POSITION, 0)
            val dateString = it.getString(KEY_PLAN_DATE)
            if (!dateString.isNullOrEmpty()) {
                try {
                    currentPlanDate = LocalDate.parse(dateString)
                } catch (e: Exception) {
                    LogManager.e(TAG, "恢复计划日期失败: ${e.message}")
                    currentPlanDate = LocalDate.now()
                }
            }
            LogManager.d(TAG, "状态已恢复: scrollPosition=$savedScrollPosition, planDate=$currentPlanDate")
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
}