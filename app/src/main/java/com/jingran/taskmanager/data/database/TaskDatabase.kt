package com.jingran.taskmanager.data.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jingran.taskmanager.data.dao.ShortTermTaskDao
import com.jingran.taskmanager.data.dao.LongTermTaskDao
import com.jingran.taskmanager.data.dao.SubTaskDao
import com.jingran.taskmanager.data.dao.PlanItemDao
import com.jingran.taskmanager.data.dao.FixedScheduleDao
import com.jingran.taskmanager.data.dao.DailyStatsDao
import com.jingran.taskmanager.data.dao.CourseScheduleDao
import com.jingran.taskmanager.data.dao.ImportRecordDao
import com.jingran.taskmanager.data.dao.SyncRecordDao
import com.jingran.taskmanager.data.dao.BackupRecordDao
import com.jingran.taskmanager.data.entity.ShortTermTask
import com.jingran.taskmanager.data.entity.LongTermTask
import com.jingran.taskmanager.data.entity.SubTask
import com.jingran.taskmanager.data.entity.PlanItem
import com.jingran.taskmanager.data.entity.FixedSchedule
import com.jingran.taskmanager.data.entity.DailyStats
import com.jingran.taskmanager.data.entity.CourseSchedule
import com.jingran.taskmanager.data.entity.ImportRecord
import com.jingran.taskmanager.data.entity.SyncRecord
import com.jingran.taskmanager.data.entity.BackupRecord
import com.jingran.taskmanager.data.entity.TaskTypeConverters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.SecureRandom
import javax.crypto.KeyGenerator

/**
 * Room 数据库管理类
 * 配置数据库实体、DAO 和加密
 */
@Database(
    entities = [
        ShortTermTask::class,
        LongTermTask::class,
        SubTask::class,
        PlanItem::class,
        FixedSchedule::class,
        DailyStats::class,
        CourseSchedule::class,
        ImportRecord::class,
        SyncRecord::class,
        BackupRecord::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(TaskTypeConverters::class)
abstract class TaskDatabase : RoomDatabase() {
    
    abstract fun shortTermTaskDao(): ShortTermTaskDao
    abstract fun longTermTaskDao(): LongTermTaskDao
    abstract fun subTaskDao(): SubTaskDao
    abstract fun planItemDao(): PlanItemDao
    abstract fun fixedScheduleDao(): FixedScheduleDao
    abstract fun dailyStatsDao(): DailyStatsDao
    abstract fun courseScheduleDao(): CourseScheduleDao
    abstract fun importRecordDao(): ImportRecordDao
    abstract fun syncRecordDao(): SyncRecordDao
    abstract fun backupRecordDao(): BackupRecordDao
    
    /**
     * 执行事务操作
     * 确保多个数据库操作的原子性
     */
    suspend fun <T> withTransaction(block: suspend () -> T): T {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            var result: T? = null
            runInTransaction {
                result = kotlinx.coroutines.runBlocking {
                    block()
                }
            }
            result!!
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: TaskDatabase? = null
        
        private const val DATABASE_NAME = "task_database"
        private const val ENCRYPTION_KEY_ALIAS = "task_db_key"
        private const val TAG = "TaskDatabase"
        
        fun getDatabase(context: Context): TaskDatabase {
            Log.d(TAG, "getDatabase called")
            
            return INSTANCE ?: synchronized(this) {
                Log.d(TAG, "Creating new database instance")
                
                try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        TaskDatabase::class.java,
                        DATABASE_NAME
                    )
                    // .openHelperFactory(getSupportFactory(context)) // 暂时禁用加密
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration() // 添加这行以避免迁移问题
                    .build()
                    
                    Log.d(TAG, "Database instance created successfully")
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating database instance", e)
                    throw e
                }
            }
        }
        
        /**
         * 获取加密支持工厂
         * 注：简化版本，暂时不使用第三方加密库
         */
        private fun getSupportFactory(context: Context): androidx.sqlite.db.SupportSQLiteOpenHelper.Factory? {
            // 暂时返回 null，使用默认的非加密数据库
            // 后续可以集成 SQLCipher 或其他加密方案
            return null
        }
        
        /**
         * 获取或创建加密密钥
         */
        private fun getOrCreateEncryptionKey(context: Context): String {
            val sharedPrefs = context.getSharedPreferences("encryption_prefs", Context.MODE_PRIVATE)
            var key = sharedPrefs.getString(ENCRYPTION_KEY_ALIAS, null)
            
            if (key == null) {
                // 生成新的加密密钥
                val keyGenerator = KeyGenerator.getInstance("AES")
                keyGenerator.init(256)
                val secretKey = keyGenerator.generateKey()
                key = android.util.Base64.encodeToString(secretKey.encoded, android.util.Base64.DEFAULT)
                
                // 保存密钥
                sharedPrefs.edit()
                    .putString(ENCRYPTION_KEY_ALIAS, key)
                    .apply()
            }
            
            return key!!
        }
    }
    
    /**
     * 数据库回调，用于初始化数据
     */
    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // 数据库创建时的初始化操作
        }
    }
}