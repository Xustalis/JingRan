package com.jingran.taskmanager.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.jingran.taskmanager.data.entity.PlanItem

/**
 * 计划项数据访问对象
 * 提供每日计划的数据库操作方法
 */
@Dao
interface PlanItemDao {
    
    @Query("SELECT * FROM plan_items WHERE planDate = :date ORDER BY startTime ASC")
    fun getPlanItemsByDate(date: Long): LiveData<List<PlanItem>>
    
    @Query("SELECT * FROM plan_items WHERE planDate = :date ORDER BY startTime ASC")
    fun getByDate(date: Long): LiveData<List<PlanItem>>
    
    @Query("SELECT * FROM plan_items WHERE planDate = :date ORDER BY startTime ASC")
    suspend fun getPlanItemsByDateSync(date: Long): List<PlanItem>
    
    @Query("SELECT * FROM plan_items WHERE planDate = :date ORDER BY startTime ASC")
    suspend fun getByDateSync(date: Long): List<PlanItem>
    
    @Query("SELECT * FROM plan_items WHERE id = :id")
    suspend fun getPlanItemById(id: Long): PlanItem?
    
    @Insert
    suspend fun insertPlanItem(planItem: PlanItem)
    
    @Insert
    suspend fun insertPlanItems(planItems: List<PlanItem>)
    
    @Insert
    suspend fun insert(planItem: PlanItem)
    
    @Insert
    suspend fun insertAll(planItems: List<PlanItem>)
    
    @Update
    suspend fun updatePlanItem(planItem: PlanItem)
    
    @Update
    suspend fun update(planItem: PlanItem)
    
    @Delete
    suspend fun deletePlanItem(planItem: PlanItem)
    
    @Delete
    suspend fun delete(planItem: PlanItem)
    
    @Query("DELETE FROM plan_items WHERE planDate = :date")
    suspend fun deletePlanItemsByDate(date: Long)
    
    @Query("UPDATE plan_items SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updatePlanItemCompletion(id: Long, isCompleted: Boolean)
    
    @Query("DELETE FROM plan_items WHERE taskId = :taskId")
    suspend fun deletePlanItemsByTaskId(taskId: Long)
    
    /**
     * 获取所有计划项
     */
    @Query("SELECT * FROM plan_items ORDER BY planDate ASC, startTime ASC")
    suspend fun getAllPlanItems(): List<PlanItem>
}