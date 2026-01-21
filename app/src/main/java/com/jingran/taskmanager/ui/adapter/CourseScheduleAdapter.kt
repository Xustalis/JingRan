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
import com.jingran.taskmanager.R
import com.jingran.taskmanager.data.entity.CourseSchedule
import com.jingran.taskmanager.data.entity.CourseType
import com.jingran.taskmanager.data.entity.ScheduleType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneOffset

class CourseScheduleAdapter(
    private val onItemClick: (CourseSchedule) -> Unit,
    private val onDeleteClick: (CourseSchedule) -> Unit
) : ListAdapter<CourseSchedule, CourseScheduleAdapter.CourseViewHolder>(
    CourseDiffCallback()
) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course_schedule, parent, false)
        return CourseViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView as MaterialCardView
        private val textCourseName: TextView = itemView.findViewById(R.id.tvCourseName)
        // private val textCourseCode: TextView = itemView.findViewById(R.id.tvCourseCode)
        private val textTeacher: TextView = itemView.findViewById(R.id.tvTeacher)
        private val textLocation: TextView = itemView.findViewById(R.id.tvLocation)
        private val textTime: TextView = itemView.findViewById(R.id.tvScheduleTime)
        private val textCredits: TextView = itemView.findViewById(R.id.tvCredits)
        private val textCourseType: TextView = itemView.findViewById(R.id.tvScheduleType)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        
        fun bind(course: CourseSchedule) {
            // 设置课程名称
            textCourseName.text = course.courseName
            
            // 设置课程代码
            // if (course.courseCode?.isNotEmpty() == true) {
            //     textCourseCode.text = course.courseCode
            //     textCourseCode.visibility = View.VISIBLE
            // } else {
            //     textCourseCode.visibility = View.GONE
            // }
            
            // 设置教师
            if (course.instructor?.isNotEmpty() == true) {
                textTeacher.text = "教师：${course.instructor}"
                textTeacher.visibility = View.VISIBLE
            } else {
                textTeacher.visibility = View.GONE
            }
            
            // 设置地点
            if (course.building?.isNotEmpty() == true) {
                textLocation.text = "地点：${course.building}"
                textLocation.visibility = View.VISIBLE
            } else {
                textLocation.visibility = View.GONE
            }
            
            // 设置时间
            val timeText = formatScheduleTime(course)
            textTime.text = timeText
            
            // 设置学分
            if (course.credits != null && course.credits > 0) {
                textCredits.text = "${course.credits}学分"
                textCredits.visibility = View.VISIBLE
            } else {
                textCredits.visibility = View.GONE
            }
            
            // 设置课程类型
            val typeText = when (course.courseType) {
                CourseType.REQUIRED -> "必修"
                CourseType.ELECTIVE -> "选修"
                CourseType.PUBLIC -> "公共课"
                CourseType.PROFESSIONAL -> "专业课"
                CourseType.LECTURE -> "理论课"
                CourseType.LAB -> "实验课"
                CourseType.SEMINAR -> "研讨课"
                CourseType.TUTORIAL -> "辅导课"
                CourseType.WORKSHOP -> "工作坊"
                CourseType.FIELD_TRIP -> "实地考察"
                CourseType.EXAM -> "考试"
                CourseType.REVIEW -> "复习课"
                CourseType.OTHER -> "其他"
            }
            textCourseType.text = typeText
            
            // 设置点击事件
            cardView.setOnClickListener {
                onItemClick(course)
            }
            
            btnDelete.setOnClickListener {
                onDeleteClick(course)
            }
        }
        
        private fun formatScheduleTime(course: CourseSchedule): String {
            // TODO: 需要通过FixedSchedule获取时间信息
            return "时间待定"
        }
        
        private fun getDayOfWeekText(dayOfWeek: Int): String {
            return when (dayOfWeek) {
                1 -> "周一"
                2 -> "周二"
                3 -> "周三"
                4 -> "周四"
                5 -> "周五"
                6 -> "周六"
                7 -> "周日"
                else -> "未知"
            }
        }
        
        private fun formatTime(timeInMinutes: Int): String {
            val hours = timeInMinutes / 60
            val minutes = timeInMinutes % 60
            return String.format("%02d:%02d", hours, minutes)
        }
    }
    
    class CourseDiffCallback : DiffUtil.ItemCallback<CourseSchedule>() {
        override fun areItemsTheSame(oldItem: CourseSchedule, newItem: CourseSchedule): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: CourseSchedule, newItem: CourseSchedule): Boolean {
            return oldItem == newItem
        }
    }
}