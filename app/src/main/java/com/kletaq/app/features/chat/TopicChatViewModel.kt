package com.kletaq.app.features.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

data class TutorState(
    val loading: Boolean = true, val busy: Boolean = false, val context: TutorContext? = null,
    val messages: List<TutorMessage> = emptyList(), val reply: String = "", val error: String? = null,
    val canSend: Boolean = false
)

class TopicChatViewModel(uid: String, topicId: String, private val title: String) : ViewModel() {
    private val repository = TopicChatRepository(uid, topicId)
    private val mutableState = MutableStateFlow(TutorState())
    val state = mutableState.asStateFlow()
    private var pending: Pair<TutorMessage, TutorMessage>? = null
    init { reload() }

    private fun rethrowCancellation(e: Exception) { if (e is CancellationException && e !is TimeoutCancellationException) throw e }

    fun reload() {
        if (mutableState.value.busy) return
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true, error = null, canSend = false)
            try {
                withTimeout(20000) {
                    val context = repository.context(title)
                    val history = repository.load()
                    mutableState.value = TutorState(loading = false, context = context, messages = history, canSend = true)
                }
            } catch (e: Exception) {
                rethrowCancellation(e)
                mutableState.value = mutableState.value.copy(loading = false, error = "Couldn't load chat. Check your connection and try again.")
            }
        }
    }

    fun send(input: String) {
        val question = input.trim().take(2000)
        val current = mutableState.value
        val context = current.context ?: return
        if (question.isBlank() || current.busy || !current.canSend || pending != null) return
        mutableState.value = current.copy(busy = true, error = null, reply = "")
        viewModelScope.launch {
            try {
                val user = TutorMessage(UUID.randomUUID().toString(), "user", question, System.currentTimeMillis())
                mutableState.value = mutableState.value.copy(messages = current.messages + user)
                val answer = StringBuilder()
                withTimeout(90000) {
                    repository.answer(context, current.messages, question).collect { chunk ->
                        answer.append(chunk)
                        mutableState.value = mutableState.value.copy(reply = answer.toString())
                    }
                }
                check(answer.isNotBlank()) { "No answer returned" }
                val model = TutorMessage(UUID.randomUUID().toString(), "model", answer.toString(), System.currentTimeMillis())
                pending = user to model
                mutableState.value = mutableState.value.copy(messages = current.messages + user + model, reply = "")
                persistPending(context)
            } catch (e: Exception) {
                rethrowCancellation(e)
                mutableState.value = mutableState.value.copy(messages = current.messages, reply = "",
                    error = if (e is TutorRateLimitException)
                        "Limit reached: 5 requests per minute across all topics. Try again in ${e.seconds} seconds. Your question has been kept."
                    else if (e is TutorAppCheckException)
                        "Firebase couldn't verify this app. App Check registration is required. Emulator builds need a registered development token; phone builds need valid Play Integrity setup. Your question has been kept."
                    else if (e is TimeoutCancellationException)
                        "The request timed out. Your question has been kept; please try again."
                    else "AI request failed (${e.javaClass.simpleName}: ${e.localizedMessage ?: e.message ?: "Unknown error"}). Your question has been kept.")
            } finally { mutableState.value = mutableState.value.copy(busy = false) }
        }
    }

    private suspend fun persistPending(context: TutorContext) {
        val pair = pending ?: return
        try {
            withTimeout(20000) { repository.saveTurn(pair.first, pair.second, context) }
            pending = null
            mutableState.value = mutableState.value.copy(error = null, canSend = true)
        } catch (e: Exception) {
            rethrowCancellation(e)
            mutableState.value = mutableState.value.copy(canSend = false, error = "Answer received, but chat wasn't saved. Retry saving before sending another question.")
        }
    }

    fun retry() {
        val context = mutableState.value.context
        if (pending == null || context == null) { reload(); return }
        if (mutableState.value.busy) return
        mutableState.value = mutableState.value.copy(busy = true)
        viewModelScope.launch { try { persistPending(context) } finally { mutableState.value = mutableState.value.copy(busy = false) } }
    }
}
