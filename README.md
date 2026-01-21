# 井然 (JingRan) - 智能任务管理系统

**井然** 是一款原生 Android 应用程序，旨在通过“智能规划”帮助用户高效管理任务。它结合了长期目标与短期可执行任务，采用“本地优先”的设计理念，确保数据隐私与极速体验。

## 📱 项目概览

*   **应用名称**: 井然 (JingRan - 取“井然有序”之意)
*   **平台**: Android
*   **开发语言**: Kotlin
*   **架构模式**: MVVM
*   **设计风格**: iOS 风格极简 UI (扁平化设计, 标准间距, 圆角卡片)

## ✨ 核心功能

1.  **智能规划 (Smart Planning)**: 智能任务调度算法，根据优先级、截止日期和用户精力水平自动生成每日计划。
2.  **双层任务体系**:
    *   **长期任务**: 设定远期目标，支持进度追踪和子任务拆解。
    *   **短期任务**: 具体的、可立即执行的待办事项。
    *   **任务关联**: 将短期任务关联到长期目标，积跬步以至千里。
3.  **本地优先 (Local-First)**: 所有数据使用 Room 数据库存储在本地，无需联网即可使用，确保存储安全。
4.  **课程表管理**: 集成课程/日程表功能，学习生活两不误。
5.  **数据同步**: (规划中) 支持跨设备数据备份与同步。

## 🛠 技术栈

*   **语言**: Kotlin
*   **UI 框架**: Android View System (XML), ViewBinding, Material Design 3
*   **数据库**: Room Database (SQLite 封装)
*   **架构组件**: ViewModel, LiveData, Repository Pattern
*   **依赖注入**: Manual DI (手动依赖注入，为保证稳健性简化了 Dagger)
*   **异步处理**: Kotlin Coroutines & Flow

## 🚀 快速开始

### 环境要求
*   Android Studio Iguana 或更新版本
*   JDK 17
*   Android SDK Platform 34

### 构建项目
1.  克隆仓库到本地。
2.  使用 Android Studio 打开项目。
3.  等待 Gradle Sync 完成。
4.  连接模拟器或真机运行。

```bash
./gradlew assembleDebug
```

## 📁 项目结构

*   `app/src/main/java/com/jingran/taskmanager`:
    *   `data`: 数据层，包含实体 (Entities)、DAO、数据库 (Database) 和 仓库 (Repositories)。
    *   `di`: 依赖注入模块。
    *   `service`: 后台服务 (如智能规划服务 IntelligentPlanningService)。
    *   `ui`: 界面层，包含 Activity, Fragment, Adapter 等。
    *   `viewmodel`: MVVM 架构的 ViewModel 层。
    *   `utils`: 工具类 (LogManager 等)。

## 📝 许可证
[此处添加许可证信息]
