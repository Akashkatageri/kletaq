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
import com.kletaq.app.features.flashcards.model.DeckStudyCounts
import com.kletaq.app.features.flashcards.model.Flashcard
import com.kletaq.app.features.flashcards.model.FlashcardDeck
import com.kletaq.app.features.flashcards.model.FlashcardReviewRating
import com.kletaq.app.features.flashcards.model.FlashcardReviewSnapshot
import com.kletaq.app.features.flashcards.model.FlashcardType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Stack

object FlashcardRepository {

    private const val TAG = "FlashcardRepository"
    private const val PREFS_NAME = "kletaq_flashcards_store"
    private const val KEY_CARDS_JSON = "flashcards_list_json"
    private const val KEY_DECKS_JSON = "flashcards_decks_json"

    private val _cards = MutableStateFlow<List<Flashcard>>(emptyList())
    val cards: StateFlow<List<Flashcard>> = _cards.asStateFlow()

    private val _decks = MutableStateFlow<List<FlashcardDeck>>(emptyList())
    val decks: StateFlow<List<FlashcardDeck>> = _decks.asStateFlow()

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
                _cards.value = list
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
        } catch (e: Exception) {
            Log.e(TAG, "Error loading flashcards: ${e.message}")
        }
    }

    @Synchronized
    private fun saveToStorage(cardsList: List<Flashcard>, decksList: List<FlashcardDeck>? = null) {
        _cards.value = cardsList
        if (decksList != null) {
            _decks.value = decksList
        }
        val prefs = getPrefs() ?: return
        try {
            val cardsArray = JSONArray()
            for (c in cardsList) {
                cardsArray.put(serializeCard(c))
            }
            val editor = prefs.edit().putString(KEY_CARDS_JSON, cardsArray.toString())
            if (decksList != null) {
                val decksArray = JSONArray()
                for (d in decksList) {
                    decksArray.put(serializeDeck(d))
                }
                editor.putString(KEY_DECKS_JSON, decksArray.toString())
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
        val topicCards = getCardsForTopic(topicId).filterNot { it.isSuspended }
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
        val deckCards = getCardsForDeck(deckId).filterNot { it.isSuspended }
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
        val all = _cards.value.filterNot { it.isSuspended }
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
        val pool = _cards.value.filterNot { it.isSuspended }.filter { card ->
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
        val current = _cards.value.toMutableList()
        for (card in newCards) {
            val idx = current.indexOfFirst { it.id == card.id }
            if (idx >= 0) {
                current[idx] = card
            } else {
                current.add(0, card)
            }
        }
        saveToStorage(current)
    }

    fun deleteCard(cardId: String) {
        val current = _cards.value.toMutableList()
        current.removeAll { it.id == cardId }
        saveToStorage(current)
    }

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

    fun recordReview(cardId: String, rating: FlashcardReviewRating): Flashcard {
        val existing = getCard(cardId) ?: throw IllegalArgumentException("Card not found: $cardId")
        undoStack.push(FlashcardReviewSnapshot(cardId = cardId, previousState = existing))

        val deck = _decks.value.find { it.id == existing.deckId } ?: FlashcardDeck()
        val updated = AnkiScheduler.scheduleNextReview(existing, rating, deck)
        saveCard(updated)
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
        val array = JSONArray()
        for (c in _cards.value) {
            array.put(serializeCard(c))
        }
        return array.toString(2)
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
        } catch (e: Exception) {
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
    }

    private fun serializeCard(c: Flashcard): JSONObject {
        return JSONObject().apply {
            put("id", c.id)
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
            put("tags", JSONArray(c.tags))
            put("flagColor", c.flagColor.name)
            put("isSuspended", c.isSuspended)
            put("isMarked", c.isMarked)
            put("queueState", c.queueState.name)
            put("reps", c.reps)
            put("lapses", c.lapses)
            put("easeFactor", c.easeFactor)
            put("intervalDays", c.intervalDays)
            put("dueTimestamp", c.dueTimestamp)
            put("learningStepIndex", c.learningStepIndex)
            put("lastReviewedTimestamp", c.lastReviewedTimestamp)
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

        return Flashcard(
            id = obj.optString("id"),
            deckId = obj.optString("deckId"),
            deckName = obj.optString("deckName"),
            topicId = obj.optString("topicId"),
            topicTitle = obj.optString("topicTitle"),
            subjectId = obj.optString("subjectId"),
            subjectName = obj.optString("subjectName"),
            cardType = FlashcardType.valueOf(obj.optString("cardType", FlashcardType.BASIC.name)),
            front = obj.optString("front"),
            back = obj.optString("back"),
            clozeText = obj.optString("clozeText"),
            hint = obj.optString("hint"),
            tags = tagsList,
            flagColor = CardFlag.valueOf(obj.optString("flagColor", CardFlag.NONE.name)),
            isSuspended = obj.optBoolean("isSuspended", false),
            isMarked = obj.optBoolean("isMarked", false),
            queueState = CardQueueState.valueOf(obj.optString("queueState", CardQueueState.NEW.name)),
            reps = obj.optInt("reps", 0),
            lapses = obj.optInt("lapses", 0),
            easeFactor = obj.optDouble("easeFactor", 2.50),
            intervalDays = obj.optInt("intervalDays", 0),
            dueTimestamp = obj.optLong("dueTimestamp", System.currentTimeMillis()),
            learningStepIndex = obj.optInt("learningStepIndex", 0),
            lastReviewedTimestamp = obj.optLong("lastReviewedTimestamp", 0L),
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
            newCardsPerDay = obj.optInt("newCardsPerDay", 20),
            maxReviewsPerDay = obj.optInt("maxReviewsPerDay", 200),
            learningStepsMinutes = if (stepsList.isNotEmpty()) stepsList else listOf(1, 10),
            graduatingIntervalDays = obj.optInt("graduatingIntervalDays", 1),
            easyIntervalDays = obj.optInt("easyIntervalDays", 4),
            startingEase = obj.optDouble("startingEase", 2.5),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
        )
    }
}
