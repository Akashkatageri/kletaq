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

- `StudyOSApplication.kt` auth state listener and `WidgetDataHelper.clearAndRefresh(ctx)` must remain active to clear SharedPreferences and Glance DataStore per-widget state on account sign-out/switch.
