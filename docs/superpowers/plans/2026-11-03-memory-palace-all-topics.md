# Memory Palace All-Topics Generalization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generalize Memory Palace (Method of Loci) to support all subject topics, concepts, algorithms, definitions, and theory notes rather than being restricted or framed strictly around mathematical derivations.

**Architecture:** Maintain existing data models (`PalaceDerivation`, `PalaceStep`) and JSON persistence keys for 100% backward compatibility, while upgrading the multimodal AI unbundler prompt and all user-facing screens (`PalaceHubScreen`, `CreateDerivationScreen`, `PalaceWalkScreen`, `DerivationPreviewSheet`) to support general academic topics seamlessly.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, UnifiedAiClient (Gemini 2.5 Flash / Firebase AI), Android Benchmark build variant.

**Spec:** Minimalist Hub-Only Generalization chosen by user — all topics and concepts supported in Memory Palace Hub, prompt generalized, and UI copy generalized without breaking existing data structures.

## Global Constraints
- Preserve backward compatibility with existing saved user palaces, derivations, and reviews.
- Strict Zero-Hardcoding policy: No fake steps or mock data.
- Benchmark build variant only (`testBenchmarkUnitTest`, `installBenchmark`).
- Clean, concise changes following the Ponytail minimal code ladder.

## Review Focus
1. Non-mathematical topics (e.g. Operating Systems Deadlocks, French Revolution, Medical Anatomy): AI unbundler must produce clean concept milestone points even when formulas are absent.
2. Formatted output compatibility: `formulaLatex` field in `PalaceStep` must gracefully store core definitions, code snippets, or key milestone statements when LaTeX math is not present.
3. Empty state & creation: New users without derivations should see "Anchor Topic / Concept" with intuitive multi-subject placeholders.
4. Spaced repetition scheduling: Next review calculations remain intact for any topic type.
5. Error handling: Clear, helpful error messages if document extraction fails without assuming the document was a math derivation.

---

### Task 1: Generalize AI Prompt and Extraction in `MemoryPalaceAiHelper.kt`

**Files:**
- Modify: `app/src/main/java/com/kletaq/app/features/memorypalace/ai/MemoryPalaceAiHelper.kt`

- [ ] **Step 1: Update AI Prompt for all topic types**
In `buildPrompt(topicTitle: String?, loci: List<PalaceLocus>)`:
- Frame the persona as a World Memory Champion using Method of Loci for any academic subject (engineering, science, humanities, medicine, law, business, math, algorithms).
- Clarify that for non-math topics, `formulaLatex` contains the core definition, key takeaway, primary sentence, or code snippet without requiring LaTeX dollar signs.
- Ensure `stepTitle` provides a concise milestone name for each concept point.

- [ ] **Step 2: Generalize failure and log messages**
- Update error messages from "AI could not extract legible derivation steps" to "AI could not extract legible topic steps from the document. Please ensure the pages are clear and well-lit, or retry when connected."

---

### Task 2: Generalize UI and Copy in `PalaceHubScreen.kt`

**Files:**
- Modify: `app/src/main/java/com/kletaq/app/features/memorypalace/ui/PalaceHubScreen.kt`

- [ ] **Step 1: Update FAB and action copy**
- Change Floating Action Button text from "Anchor Derivation" to "Anchor Topic".
- Change empty state title from "No Derivations Anchored" to "No Topics Anchored".
- Change empty state description to "Anchor subject topics, algorithms, derivations, or complex concepts to your room landmarks."
- Change empty state button to "Anchor a Topic".

- [ ] **Step 2: Update item card dialogs and labels**
- Update Delete confirmation dialog from "Delete Derivation" to "Delete Topic: Remove this topic from your palace?".
- Update header title for active topics list to "Anchored Topics & Concepts".

---

### Task 3: Generalize `CreateDerivationScreen.kt` for Any Topic

**Files:**
- Modify: `app/src/main/java/com/kletaq/app/features/memorypalace/ui/CreateDerivationScreen.kt`

- [ ] **Step 1: Generalize input placeholders and descriptions**
- Update topic title label to "Topic / Concept Title".
- Update placeholder to "e.g. Operating System Deadlocks, Poisson Derivation, Photosynthesis".
- Update document upload description to "Handwritten notes, textbook pages, slides, or summary notes (Up to 4)".

- [ ] **Step 2: Generalize progress and toast notifications**
- Update progress text: "Decomposing into $count loci milestones with vivid spatial mnemonics...".
- Update confirmation toast: "Anchored $count steps to your Palace!".

---

### Task 4: Generalize Walk & Preview Screens

**Files:**
- Modify: `app/src/main/java/com/kletaq/app/features/memorypalace/ui/PalaceWalkScreen.kt`
- Modify: `app/src/main/java/com/kletaq/app/features/memorypalace/ui/components/DerivationPreviewSheet.kt`

- [ ] **Step 1: Update `DerivationPreviewSheet.kt`**
- Change preview sheet title to "Review Topic Milestones".
- Change confirmation button text to "Confirm & Anchor Topic".

- [ ] **Step 2: Update `PalaceWalkScreen.kt`**
- Change loading text from "Loading palace derivation..." to "Loading palace topic...".
- Change walk completion dialog title from "Derivation Walk Complete" to "Topic Walk Complete".
- Change completion message to "All topic steps were reviewed and anchored to your physical loci."

---

### Task 5: Benchmark Verification & Device Testing

**Files:**
- Verify: All touched files

- [ ] **Step 1: Run unit tests**
Run `./gradlew testBenchmarkUnitTest` and confirm all tests pass.

- [ ] **Step 2: Build & install Benchmark APK**
Run `./gradlew installBenchmark` to deploy to connected emulator.

- [ ] **Step 3: Verify on device**
Verify the Memory Palace Hub, Topic Creation form, and Walk screens on the emulator.

