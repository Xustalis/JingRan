package com.jingran.taskmanager.data.repository

import androidx.lifecycle.LiveData
import com.jingran.taskmanager.data.dao.PlanItemDao
import com.jingran.taskmanager.data.entity.PlanItem

open class PlanRepository constructor(
    private val planItemDao: PlanItemDao
) : BaseRepository() {
    
    /**
     * 根据日期获取计划项
     */
    fun getPlanItemsByDate(date: Long): LiveData<List<PlanItem>> = 
        planItemDao.getPlanItemsByDate(date)
    
    /**
     * 根据日期同步获取计划项
     */
    suspend fun getPlanItemsByDateSync(date: Long): List<PlanItem> = ioCall {
        planItemDao.getPlanItemsByDateSync(date)
    }
    
    /**
     * 根据ID获取计划项
     */
    suspend fun getPlanItemById(id: Long): PlanItem? = ioCall {
        planItemDao.getPlanItemById(id)
    }
    
    /**
     * 插入计划项
     */
    suspend fun insertPlanItem(planItem: PlanItem) = ioCall {
        planItemDao.insertPlanItem(planItem)
    }
    
    /**
     * 批量插入计划项
     */
    suspend fun insertPlanItems(planItems: List<PlanItem>) = ioCall {
        planItemDao.insertPlanItems(planItems)
    }
    
    /**
     * 更新计划项
     */
    suspend fun updatePlanItem(planItem: PlanItem) = ioCall {
        planItemDao.updatePlanItem(planItem)
    }
    
    /**
     * 删除计划项
     */
    suspend fun deletePlanItem(planItem: PlanItem) = ioCall {
        planItemDao.deletePlanItem(planItem)
    }
    
    /**
     * 根据日期删除计划项
     */
    suspend fun deletePlanItemsByDate(date: Long) = ioCall {
        planItemDao.deletePlanItemsByDate(date)
    }
    
    /**
     * 根据任务ID删除计划项
     */
    suspend fun deletePlanItemsByTaskId(taskId: Long) = ioCall {
        planItemDao.deletePlanItemsByTaskId(taskId)
    }
    
    /**
     * 更新计划项完成状态
     */
    suspend fun updatePlanItemCompletion(id: Long, isCompleted: Boolean) = ioCall {
        planItemDao.updatePlanItemCompletion(id, isCompleted)
    }
    
    /**
     * 批量更新计划项完成状态
     */
    suspend fun updatePlanItemsCompletion(ids: List<Long>, isCompleted: Boolean) = ioCall {
        ids.forEach { id ->
            planItemDao.updatePlanItemCompletion(id, isCompleted)
        }
    }
    
    /**
     * 获取所有计划项
     */
    suspend fun getAllPlanItems(): List<PlanItem> = ioCall {
        planItemDao.getAllPlanItems()
    }
    
    /**
     * 获取指定日期范围内的计划项
     */
    suspend fun getPlanItemsInRange(startDate: Long, endDate: Long): List<PlanItem> = ioCall {
        // 注意：这里需要在PlanItemDao中添加相应的查询方法
        val allItems = planItemDao.getAllPlanItems()
        allItems.filter { it.planDate in startDate..endDate }
    }
    
    /**
     * 获取指定日期的完成率
     */
    suspend fun getCompletionRateByDate(date: Long): Float = ioCall {
        val items = planItemDao.getPlanItemsByDateSync(date)
        if (items.isEmpty()) return@ioCall 0f
        
        val completedCount = items.count { it.isCompleted }
        completedCount.toFloat() / items.size
    }
    
    /**
     * 获取指定日期范围内的平均完成率
     */
    suspend fun getAverageCompletionRate(startDate: Long, endDate: Long): Float = ioCall {
        val items = getAllPlanItems().filter { it.planDate in startDate..endDate }
        if (items.isEmpty()) return@ioCall 0f
        
        val completedCount = items.count { it.isCompleted }
        completedCount.toFloat() / items.size
    }
}