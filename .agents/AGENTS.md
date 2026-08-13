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
