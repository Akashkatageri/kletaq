# Academic & Study Tools: Comprehensive Architecture Analysis & Feature Recommendations

**Author**: Academic & Study Tools Explorer (`teamwork_explorer_academic_1`)  
**Date**: 2026-10-31  
**Project**: Kletaq Android Application  
**Target File**: `.agents/teamwork/teamwork_explorer_academic_1/handoff.md`  

---

## 1. Observation

### 1.1 Bundled Academic Assets & Curriculum Schemas
- **Pre-compiled Verified Subject Packs** (`app/src/main/assets/academic/subjects/*.json`):
  - Files observed: `1BCS301.json` (557 KB), `1BCS302.json` (470 KB), `1BCS303.json` (305 KB), `1BCS304.json` (265 KB), `1BCS305.json` (303 KB), `1BCSL306.json` (201 KB), `1BCSL307A.json` (182 KB), and `python/1BPLC105B.json`.
  - Schema (`1BCSL306.json:1-55`):
    - `subjectId`, `subjectName`, `scheme` ("2025_SCHEME").
    - `lessons`: Keyed by topic ID (e.g. `"vtu-ise-s3-1bcsl306-2025-m1-t1"`).
    - Sub-fields per lesson: `title`, `simpleConcept` (intuitive real-world analogy), `technicalConcept` (rigorous CS/math formulation with LaTeX and code definitions), `keyPoints` (ordered bullet list), `codeSnippets` (objects containing `title`, `language`, `code`, `explanation`), `formulas` (`name`, `expression`, `description`), `examModelAnswers` (`question`, `marks`, `stepByStepAnswer`, `markingTip`), `pyqs` (`year`, `marks`, `question`), `quickQuiz` (`question`, `options`, `correctIndex`, `explanation`), and `quickRecall` (one-line memory cue).
- **University Syllabus Hierarchies** (`app/src/main/assets/syllabus/vtu/`):
  - Scheme directories observed: `2022/` and `2025/` across branches `aiml`, `cse`, `ece`, `ise`, covering semesters 1 through 8 (`sem1.json` ... `sem8.json` and `semester1.json` ... `semester8.json`).
  - Schema (`vtu/2025/cse/sem3.json:1-25`):
    - Array of subjects with `id`, `code`, `name`, `semester`, `credits`, and `modules`.
    - Each module contains `id`, `name`, and an array of `topics` (`id`, `name`, `difficulty`, `estimatedTime`).

### 1.2 Data Repositories & Syllabus Resolvers
- **`AcademicContentRepository.kt`** (`app/src/main/java/com/kletaq/app/data/repository/`):
  - `getStudyPack(topicId, topicTitle, subjectCode, subjectName)`: Multi-tier fallback resolver.
    - Tier 1: In-memory cache `subjectPacksCache` (`lines 27, 37`).
    - Tier 2: Case-insensitive match (`line 44`).
    - Tier 3: Regex match by Module & Topic index `m(\\d+)[_-]t(\\d+)` (`lines 50-62`).
    - Tier 4: Substring/title fuzzy search (`lines 65-76`).
    - Tier 5: Baseline generated topic pack (`generateBaselinePack`, `lines 78-80`).
  - Subject code normalizer (`normalizeSubjectCode`, `lines 127-137`): Maps aliases (e.g., `BCS301`, `1BMATCS301`, `BMAT301` -> `1BCS301`; `BCSL305`, `1BCS306` -> `1BCSL306`).
- **`KletaqAcademicRepository.kt`** (`app/src/main/java/com/kletaq/app/data/repository/`):
  - `getSemestersForUser(...)` (`lines 73-243`): Dynamically resolves unlocked vs. locked semesters, merges backlog subjects at the top of the active semester journey, and injects custom imported subjects.
  - **Architectural Debt**: Lines 280 to 6776 (~492 KB) hardcode in-memory `SemesterJourney` objects for Semesters 1 to 4 with hundreds of static `UnitJourney` and `LessonNode` definitions.
- **`CustomSyllabusManager.kt` & `SyllabusAiImporter.kt`** (`com/kletaq/app/core/syllabus/`):
  - `SyllabusAiImporter.importSyllabus(rawText, targetSemester)`: Sends unstructured syllabus text to `gemini-3.1-flash-lite` via `Firebase.ai(backend = GenerativeBackend.googleAI())` (`lines 61-72`). Generates validated 5-module JSON tree.
  - Deterministic fallback parser (`lines 91-93`): Parses text offline using regex patterns (`Module \\d+`, `Unit \\d+`, roman numerals).
  - Persistence: Saves custom syllabus structures to `custom_syllabi.json` in `context.filesDir` (`CustomSyllabusManager.kt:97`).

### 1.3 Practical Lab Defense System
- **`LabDataModels.kt`** (`com/kletaq/app/features/lab/`):
  - `ProgramMasteryStatus` (`lines 5-10`): `UNTOUCHED`, `SKELETON_LEARNED`, `BLIND_VERIFIED`, `COLLEGE_EXECUTED`.
  - `LabProgramItem` (`lines 12-28`): Contains `programNumber`, `title`, `shortTitle`, `language`, `fullCode`, `blueprintBlock`, `coreFunctionBlock`, `guardBlock`, `driverBlock`, `algorithmSteps`, `vivaQuestions` (`List<Pair<String, String>>`), `crashTraps` (`List<String>`), and `masteryStatus`.
  - `ExamSimulationChits` (`lines 30-35`): Represents the two randomly drawn programs for university lab exams.
- **`LabDefenseRepository.kt`**:
  - Auto-detection (`isSubjectLab`, `lines 32-46`): Identifies lab courses by keywords (`lab`, `laboratory`, `practical`, or `.*\\d+.*l.*` regex) with user override persistence (`KEY_IS_LAB_PREFIX`).
  - Readiness Metric (`calculateReadiness`, `lines 101-108`):
    $$\text{Completion Ratio} = \frac{\text{Verified Count}}{\text{Total Programs}}$$
    $$\text{Pass Probability} = (\text{Completion Ratio})^2$$
    Squaring reflects the joint probability of drawing 2 chits in the university exam and knowing *both*.
  - Rule 7 Protection (`lines 170-179`): Automatically detects and deletes legacy placeholder text (e.g. `"Initialize environment and variables"`, `"Execute the primary logic computation"`).
- **`DrillDialogs.kt`**:
  - Drill 1: `DrillStepOrderDialog` (`lines 35-198`): Interactive swap drill for scrambled algorithm steps.
  - Drill 2: `DrillCoreFunctionDialog` (`lines 205-379`): Blackout code recall drill hiding the critical 20-30 line algorithmic core function.
  - Drill 3: `DrillPenAndPaperDialog` (`lines 385-619`): 15-minute blind-write timer with Crash-Trap Audit Checklist (segmentation fault traps, pointer boundary checks) and full master code reference.
  - Drill 4: `VivaVoceDefenseDialog` (`lines 626-750`): Flip flashcards testing examiner Viva Voce questions and scoring answers.
  - Final Exam Simulator: `FinalExamSimulatorDialog` (`lines 757-920`): Draws 2 random chits (Part A & Part B), runs a 3-hour timer, and shows official university evaluation rubric (write-up 20%, execution 40%, viva 20%, results 20%).
- **`LabManualAiParser.kt`**:
  - `parseLabManualPdf(pdfBytes)` (`lines 87-134`): Streams PDF bytes directly via `InlineDataPart(pdfBytes, "application/pdf")` into `gemini-3.1-flash-lite`. Deconstructs programs into active recall JSON structure.

### 1.4 Handwritten Notes Ingestion, OCR & Note-Grounded Flashcards
- **`CustomNotesManager.kt`**:
  - Native rasterization of user PDFs using Android's `PdfRenderer` (`lines 8, 23`).
  - Smart JPEG compression: Max dimension 1080px, 60% quality, capping pages under 110 KB (`lines 32-35`).
  - Offline-first cache in `context.filesDir/custom_notes/<subject>/m<module>/` and cloud sync via Cloudflare R2 (`CloudflareR2Uploader.kt`).
- **`NotesTextExtractor.kt`**:
  - On-device ML Kit OCR (`com.google.mlkit.vision.text.TextRecognition`, `lines 8-9`).
  - Extracts text page-by-page, caching results in `notes_manifest.json` keyed by file fingerprint (`"${file.length()}_${file.lastModified()}"`).
- **`TopicFlashcardRepository.kt`**:
  - Generates topic-scoped flashcards directly grounded in the student's handwritten notes text (`lines 73-140`) using `gemini-3.1-flash-lite`.
  - Persists to Firestore (`users/{uid}/topic_flashcards/{topicId}`) with fast sub-millisecond memory caching (`ConcurrentHashMap`).

### 1.5 Exam Sprint & Spaced Repetition Engine
- **`ExamSprintModels.kt` & `ExamSprintAiHelper.kt`**:
  - `ExamSprint` model tracking days remaining, mastered questions count, mock tests taken, and best score.
  - `ExamSprintAiHelper.parseUploadedAssignment`: Multimodal extraction of assignments from PDFs/images or text using `gemini-3.1-flash-lite`.
  - Rule 7 compliant extraction from syllabus assets (`extractFromSyllabus`, `lines 173-200`).
- **`SpacedRepetitionEngine.kt`**:
  - Data-driven SM-2 algorithm consuming parameters from `AdaptiveEngineConfig`.
  - Memory health decay curves based on Ebbinghaus retention:
    $$R = e^{-0.70 \cdot (\text{elapsedDays} / \text{intervalDays})}$$
  - Interval Fuzzing (`applyIntervalFuzz`, `lines 166-172`): Injects deterministic $\pm 5\%$ dispersion into intervals $> 7$ days to prevent review pile-ups.
  - Exam-Date Interval Tapering (`calculateTaperedInterval`, `lines 178-188`): Clamps intervals as exam date approaches, ensuring a final review pass 24–48 hours prior.
  - Latency Awareness (`lines 198-204`): Hesitant recall ($>8$s) discounts ease factor by $-0.05$.

---

## 2. Logic Chain

1. **Academic Content Richness vs. Duplication**:
   - Observation 1.1 reveals high-quality syllabus JSON assets in `assets/academic/subjects/` with analogies, code snippets, LaTeX formulas, PYQs, and rubrics.
   - Observation 1.2 reveals `KletaqAcademicRepository.kt` has 6,776 lines of Kotlin code hardcoding CSE Semesters 1-4.
   - *Inference*: The hardcoded Kotlin repository creates architectural duplication, increases APK binary size, and violates single-source-of-truth principles. Migrating `KletaqAcademicRepository` to dynamically load `assets/syllabus/` JSON files will make syllabus updates instant, reduce APK size, and eliminate maintenance overhead without altering any UI behavior.

2. **Lab Defense System Completeness & Missing Runtime Execution**:
   - Observation 1.3 shows that the Lab Defense system contains the complete mental pipeline: algorithm ordering, blackout recall, 15m blind writing, crash traps checklist, viva flashcards, and 2-chit exam simulation.
   - *Inference*: Despite the depth of mental drills, students cannot execute their code inside the app to verify test cases or debug runtime output. A lightweight, client-side or sandboxed dry-runner / test-case validator would bridge the final gap between theoretical recall and physical lab execution.

3. **Handwritten Notes Integration is a Major Strategic Moat**:
   - Observation 1.4 confirms on-device ML Kit OCR extracting handwritten text and feeding it into `gemini-3.1-flash-lite` to generate note-grounded flashcards and AI study assistance.
   - *Inference*: This is one of Kletaq's strongest unique value propositions. However, notes text is currently used only for flashcards and AI doubt context. It is not indexed for global search or used to generate cross-module revision summaries.

4. **Exam Preparation Synergy**:
   - Observations 1.3, 1.4, and 1.5 show three distinct exam tools: `QuestionPaperScreen` (100-mark SEE model papers), `ExamSprint` (targeted internals/midterms), and `FinalExamSimulatorDialog` (lab practical exams).
   - *Inference*: The exam prep tools operate independently. There is no unified "Exam Readiness Dashboard" predicting student SGPA, passing probability, and remaining high-yield topics across theory, internals, and labs.

---

## 3. Caveats

- **Network Constraints**: Multimodal PDF parsing (`LabManualAiParser`, `SyllabusAiImporter`, `ExamSprintAiHelper`) requires active internet connectivity (or custom API keys configured in `SecureAiPreferences`). Offline fallback relies on deterministic regex parsing.
- **Device Memory on Low-End Hardware**: Rendering multiple PDF pages in high resolution (2048px) via `PdfRenderer` in `CustomModelPaperManager` or `CustomNotesManager` may cause memory spikes on devices with $\le 3$GB RAM if garbage collection is delayed.
- **Rule 7 Compliance**: Any new features or curriculum expansions must strictly load verified JSON syllabus schemas or utilize multimodal AI extraction. Synthetic dummy data or generic mock steps must never be added.

---

## 4. Conclusion & Feature Recommendations

Based on our comprehensive exploration, we recommend the following 7 high-impact academic feature updates for Kletaq, prioritized by learning impact and architectural alignment.

### Priority Matrix

| # | Feature Recommendation | User Value | Learning Impact | Effort | Complexity | Rule 7 Adherence |
|---|------------------------|------------|-----------------|--------|------------|------------------|
| **F1** | **Interactive Lab Code Dry-Runner & I/O Test Validator** | Very High | Transformative | Medium | Medium | Strict (Uses verified lab code) |
| **F2** | **Spoken Viva Voce Examiner (Audio AI Voice Defense)** | Very High | Transformative | Medium | Medium | Strict (Grounded in syllabus viva) |
| **F3** | **5-Year PYQ Trend Heatmap & High-Yield Topic Ranker** | High | High | Low | Low | Strict (Parsed from verified PYQ assets) |
| **F4** | **Dynamic JSON Syllabus Engine (Refactor KletaqAcademicRepository)** | High | High | Medium | Medium | Strict (Eliminates hardcoded Kotlin) |
| **F5** | **1-Page Formula & Derivation Cheat Sheet PDF Exporter** | High | High | Low | Low | Strict (Extracted from TopicStudyPack) |
| **F6** | **CIE + SEE Weighted Grade & SGPA Predictor** | High | High | Low | Low | Strict (Calculated from VTU formulas) |
| **F7** | **Semantic Note Search & Concept Cross-Referencing** | High | Medium | Medium | Medium | Strict (Grounded in student notes OCR) |

---

### Detailed Feature Specifications

#### F1. Interactive Lab Code Dry-Runner & I/O Test-Case Validator
- **Description**: Add an interactive code runner to `LabDefenseScreen` allowing students to test-run their C, Java, or Python lab programs with sample input test cases directly on Android.
- **Implementation**:
  - Utilize client-side WebAssembly runner (e.g. `quickjs-android` for JavaScript/Python or a sandboxed runner) or cloud API fallback via Judge0/Piston.
  - Include 3 pre-configured official university test cases per program (e.g., Stack: push 3 items, test overflow, pop 1 item, test underflow).
  - Show side-by-side Expected Output vs. Actual Output.
- **User Value**: Bridges the critical gap between blind-writing code on paper and seeing it execute successfully in college lab terminals. Eliminates the panic of compilation errors on exam day.
- **Effort & Complexity**: **Medium Effort** (3-4 days), **Medium Complexity**. Fits cleanly into `DrillDialogs.kt` as "Drill 2.5: Test Case Execution".

#### F2. Spoken Viva Voce Examiner (Audio AI Voice Defense)
- **Description**: Upgrade the flashcard-based `VivaVoceDefenseDialog` into an interactive spoken examiner mode.
- **Implementation**:
  - Android's native `TextToSpeech` (TTS) speaks the examiner's viva question in an authoritative, clear voice.
  - The student taps a microphone button and answers orally using Android's native `SpeechRecognizer` (STT).
  - `gemini-3.1-flash-lite` evaluates the spoken answer against the official syllabus marking rubric, returning:
    1. Score: 0 to 5 Marks
    2. Missing key technical terms (e.g., "forgot to mention $O(1)$ auxiliary memory")
    3. Examiner follow-up question.
- **User Value**: Lab external vivas are notoriously intimidating because students have never practiced verbalizing technical explanations. Spoken practice builds confidence and eliminates hesitation.
- **Effort & Complexity**: **Medium Effort** (2-3 days), **Medium Complexity**. Leverages existing `SpeechRecognizer` APIs and `UnifiedAiClient`.

#### F3. 5-Year PYQ Trend Heatmap & High-Yield Topic Ranker
- **Description**: Add a visual frequency badge and module heatmap showing how many times each topic has appeared in official university exams over the last 5 years.
- **Implementation**:
  - `TopicStudyPack` already contains `pyqs: List<PyqItem>` with years (e.g. "VTU Lab Exam 2024", "VTU Jan 2023").
  - Aggregate PYQ occurrences across subjects to compute a "Exam Probability Index" (e.g., 🔥 85% Exam Likelihood).
  - Display a flame badge on high-yield topic nodes in `JourneyScreen` (Metro roadmap).
- **User Value**: Engineering students study with limited time before exams. Knowing exactly which 3 topics in a module guarantee 14 out of 20 marks allows optimal study prioritization.
- **Effort & Complexity**: **Low Effort** (1 day), **Low Complexity**. 100% offline, data already exists in JSON assets.

#### F4. Dynamic JSON Syllabus Engine (Refactor `KletaqAcademicRepository`)
- **Description**: Refactor `KletaqAcademicRepository.kt` to load semester journeys dynamically from `app/src/main/assets/syllabus/` JSON files, replacing lines 280-6776 of static Kotlin objects.
- **Implementation**:
  - Implement a JSON-to-`SemesterJourney` deserializer with fast in-memory caching.
  - Keep the existing `LessonStatusEvaluator`, backlog subject positioning, and custom imported subject merging intact.
  - Delete ~6,000 lines of duplicate Kotlin code from `KletaqAcademicRepository.kt`.
- **User Value**: Reduces APK binary download size by ~3MB, ensures changes to syllabus JSON files instantly reflect across the entire app without code recompilation, and makes supporting new branches trivial.
- **Effort & Complexity**: **Medium Effort** (2 days), **Low Complexity**. High architectural benefit.

#### F5. 1-Page Formula & Derivation Cheat Sheet PDF Exporter
- **Description**: Add a 1-tap "Export Revision Cheat Sheet" button in `LessonStudyScreen` and `JourneyScreen` that compiles all formulas, diagrams, and derivation checkpoints for a subject into a clean, printable 1-to-2 page PDF.
- **Implementation**:
  - Query `AcademicContentRepository.getAllFormulasForSubject`.
  - Use Android's `PdfDocument` canvas or HTML-to-PDF rendering to lay out equations in a 2-column compact grid.
  - Save to device Downloads folder and open via Android system share sheet.
- **User Value**: Students love physical paper cheat sheets to glance at 15 minutes before walking into the examination hall when phones are prohibited.
- **Effort & Complexity**: **Low Effort** (1-2 days), **Low Complexity**.

#### F6. CIE + SEE Weighted Grade & SGPA Predictor
- **Description**: Add an academic performance calculator inside `JourneyScreen` and `ProfileScreen`.
- **Implementation**:
  - Input: Internal assessment marks (Test 1, Test 2, Assignments, Lab Internals = CIE 50 Marks).
  - Automatically calculates minimum required Semester End Exam (SEE) marks to secure passing (minimum 18/50 in SEE, 40/100 aggregate) or target grades (S Grade: 90+, A Grade: 80+).
  - Projects semester SGPA based on subject credit weightages (`credits` field in syllabus JSON).
- **User Value**: Gives students total clarity on their exact exam targets, relieving anxiety and replacing guesswork with mathematical targets.
- **Effort & Complexity**: **Low Effort** (1 day), **Low Complexity**. Pure math calculation based on official VTU grading guidelines.

#### F7. Semantic Note Search & Concept Cross-Referencing
- **Description**: Allow students to search across all their uploaded handwritten notes using full-text OCR indexing.
- **Implementation**:
  - Index `notes_manifest.json` OCR text across all modules in `NotesTextExtractor`.
  - In `SearchScreen.kt`, add a "My Notes" search tab alongside the existing topic search.
  - Tapping a search result jumps directly to the exact page in `HandwrittenNotesViewer` with the query term highlighted.
- **User Value**: Turns messy 100-page notebook PDFs into an instantly searchable personal engineering textbook.
- **Effort & Complexity**: **Medium Effort** (2 days), **Medium Complexity**.

---

## 5. Verification Method

### How to Independently Verify These Findings:
1. **Asset Inspection**:
   - Inspect `app/src/main/assets/academic/subjects/1BCSL306.json` lines 1-100 to verify the schema (`simpleConcept`, `codeSnippets`, `examModelAnswers`, `quickQuiz`).
   - Inspect `app/src/main/assets/syllabus/vtu/2025/cse/sem3.json` to verify the module/topic hierarchy.
2. **Lab Defense Architecture**:
   - Inspect `app/src/main/java/com/kletaq/app/features/lab/LabDefenseRepository.kt` lines 32-46 for auto-detection heuristics and lines 101-108 for the squared readiness formula.
   - Inspect `app/src/main/java/com/kletaq/app/features/lab/LabManualAiParser.kt` lines 87-134 for multimodal PDF byte streaming to Gemini.
3. **Handwritten Notes & Spaced Repetition**:
   - Inspect `app/src/main/java/com/kletaq/app/core/notes/NotesTextExtractor.kt` lines 8-30 for ML Kit on-device OCR.
   - Inspect `app/src/main/java/com/kletaq/app/core/utils/SpacedRepetitionEngine.kt` lines 166-188 for interval fuzzing and exam-date interval tapering.
4. **Codebase Size Verification**:
   - Run:
     ```powershell
     (Get-Content "app/src/main/java/com/kletaq/app/data/repository/KletaqAcademicRepository.kt").Count
     ```
     Verifies 6,776 lines of code (~492 KB) containing hardcoded Kotlin semester definitions.
5. **Compilation Verification**:
   - Run the mandated Benchmark build check:
     ```powershell
     ./gradlew assembleBenchmarkDebug
     ```
   - Verifies the codebase builds cleanly without any errors.

### Invalidation Conditions:
- If `assets/academic/subjects/` files are altered or removed, the asset-based schema observations would need to be updated.
- If Android `PdfRenderer` behavior changes in future API levels, PDF downsampling logic in `CustomNotesManager` should be re-tested.
