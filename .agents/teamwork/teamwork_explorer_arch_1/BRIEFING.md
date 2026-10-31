# BRIEFING — 2026-10-31T12:12:00Z

## Mission
Comprehensive technical investigation of Kletaq's architecture, cloud sync, performance, offline resilience, and infrastructure, producing high-impact, actionable feature recommendations.

## 🔒 My Identity
- Archetype: teamwork_explorer
- Roles: Architecture, Cloud Sync, Performance & Infrastructure Specialist
- Working directory: c:\Users\Akash Katageri\Documents\New folder\Kletaq\.agents\teamwork\teamwork_explorer_arch_1\
- Original parent: 9cdeba0e-3116-4e08-8c9e-9a61b478c70e
- Milestone: Milestone 1 (App Exploration & Architectural Analysis)

## 🔒 Key Constraints
- Read-only investigation — do NOT implement
- BENCHMARK BUILD VARIANT (Rule 2): Non-debuggable `benchmark` build type in `app/build.gradle.kts`. Must remain default.
- REAL-TIME & ATOMIC SYNC ARCHITECTURE (Rule 4): `KletaqApplication.kt` auth state listener & Firestore snapshotListener; instant logout wipe; single-pass atomic refresh in `WidgetDataHelper.refreshWidgets`.
- APP RELEASE & OTA UPDATE ARCHITECTURE (Rule 5 & 6): In-app updater with tiered failover (`trykletaq.cv` -> `kletaq.5122006.xyz` -> GitHub Releases API).
- STRICT ZERO-HARDCODING POLICY (Rule 7): Academic data must come from official syllabus assets, user syllabus structures, or multimodal AI extraction.
- 2x1 Panda Widget Layout & Sizing Rules (Rule 1): Locked layout rules.

## Current Parent
- Conversation ID: 9cdeba0e-3116-4e08-8c9e-9a61b478c70e
- Updated: 2026-10-31T12:05:00Z

## Investigation State
- **Explored paths**:
  * Build system: `build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`, `update-website-apk.ps1`, `.github/workflows/release-apk.yml`.
  * Application & Sync: `KletaqApplication.kt`, `SecurityGuard.kt`, `WidgetDataHelper.kt`, `PandaMiniWidget.kt`, `PandaDashboardWidget.kt`.
  * Updates & Network: `AppUpdateManager.kt`, `CloudflareR2Uploader.kt`, `UnifiedAiClient.kt`.
  * Background Work: `NotificationWorkScheduler.kt`, `MidnightStreakWorker.kt`, `AlarmReceiver.kt`.
  * Data & Repositories: `ProgressionRepositoryImpl.kt`, `UserStats.kt`, `UserSettingsRepository.kt`, `TaskRepository.kt`, `AcademicContentRepository.kt`, `CustomSyllabusManager.kt`, `CustomNotesManager.kt`, `FriendRepository.kt`, `NotificationCoordinator.kt`, `SoundEffectsManager.kt`, `firestore.rules`.
  * UI Entry Points: `MainActivity.kt`, `HomeScreen.kt`, `ProgressViewModel.kt`, `HomeViewModel.kt`.
- **Key findings**:
  1. Complete absence of a local relational database (`data/local` has only `.gitkeep`; zero Room implementation).
  2. Heavy reliance on `firestore.runTransaction` for session completions, XP updates, and streak changes, which strictly fail offline.
  3. UI Composables directly register Firestore `addSnapshotListener` connections (e.g. `HomeScreen`, `ProfileScreen`), bypassing Clean Architecture layers.
  4. Monolithic Kotlin data classes (`KletaqAcademicRepository.kt` ~492 KB, `VtuSubjectQuestionBank.kt` ~148 KB) eagerly preloaded into memory on startup.
  5. Missing Firebase Crashlytics, Firebase Performance Monitoring, and Baseline Profiles.
  6. Startup thread congestion: `SecurityGuard` synchronously reads `/proc/self/maps` and starts raw Java threads during `Application.onCreate()`; `FriendRepository.init` queries and deletes test leaderboard records on every client startup.
  7. OTA update pipeline and Widget Glance architecture are robust and strictly compliant with project constraints.
- **Unexplored areas**: None for core architecture; all critical paths fully mapped.

## Key Decisions Made
- Architected a 6-pillar feature recommendation roadmap covering:
  1. Room DB + Transaction Sync Queue (offline-first resilience).
  2. WorkManager Resilient Offline Sync Worker & Constrained Prefetching.
  3. Baseline Profiles & Startup Profiling.
  4. Decoupled Architecture & Asset Extraction for Monolithic Repositories.
  5. Firebase Crashlytics & Real-Time Performance Observability.
  6. Clean Architecture Enforcement & UI Decoupling.

## Artifact Index
- DISPATCH.md — Initial dispatch message
- BRIEFING.md — Working memory and status
- progress.md — Liveness heartbeat and milestone tracker
- handoff.md — Comprehensive 5-component report
