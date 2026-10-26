package com.kletaq.app.core.flashcards

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kletaq.app.core.notes.CustomNotesManager
import com.kletaq.app.core.notes.NotesTextExtractor
import com.kletaq.app.data.model.TopicFlashcard
import com.kletaq.app.data.model.TopicFlashcardDeck
import com.kletaq.app.data.repository.UserSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object TopicFlashcardRepository {
    private const val TAG = "TopicFlashcardRepo"
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    // Fast in-memory cache to make screen switching instant
    private val deckCache = ConcurrentHashMap<String, TopicFlashcardDeck>()

    fun getCachedDeck(topicId: String): TopicFlashcardDeck? = deckCache[topicId]

    fun observeDeck(topicId: String): Flow<TopicFlashcardDeck?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(deckCache[topicId])
            awaitClose { }
            return@callbackFlow
        }

        // Emit cache first
        deckCache[topicId]?.let { trySend(it) }

        val listener = firestore.collection("users")
            .document(uid)
            .collection("topic_flashcards")
            .document(topicId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error listening to deck for $topicId: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val deck = snapshot.toObject(TopicFlashcardDeck::class.java)
                    if (deck != null) {
                        deckCache[topicId] = deck
                        trySend(deck)
                    }
                } else {
                    if (!deckCache.containsKey(topicId)) {
                        trySend(null)
                    }
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun generateDeck(
        context: Context,
        subjectCode: String,
        moduleIndex: Int,
        topicId: String,
        topicTitle: String
    ): Result<TopicFlashcardDeck> = withContext(Dispatchers.IO) {
        try {
            val uid = auth.currentUser?.uid ?: return@withContext Result.failure(Exception("User not authenticated"))

            val notePages = CustomNotesManager.getNotesForModule(context, subjectCode, moduleIndex)
            val manifest = NotesTextExtractor.loadCachedManifest(context, subjectCode, moduleIndex)
            val scopedRange = NotesTextExtractor.resolveTopicPageRange(
                topicId = topicId,
                topicTitle = topicTitle,
                topicIndex = 0,
                totalTopicsInModule = 8,
                totalPages = notePages.size,
                manifest = manifest
            )

            // Extract notes text for scoped pages
            val scopedExcerpts = StringBuilder()
            var hasGroundedText = false
            if (manifest != null && scopedRange != null) {
                val matchingPages = manifest.pages.filter { it.pageIndex in scopedRange }
                for (p in matchingPages) {
                    if (p.text.isNotBlank()) {
                        hasGroundedText = true
                        val cleanText = if (p.text.length > 1800) p.text.take(1800) + "…" else p.text
                        scopedExcerpts.append("[Page ${p.pageIndex}]:\n$cleanText\n\n")
                    }
                }
            }

            val systemPrompt = """
                You are an expert VTU Engineering Professor and Flashcard Creator for: $topicTitle.
                Your task is to generate 4 to 6 high-yield, exam-focused study flashcards based on the provided classroom notes excerpts.

                CATEGORIES TO COVER:
                1. "FORMULA": Key mathematical formula, symbol definitions, and variable meanings.
                2. "DERIVATION": Core derivation steps or key algebraic moves.
                3. "CONCEPT": Intuitive, clear definition or core principle.
                4. "EXAM_TRAP": Crucial mistake or common trap where students lose marks.

                RULES:
                - If notes excerpts are provided, ground facts and cite the source page in "pageCitation" (e.g. "Page 2").
                - If excerpts have minor handwriting abbreviations, bridge them with standard engineering curriculum principles.
                - Use readable Unicode math symbols (², √, ×, ≤, ∂, subscripts). Do NOT use LaTeX backslashes or dollar signs.
                - Keep fronts clear and direct questions/prompts. Keep backs concise and formatted with bullet points.
                - Output MUST be a single valid JSON object strictly matching this schema:
                {
                  "cards": [
                    {
                      "front": "Question or derivation prompt",
                      "back": "Clear answer or step-by-step breakdown",
                      "category": "FORMULA" | "DERIVATION" | "CONCEPT" | "EXAM_TRAP",
                      "pageCitation": "Page X",
                      "hint": "Optional short hint"
                    }
                  ]
                }
                Do NOT output markdown backticks or text outside the JSON object.
            """.trimIndent()

            val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = "gemini-3.1-flash-lite",
                generationConfig = generationConfig {
                    maxOutputTokens = 2048
                    temperature = 0.2f
                    responseMimeType = "application/json"
                },
                systemInstruction = content { text(systemPrompt) }
            )

            val prompt = if (hasGroundedText) {
                "Generate 4-6 high-yield exam flashcards for the topic '$topicTitle' using these attached student notes excerpts:\n\n$scopedExcerpts"
            } else {
                "Generate 4-6 high-yield exam flashcards for the VTU engineering course topic '$topicTitle' in Subject '$subjectCode', Module $moduleIndex."
            }

            val response = model.generateContent(prompt)
            val rawJson = response.text?.trim().orEmpty()
            val cleanJson = rawJson
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val parsedCards = mutableListOf<TopicFlashcard>()
            try {
                val rootObj = JSONObject(cleanJson)
                val cardsArray = rootObj.optJSONArray("cards")
                if (cardsArray != null) {
                    for (i in 0 until cardsArray.length()) {
                        val cardObj = cardsArray.getJSONObject(i)
                        parsedCards.add(
                            TopicFlashcard(
                                id = UUID.randomUUID().toString(),
                                front = cardObj.optString("front"),
                                back = cardObj.optString("back"),
                                category = cardObj.optString("category", "CONCEPT").uppercase(),
                                pageCitation = cardObj.optString("pageCitation", if (scopedRange != null) "Page ${scopedRange.first}" else ""),
                                hint = cardObj.optString("hint")
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed parsing JSON: ${e.message}")
            }

            // Fallback deterministic cards if generation or parsing was empty
            if (parsedCards.isEmpty()) {
                parsedCards.addAll(generateFallbackCards(topicTitle, scopedRange))
            }

            val deck = TopicFlashcardDeck(
                topicId = topicId,
                topicTitle = topicTitle,
                subjectCode = subjectCode,
                moduleIndex = moduleIndex,
                cards = parsedCards,
                generatedAt = System.currentTimeMillis()
            )

            // Save to Firestore and memory cache
            firestore.collection("users")
                .document(uid)
                .collection("topic_flashcards")
                .document(topicId)
                .set(deck)
                .await()

            deckCache[topicId] = deck
            Result.success(deck)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating flashcards: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun generateFallbackCards(topicTitle: String, scopedRange: IntRange?): List<TopicFlashcard> {
        val citation = if (scopedRange != null) "Page ${scopedRange.first}" else "Notes"
        return listOf(
            TopicFlashcard(
                id = UUID.randomUUID().toString(),
                front = "What is the core definition and purpose of $topicTitle?",
                back = "• Core principle: Fundamental topic requirement in module engineering syllabus.\n• Focus: Master standard terms and governing formulas.",
                category = "CONCEPT",
                pageCitation = citation,
                hint = "Think about fundamental principles"
            ),
            TopicFlashcard(
                id = UUID.randomUUID().toString(),
                front = "What is the standard formula or relationship in $topicTitle?",
                back = "• Governing expression: Check derivation and variables defined in your notes.\n• Units & Constraints: Always verify boundary conditions.",
                category = "FORMULA",
                pageCitation = citation,
                hint = "Review variables and units"
            ),
            TopicFlashcard(
                id = UUID.randomUUID().toString(),
                front = "What is the #1 common exam trap or mistake in $topicTitle?",
                back = "• Common pitfall: Mixing sign conventions or omitting intermediate steps in calculations.\n• Exam Tip: Write explicit assumptions before deriving.",
                category = "EXAM_TRAP",
                pageCitation = citation,
                hint = "Where do students typically lose marks?"
            )
        )
    }
}
