package com.jingran.taskmanager.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jingran.taskmanager.R
import com.jingran.taskmanager.data.entity.ShortTermTask
import com.jingran.taskmanager.data.entity.TaskPriority
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * iOS Reminders 风格的短期任务适配器
 */
class ShortTermTaskAdapter(
    private val onItemClick: (ShortTermTask) -> Unit,
    private val onCompleteClick: (ShortTermTask) -> Unit,
    private val onDeleteClick: (ShortTermTask) -> Unit // 保留接口以支持 swipe-to-delete
) : ListAdapter<ShortTermTask, ShortTermTaskAdapter.TaskViewHolder>(
    TaskDiffCallback()
) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_short_term_task, parent, false)
        return TaskViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: View = itemView.findViewById(R.id.itemContainer)
        private val checkIcon: ImageView = itemView.findViewById(R.id.checkBoxComplete)
        private val textTitle: TextView = itemView.findViewById(R.id.textTitle)
        private val textSubtitle: TextView = itemView.findViewById(R.id.textSubtitle)
        private val chevronIcon: ImageView = itemView.findViewById(R.id.chevronIcon)
        
        fun bind(task: ShortTermTask) {
            // 标题
            textTitle.text = task.title
            
            // 副标题：组合截止时间、优先级、时长
            val subtitleParts = mutableListOf<String>()
            
            task.deadline?.let { deadlineTimestamp ->
                val deadline = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(deadlineTimestamp), 
                    ZoneOffset.systemDefault()
                )
                val formatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")
                subtitleParts.add(deadline.format(formatter))
            }
            
            val priorityText = when (task.priority) {
                TaskPriority.URGENT -> "紧急"
                TaskPriority.HIGH -> "高优先级"
                TaskPriority.MEDIUM -> "中优先级"
                TaskPriority.LOW -> "低优先级"
            }
            subtitleParts.add(priorityText)
            
            subtitleParts.add("${task.duration}分钟")
            
            textSubtitle.text = subtitleParts.joinToString(" · ")
            
            // 完成状态图标
            if (task.isCompleted) {
                checkIcon.setImageResource(R.drawable.ic_circle_check)
                container.alpha = 0.5f
            } else {
                checkIcon.setImageResource(R.drawable.ic_circle_outline)
                container.alpha = 1.0f
            }
            
            // 点击事件
            container.setOnClickListener {
                onItemClick(task)
            }
            
            checkIcon.setOnClickListener {
                onCompleteClick(task)
            }
            
            chevronIcon.setOnClickListener {
                onItemClick(task)
            }
        }
    }
    
    private class TaskDiffCallback : DiffUtil.ItemCallback<ShortTermTask>() {
        override fun areItemsTheSame(oldItem: ShortTermTask, newItem: ShortTermTask): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: ShortTermTask, newItem: ShortTermTask): Boolean {
            return oldItem == newItem
        }
    }
}