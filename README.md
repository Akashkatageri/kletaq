<div align="center">

# 🎓 Kletaq

### *Gamified Curriculum Mastery & Focus Engine for Engineering Students*

Gamified academic learning, bite-sized curriculum journeys, deep focus timer, automated streak engine, and reactive companion widgets built for engineering university students.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.10-purple.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202025.02.00-green.svg?style=flat&logo=android)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Material%203-Enabled-blue.svg?style=flat)](https://m3.material.io)
[![Firebase](https://img.shields.io/badge/Firebase-Auth%20%7C%20Firestore-orange.svg?style=flat&logo=firebase)](https://firebase.google.com)
[![Glance Widgets](https://img.shields.io/badge/Android%20Glance-Widgets-teal.svg?style=flat)](https://developer.android.com/jetpack/compose/glance)
[![WorkManager](https://img.shields.io/badge/WorkManager-Background%20Workers-red.svg?style=flat)](https://developer.android.com/topic/libraries/architecture/workmanager)
[![Build Variant](https://img.shields.io/badge/Build%20Variant-Benchmark-brightgreen.svg?style=flat)](#-build--installation-rules)
[![Website](https://img.shields.io/badge/Website-kletaq.5122006.xyz-00C49F.svg?style=flat)](https://kletaq.5122006.xyz)
[![Release](https://img.shields.io/badge/Release-v--Latest-blueviolet.svg?style=flat)](https://github.com/Akashkatageri/kletaq/releases/tag/Latest)

</div>

---

## 📖 Overview

**Kletaq** transforms rigid, stressful engineering syllabuses (VTU 2021/2022/2025 Schemes, non-VTU universities, and autonomous engineering curriculums) into an engaging, gamified learning journey. Designed specifically for engineering undergraduates, Kletaq combines visual roadmap creation tools, AI syllabus importing, visual syllabus node maps, high-density study packs, handwritten notes viewers, weekly competitive leagues, class timetable cards, deep-focus Pomodoro timers with OLED battery-saver ambient mode, automated streak engines, and reactive home-screen companion widgets to help students master technical subjects and build unbroken daily study habits.

---

## ✨ Key Features

### 🛠️ Visual Roadmap Builder & Universal Syllabus Support
- **Interactive Visual Builder**: Build custom curriculum journeys manually with dedicated visual buttons to add **Subjects**, **Modules**, and **Topic Nodes** directly in the UI.
- **Custom Subject Customization**: Configure custom subject names, optional course codes, semester assignments (S1 to S8+), and emoji icons (📚, 💻, 🧠, ⚙️, 📐, 🔬, ⚡, 🌐).
- **Inline Node & Module Appenders**: Seamlessly append new topic nodes with custom study timers (15m, 25m, 35m, 45m) and expand subjects with new modules anytime.
- **Real-Time Reactive Synchronization**: Automatically persists to local JSON and synchronizes across the Home screen and Journey screen with live revision state tracking.
- **AI-Powered Syllabus Importer**: Upload PDF course documents or paste raw text to automatically extract and structure modules and topic nodes using Gemini AI.

### 🗺️ Adaptive Curriculum Journey & Study Packs
- **Visual Node Map**: Progress through engineering modules and units rendered as interactive milestone path nodes with automatic expansion to the last studied unit.
- **Bite-Sized Lessons**: Theory, mathematical formulations, university exam model questions, and conceptual breakdowns structured for quick comprehension.
- **High-Density Study Packs**: Interactive course packs covering 3rd semester subjects (Mathematics, Java Lab Manual, DSA Lab, IoT, and Allied subjects) aligned to VTU 2022 and 2025 schemes.
- **Backlog Recovery Missions**: Tailored recovery paths for students clearing previous semester backlogs alongside regular coursework.

### 📝 Handwritten Notes Engine & PDF Viewer
- **Integrated Lesson Notes**: Handwritten lecture notes and reference materials embedded directly into topic nodes.
- **Pinch-to-Zoom & Rotation**: Fluid multi-touch zoom and 90° per-page rotation for studying dense formulas and schematics.
- **Client-Side Notes Upload**: Attach personal PDFs or lecture images directly to any syllabus topic with offline local storage.
- **In-Session Quick Access**: Slide open topic notes and attached PDFs directly from the focus timer without leaving the study flow.

### 🏆 Competitive Weekly Leagues & Social Leaderboards
- **10 Competitive League Tiers**: Advance from **Bronze** through Silver, Gold, Sapphire, Ruby, Emerald, Amethyst, Pearl, Obsidian, to **Diamond League**.
- **Promotion & Demotion Zones**: Live weekly rankings with real-time promotion and demotion thresholds resetting every Sunday midnight.
- **3 Dynamic Leaderboard Scopes**:
  - `🏆 League`: Compete against peers in your current league division.
  - `👥 Friends`: Mutual classmate rankings and friendly rivalries.
  - `🌐 Global`: University-wide global XP standing.
- **Peer Profiles & 1-Tap Invites**: Inspect student profiles, motivation status, college, and branch; share personal invite links to challenge classmates.

### 📅 Class Timetable & Schedule Card
- **Dashboard Timetable Card**: Live home screen widget showcasing current and upcoming college lectures for the day.
- **Single-Stream Ingestion**: Upload schedules directly from images or PDFs with multi-pass size optimization and offline persistence.

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

### 🔥 Resilient Automated Midnight Streak Engine
- **Autonomous Midnight Worker**: Executes automatically at 12:01 AM every day via Android `WorkManager` without requiring the app to be open.
- **Streak Shield Protection & Auto-Refill**:
  - Maximum shield cap of 3.
  - Automatically refills shields back to 3 whenever shields drop to 1 or below.
  - Grants 2 free weekly shields.
  - Zero-streak immunity protects new users from losing shields before starting.
- **Daily 7:00 PM Study Reminders**: Proactive streak expiration notifications warning students before midnight.

### ⏱️ Deep Focus Timer with OLED Ambient Mode
- **⚡ Parkinson's Law 2× Bonus XP**: Finish the focus quest within the estimated time to earn 40 XP (2× boost) instead of 20 XP.
- **🌙 OLED Black Ambient Saver**: Ultra-minimalist dark mode designed for AMOLED screens to save battery and prevent burn-in during long study marathons.
- **Screen Wake Lock**: Keep-awake toggle prevents display timeouts while reviewing topics or working through problem sets.
- **Topic-Linked Intervals**: Bind focus intervals (15m, 25m, 45m, 60m+) directly to syllabus units and exam tasks.

### 🤖 AI Topic Tutor (Gemini Powered)
- **Contextual In-Topic Guidance**: Socratic AI assistant built into syllabus topics to answer specific doubts step-by-step.
- **Markdown & Code Rendering**: Native syntax-highlighted code snippets, bullet summaries, and math formulas.
- **Built-In Rate Limiting**: Intelligent client-side throttling to ensure high responsiveness and fair API usage.

### 🔄 In-App OTA Updates & Automated Release Pipeline
- **OTA Updates ("Check for Updates")**: Built-in APK updater under Settings checks GitHub Releases (`tag/Latest`) and installs updates directly in-app.
- **Official Web Distribution**: Live website at [kletaq.5122006.xyz](https://kletaq.5122006.xyz) automatically updated via Cloudflare Pages.
- **Automated CI/CD**: GitHub Actions workflow compiles the benchmark APK and deploys releases on every master commit.

---

## 🧪 Scientific Study Techniques & Cognitive Architecture

Kletaq is engineered around validated cognitive psychology, educational neuroscience, and behavioral economics methodologies:

### 1. ⚡ Parkinson's Law & Urgency Engineering (2× Bonus XP)
* **Scientific Basis:** Formulated by Cyril Northcote Parkinson (1955): *"Work expands so as to fill the time available for its completion."* When students study without strict time limits, attention wanders and procrastination sets in. Intentionally compressing the target duration by **15–20%** creates healthy urgency, sharpens focus, and accelerates flow ("aim for completion, not perfection").
* **Implementation in Kletaq:**
  * **Compressed Time Estimator**: Integrated into [`QuestFocusTimerScreen.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/features/quest/QuestFocusTimerScreen.kt) allowing students to set tighter target durations against the timer.
  * **Live Timer Stakes**: [`QuestFocusTimerScreen.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/features/quest/QuestFocusTimerScreen.kt) displays the active `⚡ Parkinson's Timer: 2x XP if finished on time!` badge. Extending time forfeits the bonus.
  * **Gamified 2× Reward**: On-time completion in [`ProgressionRepositoryImpl.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/data/repository/ProgressionRepositoryImpl.kt) awards **40 XP (2× boost)** instead of standard 20 XP.
  * **Celebration Dialog**: [`QuestCompletionDialog.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/features/quest/components/QuestCompletionDialog.kt) highlights `⚡ Parkinson's Law: 2x Bonus XP Active!`.

---

### 2. 🧠 Spaced Repetition (SuperMemo SM-2 & Ebbinghaus Forgetting Curve)
* **Scientific Basis:** Hermann Ebbinghaus's forgetting curve proves that memory decays exponentially over time ($R = e^{-0.70 \cdot \frac{t}{S}}$). Reviewing material at strategically expanding intervals right before forgetting resets memory retention and converts knowledge into permanent long-term storage.
* **Implementation in Kletaq:**
  * [`SpacedRepetitionEngine.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/core/utils/SpacedRepetitionEngine.kt) & [`AdaptiveEngineConfig.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/core/config/AdaptiveEngineConfig.kt)
  * Dynamic **SM-2 Easiness Factors** (range $1.3$ to $3.5$) adjusted based on student review ratings (`AGAIN`, `HARD`, `GOOD`, `EASY`).
  * **Memory Health Tracking**: Each topic has an active retention score categorized into 5 decay states: `FRESH` ($\ge 90\%$), `STABLE` ($\ge 75\%$), `FADING` ($\ge 60\%$), `WEAK` ($\ge 40\%$), and `CRITICAL` ($< 40\%$).
  * Automatic scheduling of a **Daily Review Queue** to review fading topics before exams.

---

### 3. 📝 Active Recall & Self-Testing (The Testing Effect)
* **Scientific Basis:** Roediger & Karpicke’s research confirms that actively retrieving knowledge from memory produces substantially higher retention and exam transfer compared to passive reading or highlighting.
* **Implementation in Kletaq:**
  * [`LessonScreen.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/features/lesson/LessonScreen.kt) & [`LessonCompletionFeedbackSheet.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/features/lesson/components/LessonCompletionFeedbackSheet.kt)
  * Topics require answering interactive checkpoint questions, problem validations, and self-assessing confidence (1–5 scale) to graduate each milestone.

---

### 4. ⏱️ The Pomodoro Technique & Time-Boxing (Cognitive Fatigue Reduction)
* **Scientific Basis:** Francesco Cirillo's time-boxing combats cognitive fatigue by constraining focused work to distraction-free blocks (traditionally 25 minutes) followed by dedicated rest, aligning with ultradian cycles.
* **Implementation in Kletaq:**
  * [`TimerScreen.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/features/focus/TimerScreen.kt) & [`QuestFocusTimerScreen.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/features/quest/QuestFocusTimerScreen.kt)
  * Focus sessions are directly anchored to specific curriculum topics with circular progress timers, customizable intervals (15m, 25m, 45m, 60m+), and pause/resume states.

---

### 5. 🧩 Microlearning & Cognitive Load Theory (Miller's Law / Chunking)
* **Scientific Basis:** George Miller’s $7 \pm 2$ rule and John Sweller's Cognitive Load Theory demonstrate that working memory is easily overwhelmed by dense engineering textbook chapters.
* **Implementation in Kletaq:**
  * [`LessonStatusEvaluator.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/domain/progression/LessonStatusEvaluator.kt) & [`JourneyScreen.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/features/journey/JourneyScreen.kt)
  * Deconstructs multi-module engineering syllabuses into a visual map of single-concept lesson nodes, reducing intrinsic cognitive load to bite-sized 3–5 minute learning increments.

---

### 6. 🔀 Interleaved Practice vs. Blocked Practice
* **Scientific Basis:** Cognitive psychology (Kornell & Bjork) proves that mixing different subject matters and interleaving older review items with new concepts leads to significantly better problem-solving flexibility than "massed/blocked" cramming.
* **Implementation in Kletaq:**
  * [`DailyTasksSection.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/features/home/components/DailyTasksSection.kt) & [`HomeViewModel.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/features/home/HomeViewModel.kt)
  * The daily study planner dynamically blends overdue spaced repetition reviews with newly unlocked lesson nodes and backlog subject milestones instead of repetitive, single-subject cramming.

---

### 7. 🔥 Habit Loops & Loss Aversion (Behavioral Economics)
* **Scientific Basis:** Charles Duhigg’s Habit Loop (*Cue $\rightarrow$ Routine $\rightarrow$ Reward*) paired with Kahneman & Tversky’s Prospect Theory (*Loss Aversion*—people are significantly more motivated to protect an existing streak than to gain an arbitrary reward).
* **Implementation in Kletaq:**
  * [`StreakManager.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/domain/streak/StreakManager.kt), [`MidnightStreakWorker.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/notifications/MidnightStreakWorker.kt), and [`PandaMiniWidget.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/widgets/ui/PandaMiniWidget.kt)
  * **Daily Cues:** 7:00 PM proactive streak expiration reminders.
  * **Loss Aversion:** Burning study flame counter, weekly grants, and consumable **Streak Shields** (auto-refilled when $\le 1$) to protect accumulated streaks against unexpected missed days.
  * **Rewards:** Level progression XP, league promotions, and reactive Panda widget companion states.

---

### 8. 💡 The Socratic Method & Feynman Technique (AI Topic Tutor)
* **Scientific Basis:** Richard Feynman’s principle that true mastery comes from explaining complex topics in plain, foundational terms, combined with Socrates' guided inquiry to uncover conceptual gaps.
* **Implementation in Kletaq:**
  * [`TopicChatViewModel.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/features/chat/TopicChatViewModel.kt) & [`TopicChatEntry.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/features/chat/TopicChatEntry.kt)
  * Gemini-powered in-lesson tutor guides students step-by-step with structured markdown explanations, code snippets, and probing questions rather than dumping raw answers.

---

## 🏛️ System Architecture

Kletaq follows **Clean Architecture** with a strictly decoupled, modular, feature-first structure:

```
com.kletaq.app/
├── core/                   # Shared framework, design system & navigation
│   ├── config/             # Adaptive engine & learning parameters
│   ├── constants/          # App constants & configuration
│   ├── di/                 # Dependency injection (Hilt)
│   ├── navigation/         # Central Compose navigation graph & routes
│   ├── notes/              # Custom student notes persistence & caching
│   ├── sound/              # Audio playback & study sounds
│   ├── theme/              # Color tokens, typography, dark/light theme
│   ├── timetable/          # Timetable parser, compression & local store
│   ├── update/             # In-app OTA updater & GitHub release client
│   └── util/               # Notifications, extensions, alarms, receivers
│
├── data/                   # Data layer (remote, local, repositories)
│   ├── content/            # JSON syllabus loaders, study packs & offline assets
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
│   ├── calendar/           # Study calendar & schedule alarms
│   ├── chat/               # Socratic AI Topic Tutor & markdown rendering
│   ├── focus/              # Deep focus timer, OLED ambient mode & sound sheets
│   ├── friends/            # Competitive weekly leagues, classmate invites & student profiles
│   ├── home/               # Dashboard, class timetable card, daily tasks
│   ├── journey/            # Curriculum node path, unit sections & study packs
│   ├── lesson/             # Interactive study runner, handwritten notes viewer
│   ├── onboarding/         # University, scheme, branch & username setup
│   ├── profile/            # User profile, study goals & statistics
│   ├── quest/              # Study quests, time estimators & 2x XP celebrations
│   ├── settings/           # Settings, active backlogs, archived terms, OTA updater
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

| Layer | Technologies | Version |
| :--- | :--- | :--- |
| **Language** | Kotlin | 2.1.10 |
| **Android Gradle Plugin** | AGP | 8.8.2 |
| **UI Framework** | Jetpack Compose (BOM) + Material Design 3 | 2025.02.00 |
| **Architecture** | Clean Architecture + MVVM + Unidirectional Data Flow (UDF) | — |
| **Dependency Injection** | Dagger Hilt | 2.55 |
| **Navigation** | Jetpack Navigation Compose | 2.8.8 |
| **App Widgets** | Android Jetpack Glance (Material 3) | 1.1.1 |
| **Background Scheduling**| Android WorkManager (Daily reminders & midnight workers) | 2.10.0 |
| **Backend & Cloud** | Firebase Auth, Cloud Firestore, Play Integrity | BOM 33.9.0 |
| **AI Integration** | Google Generative AI (Gemini Flash) with client rate-limiting | 17.14.0 |
| **Concurrency** | Kotlin Coroutines & StateFlow / SharedFlow | 1.10.1 |
| **Data Persistence** | Jetpack Preferences DataStore | 1.1.2 |
| **Data Immutability** | kotlinx-collections-immutable | 0.3.8 |
| **Image & Document Rendering** | Android PdfRenderer, BitmapFactory, Coil Compose | — |

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
4. **Benchmark Build Variant**:
   - Always compile and install using the `benchmark` variant (`installBenchmark` / `assembleBenchmark`) for physical device performance.
5. **App Release & Website Distribution**:
   - Pushes to `origin master` trigger `.github/workflows/release-apk.yml` to compile and publish the latest APK to GitHub Releases.
   - Run `update-website-apk.ps1` to sync the public APK to `kletaq-website` ([kletaq.5122006.xyz](https://kletaq.5122006.xyz)).

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
