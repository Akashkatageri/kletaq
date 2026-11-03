# Implementation Plan: Memory Palace Flowchart Alignment

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Align the existing Memory Palace feature with the user's 3-phase flowchart (Preview & Edit stage before saving, dynamic Camera Zoom & Smooth Pan to active loci pins in Walk Mode, 3-tier memory rating: Hard / Good / Easy, and surreal mnemonic visual cue cards), structured for safe concurrent work alongside another active agent.

**Architecture:**
- **Preview & Refine Stage (`CreateDerivationScreen.kt` & `DerivationPreviewSheet.kt`)**: Add an interactive review loop post-AI decomposition allowing the student to inspect $N$ steps mapped to pins, edit equations/story, or re-roll individual mnemonics before saving to the database.
- **Cinematic Zoom & Pan Camera (`CameraPanRoomViewer.kt`)**: Implement animated focal point zoom (1.8x–2.2x scale centered on active pin $(x\%, y\%)$) with smooth animated pan transitions between sequential pins in `PalaceWalkScreen`.
- **Tri-State Spaced Recall Rating**: Enhance Walk Mode feedback with 3 buttons (**Hard** / **Good** / **Easy**) mapped to in-session retry queues and SM-2 interval modifiers.
- **Surreal Mnemonic Cue Card (`MnemonicCueCard.kt`)**: Render a dedicated pre-reveal visual cue card highlighting the room anchor and surreal mental cue before revealing the formula.

**Tech Stack:** Jetpack Compose, Kotlin Coroutines, Gemini 2.0 Flash / UnifiedAiClient, Android Canvas & GraphicsLayer Animation, Room / SharedPreferences.

**Spec Reference:** `docs/superpowers/specs/2026-11-01-memory-palace-design.md`

---

## Multi-Agent Safety & Isolation Rules

> [!IMPORTANT]
> The user stated they are working with **another agent simultaneously** on this repository.
> To prevent merge collisions and overwritten code:
> 1. **Modular New Files First**: Extract new visual features into new, dedicated files under `ui/components/` (`DerivationPreviewSheet.kt`, `CameraPanRoomViewer.kt`, `MnemonicCueCard.kt`) instead of injecting hundreds of lines into shared screens.
> 2. **Surgical Diffs**: Modify shared screens (`CreateDerivationScreen.kt`, `PalaceWalkScreen.kt`) with minimal 10–20 line hooks.
> 3. **Frequent Atomic Commits**: Commit each task immediately upon verification so the working tree remains clean.
> 4. **No Git Stashes / Rebases**: Do not rebase or force-push. Always pull / check `git status` before writing.

---

## Global Constraints

- **Build Variant**: Must compile and verify under **Benchmark** variant only (`compileBenchmarkKotlin`, `testBenchmarkUnitTest`, `assembleBenchmark`).
- **Zero Hardcoding**: Never insert mock derivation steps or fake syllabi.
- **No Heavy 3D / AR Dependencies**: Keep 2D photo-based pin navigation lightweight and battery-efficient.

---

## Proposed Changes

### Component 1: Preview & Edit Stage (Phase 2 Alignment)

#### [NEW] `app/src/main/java/com/kletaq/app/features/memorypalace/ui/components/DerivationPreviewSheet.kt`
- Displays the decomposed derivation before persisting to database.
- Shows list of $N$ steps mapped to room loci:
  - Locus badge (e.g. `Pin #1: Study Lamp`)
  - Editable Step Title
  - Editable LaTeX / Monospace formula
  - Editable Sensory Mnemonic Story with a "Re-roll Story" button
  - Page Citation badge linking to source notes
- Actions:
  - "Save & Anchor to Palace" (commits to `MemoryPalaceRepository`)
  - "Back to Inputs" (returns to URI / title picker)

#### [MODIFY] `app/src/main/java/com/kletaq/app/features/memorypalace/ui/CreateDerivationScreen.kt`
- Intercept successful decomposition:
  - Instead of immediately invoking `MemoryPalaceRepository.saveDerivationWithSteps` and navigating away, set `previewState = DecomposedResult(steps, sourcePaths)`.
  - When `previewState != null`, show `DerivationPreviewSheet`.

---

### Component 2: Camera Zoom & Pan on Loci Pins (Phase 3 Alignment)

#### [NEW] `app/src/main/java/com/kletaq/app/features/memorypalace/ui/components/CameraPanRoomViewer.kt`
- Renders the room photo with animated camera coordinates.
- Uses `animateFloatAsState` with `tween(650, easing = FastOutSlowInEasing)` for:
  - `targetScale`: 1.0f (Full Room) vs 2.1f (Focused Pin)
  - `targetOffsetX`: animates camera center towards `(0.5f - locus.xPercent) * width * scale`
  - `targetOffsetY`: animates camera center towards `(0.5f - locus.yPercent) * height * scale`
- Provides an overlay toggle: **"Full Room View"** vs **"Pin Focus"**.
- Draws the active locus pin with a pulsing glow ring and order index.

#### [MODIFY] `app/src/main/java/com/kletaq/app/features/memorypalace/ui/PalaceWalkScreen.kt`
- Replace static `BoxWithConstraints` image rendering with `CameraPanRoomViewer`.
- When advancing steps (`currentQueueIndex++`), camera smoothly animates and pans from Pin $i$ to Pin $i+1$.

---

### Component 3: 3-Tier Rating & Spaced Repetition (Phase 3 Alignment)

#### [MODIFY] `app/src/main/java/com/kletaq/app/features/memorypalace/data/MemoryPalaceRepository.kt`
- Update `calculateNextReview` to accept a 3-tier rating enum or parameter:
  - `RecallRating.HARD`: Struggled to recall. In-session retry at end of queue; next review scheduled in 15 mins (Sprint) or 4 hours (Mastery). Stage unchanged.
  - `RecallRating.GOOD`: Recalled correctly. Advances to next stage (Sprint: 30m / 4h / exam morning; Mastery: Day 1, 3, 7, 14, 30).
  - `RecallRating.EASY`: Effortless recall. Advances stage with a 1.5x interval multiplier bonus.

#### [MODIFY] `app/src/main/java/com/kletaq/app/features/memorypalace/ui/PalaceWalkScreen.kt`
- Replace 2-button action row ("Struggled" / "Remembered") with 3 distinct rating buttons post-reveal:
  - 🔴 **Hard**: Re-queues step to end of session + logs hard review.
  - 🟡 **Good**: Advances step normally + logs good review.
  - 🟢 **Easy**: Advances step + logs easy review with interval bonus.

---

### Component 4: Surreal Mnemonic Visual Cue Card

#### [NEW] `app/src/main/java/com/kletaq/app/features/memorypalace/ui/components/MnemonicCueCard.kt`
- Pre-reveal visual cue card in Walk Mode:
  - Thematic icon badge matching the locus (e.g. 🛋️ Sofa, 💡 Lamp, 🚪 Door, 🪑 Desk)
  - Surreal cue hint teaser: *"Visualize what absurd event is happening at your [Locus Name]..."*
  - "Reveal Equation & Mental Story" primary action button.

---

## Verification Plan

### Automated Tests
Run unit tests under Benchmark build type:
```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"; ./gradlew :app:testBenchmarkUnitTest
```

New unit tests to add:
- `app/src/test/java/com/kletaq/app/features/memorypalace/MemoryPalaceFlowchartTest.kt`:
  - Test 3-tier recall calculation (`HARD`, `GOOD`, `EASY` intervals).
  - Test in-session re-queue behavior on `HARD`.
  - Test preview step editing & serialization.

### Compilation Check
Compile Benchmark APK:
```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"; ./gradlew assembleBenchmark
```

### Manual Verification
1. **Room Pinning**: Verify pin placement in `PalacePinDropScreen`.
2. **Preview Stage**: Upload derivation notes -> click "Decompose & Anchor" -> verify the interactive `DerivationPreviewSheet` appears showing mapped pins before saving.
3. **Walk Mode Animation**: Start Walk Mode -> verify camera zooms into Pin 1, and tapping "Good" smoothly pans the camera across the room photo to Pin 2.
4. **Rating**: Verify tapping "Hard" re-queues the step to the end of the walk.

