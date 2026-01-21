package com.jingran.taskmanager.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jingran.taskmanager.databinding.ActivityDataSyncBinding
import com.jingran.taskmanager.service.DataSyncService
import com.jingran.taskmanager.service.SyncType
import com.jingran.taskmanager.service.SyncStatus
import com.jingran.taskmanager.ui.adapter.SyncHistoryAdapter
import com.jingran.taskmanager.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DataSyncActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityDataSyncBinding
    private lateinit var taskViewModel: TaskViewModel
    private lateinit var dataSyncService: DataSyncService
    private lateinit var syncHistoryAdapter: SyncHistoryAdapter
    
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { exportData(it) }
    }
    
    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { importData(it) }
    }
    
    private val backupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { createBackup(it) }
    }
    
    private val restoreLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { restoreBackup(it) }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDataSyncBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initializeComponents()
        setupUI()
        setupClickListeners()
        loadSyncHistory()
    }
    
    private fun initializeComponents() {
        taskViewModel = ViewModelProvider(this)[TaskViewModel::class.java]
        dataSyncService = DataSyncService(taskViewModel.getRepository())
        
        syncHistoryAdapter = SyncHistoryAdapter { syncRecord ->
            // Handle sync record click if needed
        }
    }
    
    private fun setupUI() {
        // Setup navigation bar
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        // Setup RecyclerView
        binding.recyclerViewSyncHistory.apply {
            layoutManager = LinearLayoutManager(this@DataSyncActivity)
            adapter = syncHistoryAdapter
        }
        
        updateSyncStatus()
    }
    
    private fun setupClickListeners() {
        // Full Sync
        binding.btnFullSync.setOnClickListener {
            performFullSync()
        }
        
        // Incremental Sync
        binding.btnIncrementalSync.setOnClickListener {
            performIncrementalSync()
        }
        
        // Export Data
        binding.btnExportData.setOnClickListener {
            val fileName = "taskmanager_export_${getCurrentTimestamp()}.json"
            exportLauncher.launch(fileName)
        }
        
        // Import Data
        binding.btnImportData.setOnClickListener {
            importLauncher.launch("application/json")
        }
        
        // Create Backup
        binding.btnCreateBackup.setOnClickListener {
            val fileName = "taskmanager_backup_${getCurrentTimestamp()}.zip"
            backupLauncher.launch(fileName)
        }
        
        // Restore Backup
        binding.btnRestoreBackup.setOnClickListener {
            restoreLauncher.launch("application/zip")
        }
        
        // Clear Sync History
        binding.btnClearHistory.setOnClickListener {
            clearSyncHistory()
        }
    }
    
    private fun performFullSync() {
        binding.btnFullSync.isEnabled = false
        binding.tvSyncStatus.text = "正在执行完整同步..."
        
        com.jingran.utils.CoroutineErrorHandler.safeLifecycleCoroutineExecute(
            lifecycleOwner = this,
            context = this@DataSyncActivity,
            onError = { errorInfo ->
                binding.btnFullSync.isEnabled = true
                Toast.makeText(this@DataSyncActivity, "完整同步失败: ${errorInfo.userMessage}", Toast.LENGTH_SHORT).show()
            }
        ) {
            val result = dataSyncService.performFullSync()
            
            if (result.success) {
                Toast.makeText(this@DataSyncActivity, "完整同步成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@DataSyncActivity, "同步失败: ${result.message}", Toast.LENGTH_LONG).show()
            }
            
            updateSyncStatus()
            loadSyncHistory()
            binding.btnFullSync.isEnabled = true
        }
    }
    
    private fun performIncrementalSync() {
        binding.btnIncrementalSync.isEnabled = false
        binding.tvSyncStatus.text = "正在执行增量同步..."
        
        com.jingran.utils.CoroutineErrorHandler.safeLifecycleCoroutineExecute(
            lifecycleOwner = this,
            context = this@DataSyncActivity,
            onError = { errorInfo ->
                binding.btnIncrementalSync.isEnabled = true
                Toast.makeText(this@DataSyncActivity, "增量同步失败: ${errorInfo.userMessage}", Toast.LENGTH_SHORT).show()
            }
        ) {
            val result = dataSyncService.performIncrementalSync()
            
            if (result.success) {
                Toast.makeText(this@DataSyncActivity, "增量同步成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@DataSyncActivity, "同步失败: ${result.message}", Toast.LENGTH_LONG).show()
            }
            
            updateSyncStatus()
            loadSyncHistory()
            binding.btnIncrementalSync.isEnabled = true
        }
    }
    
    private fun exportData(uri: Uri) {
        binding.btnExportData.isEnabled = false
        
        com.jingran.utils.CoroutineErrorHandler.safeLifecycleCoroutineExecute(
            lifecycleOwner = this,
            context = this@DataSyncActivity,
            onError = { errorInfo ->
                binding.btnExportData.isEnabled = true
                Toast.makeText(this@DataSyncActivity, "数据导出失败: ${errorInfo.userMessage}", Toast.LENGTH_SHORT).show()
            }
        ) {
            val result = dataSyncService.exportData(uri)
            
            if (result.success) {
                Toast.makeText(this@DataSyncActivity, "数据导出成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@DataSyncActivity, "导出失败: ${result.message}", Toast.LENGTH_LONG).show()
            }
            
            loadSyncHistory()
            binding.btnImportData.isEnabled = true
        }
    }
    
    private fun importData(uri: Uri) {
        binding.btnImportData.isEnabled = false
        
        com.jingran.utils.CoroutineErrorHandler.safeLifecycleCoroutineExecute(
            lifecycleOwner = this,
            context = this@DataSyncActivity,
            onError = { errorInfo ->
                binding.btnImportData.isEnabled = true
                Toast.makeText(this@DataSyncActivity, "数据导入失败: ${errorInfo.userMessage}", Toast.LENGTH_SHORT).show()
            }
        ) {
            val result = dataSyncService.importData(uri)
            
            if (result.success) {
                Toast.makeText(this@DataSyncActivity, "数据导入成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@DataSyncActivity, "导入失败: ${result.message}", Toast.LENGTH_LONG).show()
            }
            
            loadSyncHistory()
            binding.btnCreateBackup.isEnabled = true
        }
    }
    
    private fun createBackup(uri: Uri) {
        binding.btnCreateBackup.isEnabled = false
        
        com.jingran.utils.CoroutineErrorHandler.safeLifecycleCoroutineExecute(
            lifecycleOwner = this,
            context = this@DataSyncActivity,
            onError = { errorInfo ->
                binding.btnCreateBackup.isEnabled = true
                Toast.makeText(this@DataSyncActivity, "备份创建失败: ${errorInfo.userMessage}", Toast.LENGTH_SHORT).show()
            }
        ) {
            val result = dataSyncService.createBackup(uri)
            
            if (result.success) {
                Toast.makeText(this@DataSyncActivity, "备份创建成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@DataSyncActivity, "备份失败: ${result.message}", Toast.LENGTH_LONG).show()
            }
            
            loadSyncHistory()
            binding.btnRestoreBackup.isEnabled = true
        }
    }
    
    private fun restoreBackup(uri: Uri) {
        binding.btnRestoreBackup.isEnabled = false
        
        com.jingran.utils.CoroutineErrorHandler.safeLifecycleCoroutineExecute(
            lifecycleOwner = this,
            context = this@DataSyncActivity,
            onError = { errorInfo ->
                binding.btnRestoreBackup.isEnabled = true
                Toast.makeText(this@DataSyncActivity, "备份恢复失败: ${errorInfo.userMessage}", Toast.LENGTH_SHORT).show()
            }
        ) {
            val result = dataSyncService.restoreBackup(uri)
            
            if (result.success) {
                Toast.makeText(this@DataSyncActivity, "备份恢复成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@DataSyncActivity, "恢复失败: ${result.message}", Toast.LENGTH_LONG).show()
            }
            
            loadSyncHistory()
        }
    }
    
    private fun clearSyncHistory() {
        com.jingran.utils.CoroutineErrorHandler.safeLifecycleCoroutineExecute(
            lifecycleOwner = this,
            context = this@DataSyncActivity,
            onError = { errorInfo ->
                Toast.makeText(this@DataSyncActivity, "清除同步历史失败: ${errorInfo.userMessage}", Toast.LENGTH_SHORT).show()
            }
        ) {
            dataSyncService.clearSyncHistory()
            Toast.makeText(this@DataSyncActivity, "同步历史已清除", Toast.LENGTH_SHORT).show()
            loadSyncHistory()
        }
    }
    
    private fun updateSyncStatus() {
        com.jingran.utils.CoroutineErrorHandler.safeLifecycleCoroutineExecute(
            lifecycleOwner = this,
            context = this@DataSyncActivity,
            onError = { errorInfo ->
                Toast.makeText(this@DataSyncActivity, "同步状态更新失败: ${errorInfo.userMessage}", Toast.LENGTH_SHORT).show()
            }
        ) {
            val lastSync = dataSyncService.getLastSyncTime()
            val status = dataSyncService.getSyncStatus()
            
            binding.tvSyncStatus.text = when (status) {
                SyncStatus.IDLE -> "同步空闲"
                SyncStatus.SYNCING -> "正在同步..."
                SyncStatus.SUCCESS -> "同步成功"
                SyncStatus.FAILED -> "同步失败"
                SyncStatus.CONFLICT -> "存在冲突"
                SyncStatus.CANCELLED -> "同步已取消"
            }
            
            binding.tvLastSyncTime.text = if (lastSync != null) {
                "上次同步: ${formatDateTime(lastSync)}"
            } else {
                "尚未同步"
            }
        }
    }
    
    private fun loadSyncHistory() {
        com.jingran.utils.CoroutineErrorHandler.safeLifecycleCoroutineExecute(
            lifecycleOwner = this,
            context = this@DataSyncActivity,
            onError = { errorInfo ->
                Toast.makeText(this@DataSyncActivity, "加载同步历史失败: ${errorInfo.userMessage}", Toast.LENGTH_SHORT).show()
            }
        ) {
            val history = dataSyncService.getSyncHistory()
            syncHistoryAdapter.updateHistory(history)
        }
    }
    
    private fun getCurrentTimestamp(): String {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return formatter.format(Date())
    }
    
    private fun formatDateTime(timestamp: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }
}