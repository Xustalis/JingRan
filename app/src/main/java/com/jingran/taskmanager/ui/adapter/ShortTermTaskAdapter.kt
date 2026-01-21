package com.jingran.taskmanager.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.checkbox.MaterialCheckBox
import com.jingran.taskmanager.R
import com.jingran.taskmanager.data.entity.ShortTermTask
import com.jingran.taskmanager.data.entity.TaskPriority
import androidx.core.content.ContextCompat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneOffset

class ShortTermTaskAdapter(
    private val onItemClick: (ShortTermTask) -> Unit,
    private val onCompleteClick: (ShortTermTask) -> Unit,
    private val onDeleteClick: (ShortTermTask) -> Unit
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
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        private val checkBox: MaterialCheckBox = itemView.findViewById(R.id.checkBoxComplete)
        private val textTitle: TextView = itemView.findViewById(R.id.textTitle)
        private val textDeadline: TextView = itemView.findViewById(R.id.textDeadline)
        private val textDuration: TextView = itemView.findViewById(R.id.textDuration)
        private val textPriority: TextView = itemView.findViewById(R.id.textPriority)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        
        fun bind(task: ShortTermTask) {
            textTitle.text = task.title
            
            // 显示截止时间
            task.deadline?.let { deadlineTimestamp ->
                val deadline = LocalDateTime.ofInstant(Instant.ofEpochMilli(deadlineTimestamp), ZoneOffset.systemDefault())
                val formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")
                textDeadline.text = "截止: ${deadline.format(formatter)}"
                textDeadline.visibility = View.VISIBLE
            } ?: run {
                textDeadline.visibility = View.GONE
            }
            
            // 显示时长
            textDuration.text = "时长: ${task.duration}分钟"
            
            // 显示优先级
            val priorityText = when (task.priority) {
                TaskPriority.HIGH -> "高"
                TaskPriority.MEDIUM -> "中"
                TaskPriority.LOW -> "低"
                TaskPriority.URGENT -> "紧急"
            }
            textPriority.text = "优先级: $priorityText"
            
            // 根据优先级设置颜色
            val priorityColor = when (task.priority) {
                TaskPriority.HIGH -> ContextCompat.getColor(itemView.context, R.color.ios_red)
                TaskPriority.MEDIUM -> ContextCompat.getColor(itemView.context, R.color.ios_orange)
                TaskPriority.LOW -> ContextCompat.getColor(itemView.context, R.color.ios_blue)
                TaskPriority.URGENT -> ContextCompat.getColor(itemView.context, R.color.ios_red)
            }
            textPriority.setTextColor(priorityColor)
            
            // 设置完成状态
            checkBox.isChecked = task.isCompleted
            
            // 如果任务已完成，设置不同的样式
            if (task.isCompleted) {
                cardView.alpha = 0.6f
                textTitle.alpha = 0.6f
            } else {
                cardView.alpha = 1.0f
                textTitle.alpha = 1.0f
            }
            
            // 设置点击事件
            cardView.setOnClickListener {
                onItemClick(task)
            }
            
            checkBox.setOnClickListener {
                onCompleteClick(task)
            }
            
            btnDelete.setOnClickListener {
                onDeleteClick(task)
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