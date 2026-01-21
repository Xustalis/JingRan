package com.jingran.taskmanager.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jingran.taskmanager.data.entity.PlanItem
import com.jingran.taskmanager.databinding.ItemPlanItemBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * 计划项适配器
 */
class PlanItemAdapter(
    private val onItemClick: (PlanItem) -> Unit
) : ListAdapter<PlanItem, PlanItemAdapter.PlanItemViewHolder>(PlanItemDiffCallback()) {
    
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanItemViewHolder {
        val binding = ItemPlanItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlanItemViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: PlanItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class PlanItemViewHolder(
        private val binding: ItemPlanItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(planItem: PlanItem) {
            binding.apply {
                // 设置时间
                val startTime = timeFormat.format(Date(planItem.startTime))
                val endTime = timeFormat.format(Date(planItem.endTime))
                textViewTime.text = "$startTime - $endTime"
                
                // 设置任务标题（这里需要从任务ID获取任务信息）
                textViewTaskTitle.text = "任务 #${planItem.taskId}"
                
                // 计算时长
                val durationMinutes = (planItem.endTime - planItem.startTime) / (60 * 1000L)
                textViewDuration.text = "${durationMinutes}分钟"
                
                // 设置点击事件
                root.setOnClickListener {
                    onItemClick(planItem)
                }
            }
        }
    }
    
    class PlanItemDiffCallback : DiffUtil.ItemCallback<PlanItem>() {
        override fun areItemsTheSame(oldItem: PlanItem, newItem: PlanItem): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: PlanItem, newItem: PlanItem): Boolean {
            return oldItem == newItem
        }
    }
}