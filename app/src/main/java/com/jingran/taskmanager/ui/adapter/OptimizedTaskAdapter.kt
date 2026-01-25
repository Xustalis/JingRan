package com.jingran.taskmanager.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.jingran.taskmanager.data.entity.ShortTermTask
import com.jingran.taskmanager.data.entity.LongTermTask
import com.jingran.taskmanager.databinding.ItemShortTermTaskBinding
import com.jingran.taskmanager.databinding.ItemLongTermTaskBinding
import com.jingran.taskmanager.ui.animation.AnimationUtils
import com.jingran.utils.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class BaseTaskAdapter<T, VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {
    
    protected var items: List<T> = emptyList()
        set(value) {
            val oldItems = field
            field = value
            val diffResult = DiffUtil.calculateDiff(createDiffCallback())
            diffResult.dispatchUpdatesTo(this)
        }
    
    protected abstract fun createDiffCallback(): DiffUtil.ItemCallback<T>
    
    override fun getItemCount(): Int = items.size
    
    override fun getItemId(position: Int): Long {
        val item = items[position]
        return when (item) {
            is ShortTermTask -> (item as ShortTermTask).id
            is LongTermTask -> (item as LongTermTask).id
            else -> position.toLong()
        }
    }
}

class OptimizedShortTermTaskAdapter(
    private val onItemClick: (ShortTermTask) -> Unit,
    private val onItemLongClick: (ShortTermTask) -> Unit,
    private val onItemComplete: (ShortTermTask, Boolean) -> Unit
) : BaseTaskAdapter<ShortTermTask, OptimizedShortTermTaskAdapter.ViewHolder>() {
    
    private val TAG = "OptimizedShortTermTaskAdapter"
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemShortTermTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = items[position]
        holder.bind(task)
    }
    
    override fun createDiffCallback(): DiffUtil.ItemCallback<ShortTermTask> {
        return object : DiffUtil.ItemCallback<ShortTermTask>() {
            override fun areItemsTheSame(oldItem: ShortTermTask, newItem: ShortTermTask): Boolean {
                return oldItem.id == newItem.id
            }
            
            override fun areContentsTheSame(oldItem: ShortTermTask, newItem: ShortTermTask): Boolean {
                return oldItem == newItem
            }
            
            override fun getChangePayload(oldItem: ShortTermTask, newItem: ShortTermTask): Any? {
                if (oldItem.id == newItem.id) {
                    if (oldItem.title != newItem.title) return PAYLOAD_TITLE_CHANGED
                    if (oldItem.isCompleted != newItem.isCompleted) return PAYLOAD_COMPLETION_CHANGED
                    if (oldItem.priority != newItem.priority) return PAYLOAD_PRIORITY_CHANGED
                }
                return null
            }
        }
    }
    
    inner class ViewHolder(
        private val binding: ItemShortTermTaskBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(task: ShortTermTask) {
            binding.apply {
                textTitle.text = task.title
                textSubtitle.text = formatSubtitle(task)
                checkBoxComplete.isChecked = task.isCompleted
                
                val priorityColor = getPriorityColor(task.priority)
                priorityBadge.apply {
                    text = getPriorityText(task.priority)
                    setChipBackgroundColorResource(priorityColor)
                    visibility = if (task.priority != TaskPriority.MEDIUM) View.VISIBLE else View.GONE
                }
                
                root.setOnClickListener {
                    onItemClick(task)
                }
                
                root.setOnLongClickListener {
                    onItemLongClick(task)
                    return@setOnLongClickListener true
                }
                
                checkBoxComplete.setOnCheckedChangeListener { _, isChecked ->
                    onItemComplete(task, isChecked)
                }
            }
        }
        
        private fun formatSubtitle(task: ShortTermTask): String {
            val parts = mutableListOf<String>()
            
            if (task.deadline != null) {
                parts.add(formatDeadline(task.deadline!!))
            }
            
            if (task.duration > 0) {
                parts.add("${task.duration}分钟")
            }
            
            return parts.joinToString(" · ")
        }
        
        private fun formatDeadline(deadline: Long): String {
            val currentTime = System.currentTimeMillis()
            val hoursUntilDeadline = (deadline - currentTime) / (60 * 60 * 1000L)
            
            return when {
                hoursUntilDeadline < 0 -> "已过期"
                hoursUntilDeadline < 1 -> "1小时内"
                hoursUntilDeadline < 6 -> "${hoursUntilDeadline.toInt()}小时"
                hoursUntilDeadline < 24 -> "${(hoursUntilDeadline / 60).toInt()}小时"
                else -> "${(hoursUntilDeadline / (60 * 24)).toInt()}天"
            }
        }
        
        private fun getPriorityText(priority: TaskPriority): String {
            return when (priority) {
                TaskPriority.URGENT -> "紧急"
                TaskPriority.HIGH -> "高优先级"
                TaskPriority.MEDIUM -> "中优先级"
                TaskPriority.LOW -> "低优先级"
            }
        }
        
        private fun getPriorityColor(priority: TaskPriority): Int {
            return when (priority) {
                TaskPriority.URGENT -> R.color.priority_urgent
                TaskPriority.HIGH -> R.color.priority_high
                TaskPriority.MEDIUM -> R.color.priority_medium
                TaskPriority.LOW -> R.color.priority_low
            }
        }
    }
    
    companion object {
        const val PAYLOAD_TITLE_CHANGED = "title_changed"
        const val PAYLOAD_COMPLETION_CHANGED = "completion_changed"
        const val PAYLOAD_PRIORITY_CHANGED = "priority_changed"
    }
}

class OptimizedLongTermTaskAdapter(
    private val onItemClick: (LongTermTask) -> Unit,
    private val onItemLongClick: (LongTermTask) -> Unit,
    private val onItemComplete: (LongTermTask, Boolean) -> Unit
) : BaseTaskAdapter<LongTermTask, OptimizedLongTermTaskAdapter.ViewHolder>() {
    
    private val TAG = "OptimizedLongTermTaskAdapter"
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLongTermTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = items[position]
        holder.bind(task)
    }
    
    override fun createDiffCallback(): DiffUtil.ItemCallback<LongTermTask> {
        return object : DiffUtil.ItemCallback<LongTermTask>() {
            override fun areItemsTheSame(oldItem: LongTermTask, newItem: LongTermTask): Boolean {
                return oldItem.id == newItem.id
            }
            
            override fun areContentsTheSame(oldItem: LongTermTask, newItem: LongTermTask): Boolean {
                return oldItem == newItem
            }
            
            override fun getChangePayload(oldItem: LongTermTask, newItem: LongTermTask): Any? {
                if (oldItem.id == newItem.id) {
                    if (oldItem.title != newItem.title) return PAYLOAD_TITLE_CHANGED
                    if (oldItem.isCompleted != newItem.isCompleted) return PAYLOAD_COMPLETION_CHANGED
                    if (oldItem.description != newItem.description) return PAYLOAD_DESCRIPTION_CHANGED
                }
                return null
            }
        }
    }
    
    inner class ViewHolder(
        private val binding: ItemLongTermTaskBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(task: LongTermTask) {
            binding.apply {
                textTitle.text = task.title
                textGoal.text = task.description
                textCreateTime.text = formatCreateTime(task.createTime)
                checkBoxComplete.isChecked = task.isCompleted
                
                root.setOnClickListener {
                    onItemClick(task)
                }
                
                root.setOnLongClickListener {
                    onItemLongClick(task)
                    return@setOnLongClickListener true
                }
                
                checkBoxComplete.setOnCheckedChangeListener { _, isChecked ->
                    onItemComplete(task, isChecked)
                }
            }
        }
        
        private fun formatCreateTime(timestamp: Long): String {
            val calendar = java.util.Calendar.getInstance().apply {
                timeInMillis = timestamp
            }
            return "${calendar.get(java.util.Calendar.YEAR)}年${calendar.get(java.util.Calendar.MONTH) + 1}月${calendar.get(java.util.Calendar.DAY_OF_MONTH)}日"
        }
    }
    
    companion object {
        const val PAYLOAD_TITLE_CHANGED = "title_changed"
        const val PAYLOAD_COMPLETION_CHANGED = "completion_changed"
        const val PAYLOAD_DESCRIPTION_CHANGED = "description_changed"
    }
}