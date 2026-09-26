<div align="center">

# 🎓 Kletaq

### *The Duolingo for Engineering Students*

Gamified academic learning, bite-sized curriculum journeys, deep focus timer, automated streak engine, and reactive companion widgets built for engineering university students.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.10.01-green.svg?style=flat&logo=android)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Material%203-Enabled-blue.svg?style=flat)](https://m3.material.io)
[![Firebase](https://img.shields.io/badge/Firebase-Auth%20%7C%20Firestore-orange.svg?style=flat&logo=firebase)](https://firebase.google.com)
[![Glance Widgets](https://img.shields.io/badge/Android%20Glance-Widgets-teal.svg?style=flat)](https://developer.android.com/jetpack/compose/glance)
[![WorkManager](https://img.shields.io/badge/WorkManager-Background%20Workers-red.svg?style=flat)](https://developer.android.com/topic/libraries/architecture/workmanager)
[![Build Variant](https://img.shields.io/badge/Build%20Variant-Benchmark-brightgreen.svg?style=flat)](#-build--installation-rules)

</div>

---

## 📖 Overview

**Kletaq** transforms rigid, stressful engineering syllabuses (VTU and autonomous engineering curriculums) into an engaging, gamified learning journey. Designed specifically for engineering undergraduates, Kletaq combines Duolingo-style node maps, bite-sized lessons, interactive quizzes, deep-focus Pomodoro timers, peer networking, and smart home-screen widgets to help students master challenging technical subjects and build unbroken daily study habits.

---

## ✨ Key Features

### 🗺️ Adaptive Curriculum Journey
- **Visual Node Map**: Progress through engineering modules and units rendered as interactive Duolingo-inspired path nodes.
- **Bite-Sized Lessons**: Theory, clean code snippets, and conceptual breakdowns structured for quick comprehension.
- **Interactive Quizzes**: Instant-feedback quizzes validate topic mastery before unlocking subsequent milestones.
- **Backlog Recovery Missions**: Tailored recovery paths for students clearing previous semester backlogs alongside regular coursework.

### 🐼 Dynamic Companion Home Screen Widgets (Glance)
- **2×1 Mini Panda Widget**: Compact home screen companion showcasing your current study flame (`🔥 streakCount`), motivational status, and an animated Panda companion.
- **5 Approved Panda Visual States**:
  - `panda_ready`: Primed for today's study session.
  - `panda_welcome`: Welcomes the student back into the app.
  - `panda_complete`: Celebrates completing today's daily goal.
  - `panda_revision`: Encourages revisiting previous weak concepts.
  - `panda_sleepy`: Late-night reminder to rest or protect the streak.
- **Dashboard Widget**: Comprehensive overview of today's study XP, completed daily tasks, and current subject focus.
- **Real-Time Atomic Sync**: Zero-latency widget updates powered by dual-layer SharedPreferences and Glance DataStore synchronization.

### 🔥 Duolingo-Style Automated Midnight Streak Engine
- **Autonomous Midnight Worker**: Executes automatically at 12:01 AM every day via Android `WorkManager` without requiring the student to open the app.
  - **Streak Shields**: Automatically consumes a shield to protect your streak if a day is missed.
  - **Flame Reset**: Resets today's XP at midnight so the flame greys out until the student logs their daily session.
  - **Atomic Widget Refresh**: Instantly updates all home screen widgets when streaks change or expire.
- **Daily 7:00 PM Study Reminders**: Proactive streak expiration notifications warning students before midnight.

### ⏱️ Deep Focus Timer & Quests
- **Topic-Linked Focus Sessions**: Bind focus intervals directly to syllabus units, topics, or exam revisions.
- **Flexible Controls**: Circular progress hero with pause, resume, and custom duration sheets (15m, 25m, 45m, 60m+).
- **Celebration Modals**: Rewarding completion dialogs with XP gains, sound feedback, and session analytics.

### 🤝 Peer Network & Social Leaderboards
- **Student Public Profiles**: Inspect peer profiles with college, branch, semester, study motivation note, and global stats.
- **Friend Requests**: Send, accept, decline, and manage classmate connections with real-time Firestore sync.
- **Mutual Streak Competitions**: Compare daily study streaks and compete on branch & university leaderboards.

### 🤖 AI Topic Tutor (Gemini Powered)
- **Contextual In-Topic Guidance**: Socratic AI assistant built into syllabus topics to answer specific doubts.
- **Markdown & Code Rendering**: Native syntax-highlighted code snippets and structured conceptual answers.
- **Built-In Rate Limiting**: Intelligent throttling to ensure high responsiveness and maintain fair usage.

### 🎓 Engineering Curriculum Onboarding
- **University & Scheme Support**: VTU 2021/2022 Schemes, autonomous colleges, and branch selection (CSE, ISE, ECE, ME, Civil, AIML, etc.).
- **Cycle & Semester Customization**: Physics cycle, Chemistry cycle, and semesters 1 through 8.

---

## 🏛️ System Architecture

Kletaq follows **Clean Architecture** with a strictly decoupled, modular, feature-first structure:

```
com.kletaq.app/
├── core/                   # Shared framework, design system & navigation
│   ├── constants/          # App constants & configuration
│   ├── di/                 # Dependency injection (Hilt)
│   ├── navigation/         # Central Compose navigation graph & routes
│   ├── theme/              # Color tokens, typography, dark/light theme
│   └── util/               # Notification helpers, extensions, receivers
│
├── data/                   # Data layer (remote, local, repositories)
│   ├── content/            # JSON syllabus loaders & offline assets
│   ├── local/              # DataStore & Room persistence
│   ├── model/              # Firestore entities & data transfer objects
│   └── repository/         # Repository implementations (Auth, Friends, Tasks, Stats)
│
├── domain/                 # Pure domain business logic layer
│   ├── achievement/        # Badge & achievement computation
│   ├── journey/            # Adaptive curriculum progression engine
│   ├── model/              # Domain models & entities
│   └── streak/             # Streak calculation, shields & validation rules
│
├── features/               # Feature presentation layer (Jetpack Compose & ViewModels)
│   ├── auth/               # Google Sign-In & authentication
│   ├── chat/               # AI Topic Tutor & markdown rendering
│   ├── focus/              # Deep focus timer & topic sheets
│   ├── friends/            # Peer search, friend requests & student profiles
│   ├── home/               # Dashboard, daily tasks, progression card
│   ├── journey/            # Curriculum node path & unit sections
│   ├── lesson/             # Interactive lesson runner & quizzes
│   ├── onboarding/         # University, scheme, branch & username setup
│   ├── profile/            # User profile, study goals & settings
│   ├── quest/              # Study quests, time estimators & completion
│   └── tasks/              # Planner, habit tracker & exam roadmap
│
├── notifications/          # WorkManager background workers
│   ├── DailyStudyReminderWorker.kt   # 7:00 PM study reminders
│   ├── MidnightStreakWorker.kt       # 12:01 AM autonomous streak resets
│   └── NotificationWorkScheduler.kt  # WorkManager scheduler
│
├── widgets/                # App Widgets (Jetpack Glance)
│   ├── data/               # WidgetDataHelper & DataStore sync
│   ├── state/              # Glance widget state definitions
│   └── ui/                 # PandaMiniWidget (2x1) & PandaDashboardWidget
│
└── MainActivity.kt         # Single-activity Compose entry point
```

---

## ⚡ Build & Installation Rules

### Recommended Build Variant: `benchmark`

For fluid 120Hz physical device scrolling and accurate profiling, Kletaq features a custom, non-debuggable `benchmark` build variant:

```kotlin
create("benchmark") {
    initWith(getByName("release"))
    isDebuggable = false
    isMinifyEnabled = false
    signingConfig = signingConfigs.getByName("debug")
    matchingFallbacks.add("release")
}
```

> [!NOTE]
> Android's debug ART runtime introduces synthetic micro-stutters during Compose scrolling. Building with `benchmark` provides production-level release performance while retaining debug keystore signing.

### 🚀 Running the App

```bash
# 1. Clone the repository
git clone https://github.com/Akashkatageri/kletaq.git
cd kletaq

# 2. Build and install the Benchmark variant (Recommended for physical devices)
./gradlew installBenchmark

# 3. Or build and install standard Debug variant
./gradlew installDebug
```

---

## 🛠️ Tech Stack & Dependencies

| Layer | Technologies |
| :--- | :--- |
| **Language** | Kotlin 2.0.21 |
| **UI Framework** | Jetpack Compose (BOM 2024.10.01) + Material Design 3 |
| **Architecture** | Clean Architecture + MVVM + Unidirectional Data Flow (UDF) |
| **Dependency Injection** | Dagger Hilt |
| **App Widgets** | Android Jetpack Glance (Compose for App Widgets) |
| **Background Scheduling**| Android WorkManager (Daily reminders & midnight workers) |
| **Backend & Cloud** | Firebase Authentication, Cloud Firestore (Real-Time Listeners) |
| **AI Integration** | Google Generative AI (Gemini Flash) with client rate-limiting |
| **Concurrency** | Kotlin Coroutines & StateFlow / SharedFlow |
| **Data Immutability** | kotlinx-collections-immutable |
| **Image Loading** | Coil Compose |

---

## 🔒 Permanent Architectural Invariants

1. **2×1 Panda Widget Layout**:
   - Fixed `42.dp` bottom illustration container anchored at `Alignment.BottomCenter`.
   - Panda image strictly uses `GlanceModifier.width(68.dp).height(42.dp)` with `ContentScale.Fit`.
   - Top text layer (`🔥 $streakCount` and message) stays within a top `Column` with `.defaultWeight()` and never overlaps the illustration area.
2. **Glance Atomic Sync**:
   - Auth state changes trigger single-pass DataStore updates and cache clears across all active widget instances.
3. **Automated Midnight Reset**:
   - `MidnightStreakWorker` guarantees streak evaluation and shield protection at 12:01 AM without relying on user interaction.

---

## 🤝 Contributing

Contributions, feature suggestions, and syllabus data submissions are welcome!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'feat: Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.
