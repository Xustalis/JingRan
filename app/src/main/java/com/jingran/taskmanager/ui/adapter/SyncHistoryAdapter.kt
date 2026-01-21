package com.jingran.taskmanager.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.jingran.taskmanager.R
import com.jingran.taskmanager.databinding.ItemSyncHistoryBinding
import com.jingran.taskmanager.service.SyncRecord
import com.jingran.taskmanager.service.SyncType
import com.jingran.taskmanager.service.SyncStatus
import java.text.SimpleDateFormat
import java.util.*

class SyncHistoryAdapter(
    private val onItemClick: (SyncRecord) -> Unit
) : RecyclerView.Adapter<SyncHistoryAdapter.SyncHistoryViewHolder>() {
    
    private var syncHistory = listOf<SyncRecord>()
    
    fun updateHistory(newHistory: List<SyncRecord>) {
        syncHistory = newHistory
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SyncHistoryViewHolder {
        val binding = ItemSyncHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SyncHistoryViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: SyncHistoryViewHolder, position: Int) {
        holder.bind(syncHistory[position])
    }
    
    override fun getItemCount(): Int = syncHistory.size
    
    inner class SyncHistoryViewHolder(
        private val binding: ItemSyncHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(syncRecord: SyncRecord) {
            binding.apply {
                // Set sync type
                tvSyncType.text = when (syncRecord.syncType) {
                    SyncType.FULL -> "完整同步"
                    SyncType.INCREMENTAL -> "增量同步"
                    SyncType.EXPORT -> "数据导出"
                    SyncType.IMPORT -> "数据导入"
                    SyncType.BACKUP -> "创建备份"
                    SyncType.RESTORE -> "恢复备份"
                    SyncType.UPLOAD_ONLY -> "仅上传"
                    SyncType.DOWNLOAD_ONLY -> "仅下载"
                    SyncType.BIDIRECTIONAL -> "双向同步"
                }
                
                // Set sync status
                tvSyncStatus.text = when (syncRecord.status) {
                    SyncStatus.IDLE -> "空闲"
                    SyncStatus.SYNCING -> "同步中"
                    SyncStatus.SUCCESS -> "成功"
                    SyncStatus.FAILED -> "失败"
                    SyncStatus.CONFLICT -> "冲突"
                    SyncStatus.CANCELLED -> "已取消"
                }
                
                // Set status color
                val statusColor = when (syncRecord.status) {
                    SyncStatus.SUCCESS -> ContextCompat.getColor(root.context, R.color.ios_green)
                    SyncStatus.FAILED -> ContextCompat.getColor(root.context, R.color.ios_red)
                    SyncStatus.CONFLICT -> ContextCompat.getColor(root.context, R.color.ios_orange)
                    SyncStatus.SYNCING -> ContextCompat.getColor(root.context, R.color.ios_blue)
                    SyncStatus.IDLE -> ContextCompat.getColor(root.context, R.color.ios_label_secondary)
                    SyncStatus.CANCELLED -> ContextCompat.getColor(root.context, R.color.ios_label_tertiary)
                }
                tvSyncStatus.setTextColor(statusColor)
                
                // Set sync time
                val formatter = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
                tvSyncTime.text = formatter.format(Date(syncRecord.timestamp))
                
                // Set sync details
                val details = buildString {
                    if (syncRecord.recordsProcessed > 0) {
                        append("处理记录: ${syncRecord.recordsProcessed}")
                    }
                    if (syncRecord.recordsUpdated > 0) {
                        if (isNotEmpty()) append(" | ")
                        append("更新: ${syncRecord.recordsUpdated}")
                    }
                    if (syncRecord.recordsDeleted > 0) {
                        if (isNotEmpty()) append(" | ")
                        append("删除: ${syncRecord.recordsDeleted}")
                    }
                    if (syncRecord.conflictsResolved > 0) {
                        if (isNotEmpty()) append(" | ")
                        append("冲突解决: ${syncRecord.conflictsResolved}")
                    }
                }
                
                tvSyncDetails.text = if (details.isNotEmpty()) {
                    details
                } else {
                    "无详细信息"
                }
                
                // Set error message if exists
                if (syncRecord.errorMessage.isNotEmpty()) {
                    tvErrorMessage.text = syncRecord.errorMessage
                    tvErrorMessage.visibility = android.view.View.VISIBLE
                } else {
                    tvErrorMessage.visibility = android.view.View.GONE
                }
                
                // Set duration
                val duration = syncRecord.endTime - syncRecord.timestamp
                if (duration > 0) {
                    tvDuration.text = "耗时: ${formatDuration(duration)}"
                    tvDuration.visibility = android.view.View.VISIBLE
                } else {
                    tvDuration.visibility = android.view.View.GONE
                }
                
                // Set click listener
                root.setOnClickListener {
                    onItemClick(syncRecord)
                }
            }
        }
        
        private fun formatDuration(durationMs: Long): String {
            val seconds = durationMs / 1000
            return when {
                seconds < 60 -> "${seconds}秒"
                seconds < 3600 -> "${seconds / 60}分${seconds % 60}秒"
                else -> "${seconds / 3600}时${(seconds % 3600) / 60}分"
            }
        }
    }
}