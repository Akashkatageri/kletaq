# Memory Palace (Method of Loci) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a complete Method of Loci "Memory Palace" feature for Kletaq where students snap room photos, tap to place ordered loci pins, upload mathematical derivations or code algorithms to receive AI-generated sensory stories & exam trap alerts, and practice with high-speed mental walkthroughs and spaced repetition.

**Architecture:** Dedicated feature module (`features/memorypalace`) with Room entities for palaces, room images, pins, derivations, and steps. Unbundling engine using ML Kit + Gemini 2.0 Flash multimodal vision. Jetpack Compose UI with interactive touch-pinning, pulsing locus highlighting, and dual-mode spaced repetition.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Room Database, Google Gemini 2.0 Flash, Android ML Kit Text Recognition, Coil for image loading.

**Spec:** `docs/superpowers/specs/2026-11-01-memory-palace-design.md`

## Global Constraints
* Must strictly build, compile, and run under the `benchmark` variant (`compileBenchmarkKotlin`, `assembleBenchmark`, `installBenchmark`). Never use debug or release.
* Strict zero-hardcoding policy: all derivations, algorithms, and steps must dynamically originate from user-uploaded PDFs/notes.
* Room photos must be compressed and saved to app-internal storage (`context.filesDir/palace_images/`) to prevent OOM errors.
* No external 3D/AR libraries (no ARCore, no Filament). Clean 2D interactive Compose canvas with relative coordinate pins.

## Review Focus
1. **Loci Pin Coordinate Scaling:** Pins must use relative `(xPercent, yPercent)` (0.0 to 1.0) so pin positions remain pixel-accurate across different screen sizes, aspect ratios, and orientations.
2. **AI Step Fitting:** When derivation has fewer steps than total palace pins, it must cleanly use the first $N$ pins without generating blank or hallucinated steps.
3. **In-Session Retry Rescheduling:** If a student rates a step "Struggled", that step must immediately re-appear at the end of the current walking session before the session can be marked complete.
4. **"Tomorrow's Exam" Midnight Boundary:** If an exam is scheduled for 9:00 AM tomorrow, review intervals (+30m, 9:00 PM evening, 7:00 AM morning) must not produce negative delays or reverse order if created late at night.
5. **Image File Cleanup on Palace Deletion:** When a palace or image is deleted from Room, the physical JPEG files in internal storage must also be deleted to prevent orphan storage leaks.

---

### Task 1: Database Entities, DAOs & Schema (`features/memorypalace/data/`)

**Files:**
- Create: `app/src/main/java/com/kletaq/app/features/memorypalace/model/MemoryPalaceEntities.kt`
- Create: `app/src/main/java/com/kletaq/app/features/memorypalace/data/PalaceDao.kt`
- Modify: `app/src/main/java/com/kletaq/app/data/local/AppDatabase.kt`
- Test: `app/src/test/java/com/kletaq/app/features/memorypalace/PalaceDaoTest.kt`

**Interfaces:**
- Produces: `MemoryPalaceEntity`, `PalaceImageEntity`, `PalaceLocusEntity`, `PalaceDerivationEntity`, `PalaceStepEntity`, `PalaceWithImagesAndLoci`, `DerivationWithSteps`, `PalaceDao`.

- [x] **Step 1: Write the failing unit test**
Create `PalaceDaoTest.kt` verifying inserting a palace with 2 images and 6 loci pins, inserting a derivation with 6 steps, and querying derivations due for review.

- [x] **Step 2: Run test to verify it fails**
Run: `./gradlew testBenchmarkUnitTest --tests com.kletaq.app.features.memorypalace.PalaceDaoTest`
Expected: FAIL (classes not defined).

- [x] **Step 3: Implement entities and DAO**
Create `MemoryPalaceEntities.kt` and `PalaceDao.kt`. Register entities in `AppDatabase.kt` and bump database version with migration/destructive fallback.

- [x] **Step 4: Run test to verify it passes**
Run: `./gradlew testBenchmarkUnitTest --tests com.kletaq.app.features.memorypalace.PalaceDaoTest`
Expected: PASS.

- [x] **Step 5: Commit**
```bash
git add app/src/main/java/com/kletaq/app/features/memorypalace/model/ app/src/main/java/com/kletaq/app/features/memorypalace/data/PalaceDao.kt app/src/main/java/com/kletaq/app/data/local/AppDatabase.kt app/src/test/java/com/kletaq/app/features/memorypalace/PalaceDaoTest.kt
git commit -m "feat(memorypalace): add Room entities and PalaceDao"
```

---

### Task 2: AI Derivation Unbundling Engine (`features/memorypalace/ai/`)

**Files:**
- Create: `app/src/main/java/com/kletaq/app/features/memorypalace/ai/MemoryPalaceAiHelper.kt`
- Test: `app/src/test/java/com/kletaq/app/features/memorypalace/MemoryPalaceAiHelperTest.kt`

**Interfaces:**
- Consumes: `SecureAiPreferences`, `PalaceLocusEntity`
- Produces: `suspend fun decomposeDerivation(context: Context, uris: List<Uri>, topicTitle: String?, loci: List<PalaceLocusEntity>): Result<List<PalaceStepEntity>>`

- [x] **Step 1: Write the failing unit test**
Create `MemoryPalaceAiHelperTest.kt` verifying JSON parsing of AI response containing LaTeX formulas, Patrick Jane sensory hooks, and VTU exam traps matching provided loci names.

- [x] **Step 2: Run test to verify it fails**
Run: `./gradlew testBenchmarkUnitTest --tests com.kletaq.app.features.memorypalace.MemoryPalaceAiHelperTest`
Expected: FAIL.

- [x] **Step 3: Implement `MemoryPalaceAiHelper.kt`**
Build the multimodal Gemini prompt with world memory champion persona, LaTeX formatting rules, sensory storytelling instructions, and clean JSON parser with fallback extraction.

- [x] **Step 4: Run test to verify it passes**
Run: `./gradlew testBenchmarkUnitTest --tests com.kletaq.app.features.memorypalace.MemoryPalaceAiHelperTest`
Expected: PASS.

- [x] **Step 5: Commit**
```bash
git add app/src/main/java/com/kletaq/app/features/memorypalace/ai/ app/src/test/java/com/kletaq/app/features/memorypalace/MemoryPalaceAiHelperTest.kt
git commit -m "feat(memorypalace): add MemoryPalaceAiHelper for derivation decomposition"
```

---

### Task 3: Repository & Spaced Repetition State Machine (`features/memorypalace/data/`)

**Files:**
- Create: `app/src/main/java/com/kletaq/app/features/memorypalace/data/MemoryPalaceRepository.kt`
- Test: `app/src/test/java/com/kletaq/app/features/memorypalace/MemoryPalaceRepositoryTest.kt`

**Interfaces:**
- Consumes: `PalaceDao`, `MemoryPalaceAiHelper`
- Produces: `savePalaceWithPins`, `createDerivationFromDocument`, `recordStepReviewResult`, `getDueDerivations`

- [x] **Step 1: Write the failing unit test**
Create `MemoryPalaceRepositoryTest.kt` testing:
1. "Tomorrow's Exam" review timestamps (Pass 1 at +30m, Pass 2 at 9 PM, Pass 3 at 7 AM).
2. "Semester Mastery" progression (Day 1 &rarr; Day 3 &rarr; Day 7).
3. Rescheduling when rating is "Struggled".

- [x] **Step 2: Run test to verify it fails**
Run: `./gradlew testBenchmarkUnitTest --tests com.kletaq.app.features.memorypalace.MemoryPalaceRepositoryTest`
Expected: FAIL.

- [x] **Step 3: Implement `MemoryPalaceRepository.kt`**
Implement storage orchestration, bitmap saving to `context.filesDir/palace_images/`, and review schedule calculation logic.

- [x] **Step 4: Run test to verify it passes**
Run: `./gradlew testBenchmarkUnitTest --tests com.kletaq.app.features.memorypalace.MemoryPalaceRepositoryTest`
Expected: PASS.

- [x] **Step 5: Commit**
```bash
git add app/src/main/java/com/kletaq/app/features/memorypalace/data/MemoryPalaceRepository.kt app/src/test/java/com/kletaq/app/features/memorypalace/MemoryPalaceRepositoryTest.kt
git commit -m "feat(memorypalace): add MemoryPalaceRepository and review state machine"
```

---

### Task 4: Interactive Pin-Drop Palace Creation Screen (`features/memorypalace/ui/`)

**Files:**
- Create: `app/src/main/java/com/kletaq/app/features/memorypalace/ui/PalacePinDropScreen.kt`

**Interfaces:**
- Consumes: `MemoryPalaceRepository`
- Produces: Composable `PalacePinDropScreen(onPalaceCreated: (String) -> Unit, onBack: () -> Unit)`

- [x] **Step 1: Implement `PalacePinDropScreen` Composable**
Implement room photo picker/camera capture, interactive Compose canvas tracking `pointerInput` tap events to drop numbered pins with relative coordinates `(xPercent, yPercent)`, inline landmark labeling dialog, and multi-image reel.

- [x] **Step 2: Compile Benchmark variant to verify syntax**
Run: `./gradlew compileBenchmarkKotlin`
Expected: BUILD SUCCESSFUL.

- [x] **Step 3: Commit**
```bash
git add app/src/main/java/com/kletaq/app/features/memorypalace/ui/PalacePinDropScreen.kt
git commit -m "feat(memorypalace): add interactive PalacePinDropScreen"
```

---

### Task 5: Derivation Creation & Upload Screen (`features/memorypalace/ui/`)

**Files:**
- Create: `app/src/main/java/com/kletaq/app/features/memorypalace/ui/CreateDerivationScreen.kt`

**Interfaces:**
- Consumes: `MemoryPalaceRepository`
- Produces: Composable `CreateDerivationScreen(palaceId: String, onDerivationCreated: (String) -> Unit, onBack: () -> Unit)`

- [x] **Step 1: Implement `CreateDerivationScreen` Composable**
Build multi-photo/PDF selector (up to 4 pages), topic title text field, Palace selector, Mode toggle ("Tomorrow's Exam" with time picker vs "Semester Mastery"), and generation progress indicator.

- [x] **Step 2: Compile Benchmark variant to verify syntax**
Run: `./gradlew compileBenchmarkKotlin`
Expected: BUILD SUCCESSFUL.

- [x] **Step 3: Commit**
```bash
git add app/src/main/java/com/kletaq/app/features/memorypalace/ui/CreateDerivationScreen.kt
git commit -m "feat(memorypalace): add CreateDerivationScreen for notes upload"
```

---

### Task 6: Walkthrough & Recall Modes (`features/memorypalace/ui/`)

**Files:**
- Create: `app/src/main/java/com/kletaq/app/features/memorypalace/ui/PalaceWalkScreen.kt`
- Create: `app/src/main/java/com/kletaq/app/features/memorypalace/ui/components/LociPuzzleView.kt`

**Interfaces:**
- Consumes: `MemoryPalaceRepository`
- Produces: Composable `PalaceWalkScreen(derivationId: String, onSessionComplete: () -> Unit, onBack: () -> Unit)`

- [x] **Step 1: Implement `PalaceWalkScreen` with Mental Walk & Reveal**
Display room photo with animated pulsing highlight on active pin, locus name badge, tap-to-reveal card revealing LaTeX formula + Patrick Jane sensory hook + Exam Trap alert, and "Remembered Cleanly" / "Struggled" rating.

- [x] **Step 2: Implement `LociPuzzleView`**
Add secondary mode toggle allowing students to drag shuffled equation steps to their correct numbered loci pins.

- [x] **Step 3: Compile Benchmark variant**
Run: `./gradlew compileBenchmarkKotlin`
Expected: BUILD SUCCESSFUL.

- [x] **Step 4: Commit**
```bash
git add app/src/main/java/com/kletaq/app/features/memorypalace/ui/PalaceWalkScreen.kt app/src/main/java/com/kletaq/app/features/memorypalace/ui/components/
git commit -m "feat(memorypalace): add PalaceWalkScreen and LociPuzzleView"
```

---

### Task 7: Palace Hub Screen & App Navigation Integration

**Files:**
- Create: `app/src/main/java/com/kletaq/app/features/memorypalace/ui/PalaceHubScreen.kt`
- Modify: `app/src/main/java/com/kletaq/app/core/navigation/NavGraph.kt` (or active navigation route)
- Modify: `app/src/main/java/com/kletaq/app/features/home/HomeScreen.kt` (add Memory Palace entry card)

**Interfaces:**
- Links entire feature into the main app experience.

- [x] **Step 1: Implement `PalaceHubScreen`**
Displays "Due for Review" urgency carousel, list of created palaces with thumbnail cards and loci count, and "+ New Palace" action.

- [x] **Step 2: Connect Navigation routes**
Register `memory_palace_hub`, `memory_palace_create`, `memory_palace_derivation_create`, and `memory_palace_walk/{derivationId}` routes in `NavGraph.kt`.

- [x] **Step 3: Verify with Benchmark build & tests**
Run: `./gradlew testBenchmarkUnitTest assembleBenchmark`
Expected: BUILD SUCCESSFUL.

- [x] **Step 4: Commit**
```bash
git add app/src/main/java/com/kletaq/app/features/memorypalace/ui/PalaceHubScreen.kt app/src/main/java/com/kletaq/app/core/navigation/NavGraph.kt app/src/main/java/com/kletaq/app/features/home/HomeScreen.kt
git commit -m "feat(memorypalace): add PalaceHubScreen and integrate into navigation"
```

