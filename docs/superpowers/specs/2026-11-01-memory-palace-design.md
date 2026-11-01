# Memory Palace (Method of Loci) — Feature Design Document

**Date:** 2026-11-01  
**Status:** Approved by Builder Review / Ready for User Sign-off  
**Target:** Android App (`com.kletaq.app`)  
**Feature Path:** `app/src/main/java/com/kletaq/app/features/memorypalace/`

---

## 1. Intent & The "Why"
Theory subjects can be effectively revised using standard flashcards and mock exams. However, **mathematical derivations** (e.g. Poisson distribution, Cauchy-Riemann equations, Taylor series, Runge-Kutta, Fourier transforms) and **multi-step programming algorithms** (e.g. Dijkstra, AVL rotations, dynamic programming unrolling) require students to recall an exact, uninterrupted sequence of logical transformations under intense exam stress.

When students cram derivations, they frequently freeze at a specific algebraic transition or omit intermediate normalization conditions, leading to heavy mark deductions. 

The **Method of Loci (Memory Palace)**—popularized by mentalists like Patrick Jane and memory masters—is the single most effective cognitive technique for sequential memory. By anchoring each mathematical transformation to an ordered, familiar physical spot ("locus") in the student's real room (e.g., Doorway &rarr; Study Table &rarr; Bookshelf &rarr; Bed) and associating it with an exaggerated, surreal sensory story, students can effortlessly walk through their physical room in their mind during an exam to retrieve every step in exact order.

---

## 2. Goals, Non-Goals & Anti-Goals

### Goals
1. **Real-Room Photo Loci Capture (Expandable 4–12 Spots):**
   * Students snap sequential photos of landmarks in their own familiar room/hostel (e.g., 1. Door, 2. Shoe Rack, 3. Study Table, 4. Laptop, 5. Bed, 6. Wardrobe, 7. Window, up to 12 spots).
   * Palaces are flexible: a derivation uses the first $N$ spots needed. Extra spots can be snapped anytime to accommodate longer, complex proofs.
2. **AI Derivation & Algorithm Decomposition:**
   * User provides handwritten notes / textbook photos or PDF (1–4 pages) with an optional topic title (e.g., "Poisson Distribution Derivation").
   * AI decomposes the proof into $N$ exact sequential steps (up to the palace's loci count).
   * For each step, AI generates:
     1. Clean LaTeX mathematical equation or monospace code snippet.
     2. An exaggerated Patrick Jane sensory mnemonic story interacting directly with that physical room object.
     3. An **"Exam Trap"** alert (highlighting common mistakes examiners deduct marks for).
3. **Walkthrough & Recall Practice Modes:**
   * **Primary Mode (Mental Walk & Reveal):** Fast, zero typing friction. The student sees the photo of their locus, visualizes the story, works out the step mentally or on rough paper, and taps to reveal the answer.
   * **Sequence Challenge (Loci Drag & Match Puzzle):** Shuffled derivation steps that must be dragged to the correct physical room loci in order.
4. **Adaptive Spaced Repetition (Dual Mode):**
   * **"Tomorrow's Exam" Sprint:** User selects exam time (default 9:00 AM next day). Automatically schedules 3 critical recall passes:
     - Review 1: 30 minutes after creation (locks initial short-term memory).
     - Review 2: Evening / before sleep (consolidates long-term memory).
     - Review 3: Morning-of exam (2 hours before exam, primes working memory).
   * **"Semester Mastery" Mode:** Standard multi-day intervals (Day 1, Day 3, Day 7, Day 14, Day 30).
5. **In-Session Retry & "Struggled" Rescheduling:**
   * If a student rates a step as "Struggled", that card is automatically re-queued at the end of the current walking session for an immediate retry.
   * For "Tomorrow's Exam" sprints, struggling schedules a fast 20-minute re-test before advancing.
6. **Reusable Palaces:** A student creates a palace (e.g., "Hostel Room 204") once and can place multiple derivations or code algorithms inside it across the semester.

### Non-Goals
* **No 3D / ARCore Spatial Scanning:** No heavy 3D meshes or AR tracking that drains battery or fails on mid-tier Android devices. Native 2D camera photos provide 100% of the cognitive benefit without lag.
* **No Cloud Video Storage:** Palace photos are stored locally on the device (`context.filesDir/palace_images/`) to protect student privacy and save bandwidth.
* **No Synthetic / Hardcoded Derivations:** Derivations must strictly originate from user-uploaded notes/PDFs or official syllabus content (Rule 7 Zero Hardcoding).

### Anti-Goals
* **Never complicate the review flow with typing:** Math formulas are tedious to type on mobile keyboards. The recall mode must be mental/rough-paper with quick tap-to-reveal.
* **Never shuffle steps during the core walkthrough:** The entire power of the Memory Palace lies in the unshakeable sequential order of loci.

---

## 3. Architectural Constraints & Platform Rules
1. **Build Variant:** Must strictly build and run under the `benchmark` variant (`compileBenchmarkKotlin`, `installBenchmark`). Never use debug or release.
2. **Offline-First Storage:** Palace loci, derivations, and review logs are stored in Room (`AppDatabase.kt`).
3. **AI Vision & Gemini 2.0 Flash:** Parsing uses existing `SecureAiPreferences` and `ApiKeyManager` with Gemini 2.0 Flash and ML Kit OCR.
4. **Image Compression:** Real photos are scaled down to standard mobile display resolution (max 1080px wide, JPEG 80%) before saving to internal storage to prevent OOM errors and bloated app storage.

---

## 4. Architectural Approaches Considered

### Approach 1: Dedicated Feature Module (`features/memorypalace`) — *Selected*
* **Architecture:** Separate database tables (`palaces`, `palace_loci`, `palace_derivations`, `palace_steps`, `palace_reviews`), dedicated AI helper (`MemoryPalaceAiHelper`), and clean Compose screens (`PalaceHubScreen`, `PalaceLociCameraScreen`, `PalaceDerivationCreateScreen`, `PalaceWalkScreen`).
* **Why Selected:** Completely decouples physical spaces from academic subjects. A student can create their room once and attach Math, Physics, or OS derivations to it. Zero risk of regressions in existing Exam Sprint or Notes code.

### Approach 2: Embedded Inside Exam Sprint
* **Rejected:** Tightly couples palace photos to a single exam sprint. Would force students to retake room photos for every subject.

### Approach 3: Piggybacking on Standard Flashcards
* **Rejected:** Standard flashcards are unordered, atomic Q&A cards. Derivations demand strict sequential spatial anchoring.

---

## 5. Detailed Component Design

### 5.1 Database Entities (`data/local/`)
```kotlin
@Entity(tableName = "memory_palaces")
data class MemoryPalaceEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String, // e.g. "My Hostel Room"
    val description: String? = null,
    val totalLociCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "palace_loci",
    foreignKeys = [ForeignKey(
        entity = MemoryPalaceEntity::class,
        parentColumns = ["id"],
        childColumns = ["palaceId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("palaceId")]
)
data class PalaceLocusEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val palaceId: String,
    val orderIndex: Int, // 1 to 12
    val name: String, // e.g. "Study Table Lamp"
    val photoPath: String // Local absolute path to saved image
)

@Entity(
    tableName = "palace_derivations",
    foreignKeys = [ForeignKey(
        entity = MemoryPalaceEntity::class,
        parentColumns = ["id"],
        childColumns = ["palaceId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("palaceId")]
)
data class PalaceDerivationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val palaceId: String,
    val title: String, // e.g. "Poisson Distribution Derivation"
    val subjectId: String? = null,
    val mode: String, // "EXAM_SPRINT" (rapid) or "SEMESTER" (standard)
    val examTimestamp: Long? = null, // Set if EXAM_SPRINT
    val nextReviewTime: Long,
    val reviewStage: Int = 0, // 0: Review 1, 1: Review 2, 2: Review 3, 3+: Mastered
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "palace_steps",
    foreignKeys = [ForeignKey(
        entity = PalaceDerivationEntity::class,
        parentColumns = ["id"],
        childColumns = ["derivationId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("derivationId")]
)
data class PalaceStepEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val derivationId: String,
    val stepNumber: Int,
    val locusOrderIndex: Int, // 1-based index matching PalaceLocusEntity.orderIndex
    val stepTitle: String, // e.g. "Apply Limits as n -> infinity"
    val formulaLatex: String, // Formatted math / code line
    val sensoryStory: String, // Patrick Jane vivid sensory mnemonic
    val examTrap: String // Key examiner deduction warning
)
```

### 5.2 AI Engine (`features/memorypalace/ai/MemoryPalaceAiHelper.kt`)
* **Input:** Bitmaps from PDF or camera photos of handwritten/textbook notes (max 4 pages) + optional topic title + list of locus names from the palace.
* **Prompt Contract:**
  * System role: World memory champion and senior VTU engineering professor.
  * Task: Unbundle derivation into sequential steps that map to the student's actual physical loci (e.g. 1. Door, 2. Desk Lamp, 3. Bookshelf...).
  * For each step:
    1. Clean LaTeX mathematical expression or precise code line.
    2. Patrick Jane mentalist story: uses multi-sensory anchors (tactile, smell, vivid contrasting colors, absurd action, sound) directly interacting with that physical room landmark.
    3. Exam trap: the exact blunder examiners look for to cut marks.
* **Output:** Strongly-typed JSON array parsed safely into `PalaceStepEntity`.

### 5.3 UI Flow & Screens (`features/memorypalace/ui/`)
1. **`PalaceHubScreen`**:
   - Section 1: "Derivations Due for Review" (highlighting urgent "Tomorrow's Exam" sprints).
   - Section 2: "Your Memory Palaces" with Loci count and quick "+ New Palace" button.
2. **`PalaceCameraCaptureScreen`**:
   - Guided camera capture for 4 to 12 loci (e.g. *"Spot #1: Door"*, *"Spot #2: Desk"*, etc.).
   - Visual reel showing captured spots with "+ Add Another Locus" button.
3. **`CreateDerivationScreen`**:
   - Document picker (camera notes / PDF up to 4 pages).
   - Optional topic title input (e.g. "Cauchy-Riemann Derivation").
   - Target Palace selector.
   - Mode Toggle: "Tomorrow's Exam" (with exam date/time picker, defaults to tomorrow 9:00 AM) vs "Semester Mastery".
   - Generate button with progress indicator.
4. **`PalaceWalkScreen`**:
   - **Mode A (Mental Walk & Reveal):** 
     - Full-screen locus photo with `#Locus [Name]` header badge.
     - Prompt: *"Stand at your [Desk Lamp]. Recall the story and the equation."*
     - Button: *"Reveal Step"*.
     - Expands to show Formula + Patrick Jane story + Exam Trap alert.
     - Rating buttons: "Remembered Cleanly" / "Struggled" &rarr; updates spaced repetition schedule. Any struggled step is immediately re-queued at the end of the walkthrough.
   - **Mode B (Loci Puzzle):** 
     - Drag-and-drop equation cards into the ordered room spots to verify flawless sequence memory.

---

## 6. What's Left to the Builder
* Smooth animated transitions (horizontal slide + subtle zoom) when walking between loci.
* LaTeX formatting composable for crisp mathematical rendering.
* Empty state visual indicators and celebratory screen upon completing all review passes.
