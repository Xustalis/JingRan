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
import com.jingran.taskmanager.data.entity.LongTermTask
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneOffset

class LongTermTaskAdapter(
    private val onItemClick: (LongTermTask) -> Unit,
    private val onCompleteClick: (LongTermTask) -> Unit,
    private val onDeleteClick: (LongTermTask) -> Unit
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
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        private val checkBox: MaterialCheckBox = itemView.findViewById(R.id.checkBoxComplete)
        private val textTitle: TextView = itemView.findViewById(R.id.textTitle)
        private val textGoal: TextView = itemView.findViewById(R.id.textGoal)
        private val textCreateTime: TextView = itemView.findViewById(R.id.textCreateTime)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        
        fun bind(task: LongTermTask) {
            textTitle.text = task.title
            
            // 显示目标
            if (task.goal.isNotBlank()) {
                textGoal.text = "目标: ${task.goal}"
                textGoal.visibility = View.VISIBLE
            } else {
                textGoal.visibility = View.GONE
            }
            
            // 格式化创建时间
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val createTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(task.createTime), ZoneOffset.systemDefault())
            textCreateTime.text = "创建于: ${createTime.format(formatter)}"
            
            // 设置完成状态
            checkBox.isChecked = task.isCompleted
            
            // 如果任务已完成，设置不同的样式
            if (task.isCompleted) {
                cardView.alpha = 0.6f
                textTitle.alpha = 0.6f
                textGoal.alpha = 0.6f
            } else {
                cardView.alpha = 1.0f
                textTitle.alpha = 1.0f
                textGoal.alpha = 1.0f
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
    
    private class TaskDiffCallback : DiffUtil.ItemCallback<LongTermTask>() {
        override fun areItemsTheSame(oldItem: LongTermTask, newItem: LongTermTask): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: LongTermTask, newItem: LongTermTask): Boolean {
            return oldItem == newItem
        }
    }
}