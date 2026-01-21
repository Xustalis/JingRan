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
import com.jingran.taskmanager.data.entity.LongTermTask
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * iOS Reminders 风格的长期任务适配器
 */
class LongTermTaskAdapter(
    private val onItemClick: (LongTermTask) -> Unit,
    private val onCompleteClick: (LongTermTask) -> Unit,
    private val onDeleteClick: (LongTermTask) -> Unit // 保留接口以支持 swipe-to-delete
) : ListAdapter<LongTermTask, LongTermTaskAdapter.TaskViewHolder>(
    TaskDiffCallback()
) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_long_term_task, parent, false)
        return TaskViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: View = itemView.findViewById(R.id.itemContainer)
        private val checkIcon: ImageView = itemView.findViewById(R.id.checkBoxComplete)
        private val textTitle: TextView = itemView.findViewById(R.id.textTitle)
        private val textGoal: TextView = itemView.findViewById(R.id.textGoal)
        private val textCreateTime: TextView = itemView.findViewById(R.id.textCreateTime)
        private val chevronIcon: ImageView = itemView.findViewById(R.id.chevronIcon)
        
        fun bind(task: LongTermTask) {
            // 标题
            textTitle.text = task.title
            
            // 目标描述
            if (task.goal.isNotBlank()) {
                textGoal.text = task.goal
                textGoal.visibility = View.VISIBLE
            } else {
                textGoal.visibility = View.GONE
            }
            
            // 格式化创建时间
            val formatter = DateTimeFormatter.ofPattern("yyyy年M月d日")
            val createTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(task.createTime), 
                ZoneOffset.systemDefault()
            )
            textCreateTime.text = "创建于 ${createTime.format(formatter)}"
            
            // 完成状态图标 (使用绿色主题)
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
    
    private class TaskDiffCallback : DiffUtil.ItemCallback<LongTermTask>() {
        override fun areItemsTheSame(oldItem: LongTermTask, newItem: LongTermTask): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: LongTermTask, newItem: LongTermTask): Boolean {
            return oldItem == newItem
        }
    }
}