package com.jingran.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.jingran.taskmanager.receiver.NotificationReceiver
import com.jingran.taskmanager.data.entity.ShortTermTask
import com.jingran.utils.LogManager
import com.jingran.utils.ErrorHandler
import java.time.LocalDateTime
import java.time.ZoneId

open class NotificationHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "NotificationHelper"
    }
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    /**
     * 为短期任务设置提醒通知
     */
    fun scheduleTaskReminder(task: ShortTermTask) {
        LogManager.enterMethod(TAG, "scheduleTaskReminder")
        
        task.reminderTime?.let { reminderTimeTimestamp ->
            ErrorHandler.safeExecute(
                context = context,
                showErrorToUser = false,
                onError = { errorInfo ->
                    LogManager.e(TAG, "调度任务提醒失败: ${errorInfo.message}")
                }
            ) {
                LogManager.d(TAG, "为任务${task.title}调度提醒，时间: $reminderTimeTimestamp")
                
                val intent = Intent(context, NotificationReceiver::class.java).apply {
                    putExtra(NotificationReceiver.EXTRA_TASK_ID, task.id)
                    putExtra(NotificationReceiver.EXTRA_TASK_TITLE, task.title)
                    putExtra(NotificationReceiver.EXTRA_TASK_TYPE, "short")
                }
                
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    task.id.toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                val triggerTime = reminderTimeTimestamp
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
                
                LogManager.i(TAG, "任务提醒调度成功: ${task.title}")
            }
        } ?: LogManager.d(TAG, "任务${task.title}没有设置提醒时间，跳过调度")
    }
    
    /**
     * 取消任务提醒通知
     */
    fun cancelTaskReminder(taskId: Long) {
        LogManager.enterMethod(TAG, "cancelTaskReminder")
        LogManager.d(TAG, "取消任务提醒: $taskId")
        
        ErrorHandler.safeExecute(
            context = context,
            showErrorToUser = false,
            onError = { errorInfo ->
                LogManager.e(TAG, "取消任务提醒失败: ${errorInfo.message}")
            }
        ) {
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            alarmManager.cancel(pendingIntent)
            
            LogManager.i(TAG, "任务提醒取消成功: $taskId")
        }
    }
    
    /**
     * 为任务设置默认提醒时间（截止时间前30分钟）
     */
    fun setDefaultReminder(task: ShortTermTask): ShortTermTask {
        return task.deadline?.let { deadlineTimestamp ->
            val deadline = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(deadlineTimestamp),
                ZoneId.systemDefault()
            )
            val reminderTime = deadline.minusMinutes(30)
            if (reminderTime.isAfter(LocalDateTime.now())) {
                val reminderTimestamp = reminderTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                task.copy(reminderTime = reminderTimestamp)
            } else {
                task
            }
        } ?: task
    }
    
    /**
     * 检查是否有精确闹钟权限（Android 12+）
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
    
    /**
     * 批量设置多个任务的提醒
     */
    fun scheduleMultipleReminders(tasks: List<ShortTermTask>) {
        LogManager.enterMethod(TAG, "scheduleMultipleReminders")
        LogManager.d(TAG, "批量调度${tasks.size}个任务的提醒")
        
        ErrorHandler.safeExecute(
            context = context,
            showErrorToUser = false,
            onError = { errorInfo ->
                LogManager.e(TAG, "批量调度提醒失败: ${errorInfo.message}")
            }
        ) {
            for (task in tasks) {
                scheduleTaskReminder(task)
            }
            
            LogManager.i(TAG, "批量调度提醒完成，共${tasks.size}个任务")
        }
    }
    
    /**
     * 批量取消多个任务的提醒
     */
    fun cancelMultipleReminders(taskIds: List<Long>) {
        LogManager.enterMethod(TAG, "cancelMultipleReminders")
        LogManager.d(TAG, "批量取消${taskIds.size}个任务的提醒")
        
        ErrorHandler.safeExecute(
            context = context,
            showErrorToUser = false,
            onError = { errorInfo ->
                LogManager.e(TAG, "批量取消提醒失败: ${errorInfo.message}")
            }
        ) {
            taskIds.forEach { taskId ->
                cancelTaskReminder(taskId)
            }
            
            LogManager.i(TAG, "批量取消提醒完成，共${taskIds.size}个任务")
        }
    }
}
