<div align="center">

# 🎓 Kletaq

### *Gamified Curriculum Mastery, Deep Focus Engine & Ambient Companion Widgets*

Gamified academic learning, bite-sized curriculum journeys, deep focus timer, automated streak engine, note-grounded 3D flashcards, authentic model papers, and reactive companion widgets built for engineering undergraduates, pre-university, and school students.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.10-purple.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202025.02.00-green.svg?style=flat&logo=android)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Material%203-Enabled-blue.svg?style=flat)](https://m3.material.io)
[![Firebase](https://img.shields.io/badge/Firebase-Auth%20%7C%20Firestore%20%7C%20App%20Check-orange.svg?style=flat&logo=firebase)](https://firebase.google.com)
[![Cloud Sync](https://img.shields.io/badge/Cloud%20Sync-Encrypted%20Storage-00C49F.svg?style=flat)](#)
[![Glance Widgets](https://img.shields.io/badge/Android%20Glance-Widgets-teal.svg?style=flat)](https://developer.android.com/jetpack/compose/glance)
[![WorkManager](https://img.shields.io/badge/WorkManager-Background%20Workers-red.svg?style=flat)](https://developer.android.com/topic/libraries/architecture/workmanager)
[![Build Variant](https://img.shields.io/badge/Build%20Variant-Benchmark-brightgreen.svg?style=flat)](#-build--installation-rules)
[![Website](https://img.shields.io/badge/Website-kletaq.5122006.xyz-00C49F.svg?style=flat)](https://kletaq.5122006.xyz)
[![Release](https://img.shields.io/badge/Release-v--Latest-blueviolet.svg?style=flat)](https://github.com/Akashkatageri/kletaq/releases/tag/Latest)

</div>

---

## 📖 Overview

**Kletaq** transforms rigid, stressful academic curriculums into an engaging, gamified learning journey. Built for **Engineering Undergraduates** (VTU 2021/2022/2025 Schemes & autonomous universities), **Pre-University Students** (PUC 1st/2nd Year Science/Commerce/Arts), and **School Students** (Classes 1–10 CBSE, ICSE, and State Boards), Kletaq combines:

- 🚇 **Metro Roadmap & Universal Syllabus Builder**: Interactive subway-track visual roadmap with technical dot-grid canvas, custom node builders, and Gemini AI syllabus extraction.
- 📄 **Authentic Model Question Papers**: Full 100-mark VTU pattern question papers with 5-module choice, AI grading rubrics, and in-app pan & zoom viewer.
- 🗂️ **Note-Grounded 3D Flashcards**: Interactive 3D flip study cards powered by SuperMemo SM-2 spaced repetition, grounded in lecture notes.
- ☁️ **High-Performance Cloud Notes Sync**: Adaptive compressed lecture notes with instant cloud synchronization, zero-latency caching, and secure token-based user authentication.
- 🛡️ **Enterprise Security & Anti-Tamper Guard**: Runtime signature verification, root/Frida hooking detection, Firebase App Check (Play Integrity), and hardened Firestore security rules.
- 🏆 **Duolingo-Style Competitive Leagues**: 10 weekly tiers (Bronze to Diamond), live classmate activity feed ("LIVE TODAY"), 1-tap invite links, and full light/dark mode adaptation.
- 🐼 **Reactive Home Screen Companion Widgets (Glance)**: 2×1 dynamic Panda companion with 5 expressive mood states, lit vs. greyed flame daily tracking, and victory sunbursts.
- 🌙 **4:00 AM Study Day Rollover & Resilient Streaks**: Natural study day boundary for late-night learners, proactive 7:00 PM reminders, and automated streak shields.
- ⏱️ **Deep Focus Timer with Parkinson's Law**: 2× Bonus XP urgency engine, OLED battery-saver ambient mode, and topic-linked Pomodoro intervals.

---

## ✨ Key Features

### 🚇 Metro Roadmap & Universal Curriculum Journey
- **Metro Transit Track Rail**: Modules and topics render as a continuous metro line with circular station nodes, glowing active pulses, and checkmark mastery rings against a subtle technical dot-grid background.
- **Clean Topic Nodes**: Streamlined, single-row layout displaying the topic title on the left with flex truncation, paired with clean status badges on the right (`UP NEXT · +80 XP ▶`, `Mastered ✓`, or `Study →`).
- **Interactive Visual Roadmap Builder**: Add custom **Subjects**, **Modules**, and **Topic Nodes** directly in the UI. Configure course codes, academic term/semester, custom durations (15m, 25m, 35m, 45m), and topic emojis.
- **AI-Powered Syllabus Importer**: Upload syllabus PDFs or paste raw text to automatically parse, structure, and populate entire subjects using Gemini AI.
- **Subject Command Header**: Quick-switch active subjects with live search and semester filtering, accompanied by dynamic progress rings and direct access to model papers.

### 📄 Authentic Model Question Papers & Custom Attachments
- **100-Mark Authentic Exam Pattern**: Modeled strictly after official VTU examination standards with 5 Modules, choice-based sub-questions (Parts A & B), and marks allocations.
- **AI Evaluator Rubrics**: Integrated grading rubrics providing step-by-step marking schemes and conceptual breakdown for university questions.
- **Custom Model Paper Ingestion**: Attach personal university or college model papers with offline caching.
- **In-App Pan & Zoom Viewer**: Custom fullscreen viewer ([`CustomModelPaperFullscreenDialog.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/features/journey/components/CustomModelPaperFullscreenDialog.kt)) with multi-touch pinch-to-zoom and fluid drag panning.

### 🗂️ Note-Grounded 3D Interactive Flashcards
- **SuperMemo SM-2 Algorithm**: Dynamic intervals based on student review ratings (`Again`, `Hard`, `Good`, `Easy`) to reinforce retention at memory decay points.
- **Tactile 3D Card Flip**: Smooth 3D perspective card rotations with haptic feedback during review sessions.
- **Grounded in Handwritten Notes**: Flashcard decks generated and validated directly against attached lecture notes and curriculum topics.

### ☁️ High-Performance Cloud Notes & Adaptive Compression
- **Secure Cloud Storage Bridge**: Authenticated serverless backend executing strict token identity verification and user-path ownership checks before granting upload or download access.
- **Adaptive Client-Side Compression**: Multi-pass image and PDF downsampling ([`NoteCompressionEngine.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/core/notes/NoteCompressionEngine.kt)) targeting 1080p and 60% quality, preventing bandwidth bloat and keeping uploads lightning fast.
- **Dual-Layer Notes Persistence**: Resilient local offline disk cache backed by seamless cloud sync and Firestore document tracking.

### 🏫 Universal Multi-Tier Onboarding (School, PUC & College)
- **School Students (Classes 1–10)**: Support for CBSE, ICSE, and State Boards with tailored foundational curriculums.
- **Pre-University (PUC 1st & 2nd Year)**: Dedicated streams for Science (PCMB/PCMC), Commerce, and Arts.
- **Engineering Undergraduates**: Schemes for VTU (2021, 2022, 2025 Schemes) and autonomous engineering institutions.
- **Adaptive Terminology**: Dynamically shifts app-wide UI labels between **Board · Class · Academic Year** and **University · Scheme · Branch · Semester**.

### 🛡️ Enterprise Runtime Security & Anti-Tamper Hardening
- **`SecurityGuard.kt` Runtime Checks**:
  - **APK Signature Verification**: Validates signing certificate SHA-256 fingerprint against trusted whitelist to block repackaging and smali modding.
  - **Root Detection**: Scans for standard root binaries (`su`, `busybox`, `magisk`) and test-keys build tags.
  - **Frida Hooking Detection**: Inspects native `/proc/self/maps` and local memory for dynamic hooking frameworks (Frida, Xposed, Substrate).
  - **Debugger Detection**: Identifies connected debuggers (`Debug.isDebuggerConnected()`).
- **Firebase App Check & Play Integrity**: Secures backend Firestore and Auth requests against bot abuse and unauthorized clients.
- **Hardened Firestore Security Rules**: Granular user-ownership constraints (`request.auth.uid == userId`) and strict field-level validation across all user collections.
- **AI Prompt Sandboxing**: Client-side prompt isolation preventing injection attacks and token runaway during AI tutor interactions.
- **Network Security Configuration**: Strict TLS enforcement with cleartext traffic prevention across all endpoints.

### 🏆 Competitive Weekly Leagues & Classmate Feed
- **10 Duolingo-Style League Tiers**: Advance from **Bronze** through Silver, Gold, Sapphire, Ruby, Emerald, Amethyst, Pearl, Obsidian, to **Diamond League**.
- **Automated Promotion & Demotion**: Real-time ranking thresholds with automatic weekly resets every Sunday at midnight.
- **"LIVE TODAY" Classmate Activity Feed**: Real-time notification feed showcasing classmate study sprints and completed topics.
- **1-Tap Social Invites**: Shareable personal invite links (`https://kletaq.5122006.xyz/u/<username>`) with QR and one-click clipboard copying.
- **Full Light & Dark Theme Adaptation**: Surface luminance detection ensures cards, tabs, action icons, and student profile sheets render crisply without dark container glitches.

### 🐼 Reactive Companion Home Screen Widgets (Glance)
- **2×1 Panda Mini Widget**: Compact companion displaying your study flame (`🔥 streakCount`), motivational message, and animated character.
- **5 Approved Visual States**:
  - `panda_ready`: Primed for today's study session.
  - `panda_welcome`: Welcomes the student back into the app.
  - `panda_complete`: Celebrates completing today's daily goal.
  - `panda_revision`: Encourages revisiting previous weak concepts.
  - `panda_sleepy`: Late-night reminder to rest or protect the streak.
- **Dynamic Flame Tracking**: Renders a muted grey outline when today's goal is pending, lighting up into a vibrant orange fire (`🔥`) the moment study minutes are logged.
- **Radiant Emerald Sunburst**: Celebrates daily goal completion with golden trophies and victory sunburst backdrops.
- **Atomic Glance Sync**: Dual-layer SharedPreferences and Glance DataStore synchronization ensures instant updates without SystemUI throttling.

### 🌙 4:00 AM Study Day Rollover & Resilient Streak Protection
- **4:00 AM Natural Study Day Boundary**: Accommodates student night owls—sessions logged between 12:00 AM and 3:59 AM count towards the previous day's study goal.
- **Autonomous Midnight Worker**: Daily `WorkManager` background worker evaluates streak state and applies shield protections.
- **Auto-Refilling Streak Shields**:
  - Maximum shield cap of 3.
  - Automatically refills back to 3 whenever shields drop to 1 or below.
  - 2 free weekly shields.
  - Zero-streak immunity protects brand new users from losing shields before their first session.
- **Daily 7:00 PM Proactive Reminders**: Early evening notification alerts students to secure their flame before the study rollover.

### ⏱️ Deep Focus Timer with OLED Ambient Mode
- **⚡ Parkinson's Law 2× Bonus XP**: Complete focus quests within estimated time limits to earn **40 XP (2× boost)** instead of standard 20 XP.
- **🌙 OLED Black Ambient Saver**: Ultra-minimalist dark screen designed for AMOLED displays to conserve battery during long study marathons.
- **Keep-Awake Wake Lock**: Screen stay-awake toggle prevents display timeouts while solving problems or taking notes.
- **Topic-Linked Intervals**: Custom intervals (15m, 25m, 45m, 60m+) directly bound to curriculum topics.

### 🤖 Socratic AI Topic Tutor (Gemini Powered)
- **In-Topic Guidance**: Socratic AI assistant built into lesson topics to clarify technical doubts step-by-step.
- **Markdown & Syntax Highlighting**: Native rendering for equations, bullet points, and code snippets.
- **Client-Side Throttling**: Intelligent rate limiting and anti-abuse safeguards for reliable performance.

### 🔄 In-App OTA Updates & Automated CI/CD
- **In-App Updater ("Check for Updates")**: Built-in APK updater under Settings checks GitHub Releases (`tag/Latest`) and installs updates directly in-app.
- **Web Distribution & Portal**: Live website at [kletaq.5122006.xyz](https://kletaq.5122006.xyz) automatically updated via `update-website-apk.ps1`.
- **GitHub Actions Release Pipeline**: Automatically builds and releases the benchmark APK on every push to `master`.

---

## 🧪 Scientific Study Techniques & Cognitive Architecture

Kletaq is engineered around validated cognitive psychology, educational neuroscience, and behavioral economics methodologies:

### 1. ⚡ Parkinson's Law & Urgency Engineering (2× Bonus XP)
* **Scientific Basis:** Formulated by Cyril Northcote Parkinson (1955): *"Work expands so as to fill the time available for its completion."* Intentionally compressing target duration by **15–20%** creates healthy urgency, sharpens focus, and eliminates procrastination.
* **Implementation in Kletaq:**
  * **Compressed Time Estimator**: Integrated into [`QuestFocusTimerScreen.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/features/quest/QuestFocusTimerScreen.kt).
  * **Live Timer Stakes**: Displays the active `⚡ Parkinson's Timer: 2x XP if finished on time!` badge; extending time forfeits bonus.
  * **Gamified 2× Reward**: On-time completion in [`ProgressionRepositoryImpl.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/data/repository/ProgressionRepositoryImpl.kt) awards **40 XP (2× boost)**.
  * **Celebration Dialog**: [`QuestCompletionDialog.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/features/quest/components/QuestCompletionDialog.kt) highlights Parkinson's bonus completion.

---

### 2. 🧠 Spaced Repetition (SuperMemo SM-2 & Ebbinghaus Forgetting Curve)
* **Scientific Basis:** Hermann Ebbinghaus's forgetting curve proves that memory decays exponentially over time ($R = e^{-0.70 \cdot \frac{t}{S}}$). Reviewing material at expanding intervals right before forgetting converts knowledge into permanent long-term memory.
* **Implementation in Kletaq:**
  * [`SpacedRepetitionEngine.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/core/utils/SpacedRepetitionEngine.kt) & [`TopicFlashcardDeckCard.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/features/flashcards/TopicFlashcardDeckCard.kt).
  * Dynamic **SM-2 Easiness Factors** (range $1.3$ to $3.5$) adjusted based on student review ratings (`AGAIN`, `HARD`, `GOOD`, `EASY`).
  * **Memory Health Tracking**: 5 decay states: `FRESH` ($\ge 90\%$), `STABLE` ($\ge 75\%$), `FADING` ($\ge 60\%$), `WEAK` ($\ge 40\%$), and `CRITICAL` ($< 40\%$).

---

### 3. 📝 Active Recall & Note-Grounded Testing (The Testing Effect)
* **Scientific Basis:** Roediger & Karpicke’s research confirms that actively retrieving knowledge from memory produces substantially higher retention and exam transfer compared to passive reading or highlighting.
* **Implementation in Kletaq:**
  * Interactive checkpoint questions, problem validations, and note-grounded 3D flashcards requiring confidence grading (1–5 scale) before graduating milestones.

---

### 4. ⏱️ The Pomodoro Technique & Time-Boxing (Cognitive Fatigue Reduction)
* **Scientific Basis:** Francesco Cirillo's time-boxing combats cognitive fatigue by constraining focused work to distraction-free blocks (15–45 minutes) followed by dedicated rest.
* **Implementation in Kletaq:**
  * [`TimerScreen.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/features/focus/TimerScreen.kt) & [`QuestFocusTimerScreen.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/features/quest/QuestFocusTimerScreen.kt).
  * Focus sessions directly anchored to syllabus units with circular progress rings, customizable intervals, and pause/resume states.

---

### 5. 🧩 Microlearning & Cognitive Load Theory (Miller's Law / Chunking)
* **Scientific Basis:** George Miller’s $7 \pm 2$ rule and John Sweller's Cognitive Load Theory demonstrate that working memory is easily overwhelmed by dense engineering textbooks.
* **Implementation in Kletaq:**
  * [`MetroRoadmapNode.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/features/journey/components/MetroRoadmapNode.kt) & [`JourneyScreen.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/features/journey/JourneyScreen.kt).
  * Deconstructs multi-module syllabuses into a visual map of single-concept lesson nodes, reducing cognitive load to bite-sized 3–5 minute learning increments.

---

### 6. 🔀 Interleaved Practice vs. Blocked Practice
* **Scientific Basis:** Mixing different subject matters and interleaving older review items with new concepts leads to significantly better problem-solving flexibility than repetitive cramming (Kornell & Bjork).
* **Implementation in Kletaq:**
  * [`DailyTasksSection.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/features/home/components/DailyTasksSection.kt).
  * Dynamically blends overdue spaced repetition reviews with newly unlocked lesson nodes and backlog subject milestones.

---

### 7. 🔥 Habit Loops & Loss Aversion (Behavioral Economics)
* **Scientific Basis:** Charles Duhigg’s Habit Loop (*Cue $\rightarrow$ Routine $\rightarrow$ Reward*) paired with Kahneman & Tversky’s Prospect Theory (*Loss Aversion*—people are significantly more motivated to protect an existing streak than to gain an arbitrary reward).
* **Implementation in Kletaq:**
  * [`StreakManager.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/domain/streak/StreakManager.kt), [`MidnightStreakWorker.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/notifications/MidnightStreakWorker.kt), and [`PandaMiniWidget.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/widgets/ui/PandaMiniWidget.kt).
  * 4:00 AM natural day rollover, 7:00 PM proactive reminders, burning study flame counter, and consumable Streak Shields.

---

### 8. 💡 The Socratic Method & Feynman Technique (AI Topic Tutor)
* **Scientific Basis:** Richard Feynman’s principle that true mastery comes from explaining complex topics in plain terms, combined with Socrates' guided inquiry to uncover conceptual gaps.
* **Implementation in Kletaq:**
  * [`TopicChatViewModel.kt`](file:///c:/Users/Akash%20Katageri/Documents/New%20folder/Kletaq/app/src/main/java/com/kletaq/app/features/chat/TopicChatViewModel.kt).
  * Gemini-powered in-lesson tutor guides students step-by-step with structured markdown explanations, code snippets, and probing questions.

---

## 🏛️ System Architecture

Kletaq follows **Clean Architecture** with a strictly decoupled, modular, feature-first structure:

```
com.kletaq.app/
├── core/                   # Shared framework, security & design system
│   ├── config/             # Adaptive engine & learning parameters
│   ├── constants/          # App constants & configuration
│   ├── di/                 # Dependency injection (Hilt)
│   ├── navigation/         # Central Compose navigation graph & routes
│   ├── notes/              # Cloud storage uploader, compression & note cache
│   ├── security/           # SecurityGuard anti-tamper, signature & root checks
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
│   └── streak/             # Streak calculation, shields & 4 AM validation rules
│
├── features/               # Feature presentation layer (Jetpack Compose & ViewModels)
│   ├── auth/               # Google Sign-In & authentication
│   ├── calendar/           # Study calendar & schedule alarms
│   ├── chat/               # Socratic AI Topic Tutor & markdown rendering
│   ├── exam/               # Authentic VTU 100-mark model papers & rubrics
│   ├── flashcards/         # Note-grounded 3D interactive flashcard decks
│   ├── focus/              # Deep focus timer, OLED ambient mode & sound sheets
│   ├── friends/            # Competitive weekly leagues, classmate feed & student profiles
│   ├── home/               # Dashboard, hero card, timetable card, daily tasks
│   ├── journey/            # Metro roadmap track, unit sections, visual builder & model papers
│   ├── lesson/             # Interactive study runner, notes viewer
│   ├── notes/              # Note creator & custom attachment manager
│   ├── onboarding/         # School (1-10), PUC (1-2), Engineering, scheme & username setup
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
| **Backend & Cloud** | Firebase Auth, Cloud Firestore, Play Integrity (App Check) | BOM 33.9.0 |
| **Cloud Storage & Sync** | High-speed cloud object storage + Authenticated Serverless Proxy | — |
| **AI Integration** | Google Generative AI (Gemini Flash) with client rate-limiting | 17.14.0 |
| **Runtime Security** | `SecurityGuard.kt` (Signature check, Root check, Frida detection) | — |
| **Concurrency** | Kotlin Coroutines & StateFlow / SharedFlow | 1.10.1 |
| **Data Persistence** | Jetpack Preferences DataStore | 1.1.2 |
| **Document & Image Rendering** | Android PdfRenderer, BitmapFactory, Coil Compose | 2.7.0 |
| **Image Compression** | Adaptive client-side downsampling (1080p, 60q) | — |
| **Web Distribution** | Web Portal & Direct Download (`https://kletaq.5122006.xyz`) | — |

---

## 🔒 Permanent Architectural Invariants

1. **2×1 Panda Widget Layout**:
   - Fixed `42.dp` bottom illustration container anchored at `Alignment.BottomCenter`.
   - Panda image strictly uses `GlanceModifier.width(68.dp).height(42.dp)` with `ContentScale.Fit`.
   - Top text layer (`🔥 $streakCount` and message) stays within a top `Column` with `.defaultWeight()` and never overlaps the illustration area.
2. **Glance Atomic Sync**:
   - Auth state changes trigger single-pass DataStore updates and cache clears across all active widget instances.
3. **Automated Midnight Reset & 4 AM Rollover**:
   - `MidnightStreakWorker` guarantees streak evaluation and shield protection without user interaction; study sessions before 4:00 AM count towards the previous day.
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
