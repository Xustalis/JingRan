# 井然（JingRan）智能任务管理应用

一个基于Android的智能任务管理应用，采用Material Design 3设计语言，提供任务规划、智能调度、效率统计等功能。

## 项目概述

井然是一个功能完整的任务管理应用，旨在帮助用户高效管理短期和长期任务，通过智能算法优化时间分配，提升工作效率。

### 核心功能

- **任务管理**
  - 短期任务：日常待办事项，支持优先级、截止时间、能量水平
  - 长期任务：目标导向的任务，支持里程碑和进度跟踪
  - 任务关联：长期任务可分解为多个短期子任务

- **智能规划**
  - 基于优先级、紧急程度、能量水平的综合评分
  - 能量感知的任务调度
  - 动态时间分配策略（平衡、优先级聚焦、能量感知、截止时间驱动、灵活）
  - 冲突检测和自动解决

- **效率统计**
  - 任务完成率统计
  - 时间利用率分析
  - 能量效率评估
  - 历史数据追踪

- **日程管理**
  - 固定日程设置
  - 课程表导入
  - 每日计划生成

## 技术架构

### 分层架构

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                 │
│  (Activities, Fragments, Adapters, Animations)  │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                   Business Logic Layer                │
│  (ViewModels, Services, Planning, Scheduling)   │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                     Data Layer                       │
│  (Repositories, Cache, Database, Entities)         │
└─────────────────────────────────────────────────────────┘
```

### 核心组件

#### 数据层（Data Layer）

- **Entities**
  - `ShortTermTask`: 短期任务实体
  - `LongTermTask`: 长期任务实体
  - `LongTermTaskExtension`: 长期任务扩展信息
  - `SubTask`: 长短期任务关联
  - `PlanItem`: 每日计划项
  - `FixedSchedule`: 固定日程
  - `DailyStats`: 每日统计数据

- **Repositories**
  - `TaskRepository`: 核心任务数据访问
  - `ShortTermTaskRepository`: 短期任务专用
  - `LongTermTaskRepository`: 长期任务专用
  - `SubTaskRepository`: 子任务关联管理
  - `PlanRepository`: 计划项管理
  - `ScheduleRepository`: 日程管理
  - `CourseRepository`: 课程表管理
  - `StatsRepository`: 统计数据管理
  - `ImportRepository`: 数据导入管理

- **Cache**
  - `TaskCache`: 任务数据缓存，提升查询性能

#### 业务逻辑层（Business Logic Layer）

- **ViewModels**
  - `TaskViewModel`: 任务管理视图模型
  - `PlanningViewModel`: 智能规划视图模型
  - `CourseScheduleViewModel`: 课程表视图模型
  - `DataSyncViewModel`: 数据同步视图模型

- **Services**
  - `EnhancedPlanningService`: 增强规划服务
  - `AdvancedPlanningService`: 高级规划算法
  - `TaskSchedulingService`: 任务调度服务
  - `PriorityAdjustmentService`: 优先级调整服务
  - `DynamicTimeAllocationService`: 动态时间分配服务
  - `DataConsistencyService`: 数据一致性检查
  - `NotificationService`: 通知服务

#### 表现层（Presentation Layer）

- **Activities**
  - `MainActivity`: 主界面
  - `TaskEditActivity`: 任务编辑界面
  - `CourseScheduleActivity`: 课程表界面
  - `DataSyncActivity`: 数据同步界面
  - `SettingsActivity`: 设置界面

- **Fragments**
  - `TaskListFragment`: 任务列表
  - `SmartPlanningFragment`: 智能规划
  - `PlanFragment`: 每日计划
  - `StatisticsFragment`: 统计分析

- **Adapters**
  - `OptimizedTaskAdapter`: 优化的任务列表适配器
  - `PlanItemAdapter`: 计划项适配器
  - `FixedScheduleAdapter`: 固定日程适配器

- **Animations**
  - `AnimationUtils`: 动画工具类
  - `RippleEffectUtils`: 波纹效果工具类

#### 工具类（Utils）

- `LogManager`: 日志管理
- `ErrorHandler`: 错误处理
- `DateUtils`: 日期工具
- `NotificationHelper`: 通知助手
- `UiComponents`: UI组件工具
- `UiStateManager`: UI状态管理
- `UtilsManager`: 统一工具类入口

## 数据库设计

### 表结构

1. **short_term_tasks**: 短期任务表
   - 主要字段：id, title, description, priority, deadline, duration, energy_level
   - 索引：priority, is_completed, deadline, task_type, energy_level

2. **long_term_tasks**: 长期任务表
   - 主要字段：id, title, description, start_date, end_date, is_completed
   - 索引：is_completed, start_date, end_date

3. **long_term_task_extensions**: 长期任务扩展表
   - 主要字段：id, long_term_task_id, estimated_total_hours, actual_total_hours
   - 外键：long_term_task_id → long_term_tasks(id)
   - 索引：long_term_task_id

4. **sub_tasks**: 子任务关联表
   - 主要字段：id, long_task_id, short_task_id
   - 外键：long_task_id → long_term_tasks(id), short_task_id → short_term_tasks(id)
   - 索引：long_task_id, short_task_id

5. **plan_items**: 计划项表
   - 主要字段：id, task_id, plan_date, start_time, end_time
   - 索引：plan_date, task_id

6. **fixed_schedules**: 固定日程表
   - 主要字段：id, title, day_of_week, start_time, end_time
   - 索引：day_of_week

7. **daily_stats**: 每日统计表
   - 主要字段：id, date, total_tasks, completed_tasks, total_duration
   - 索引：date

8. **course_schedules**: 课程表
   - 主要字段：id, course_name, day_of_week, start_time, end_time, location
   - 索引：day_of_week

### 数据库迁移

- **MIGRATION_5_6**: 移除short_term_tasks表的parent_long_term_task_id字段
- **MIGRATION_6_7**: 创建long_term_task_extensions表

## 智能规划算法

### 评分系统

任务综合评分 = 优先级分 × 0.3 + 紧急程度分 × 0.3 + 能量匹配分 × 0.2 + 历史成功率分 × 0.2

### 调度策略

1. **平衡策略（BALANCED）**
   - 综合考虑所有因素
   - 均衡时间分配

2. **优先级聚焦（PRIORITY_FOCUSED）**
   - 高优先级任务优先安排
   - 紧急任务优先处理

3. **能量感知（ENERGY_AWARE）**
   - 根据用户能量水平安排任务
   - 高能量时段安排高能量任务

4. **截止时间驱动（DEADLINE_DRIVEN）**
   - 根据截止时间排序
   - 确保任务按时完成

5. **灵活策略（FLEXIBLE）**
   - 允许任务时间调整
   - 适应变化的需求

## 性能优化

### 缓存机制

- **TaskCache**: 任务数据缓存
  - 默认缓存大小：100项
  - 缓存时长：5分钟
  - 支持失效和过期清理

### 列表优化

- **DiffUtil**: 使用DiffUtil进行高效更新
- **ViewHolder复用**: 优化RecyclerView性能
- **分页加载**: 支持大数据集

### 性能监控

- **PerformanceMonitor**: 性能监控服务
  - 内存使用监控
  - CPU使用监控
  - 数据库操作监控
  - UI操作监控
  - 网络操作监控

## UI/UX设计

### 设计语言

- **Material Design 3**: 采用Google Material Design 3设计规范
- **颜色系统**: 统一的颜色主题，支持深色模式
- **排版系统**: 符合Material Design 3的排版规范
- **间距系统**: 标准化的间距和尺寸

### 交互设计

- **动画效果**: 流畅的过渡动画
- **触觉反馈**: 波纹效果和震动反馈
- **手势操作**: 滑动删除、长按编辑
- **空状态**: 友好的空状态提示

## 依赖管理

### 主要依赖

- **Kotlin**: 1.8.0+
- **AndroidX**: 1.6.0+
- **Material Components**: 1.9.0+
- **Room**: 2.5.2+
- **Coroutines**: 1.6.4+
- **Retrofit**: 2.9.0+
- **OkHttp**: 4.11.0+

### 移除的依赖

- **Dagger**: 已移除，使用手动DI

## 构建和运行

### 构建项目

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

### 运行测试

```bash
./gradlew test
./gradlew connectedAndroidTest
```

## 开发指南

### 代码规范

- **Kotlin编码规范**: 遵循Kotlin官方编码规范
- **命名规范**: 使用清晰的命名约定
- **注释规范**: 关键逻辑添加注释
- **异常处理**: 统一的异常处理机制

### Git工作流

1. 创建功能分支：`feature/xxx`
2. 开发并提交代码
3. 创建Pull Request
4. 代码审查
5. 合并到主分支

## 测试策略

### 单元测试

- Repository层测试
- Service层测试
- ViewModel层测试
- 工具类测试

### 集成测试

- UI流程测试
- 数据流测试
- 业务逻辑测试

### 性能测试

- 内存泄漏检测
- 响应时间测试
- 数据库查询性能测试

## 已知问题

1. **动画性能**: 部分动画在低端设备上可能卡顿
2. **大数据集**: 任务数量超过1000时可能需要分页
3. **同步冲突**: 多设备同步时可能出现冲突

## 未来计划

### 短期目标

- [ ] 添加任务标签功能
- [ ] 实现任务搜索和过滤
- [ ] 添加任务提醒功能
- [ ] 支持任务附件

### 中期目标

- [ ] 添加团队协作功能
- [ ] 实现数据云端备份
- [ ] 添加数据分析和报告
- [ ] 支持多语言

### 长期目标

- [ ] 开发Web端
- [ ] 开发桌面端
- [ ] 实现AI智能推荐
- [ ] 添加社交功能

## 贡献指南

欢迎贡献代码、报告问题或提出建议。请遵循以下步骤：

1. Fork本仓库
2. 创建功能分支
3. 提交代码
4. 推送到分支
5. 创建Pull Request

## 许可证

本项目采用MIT许可证。

## 联系方式

- 项目地址：https://github.com/Xustalis/JingRan
- 邮箱：gmxenith@gmail.com

---

**版本**: 1.0.0  
**最后更新**: 2026-01-24  
**维护者**: Xustalis
