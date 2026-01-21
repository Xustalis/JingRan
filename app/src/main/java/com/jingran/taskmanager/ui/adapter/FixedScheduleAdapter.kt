package com.jingran.taskmanager.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jingran.taskmanager.data.entity.FixedSchedule
import com.jingran.taskmanager.data.entity.RecurrenceType
import com.jingran.taskmanager.databinding.ItemFixedScheduleBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * 固定日程适配器
 */
class FixedScheduleAdapter(
    private val onItemClick: (FixedSchedule) -> Unit
) : ListAdapter<FixedSchedule, FixedScheduleAdapter.FixedScheduleViewHolder>(FixedScheduleDiffCallback()) {
    
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FixedScheduleViewHolder {
        val binding = ItemFixedScheduleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FixedScheduleViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: FixedScheduleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class FixedScheduleViewHolder(
        private val binding: ItemFixedScheduleBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(schedule: FixedSchedule) {
            binding.apply {
                // 设置标题
                textViewTitle.text = schedule.title
                
                // 设置时间
                val startTime = timeFormat.format(Date(schedule.startTime))
                val endTime = timeFormat.format(Date(schedule.endTime))
                textViewTime.text = "$startTime - $endTime"
                
                // 设置地点（如果有）
                if (!schedule.location.isNullOrEmpty()) {
                    textViewLocation.text = schedule.location
                    textViewLocation.visibility = android.view.View.VISIBLE
                } else {
                    textViewLocation.visibility = android.view.View.GONE
                }
                
                // 设置描述（如果有）
                if (!schedule.description.isNullOrEmpty()) {
                    textViewDescription.text = schedule.description
                    textViewDescription.visibility = android.view.View.VISIBLE
                } else {
                    textViewDescription.visibility = android.view.View.GONE
                }
                
                // 设置重复类型指示器
                val recurrenceText = when (schedule.recurrenceType) {
                    RecurrenceType.DAILY -> "每日"
                    RecurrenceType.WEEKLY -> "每周"
                    RecurrenceType.MONTHLY -> "每月"
                    RecurrenceType.NONE -> "单次"
                    else -> ""
                }
                
                if (recurrenceText.isNotEmpty()) {
                    textViewRecurrence.text = recurrenceText
                    textViewRecurrence.visibility = android.view.View.VISIBLE
                } else {
                    textViewRecurrence.visibility = android.view.View.GONE
                }
                
                // 设置点击事件
                root.setOnClickListener {
                    onItemClick(schedule)
                }
            }
        }
    }
    
    class FixedScheduleDiffCallback : DiffUtil.ItemCallback<FixedSchedule>() {
        override fun areItemsTheSame(oldItem: FixedSchedule, newItem: FixedSchedule): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: FixedSchedule, newItem: FixedSchedule): Boolean {
            return oldItem == newItem
        }
    }
}