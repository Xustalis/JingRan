package com.jingran.taskmanager.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jingran.taskmanager.R
import com.jingran.taskmanager.ui.activity.TaskEditActivity
import com.jingran.utils.ErrorHandler
import com.jingran.utils.LogManager

class NotificationReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "NotificationReceiver"
        const val CHANNEL_ID = "task_reminder_channel"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TITLE = "task_title"
        const val EXTRA_TASK_TYPE = "task_type"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        LogManager.enterMethod(TAG, "onReceive")
        
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "任务提醒"
        val taskType = intent.getStringExtra(EXTRA_TASK_TYPE) ?: "short"
        
        LogManager.d(TAG, "收到通知请求: taskId=$taskId, title=$taskTitle, type=$taskType")
        
        if (taskId == -1L) {
            LogManager.w(TAG, "无效的任务ID，跳过通知")
            return
        }
        
        ErrorHandler.safeExecute(
            context = context,
            showErrorToUser = false,
            onError = { errorInfo ->
                LogManager.e(TAG, "显示通知失败: ${errorInfo.message}")
            }
        ) {
            createNotificationChannel(context)
            showNotification(context, taskId, taskTitle, taskType)
            LogManager.i(TAG, "通知显示成功: $taskTitle")
        }
    }
    
    private fun createNotificationChannel(context: Context) {
        LogManager.d(TAG, "创建通知渠道")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ErrorHandler.safeExecute(
                context = context,
                showErrorToUser = false,
                onError = { errorInfo ->
                    LogManager.e(TAG, "创建通知渠道失败: ${errorInfo.message}")
                }
            ) {
                val name = "任务提醒"
                val descriptionText = "任务截止时间提醒通知"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    enableVibration(true)
                    enableLights(true)
                }
                
                val notificationManager: NotificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
                
                LogManager.d(TAG, "通知渠道创建成功")
            }
        }
    }
    
    private fun showNotification(context: Context, taskId: Long, taskTitle: String, taskType: String) {
        // 创建点击通知后的Intent
        val intent = Intent(context, TaskEditActivity::class.java).apply {
            putExtra("task_id", taskId)
            putExtra("task_type", taskType)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 构建通知
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("任务提醒")
            .setContentText("任务 \"$taskTitle\" 即将到期")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("任务 \"$taskTitle\" 即将到期，请及时处理。")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .build()
        
        // 显示通知
        try {
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID + taskId.toInt(), notification)
            }
        } catch (e: SecurityException) {
            // 处理权限不足的情况
            e.printStackTrace()
        }
    }
}