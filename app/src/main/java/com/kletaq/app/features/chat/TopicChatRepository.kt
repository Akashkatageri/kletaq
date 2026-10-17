package com.kletaq.app.features.chat

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.CancellationException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kletaq.app.core.di.KletaqApplication
import com.kletaq.app.core.notes.CustomNotesManager
import com.kletaq.app.core.notes.ModuleOcrManifest
import com.kletaq.app.core.notes.NotePageRetriever
import com.kletaq.app.core.notes.NotesTextExtractor
import com.kletaq.app.data.repository.KletaqAcademicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest

data class TutorMessage(val id: String = "", val role: String = "user", val text: String = "", val createdAt: Long = 0)
data class TutorContext(
    val topicId: String,
    val title: String,
    val subjectId: String,
    val moduleIndex: Int,
    val description: String,
    val manifest: ModuleOcrManifest?,
    val hasNotes: Boolean
)
class TutorAppCheckException(cause: Exception) : Exception("App Check verification failed", cause)

class TopicChatRepository(private val uid: String, private val topicId: String) {
    private val db = FirebaseFirestore.getInstance()
    private val chatId = MessageDigest.getInstance("SHA-256").digest(topicId.toByteArray()).joinToString("") { "%02x".format(it) }
    private val messages = db.collection("users").document(uid).collection("topicChats").document(chatId).collection("messages")
    private fun checkAccount() { check(FirebaseAuth.getInstance().currentUser?.uid == uid) { "Account changed. Reopen the chat." } }

    suspend fun context(title: String): TutorContext = withContext(Dispatchers.Default) {
        val match = KletaqAcademicRepository.getSemesters().asSequence().flatMap { semester ->
            semester.subjects.asSequence().flatMap { subject -> subject.units.asSequence().flatMap { unit ->
                unit.lessons.asSequence().map { lesson -> Triple(subject, unit, lesson) }
            } }
        }.firstOrNull { it.third.id == topicId }
        val subjectId = match?.first?.id.orEmpty()
        val moduleIndex = CustomNotesManager.resolveModuleIndex(topicId)

        val appContext = KletaqApplication.appContext
        var manifest = NotesTextExtractor.loadCachedManifest(appContext, subjectId, moduleIndex)
        val notePages = CustomNotesManager.getNotesForModule(appContext, subjectId, moduleIndex)
        if (notePages.isNotEmpty() && (manifest == null || manifest.pages.size != notePages.size)) {
            try {
                manifest = NotesTextExtractor.extractTextForModule(appContext, subjectId, moduleIndex)
            } catch (e: Exception) {
                Log.w("TopicChatRepository", "Failed extracting OCR manifest: ${e.message}")
            }
        }
        val hasNotes = notePages.isNotEmpty() && manifest != null && manifest.pages.any { it.text.isNotBlank() }

        TutorContext(
            topicId = topicId,
            title = match?.third?.title ?: title,
            subjectId = subjectId,
            moduleIndex = moduleIndex,
            description = "Subject: ${match?.first?.name.orEmpty()}\nModule: ${match?.second?.title.orEmpty()}\nTopic: ${match?.third?.title ?: title}",
            manifest = manifest,
            hasNotes = hasNotes
        )
    }

    suspend fun load(): List<TutorMessage> {
        checkAccount()
        val result = messages.orderBy("createdAt", Query.Direction.DESCENDING).limit(60).get().await()
        checkAccount()
        return result.documents.mapNotNull { it.toObject(TutorMessage::class.java) }.reversed()
    }

    suspend fun saveTurn(user: TutorMessage, model: TutorMessage, context: TutorContext) {
        checkAccount()
        val batch = db.batch()
        listOf(user, model).forEach { message ->
            batch.set(messages.document(message.id), mapOf("id" to message.id, "role" to message.role, "text" to message.text,
                "createdAt" to message.createdAt, "topicId" to topicId, "subjectId" to context.subjectId))
        }
        batch.commit().await()
        checkAccount()
    }

    fun answer(context: TutorContext, history: List<TutorMessage>, question: String): Flow<String> = flow {
        checkAccount()
        TutorRateLimiter.reserve(uid)
        checkAccount()

        // 1. If student hasn't attached notes for this module
        if (!context.hasNotes || context.manifest == null) {
            emit("No notes attached for Module ${context.moduleIndex} yet.\n\nPlease attach your classroom notes (PDF or photos) using the Attach Notes button above so I can scan and answer your doubts directly from your notes!")
            return@flow
        }

        // 2. Retrieve top relevant pages using keyword / BM25 ranker
        val retrieval = NotePageRetriever.retrieveTopPages(
            query = question,
            topicTitle = context.title,
            manifest = context.manifest,
            maxPages = 4
        )

        // 3. Evidence Gating: if retrieval found no matching sections or text
        if (!retrieval.hasSufficientEvidence || retrieval.formattedExcerpts.isBlank()) {
            emit("This question cannot be answered from your attached module notes.\n\nNo matching sections or readable details were found in the scanned pages for this topic.")
            return@flow
        }

        try {
            FirebaseAppCheck.getInstance().getAppCheckToken(false).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("TopicChatRepository", "App Check token unavailable: ${e.message}")
        }

        val system = """
            You are Kletaq's strict Note-Grounded AI Tutor for the topic: ${context.title}.
            You must answer the student's question based EXCLUSIVELY and ONLY on the attached classroom/module notes provided in the excerpts below.

            STRICT GROUNDING & CITATION RULES:
            1. Base your answer ONLY and STRICTLY on the facts, definitions, derivations, formulas, and steps directly present in the supplied note excerpts.
            2. NEVER use outside training knowledge, assumptions, or external facts not in the excerpts.
            3. You MUST cite the page number for every fact, formula, or explanation using the exact format: [Page X].
            4. If the excerpts do not contain enough readable evidence or if handwriting/formulas are illegible, state clearly:
               "The provided module notes do not contain sufficient readable details to answer this question."
            5. Use clear Markdown headings, bold text, numbered/bulleted lists, and readable Unicode symbols (∂, √, ×, ≤, ², subscripts). Never output LaTeX backslashes or dollar-sign math delimiters.
            6. When explaining, mention which page of their notes contains the concept (e.g., "From [Page ${retrieval.citedPageIndices.firstOrNull() ?: 1}]...").

            --- SUPPLIED NOTE EXCERPTS ---
            ${retrieval.formattedExcerpts}
        """.trimIndent()

        val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.1-flash-lite",
            generationConfig = generationConfig { maxOutputTokens = 1800; temperature = 0.2f },
            systemInstruction = content { text(system) }
        )

        val recent = history.takeLast(12).dropWhile { it.role != "user" }
        val chat = model.startChat(history = recent.map { message -> content(role = message.role) { text(message.text.take(6000)) } })

        val fullResponse = StringBuilder()
        chat.sendMessageStream(question).collect { response ->
            checkAccount()
            response.text?.let { chunk ->
                fullResponse.append(chunk)
                emit(chunk)
            }
        }

        // 4. Citation Validation: check if the answer references valid pages
        val responseStr = fullResponse.toString()
        val citedPages = Regex("""\[Page\s*(\d+)\]""", RegexOption.IGNORE_CASE)
            .findAll(responseStr)
            .mapNotNull { it.groupValues[1].toIntOrNull() }
            .toSet()

        val invalidCitations = citedPages.filter { it !in retrieval.citedPageIndices }
        if (invalidCitations.isNotEmpty()) {
            Log.w("TopicChatRepository", "Model cited pages not in retrieved set: $invalidCitations")
        }
    }
}
