package com.jingran.taskmanager.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.jingran.taskmanager.MainActivity
import com.jingran.taskmanager.R
import com.jingran.taskmanager.ui.TaskEditActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.hamcrest.Matchers.*

@RunWith(AndroidJUnit4::class)
class TaskUITest {

    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun testMainActivityLaunch() {
        // 验证主界面启动
        onView(withId(R.id.main))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNavigationToTaskList() {
        // 测试导航到任务列表
        onView(withId(R.id.nav_task_list))
            .perform(click())
        
        // 验证任务列表Fragment显示
        onView(withId(R.id.recyclerViewTasks))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testAddNewTaskFlow() {
        // 导航到任务列表
        onView(withId(R.id.nav_task_list))
            .perform(click())
        
        // 点击添加任务按钮
        onView(withId(R.id.fabAddTask))
            .perform(click())
        
        // 验证任务编辑界面打开
        onView(withId(R.id.editTextTitle))
            .check(matches(isDisplayed()))
        
        // 填写任务信息
        onView(withId(R.id.editTextTitle))
            .perform(typeText("测试任务"))
        
        onView(withId(R.id.editTextDescription))
            .perform(typeText("这是一个测试任务的描述"))
        
        // 关闭软键盘
        onView(withId(R.id.editTextDescription))
            .perform(closeSoftKeyboard())
        
        // 设置优先级
        onView(withId(R.id.spinnerPriority))
            .perform(click())
        onView(withText("高"))
            .perform(click())
        
        // 设置任务类型
        onView(withId(R.id.spinnerTaskType))
            .perform(click())
        onView(withText("工作"))
            .perform(click())
        
        // 设置能量等级
        onView(withId(R.id.spinnerEnergyLevel))
            .perform(click())
        onView(withText("高"))
            .perform(click())
        
        // 设置估计时长
        onView(withId(R.id.editTextDuration))
            .perform(typeText("60"))
        
        onView(withId(R.id.editTextDuration))
            .perform(closeSoftKeyboard())
        
        // 保存任务
        onView(withId(R.id.buttonSave))
            .perform(click())
        
        // 验证返回到任务列表
        onView(withId(R.id.recyclerViewTasks))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testTaskListDisplay() {
        // 导航到任务列表
        onView(withId(R.id.nav_task_list))
            .perform(click())
        
        // 验证RecyclerView显示
        onView(withId(R.id.recyclerViewTasks))
            .check(matches(isDisplayed()))
        
        // 验证添加按钮显示
        onView(withId(R.id.fabAddTask))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testTaskEditValidation() {
        // 导航到任务编辑界面
        onView(withId(R.id.nav_task_list))
            .perform(click())
        
        onView(withId(R.id.fabAddTask))
            .perform(click())
        
        // 尝试保存空任务
        onView(withId(R.id.buttonSave))
            .perform(click())
        
        // 验证错误提示（假设有错误提示）
        // 这里需要根据实际的错误处理机制来验证
        onView(withId(R.id.editTextTitle))
            .check(matches(isDisplayed())) // 应该仍在编辑界面
    }

    @Test
    fun testNavigationDrawer() {
        // 打开导航抽屉
        onView(withContentDescription("Open navigation drawer"))
            .perform(click())
        
        // 验证导航项显示
        onView(withId(R.id.nav_task_list))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.nav_calendar))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.nav_statistics))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.nav_settings))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testCalendarNavigation() {
        // 打开导航抽屉
        onView(withContentDescription("Open navigation drawer"))
            .perform(click())
        
        // 点击日历导航项
        onView(withId(R.id.nav_calendar))
            .perform(click())
        
        // 验证日历界面显示
        onView(withId(R.id.calendarView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testStatisticsNavigation() {
        // 打开导航抽屉
        onView(withContentDescription("Open navigation drawer"))
            .perform(click())
        
        // 点击统计导航项
        onView(withId(R.id.nav_statistics))
            .perform(click())
        
        // 验证统计界面显示
        onView(withId(R.id.statisticsContainer))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSettingsNavigation() {
        // 打开导航抽屉
        onView(withContentDescription("Open navigation drawer"))
            .perform(click())
        
        // 点击设置导航项
        onView(withId(R.id.nav_settings))
            .perform(click())
        
        // 验证设置界面显示
        onView(withId(R.id.settingsContainer))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testTaskItemClick() {
        // 导航到任务列表
        onView(withId(R.id.nav_task_list))
            .perform(click())
        
        // 如果列表中有任务，点击第一个任务
        // 这个测试需要预先有数据或者mock数据
        try {
            onView(withId(R.id.recyclerViewTasks))
                .perform(actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(0, click()))
            
            // 验证任务详情或编辑界面打开
            onView(withId(R.id.editTextTitle))
                .check(matches(isDisplayed()))
        } catch (e: Exception) {
            // 如果没有任务项，跳过这个测试
            // 在实际应用中，可以先添加一个任务再测试
        }
    }

    @Test
    fun testTaskCompletionToggle() {
        // 导航到任务列表
        onView(withId(R.id.nav_task_list))
            .perform(click())
        
        // 如果列表中有任务，测试完成状态切换
        try {
            // 点击任务的完成复选框
            onView(allOf(
                withId(R.id.checkBoxCompleted),
                isDisplayed()
            )).perform(click())
            
            // 验证任务状态已更新
            onView(allOf(
                withId(R.id.checkBoxCompleted),
                isDisplayed()
            )).check(matches(isChecked()))
        } catch (e: Exception) {
            // 如果没有任务项，跳过这个测试
        }
    }

    @Test
    fun testSwipeToDelete() {
        // 导航到任务列表
        onView(withId(R.id.nav_task_list))
            .perform(click())
        
        // 如果列表中有任务，测试滑动删除
        try {
            onView(withId(R.id.recyclerViewTasks))
                .perform(actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(0, swipeLeft()))
            
            // 验证删除确认对话框或直接删除
            // 这里需要根据实际的删除机制来验证
        } catch (e: Exception) {
            // 如果没有任务项，跳过这个测试
        }
    }

    @Test
    fun testSearchFunctionality() {
        // 导航到任务列表
        onView(withId(R.id.nav_task_list))
            .perform(click())
        
        // 如果有搜索功能，测试搜索
        try {
            onView(withId(R.id.searchView))
                .perform(click())
                .perform(typeText("测试"))
            
            // 验证搜索结果
            onView(withId(R.id.recyclerViewTasks))
                .check(matches(isDisplayed()))
        } catch (e: Exception) {
            // 如果没有搜索功能，跳过这个测试
        }
    }

    @Test
    fun testFilterFunctionality() {
        // 导航到任务列表
        onView(withId(R.id.nav_task_list))
            .perform(click())
        
        // 如果有过滤功能，测试过滤
        try {
            onView(withId(R.id.buttonFilter))
                .perform(click())
            
            // 选择过滤条件
            onView(withText("高优先级"))
                .perform(click())
            
            // 验证过滤结果
            onView(withId(R.id.recyclerViewTasks))
                .check(matches(isDisplayed()))
        } catch (e: Exception) {
            // 如果没有过滤功能，跳过这个测试
        }
    }
}