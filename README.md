# JingRan (井然) - Intelligent Task Manager

**JingRan** is a native Android application designed to help users manage tasks efficiently through "Smart Planning". It combines long-term goals with short-term actionable tasks, using a local-first approach.

## 📱 Project Overview

*   **App Name**: JingRan (井然 - implying order and neatness)
*   **Platform**: Android
*   **Language**: Kotlin
*   **Architecture**: MVVM
*   **Design Style**: iOS-inspired Clean UI (Flat design, standard margins, rounded corners)

## ✨ Key Features

1.  **Smart Planning**: Intelligent daily planning algorithm that schedules tasks based on priority, deadlines, and user energy levels.
2.  **Dual-Task System**:
    *   **Long-Term Tasks**: Goals with progress tracking and sub-tasks.
    *   **Short-Term Tasks**: Immediate actionable items.
    *   **Sub-Task Association**: Link short-term tasks to long-term goals.
3.  **Local-First Data**: All data is stored locally using Room Database for privacy and speed.
4.  **Course Schedule**: Integrated course/schedule management.
5.  **Synchronization**: (Planned) Data sync capabilities.

## 🛠 Technical Stack

*   **Language**: Kotlin
*   **UI Toolkit**: Android View System (XML), ViewBinding, Material Design 3
*   **Database**: Room Database (SQLite abstraction)
*   **Architecture Components**: ViewModel, LiveData, Repository Pattern
*   **Dependency Injection**: Manual DI (Simplified from Dagger for robustness)
*   **Asynchronous Processing**: Kotlin Coroutines & Flow

## 🚀 Getting Started

### Prerequisites
*   Android Studio Iguana or newer
*   JDK 17
*   Android SDK Platform 34

### Building the Project
1.  Clone the repository.
2.  Open in Android Studio.
3.  Sync Gradle Project.
4.  Run on Emulator or Physical Device.

```bash
./gradlew assembleDebug
```

## 📁 Project Structure

*   `app/src/main/java/com/jingran/taskmanager`:
    *   `data`: Entities, DAOs, Database, Repositories.
    *   `di`: Dependency Injection modules.
    *   `service`: Background services (e.g., IntelligentPlanningService).
    *   `ui`: Activities, Fragments, Adapters.
    *   `viewmodel`: MVVM ViewModels.
    *   `utils`: Utility classes (LogManager, etc.).

## 📝 License
[License Information Here]
