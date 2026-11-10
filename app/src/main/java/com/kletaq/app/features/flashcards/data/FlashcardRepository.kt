package com.kletaq.app.features.flashcards.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kletaq.app.core.di.KletaqApplication
import com.kletaq.app.features.flashcards.engine.AnkiScheduler
import com.kletaq.app.features.flashcards.model.CardFlag
import com.kletaq.app.features.flashcards.model.CardQueueState
import com.kletaq.app.features.flashcards.model.DeckPreset
import com.kletaq.app.features.flashcards.model.DeckStudyCounts
import com.kletaq.app.features.flashcards.model.Flashcard
import com.kletaq.app.features.flashcards.model.FlashcardDeck
import com.kletaq.app.features.flashcards.model.FlashcardReviewRating
import com.kletaq.app.features.flashcards.model.FlashcardReviewSnapshot
import com.kletaq.app.features.flashcards.model.FlashcardType
import com.kletaq.app.features.flashcards.model.ReviewLogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Stack
import java.util.UUID

object FlashcardRepository {

    private const val TAG = "FlashcardRepository"
    private const val PREFS_NAME = "kletaq_flashcards_store"
    private const val KEY_CARDS_JSON = "flashcards_list_json"
    private const val KEY_DECKS_JSON = "flashcards_decks_json"
    private const val KEY_PRESETS_JSON = "flashcards_presets_json"
    private const val KEY_REVIEW_LOG_JSON = "flashcards_review_log_json"

    private val _cards = MutableStateFlow<List<Flashcard>>(emptyList())
    val cards: StateFlow<List<Flashcard>> = _cards.asStateFlow()

    private val _decks = MutableStateFlow<List<FlashcardDeck>>(emptyList())
    val decks: StateFlow<List<FlashcardDeck>> = _decks.asStateFlow()

    private val _presets = MutableStateFlow<List<DeckPreset>>(listOf(DeckPreset()))
    val presets: StateFlow<List<DeckPreset>> = _presets.asStateFlow()

    private val _reviewLogs = MutableStateFlow<List<ReviewLogEntry>>(emptyList())
    val reviewLogs: StateFlow<List<ReviewLogEntry>> = _reviewLogs.asStateFlow()

    private val undoStack = Stack<FlashcardReviewSnapshot>()
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        loadFromStorage()
    }

    private fun getPrefs(): SharedPreferences? {
        return try {
            val ctx = KletaqApplication.appContext
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        } catch (_: Exception) {
            null
        }
    }

    @Synchronized
    fun loadFromStorage() {
        val prefs = getPrefs() ?: return
        try {
            val cardsJson = prefs.getString(KEY_CARDS_JSON, null)
            if (!cardsJson.isNullOrBlank()) {
                val array = JSONArray(cardsJson)
                val list = mutableListOf<Flashcard>()
                for (i in 0 until array.length()) {
                    list.add(deserializeCard(array.getJSONObject(i)))
                }
                // Unbury cards whose buriedUntilTimestamp has passed
                val now = System.currentTimeMillis()
                _cards.value = list.map { c ->
                    if (c.isBuried && c.buriedUntilTimestamp <= now) {
                        c.copy(isBuried = false, buriedUntilTimestamp = 0L)
                    } else c
                }
            }

            val decksJson = prefs.getString(KEY_DECKS_JSON, null)
            if (!decksJson.isNullOrBlank()) {
                val array = JSONArray(decksJson)
                val list = mutableListOf<FlashcardDeck>()
                for (i in 0 until array.length()) {
                    list.add(deserializeDeck(array.getJSONObject(i)))
                }
                _decks.value = list
            }

            val presetsJson = prefs.getString(KEY_PRESETS_JSON, null)
            if (!presetsJson.isNullOrBlank()) {
                val array = JSONArray(presetsJson)
                val list = mutableListOf<DeckPreset>()
                for (i in 0 until array.length()) {
                    list.add(deserializePreset(array.getJSONObject(i)))
                }
                if (list.isNotEmpty()) _presets.value = list
            }

            val logsJson = prefs.getString(KEY_REVIEW_LOG_JSON, null)
            if (!logsJson.isNullOrBlank()) {
                val array = JSONArray(logsJson)
                val list = mutableListOf<ReviewLogEntry>()
                for (i in 0 until array.length()) {
                    list.add(deserializeReviewLog(array.getJSONObject(i)))
                }
                _reviewLogs.value = list
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading flashcards: ${e.message}")
        }
    }

    @Synchronized
    private fun saveToStorage(
        cardsList: List<Flashcard>,
        decksList: List<FlashcardDeck>? = null,
        presetsList: List<DeckPreset>? = null,
        logsList: List<ReviewLogEntry>? = null
    ) {
        _cards.value = cardsList
        if (decksList != null) _decks.value = decksList
        if (presetsList != null) _presets.value = presetsList
        if (logsList != null) _reviewLogs.value = logsList

        val prefs = getPrefs() ?: return
        try {
            val editor = prefs.edit()
            val cardsArray = JSONArray()
            for (c in cardsList) {
                cardsArray.put(serializeCard(c))
            }
            editor.putString(KEY_CARDS_JSON, cardsArray.toString())

            if (decksList != null) {
                val decksArray = JSONArray()
                for (d in decksList) {
                    decksArray.put(serializeDeck(d))
                }
                editor.putString(KEY_DECKS_JSON, decksArray.toString())
            }

            if (presetsList != null) {
                val presetsArray = JSONArray()
                for (p in presetsList) {
                    presetsArray.put(serializePreset(p))
                }
                editor.putString(KEY_PRESETS_JSON, presetsArray.toString())
            }

            if (logsList != null) {
                val logsArray = JSONArray()
                for (l in logsList) {
                    logsArray.put(serializeReviewLog(l))
                }
                editor.putString(KEY_REVIEW_LOG_JSON, logsArray.toString())
            }

            editor.apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving flashcards to storage: ${e.message}")
        }
    }

    fun getAllCards(): List<Flashcard> = _cards.value

    fun getCard(cardId: String): Flashcard? = _cards.value.find { it.id == cardId }

    fun getCardsForTopic(topicId: String): List<Flashcard> {
        return _cards.value.filter { it.topicId.equals(topicId, ignoreCase = true) }
    }

    fun getCardsForDeck(deckId: String): List<Flashcard> {
        return _cards.value.filter {
            it.deckId.equals(deckId, ignoreCase = true) || it.topicId.equals(deckId, ignoreCase = true)
        }
    }

    fun getCountsForTopic(topicId: String): DeckStudyCounts {
        val topicCards = getCardsForTopic(topicId).filterNot { it.isSuspended || it.isBuried }
        val now = System.currentTimeMillis()
        val newCount = topicCards.count { it.queueState == CardQueueState.NEW }
        val learningCount = topicCards.count { it.queueState == CardQueueState.LEARNING || it.queueState == CardQueueState.RELEARNING }
        val dueCount = topicCards.count {
            (it.queueState == CardQueueState.REVIEW || it.queueState == CardQueueState.LEARNING || it.queueState == CardQueueState.RELEARNING) && it.dueTimestamp <= now
        }
        return DeckStudyCounts(
            newCount = newCount,
            learningCount = learningCount,
            dueCount = dueCount,
            totalCount = topicCards.size
        )
    }

    fun getCountsForDeck(deckId: String): DeckStudyCounts {
        val deckCards = getCardsForDeck(deckId).filterNot { it.isSuspended || it.isBuried }
        val now = System.currentTimeMillis()
        val newCount = deckCards.count { it.queueState == CardQueueState.NEW }
        val learningCount = deckCards.count { it.queueState == CardQueueState.LEARNING || it.queueState == CardQueueState.RELEARNING }
        val dueCount = deckCards.count {
            (it.queueState == CardQueueState.REVIEW || it.queueState == CardQueueState.LEARNING || it.queueState == CardQueueState.RELEARNING) && it.dueTimestamp <= now
        }
        return DeckStudyCounts(
            newCount = newCount,
            learningCount = learningCount,
            dueCount = dueCount,
            totalCount = deckCards.size
        )
    }

    fun getGlobalCounts(): DeckStudyCounts {
        val all = _cards.value.filterNot { it.isSuspended || it.isBuried }
        val now = System.currentTimeMillis()
        val newCount = all.count { it.queueState == CardQueueState.NEW }
        val learningCount = all.count { it.queueState == CardQueueState.LEARNING || it.queueState == CardQueueState.RELEARNING }
        val dueCount = all.count {
            (it.queueState == CardQueueState.REVIEW || it.queueState == CardQueueState.LEARNING || it.queueState == CardQueueState.RELEARNING) && it.dueTimestamp <= now
        }
        return DeckStudyCounts(
            newCount = newCount,
            learningCount = learningCount,
            dueCount = dueCount,
            totalCount = all.size
        )
    }

    fun getStudyQueue(deckId: String? = null, topicId: String? = null, limit: Int = 100): List<Flashcard> {
        val pool = _cards.value.filterNot { it.isSuspended || it.isBuried }.filter { card ->
            when {
                !topicId.isNullOrBlank() -> card.topicId.equals(topicId, ignoreCase = true)
                !deckId.isNullOrBlank() -> card.deckId.equals(deckId, ignoreCase = true) || card.topicId.equals(deckId, ignoreCase = true)
                else -> true
            }
        }

        val now = System.currentTimeMillis()
        // Anki Queue priority: 1. Due Relearning/Learning, 2. Due Reviews, 3. New cards
        val learningDue = pool.filter { (it.queueState == CardQueueState.LEARNING || it.queueState == CardQueueState.RELEARNING) && it.dueTimestamp <= now }
            .sortedBy { it.dueTimestamp }
        val reviewDue = pool.filter { it.queueState == CardQueueState.REVIEW && it.dueTimestamp <= now }
            .sortedBy { it.dueTimestamp }
        val newCards = pool.filter { it.queueState == CardQueueState.NEW }
            .sortedBy { it.createdAt }

        val queue = mutableListOf<Flashcard>()
        queue.addAll(learningDue)
        queue.addAll(reviewDue)
        queue.addAll(newCards.take((limit - queue.size).coerceAtLeast(0)))

        return queue.take(limit)
    }

    fun saveCard(card: Flashcard) {
        val current = _cards.value.toMutableList()
        val idx = current.indexOfFirst { it.id == card.id }
        if (idx >= 0) {
            current[idx] = card.copy(updatedAt = System.currentTimeMillis())
        } else {
            current.add(0, card.copy(updatedAt = System.currentTimeMillis()))
        }
        saveToStorage(current)
        syncCardToFirestore(card)
    }

    fun saveCardsBatch(newCards: List<Flashcard>) {
        if (newCards.isEmpty()) return
        val current = _cards.value
        val newMap = newCards.associateBy { it.id }
        val updatedList = ArrayList<Flashcard>(current.size + newCards.size)
        val existingIds = HashSet<String>(current.size)

        for (c in current) {
            existingIds.add(c.id)
            updatedList.add(newMap[c.id] ?: c)
        }
        for (c in newCards) {
            if (!existingIds.contains(c.id)) {
                updatedList.add(0, c)
            }
        }
        saveToStorage(updatedList)
    }

    fun deleteCard(cardId: String) {
        val current = _cards.value.toMutableList()
        current.removeAll { it.id == cardId }
        saveToStorage(current)
    }

    fun createDeck(
        name: String,
        description: String = "",
        subjectId: String? = null,
        parentDeckId: String? = null,
        presetId: String = "default"
    ): FlashcardDeck {
        val parent = if (parentDeckId != null) getDeck(parentDeckId) else null
        val fullName = if (parent != null && !name.contains("::")) {
            "${parent.name}::$name"
        } else {
            name
        }
        val deck = FlashcardDeck(
            name = fullName,
            description = description,
            subjectId = subjectId,
            parentDeckId = parentDeckId,
            presetId = presetId
        )
        saveDeck(deck)
        return deck
    }

    fun renameDeck(deckId: String, newName: String) {
        val deck = getDeck(deckId) ?: return
        saveDeck(deck.copy(name = newName))
    }

    fun getDeck(deckId: String): FlashcardDeck? = _decks.value.find { it.id == deckId }

    fun getAllDecks(): List<FlashcardDeck> = _decks.value

    fun saveDeck(deck: FlashcardDeck) {
        val current = _decks.value.toMutableList()
        val idx = current.indexOfFirst { it.id == deck.id }
        if (idx >= 0) {
            current[idx] = deck
        } else {
            current.add(0, deck)
        }
        saveToStorage(_cards.value, current)
    }

    fun deleteDeck(deckId: String) {
        val current = _decks.value.toMutableList()
        current.removeAll { it.id == deckId }
        saveToStorage(_cards.value, current)
    }

    fun savePreset(preset: DeckPreset) {
        val current = _presets.value.toMutableList()
        val idx = current.indexOfFirst { it.id == preset.id }
        if (idx >= 0) {
            current[idx] = preset
        } else {
            current.add(preset)
        }
        saveToStorage(_cards.value, presetsList = current)
    }

    fun getPreset(presetId: String): DeckPreset {
        return _presets.value.find { it.id == presetId } ?: DeckPreset()
    }

    fun getAllPresets(): List<DeckPreset> = _presets.value

    fun recordReviewLog(entry: ReviewLogEntry) {
        val current = _reviewLogs.value.toMutableList()
        current.add(entry)
        saveToStorage(_cards.value, logsList = current)
    }

    fun getReviewLogs(): List<ReviewLogEntry> = _reviewLogs.value

    fun buryCard(cardId: String) {
        val card = getCard(cardId) ?: return
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 4)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        saveCard(card.copy(isBuried = true, buriedUntilTimestamp = calendar.timeInMillis))
    }

    fun burySiblingCards(cardId: String) {
        val card = getCard(cardId) ?: return
        if (card.noteId.isBlank()) return
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 4)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val until = calendar.timeInMillis
        val updated = _cards.value.map { c ->
            if (c.noteId == card.noteId && c.id != cardId) {
                c.copy(isBuried = true, buriedUntilTimestamp = until)
            } else c
        }
        saveToStorage(updated)
    }

    fun unburyAll() {
        val updated = _cards.value.map { c ->
            if (c.isBuried) c.copy(isBuried = false, buriedUntilTimestamp = 0L) else c
        }
        saveToStorage(updated)
    }

    fun recordReview(
        cardId: String,
        rating: FlashcardReviewRating,
        timeSpentMs: Long = 0L
    ): Flashcard {
        val existing = getCard(cardId) ?: throw IllegalArgumentException("Card not found: $cardId")
        undoStack.push(FlashcardReviewSnapshot(cardId = cardId, previousState = existing))

        val deck = _decks.value.find { it.id == existing.deckId } ?: FlashcardDeck()
        val preset = getPreset(deck.presetId)

        val updated = AnkiScheduler.scheduleNextReview(existing, rating, deck)

        // Sibling burying if active
        if (preset.autoBurySiblings && updated.noteId.isNotBlank()) {
            burySiblingCards(updated.id)
        }

        saveCard(updated)

        // Record review log entry
        recordReviewLog(
            ReviewLogEntry(
                cardId = cardId,
                rating = rating,
                timeSpentMs = timeSpentMs,
                intervalDays = updated.intervalDays,
                lastIntervalDays = existing.intervalDays,
                reviewType = existing.queueState.name
            )
        )

        return updated
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()

    fun undoLastReview(): Flashcard? {
        if (undoStack.isEmpty()) return null
        val snapshot = undoStack.pop()
        saveCard(snapshot.previousState)
        return snapshot.previousState
    }

    fun toggleSuspend(cardId: String) {
        val card = getCard(cardId) ?: return
        saveCard(card.copy(isSuspended = !card.isSuspended))
    }

    fun toggleMark(cardId: String) {
        val card = getCard(cardId) ?: return
        saveCard(card.copy(isMarked = !card.isMarked))
    }

    fun setFlag(cardId: String, flag: CardFlag) {
        val card = getCard(cardId) ?: return
        saveCard(card.copy(flagColor = flag))
    }

    fun resetCardProgress(cardId: String) {
        val card = getCard(cardId) ?: return
        val reset = card.copy(
            queueState = CardQueueState.NEW,
            reps = 0,
            lapses = 0,
            intervalDays = 0,
            learningStepIndex = 0,
            easeFactor = 2.50,
            dueTimestamp = System.currentTimeMillis()
        )
        saveCard(reset)
    }

    fun exportToJson(): String {
        return try {
            val array = JSONArray()
            for (c in _cards.value) {
                array.put(serializeCard(c))
            }
            array.toString(2)
        } catch (_: Throwable) {
            "[]"
        }
    }

    fun importFromJson(jsonStr: String): Int {
        return try {
            val array = JSONArray(jsonStr)
            val imported = mutableListOf<Flashcard>()
            for (i in 0 until array.length()) {
                imported.add(deserializeCard(array.getJSONObject(i)))
            }
            saveCardsBatch(imported)
            imported.size
        } catch (e: Throwable) {
            Log.e(TAG, "Import error: ${e.message}")
            0
        }
    }

    fun exportToCsv(): String {
        val sb = StringBuilder()
        sb.append("Front,Back,ClozeText,CardType,DeckName,Tags\n")
        for (c in _cards.value) {
            val frontClean = c.front.replace("\"", "\"\"")
            val backClean = c.back.replace("\"", "\"\"")
            val clozeClean = c.clozeText.replace("\"", "\"\"")
            val tagsClean = c.tags.joinToString(";")
            sb.append("\"$frontClean\",\"$backClean\",\"$clozeClean\",${c.cardType.name},\"${c.deckName}\",\"$tagsClean\"\n")
        }
        return sb.toString()
    }

    private fun syncCardToFirestore(card: Flashcard) {
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            scope.launch {
                try {
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .collection("flashcards")
                        .document(card.id)
                        .set(card)
                } catch (_: Exception) {}
            }
        } catch (_: Throwable) {
            // In unit tests where FirebaseAuth is not initialized
        }
    }

    private fun serializeCard(c: Flashcard): JSONObject {
        return JSONObject().apply {
            put("id", c.id)
            put("noteId", c.noteId)
            put("deckId", c.deckId)
            put("deckName", c.deckName)
            put("topicId", c.topicId)
            put("topicTitle", c.topicTitle)
            put("subjectId", c.subjectId)
            put("subjectName", c.subjectName)
            put("cardType", c.cardType.name)
            put("front", c.front)
            put("back", c.back)
            put("clozeText", c.clozeText)
            put("hint", c.hint)
            put("frontImagePath", c.frontImagePath ?: "")
            put("backImagePath", c.backImagePath ?: "")
            put("frontImagePaths", JSONArray(c.frontImagePaths))
            put("backImagePaths", JSONArray(c.backImagePaths))
            put("audioPath", c.audioPath ?: "")
            put("typedAnswer", c.typedAnswer)
            put("tags", JSONArray(c.tags))
            put("flagColor", c.flagColor.name)
            put("isSuspended", c.isSuspended)
            put("isMarked", c.isMarked)
            put("isBuried", c.isBuried)
            put("buriedUntilTimestamp", c.buriedUntilTimestamp)
            put("isLeech", c.isLeech)
            put("queueState", c.queueState.name)
            put("reps", c.reps)
            put("lapses", c.lapses)
            put("easeFactor", c.easeFactor)
            put("intervalDays", c.intervalDays)
            put("dueTimestamp", c.dueTimestamp)
            put("learningStepIndex", c.learningStepIndex)
            put("lastReviewedTimestamp", c.lastReviewedTimestamp)
            put("stability", c.stability)
            put("difficulty", c.difficulty)
            put("elapsedDays", c.elapsedDays)
            put("scheduledDays", c.scheduledDays)
            put("createdAt", c.createdAt)
            put("updatedAt", c.updatedAt)
        }
    }

    private fun deserializeCard(obj: JSONObject): Flashcard {
        val tagsArray = obj.optJSONArray("tags") ?: JSONArray()
        val tagsList = mutableListOf<String>()
        for (i in 0 until tagsArray.length()) {
            tagsList.add(tagsArray.getString(i))
        }

        val frontImagesArray = obj.optJSONArray("frontImagePaths")
        val frontImages = mutableListOf<String>()
        if (frontImagesArray != null && frontImagesArray.length() > 0) {
            for (i in 0 until frontImagesArray.length()) {
                FlashcardMediaManager.normalizeFilename(frontImagesArray.getString(i))?.let { frontImages.add(it) }
            }
        } else {
            val legacyFront = obj.optString("frontImagePath").takeIf { it.isNotBlank() }
            legacyFront?.let { FlashcardMediaManager.normalizeFilename(it)?.let { norm -> frontImages.add(norm) } }
        }

        val backImagesArray = obj.optJSONArray("backImagePaths")
        val backImages = mutableListOf<String>()
        if (backImagesArray != null && backImagesArray.length() > 0) {
            for (i in 0 until backImagesArray.length()) {
                FlashcardMediaManager.normalizeFilename(backImagesArray.getString(i))?.let { backImages.add(it) }
            }
        } else {
            val legacyBack = obj.optString("backImagePath").takeIf { it.isNotBlank() }
            legacyBack?.let { FlashcardMediaManager.normalizeFilename(it)?.let { norm -> backImages.add(norm) } }
        }

        val rawAudio = obj.optString("audioPath").takeIf { it.isNotBlank() }
        val normalizedAudio = rawAudio?.let { FlashcardMediaManager.normalizeFilename(it) }

        return Flashcard(
            id = obj.optString("id"),
            noteId = obj.optString("noteId"),
            deckId = obj.optString("deckId"),
            deckName = obj.optString("deckName"),
            topicId = obj.optString("topicId"),
            topicTitle = obj.optString("topicTitle"),
            subjectId = obj.optString("subjectId"),
            subjectName = obj.optString("subjectName"),
            cardType = try {
                FlashcardType.valueOf(obj.optString("cardType", FlashcardType.BASIC.name))
            } catch (_: Exception) {
                FlashcardType.BASIC
            },
            front = obj.optString("front"),
            back = obj.optString("back"),
            clozeText = obj.optString("clozeText"),
            hint = obj.optString("hint"),
            frontImagePath = frontImages.firstOrNull(),
            backImagePath = backImages.firstOrNull(),
            frontImagePaths = frontImages,
            backImagePaths = backImages,
            audioPath = normalizedAudio,
            typedAnswer = obj.optString("typedAnswer"),
            tags = tagsList,
            flagColor = try {
                CardFlag.valueOf(obj.optString("flagColor", CardFlag.NONE.name))
            } catch (_: Exception) {
                CardFlag.NONE
            },
            isSuspended = obj.optBoolean("isSuspended", false),
            isMarked = obj.optBoolean("isMarked", false),
            isBuried = obj.optBoolean("isBuried", false),
            buriedUntilTimestamp = obj.optLong("buriedUntilTimestamp", 0L),
            isLeech = obj.optBoolean("isLeech", false),
            queueState = try {
                CardQueueState.valueOf(obj.optString("queueState", CardQueueState.NEW.name))
            } catch (_: Exception) {
                CardQueueState.NEW
            },
            reps = obj.optInt("reps", 0),
            lapses = obj.optInt("lapses", 0),
            easeFactor = obj.optDouble("easeFactor", 2.50),
            intervalDays = obj.optInt("intervalDays", 0),
            dueTimestamp = obj.optLong("dueTimestamp", System.currentTimeMillis()),
            learningStepIndex = obj.optInt("learningStepIndex", 0),
            lastReviewedTimestamp = obj.optLong("lastReviewedTimestamp", 0L),
            stability = obj.optDouble("stability", 0.0),
            difficulty = obj.optDouble("difficulty", 0.0),
            elapsedDays = obj.optInt("elapsedDays", 0),
            scheduledDays = obj.optInt("scheduledDays", 0),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
        )
    }

    private fun serializeDeck(d: FlashcardDeck): JSONObject {
        return JSONObject().apply {
            put("id", d.id)
            put("name", d.name)
            put("description", d.description)
            put("subjectId", d.subjectId ?: "")
            put("topicId", d.topicId ?: "")
            put("parentDeckId", d.parentDeckId ?: "")
            put("presetId", d.presetId)
            put("newCardsPerDay", d.newCardsPerDay)
            put("maxReviewsPerDay", d.maxReviewsPerDay)
            put("learningStepsMinutes", JSONArray(d.learningStepsMinutes))
            put("graduatingIntervalDays", d.graduatingIntervalDays)
            put("easyIntervalDays", d.easyIntervalDays)
            put("startingEase", d.startingEase)
            put("createdAt", d.createdAt)
        }
    }

    private fun deserializeDeck(obj: JSONObject): FlashcardDeck {
        val stepsArray = obj.optJSONArray("learningStepsMinutes") ?: JSONArray()
        val stepsList = mutableListOf<Int>()
        for (i in 0 until stepsArray.length()) {
            stepsList.add(stepsArray.getInt(i))
        }

        return FlashcardDeck(
            id = obj.optString("id"),
            name = obj.optString("name"),
            description = obj.optString("description"),
            subjectId = obj.optString("subjectId").takeIf { it.isNotBlank() },
            topicId = obj.optString("topicId").takeIf { it.isNotBlank() },
            parentDeckId = obj.optString("parentDeckId").takeIf { it.isNotBlank() },
            presetId = obj.optString("presetId", "default"),
            newCardsPerDay = obj.optInt("newCardsPerDay", 20),
            maxReviewsPerDay = obj.optInt("maxReviewsPerDay", 200),
            learningStepsMinutes = if (stepsList.isNotEmpty()) stepsList else listOf(1, 10),
            graduatingIntervalDays = obj.optInt("graduatingIntervalDays", 1),
            easyIntervalDays = obj.optInt("easyIntervalDays", 4),
            startingEase = obj.optDouble("startingEase", 2.5),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
        )
    }

    private fun serializePreset(p: DeckPreset): JSONObject {
        return JSONObject().apply {
            put("id", p.id)
            put("name", p.name)
            put("newCardsPerDay", p.newCardsPerDay)
            put("maxReviewsPerDay", p.maxReviewsPerDay)
            put("learningStepsMinutes", JSONArray(p.learningStepsMinutes))
            put("relearningStepsMinutes", JSONArray(p.relearningStepsMinutes))
            put("useFsrs", p.useFsrs)
            put("desiredRetention", p.desiredRetention)
            put("startingEase", p.startingEase)
            put("easyBonus", p.easyBonus)
            put("intervalModifier", p.intervalModifier)
            put("maximumIntervalDays", p.maximumIntervalDays)
            put("leechThreshold", p.leechThreshold)
            put("leechAction", p.leechAction)
            put("autoBurySiblings", p.autoBurySiblings)
        }
    }

    private fun deserializePreset(obj: JSONObject): DeckPreset {
        val stepsArray = obj.optJSONArray("learningStepsMinutes") ?: JSONArray()
        val stepsList = mutableListOf<Int>()
        for (i in 0 until stepsArray.length()) {
            stepsList.add(stepsArray.getInt(i))
        }
        val relearnArray = obj.optJSONArray("relearningStepsMinutes") ?: JSONArray()
        val relearnList = mutableListOf<Int>()
        for (i in 0 until relearnArray.length()) {
            relearnList.add(relearnArray.getInt(i))
        }

        return DeckPreset(
            id = obj.optString("id", "default"),
            name = obj.optString("name", "Default"),
            newCardsPerDay = obj.optInt("newCardsPerDay", 20),
            maxReviewsPerDay = obj.optInt("maxReviewsPerDay", 200),
            learningStepsMinutes = if (stepsList.isNotEmpty()) stepsList else listOf(1, 10),
            relearningStepsMinutes = if (relearnList.isNotEmpty()) relearnList else listOf(10),
            useFsrs = obj.optBoolean("useFsrs", true),
            desiredRetention = obj.optDouble("desiredRetention", 0.90),
            startingEase = obj.optDouble("startingEase", 2.50),
            easyBonus = obj.optDouble("easyBonus", 1.30),
            intervalModifier = obj.optDouble("intervalModifier", 1.00),
            maximumIntervalDays = obj.optInt("maximumIntervalDays", 36500),
            leechThreshold = obj.optInt("leechThreshold", 8),
            leechAction = obj.optString("leechAction", "TAG_ONLY"),
            autoBurySiblings = obj.optBoolean("autoBurySiblings", true)
        )
    }

    private fun serializeReviewLog(l: ReviewLogEntry): JSONObject {
        return JSONObject().apply {
            put("id", l.id)
            put("cardId", l.cardId)
            put("rating", l.rating.name)
            put("reviewTimestamp", l.reviewTimestamp)
            put("timeSpentMs", l.timeSpentMs)
            put("intervalDays", l.intervalDays)
            put("lastIntervalDays", l.lastIntervalDays)
            put("reviewType", l.reviewType)
        }
    }

    private fun deserializeReviewLog(obj: JSONObject): ReviewLogEntry {
        return ReviewLogEntry(
            id = obj.optString("id", UUID.randomUUID().toString()),
            cardId = obj.optString("cardId"),
            rating = try {
                FlashcardReviewRating.valueOf(obj.optString("rating", FlashcardReviewRating.GOOD.name))
            } catch (_: Exception) {
                FlashcardReviewRating.GOOD
            },
            reviewTimestamp = obj.optLong("reviewTimestamp", System.currentTimeMillis()),
            timeSpentMs = obj.optLong("timeSpentMs", 0L),
            intervalDays = obj.optInt("intervalDays", 0),
            lastIntervalDays = obj.optInt("lastIntervalDays", 0),
            reviewType = obj.optString("reviewType", "REVIEW")
        )
    }
}

