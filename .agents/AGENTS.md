# Permanent Project Rules & Architectural Constraints

## 🔒 1. 2×1 Panda Widget Layout & Sizing Rules (DO NOT MODIFY)

- **Reserved Illustration Area**: The panda container must always be a fixed `Box(modifier = GlanceModifier.fillMaxWidth().height(42.dp))` anchored at `Alignment.BottomCenter`.
- **Fixed Image Modifiers**: The panda image must strictly use `GlanceModifier.width(68.dp).height(42.dp)` and `contentScale = ContentScale.Fit`. NEVER use `ContentScale.Crop`, `fillMaxSize()`, `fillMaxHeight()`, offsets, or dynamic scaling.
- **Top 2-Line Text Layer**:
  - Line 1: `Text(text = "🔥 $streakCount", fontSize = 12.sp, fontWeight = FontWeight.Bold)`
  - Line 2: `Text(text = messageText, fontSize = 10.sp, fontWeight = FontWeight.Normal)`
  - Text layer must stay strictly inside top `Column` with `.defaultWeight()` and must NEVER overlap the 42.dp bottom panda area.
- **Dynamic Streak Binding**: Must ALWAYS bind dynamically to `text = "🔥 $streakCount"`. NEVER add hardcoded fallbacks (e.g. `else 3`).
- **5 Approved Visual States**: Use only `panda_ready`, `panda_welcome`, `panda_complete`, `panda_revision`, and `panda_sleepy`. Fallback un-replaced PNG states to `panda_ready`.

## 🔒 2. `benchmark` Build Variant (DO NOT MODIFY)

- Must maintain the non-debuggable `benchmark` build type in `app/build.gradle.kts`:
  ```kotlin
  create("benchmark") {
      initWith(getByName("release"))
      isDebuggable = false
      isMinifyEnabled = false
      signingConfig = signingConfigs.getByName("debug")
      matchingFallbacks.add("release")
  }
  ```
- Installed via `./gradlew installBenchmark` for production-speed UI scrolling and physical device verification.

## 🔒 3. Widget Account & Logout Sync (DO NOT MODIFY)

- `KletaqApplication.kt` auth state listener and `WidgetDataHelper.clearAndRefresh(ctx)` must remain active to clear SharedPreferences and Glance DataStore per-widget state on account sign-out/switch.

## 🔒 4. Locked Widget Real-Time & Atomic Sync Architecture (DO NOT MODIFY)

- **KletaqApplication Real-Time Auth Listener**: Must maintain `db.collection("stats").document(currentUser.uid).addSnapshotListener` to sync `WidgetDataHelper.saveStats(...)` and `WidgetDataHelper.refreshWidgets(...)` instantly on login and whenever user stats change.
- **Immediate Logout Wipe**: On `currentUser == null`, `WidgetDataHelper.clearAndRefresh(ctx)` must immediately clear SharedPreferences and Glance DataStore for all mini and dashboard widgets.
- **Glance Composable Preference Binding**: `PandaMiniWidget` and `PandaDashboardWidget` must read keys (`STREAK_KEY`, `TODAY_XP_KEY`, `COMPLETED_TASKS_KEY`, `SUBJECT_KEY`) directly from `currentState<Preferences>()` inside `provideContent`.
- **Single-Pass Atomic Refresh**: `WidgetDataHelper.refreshWidgets` must update DataStore preferences in parallel and invoke single-pass `updateAll` calls (`PandaMiniWidget().updateAll(ctx)`, `PandaDashboardWidget().updateAll(ctx)`). Do NOT add per-widget update loops or redundant broadcast spam that causes SystemUI rate-limiting/throttling.

---

# Build & Installation Rules (MANDATORY)

## Build Variant

Always use the **Benchmark** build variant.

Never build, install, run, or test any other variant unless I explicitly request it.

Allowed:

- benchmarkDebug
- benchmarkRelease (only if explicitly requested)

Not allowed:

- debug
- release
- qa
- staging
- profile
- demo
- any other variant

If the Benchmark variant cannot be built, DO NOT automatically switch to another variant.

Instead:

1. Stop immediately.
2. Explain why the Benchmark build cannot be used.
3. Ask for guidance.

Never silently fall back to another build variant.

---

## Installation

Every time I ask you to build, run, install, or test the Android app:

1. Build the Benchmark variant.
2. Install the Benchmark APK on the connected device/emulator.
3. Launch the Benchmark app.
4. Verify it starts successfully.
5. Report any build or runtime errors.

This is the default behavior for every Android task.

---

## No Assumptions

Do not decide which variant to use.

Do not use Android Studio's currently selected variant.

Do not use Gradle defaults.

Always explicitly specify the Benchmark variant.

Examples:

✅ `assembleBenchmarkDebug`

✅ `installBenchmarkDebug`

❌ `assembleDebug`

❌ `installDebug`

❌ `assembleRelease`

---

## Failure Policy

If any command attempts to build a non-Benchmark variant:

- Abort immediately.
- Explain why.
- Retry using the Benchmark variant only.

Never continue with another variant.

---

## Priority

These instructions override all previous build preferences.

Benchmark is the only default build variant unless I explicitly request another one.

---

## IDE & Runner Rules

Never press Android Studio's generic Run button or use the IDE's selected build variant.

Always invoke Gradle explicitly with the Benchmark variant (for example, `installBenchmarkDebug` or the equivalent Benchmark task). Do not rely on IDE defaults.

---

## 🔒 5. Mandatory App Release & Website Update Rule (DO NOT FORGET)

Every time updates/changes are made:
1. **In-App Updater ("Check for Updates")**:
   - Commit and push to `origin master`.
   - This triggers `.github/workflows/release-apk.yml`, which compiles the `benchmark` APK and publishes the `Latest` release on GitHub (`https://github.com/Akashkatageri/kletaq/releases/tag/Latest`).
   - This allows users to receive OTA updates directly inside the app via **Settings -> Check for Updates**.
2. **Website Distribution (`https://trykletaq.cv` / `https://kletaq.5122006.xyz`)**:
   - Run `powershell -ExecutionPolicy Bypass -File .\update-website-apk.ps1 -SkipBuild` (or without `-SkipBuild`).
   - Copies `kletaq-benchmark.apk` to `..\kletaq-website\public\kletaq.apk`, commits and pushes to `origin main` on `kletaq-website` (deploying instantly via Cloudflare Pages).
   - Also keeps `kletaq.apk` in workspace root updated.

## 🔒 6. Domain Expiration Tracking (`trykletaq.cv`)
- **Expiry Date**: **October 26, 2027** (ownership valid until this date).
- **Primary Domain**: `https://trykletaq.cv`
- **Permanent Fallback Domain**: `https://kletaq.5122006.xyz` and GitHub Releases API (`api.github.com/repos/Akashkatageri/kletaq/releases/latest`).
- **Migration Window**: By **September 2027**, verify renewal of `trykletaq.cv` or migrate primary distribution and deep links before October 26, 2027.
- **Failover Guarantee**: `AppUpdateManager.kt` has tiered failover (`trykletaq.cv` -> `kletaq.5122006.xyz` -> GitHub Releases API) so existing user devices never break if the primary domain expires.

## 🔒 7. Strict Zero-Hardcoding Policy (MANDATORY & ABSOLUTE)
- **NEVER HARDCODE ANYTHING**: Never hardcode dummy placeholders, fake programs, generic algorithm steps (e.g. "Initialize environment and variables", "Execute primary logic computation"), synthetic questions, or mock data.
- **Curriculum Integrity**: All academic curricula, practical lab programs, code snippets, algorithm steps, and viva questions MUST originate strictly from verified official syllabus assets (e.g. `assets/academic/subjects/{subjectId}.json`), user syllabus structures, or be dynamically extracted from user-uploaded college lab manual PDFs/code via multimodal AI.
- **Ask Before Deciding**: If data, assets, or syllabus mappings are ever missing, ambiguous, or not found, **STOP IMMEDIATELY AND ASK THE USER**. Never make assumptions or invent fallback data without explicit confirmation.

---

## 🔒 8. Superpowers Engineering Workflow (ALWAYS ON)
- **Skill-First Discipline**: Always check and invoke relevant skills before taking action or writing code (`Using [skill] to [purpose]`).
- **Process Before Implementation**:
  - For new features, architecture, or designs: invoke `brainstorming` before planning or writing code.
  - For bugs and regressions: invoke `systematic-debugging` first to isolate root causes.
  - For multi-step tasks: invoke `writing-plans` / `executing-plans` to follow a verifiable step-by-step checklist.
- **Test-Driven Discipline**: Enforce Red -> Green -> Refactor TDD cycles for critical logic.
- **Empirical Verification**: Never declare work complete without running verification (benchmark builds, tests, or runtime checks).

---

## 🔒 9. Ponytail Minimalist Senior Dev Principles (ALWAYS ON)
- **Lazy Senior Dev Mindset**: The best code is the code never written. Solve the whole problem with the least new code.
- **Smallest Complete Change Ladder**:
  1. *Does it need to exist?* YAGNI. Skip features, options, and flexibility nobody asked for.
  2. *Already in this codebase?* Reuse existing helpers, components, and patterns.
  3. *Standard library or native platform?* Use built-in APIs before reaching for external dependencies.
  4. *Existing installed dependency?* Use it. Never add a new dependency for a few lines of code.
  5. *Can it be one clear line?* Keep it one line.
  6. *Otherwise*: Write the absolute minimum code that works.
- **No Over-Engineering**: No unnecessary abstractions, wrappers, boilerplate, or premature generalizations. Keep existing layers and conventions.
- **Scope & Safety**: Be lazy about the solution, never about correctness. Never cut validation at boundaries, security, accessibility, or error handling that prevents data loss.
- **Transparency**: Note in 1–2 lines what was skipped or any risks the user should know.


