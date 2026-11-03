# Global Document Ingestion & Text Extraction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace blind ML Kit OCR with smart routing, 2048px `RENDER_MODE_FOR_PRINT` rasterization, standardized LaTeX-enabled Gemini prompts, and prune invalid fallbacks across Notes, Exam Sprint, and Memory Palace.

**Architecture:** 
1. Centralize multimodal document parsing in `UnifiedAiClient.kt` (`parseDocumentWithGemini`) with LaTeX formula transcription and structured JSON schemas.
2. Upgrade native PDF rasterization in `CustomNotesManager`, `ExamSprintAiHelper`, `MemoryPalaceAiHelper`, and `CustomModelPaperManager` to 2048px max dimension and `PdfRenderer.Page.RENDER_MODE_FOR_PRINT` for crisp formula strokes.
3. Add a lightweight math/garbage detector (`isGarbledOrMathText`) in `NotesTextExtractor` and `ExamSprintAiHelper` that keeps typed English on free ML Kit but routes math/handwriting to Gemini.
4. Eliminate the blind ML Kit fallback in `MemoryPalaceAiHelper` so students never receive garbled nonsense derivation steps during offline or rate-limit events.

**Tech Stack:** Kotlin, Android `PdfRenderer`, Google ML Kit Vision Text Recognition, Firebase AI / Google Gemini REST API, OkHttp, Kotlinx Coroutines, JUnit4.

**Spec:** User architecture guidance on fixing global document ingestion, resolution blurring, and routing across Kletaq.

## Global Constraints

- **Variant**: Must build exclusively under the `benchmark` variant (`./gradlew testBenchmarkUnitTest` or `./gradlew assembleBenchmark`).
- **No Hardcoding**: No mock, synthetic, or dummy questions/derivations.
- **Offline Safety**: Local offline files and cached fingerprints in `notes_manifest.json` must remain backward-compatible.
- **Lazy Engineering**: Smallest working changes, zero extraneous wrapper classes or unused libraries.

## Review Focus

1. **Memory / OOM on Multi-Page PDFs**: 2048px bitmaps must be recycled promptly in `finally` blocks after processing.
2. **Backward-Compatibility with Cached Manifests**: Existing `notes_manifest.json` files on user devices must continue loading without deserialization crashes.
3. **White Background Invariant**: Bitmaps rendered from `PdfRenderer` must strictly retain `Color.WHITE` background erasure before passing to ML Kit or Gemini.
4. **LaTeX Formula Escaping**: JSON string escapes (`\\frac`, `\\int`) must parse cleanly without crashing the JSON parser.
5. **Network / Offline Gracefulness**: When offline, the app must never replace math notes with ML Kit noise, but cleanly notify the student or queue the operation.

---

### Task 1: Centralized Gemini Document Parser in `UnifiedAiClient.kt`

**Files:**
- Modify: `app/src/main/java/com/kletaq/app/core/ai/UnifiedAiClient.kt`
- Create: `app/src/test/java/com/kletaq/app/core/ai/DocumentIngestionRoutingTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  data class ParsedDocumentPage(
      val pageNumber: Int,
      val extractedText: String,
      val formulas: List<String> = emptyList(),
      val handwrittenNotes: String = ""
  )
  data class ParsedDocumentResult(
      val title: String,
      val pages: List<ParsedDocumentPage>,
      val rawCombinedText: String
  )
  suspend fun parseDocumentWithGemini(
      bitmaps: List<Bitmap>,
      documentContext: String = ""
  ): Result<ParsedDocumentResult>
  fun parseDocumentResultJson(jsonText: String): ParsedDocumentResult?
  ```

- [ ] **Step 1: Write the failing unit test for `parseDocumentResultJson`**

Create `app/src/test/java/com/kletaq/app/core/ai/DocumentIngestionRoutingTest.kt` testing:
- Valid clean JSON with LaTeX formulas (`\int_{0}^{\infty} e^{-x} dx`).
- Markdown codefence wrapped JSON (` ```json ... ``` `).
- Malformed JSON fallback to extract text safely without crashing.

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew testBenchmarkUnitTest --tests com.kletaq.app.core.ai.DocumentIngestionRoutingTest`
Expected: Compilation failure or test failure because `parseDocumentResultJson` does not exist yet.

- [ ] **Step 3: Implement data classes, JSON parser, and `parseDocumentWithGemini` in `UnifiedAiClient.kt`**

Add `ParsedDocumentPage`, `ParsedDocumentResult`, `parseDocumentResultJson`, and `parseDocumentWithGemini(bitmaps, documentContext)` in `UnifiedAiClient.kt`:
- System prompt instructs Gemini to extract content accurately, use LaTeX formatting for formulas, transcribe handwritten scribbles, and return strict JSON.
- Reuses `generateContent` with Base64 inline JPEG compression.
- Parses the response using `parseDocumentResultJson` and returns `Result<ParsedDocumentResult>`.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew testBenchmarkUnitTest --tests com.kletaq.app.core.ai.DocumentIngestionRoutingTest`
Expected: PASS.

---

### Task 2: 2048px Resolution & `RENDER_MODE_FOR_PRINT` Upgrade

**Files:**
- Modify: `app/src/main/java/com/kletaq/app/core/notes/CustomNotesManager.kt:32,175-185`
- Modify: `app/src/main/java/com/kletaq/app/core/academic/CustomModelPaperManager.kt:70-85`
- Modify: `app/src/main/java/com/kletaq/app/features/examsprint/ai/ExamSprintAiHelper.kt:145-160`
- Modify: `app/src/main/java/com/kletaq/app/features/memorypalace/ai/MemoryPalaceAiHelper.kt:290-305`

**Interfaces:**
- Changes rendering mode from `PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY` to `PdfRenderer.Page.RENDER_MODE_FOR_PRINT`.
- Standardizes maximum rendering dimension to `2048` pixels across all PDF rasterizers.

- [ ] **Step 1: Update `CustomNotesManager.kt`**
  - Change `MAX_DIMENSION = 1080` to `MAX_DIMENSION = 2048` (or introduce high-res scaling constant for AI readability).
  - In `importFromPdfs`, change `page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)` to `PdfRenderer.Page.RENDER_MODE_FOR_PRINT`.

- [ ] **Step 2: Update `CustomModelPaperManager.kt`**
  - In `loadModelPaperBitmaps`, update `page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)` to `RENDER_MODE_FOR_PRINT`.

- [ ] **Step 3: Update `ExamSprintAiHelper.kt`**
  - In `extractTextFromUri`, change `targetWidth = 1500` to `targetWidth = 2048`.
  - Update `page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)` to `RENDER_MODE_FOR_PRINT`.

- [ ] **Step 4: Update `MemoryPalaceAiHelper.kt`**
  - In `loadDocumentBitmaps`, change max scale dimension bound from `1400f` to `2048f`.
  - Update `page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)` to `RENDER_MODE_FOR_PRINT`.

- [ ] **Step 5: Verify build compiles cleanly**

Run: `.\gradlew compileBenchmarkKotlin`
Expected: BUILD SUCCESSFUL.

---

### Task 3: Smart Routing & Garbled OCR Detection in `NotesTextExtractor.kt`

**Files:**
- Modify: `app/src/main/java/com/kletaq/app/core/notes/NotesTextExtractor.kt:150-205`
- Test: `app/src/test/java/com/kletaq/app/core/ai/DocumentIngestionRoutingTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  fun isGarbledOrMathText(text: String, wordCount: Int): Boolean
  ```

- [ ] **Step 1: Write unit test for `isGarbledOrMathText`**

Add tests in `DocumentIngestionRoutingTest.kt`:
- Clean typed prose (e.g. 50 English words) -> returns `false` (keep ML Kit).
- Low word count with fragmented symbols (`"| / \\ x ~ ="`) -> returns `true` (trigger Gemini).
- Mathematical equation indicators (`"\int", "dx", "d/dx", "∑", "lambda = np"`) -> returns `true`.
- Empty / near-empty OCR (< 3 words) -> returns `true`.

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew testBenchmarkUnitTest --tests com.kletaq.app.core.ai.DocumentIngestionRoutingTest`
Expected: Fails because `isGarbledOrMathText` is not implemented.

- [ ] **Step 3: Implement `isGarbledOrMathText` and Gemini routing in `NotesTextExtractor.kt`**

In `NotesTextExtractor.kt`:
- Add `isGarbledOrMathText(text: String, wordCount: Int): Boolean`.
- In `extractTextForModule`:
  - Run ML Kit OCR on the page.
  - If `isGarbledOrMathText(rawText, wordCount)` is true:
    - Attempt `UnifiedAiClient.parseDocumentWithGemini(listOf(bitmap))` to get the clean transcription and LaTeX formulas.
    - If successful, replace `rawText` with Gemini's extracted text and mark `isReadable = true`.
    - If network is unavailable, retain ML Kit text with `isReadable = false` so it does not poison search.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew testBenchmarkUnitTest --tests com.kletaq.app.core.ai.DocumentIngestionRoutingTest`
Expected: PASS.

---

### Task 4: Multimodal Ingestion & LaTeX Prompting in `ExamSprintAiHelper.kt`

**Files:**
- Modify: `app/src/main/java/com/kletaq/app/features/examsprint/ai/ExamSprintAiHelper.kt:45-95,140-195`
- Test: `app/src/test/java/com/kletaq/app/core/ai/DocumentIngestionRoutingTest.kt`

**Interfaces:**
- Consumes: `UnifiedAiClient.parseDocumentWithGemini`, `NotesTextExtractor.isGarbledOrMathText`

- [ ] **Step 1: Update `parsePastedQuestionsAndAnswers` prompt for strict LaTeX**
  - Add explicit instruction in `parsePastedQuestionsAndAnswers`:
    *"All mathematical expressions, formulas, matrices, and variables MUST be formatted in LaTeX ($...$ for inline, $$...$$ for block). Do NOT use plain ASCII approximations like x^2/y."*

- [ ] **Step 2: Implement smart routing in `extractTextFromUri`**
  - When scanning PDF/Image pages, inspect the first page OCR.
  - If `isGarbledOrMathText` detects math or illegible handwriting:
    - Render up to the first 4 pages as high-resolution bitmaps and pass directly to `UnifiedAiClient.parseDocumentWithGemini(bitmaps)`.
    - Use the resulting `rawCombinedText` for `parsePastedQuestionsAndAnswers`.
  - Otherwise, continue using the fast, lightweight ML Kit flow for typed text.

- [ ] **Step 3: Verify compilation**

Run: `.\gradlew compileBenchmarkKotlin`
Expected: BUILD SUCCESSFUL.

---

### Task 5: Eliminate the Blind ML Kit Fallback Trap in `MemoryPalaceAiHelper.kt`

**Files:**
- Modify: `app/src/main/java/com/kletaq/app/features/memorypalace/ai/MemoryPalaceAiHelper.kt:210-260`
- Modify: `app/src/test/java/com/kletaq/app/features/memorypalace/MemoryPalaceAiHelperTest.kt`

- [ ] **Step 1: Remove `extractTextWithMlKit` fallback from `decomposeDerivation`**
  - In `MemoryPalaceAiHelper.kt`, delete the silent fallback to ML Kit OCR when AI returns empty or fails.
  - If `UnifiedAiClient.generateContent` fails or returns unparseable content, return a descriptive error:
    `Result.failure(Exception("AI could not read or decompose this derivation. Please ensure the document is clear, or check network connection."))`
  - Ensure bitmaps remain saved in `saveDocumentBitmaps` so students don't lose their uploads.

- [ ] **Step 2: Update unit tests in `MemoryPalaceAiHelperTest.kt`**
  - Verify that derivation parsing handles valid LaTeX without degradation and rejects nonsense fallback strings.

- [ ] **Step 3: Run all unit tests**

Run: `.\gradlew testBenchmarkUnitTest`
Expected: ALL TESTS PASS.

---

### Task 6: End-to-End Build & Benchmark Verification

- [ ] **Step 1: Run full unit test suite**
  Run: `.\gradlew testBenchmarkUnitTest`
  Expected: 0 failures.

- [ ] **Step 2: Run benchmark assemble**
  Run: `.\gradlew assembleBenchmark`
  Expected: BUILD SUCCESSFUL.

