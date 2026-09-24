package com.kletaq.app.features.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import android.view.WindowManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseAuth

@Composable
fun TopicChatEntry(topicId: String, title: String) {
    var open by rememberSaveable(topicId) { mutableStateOf(false) }
    val auth = remember { FirebaseAuth.getInstance() }
    var uid by remember { mutableStateOf(auth.currentUser?.uid) }
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { uid = it.currentUser?.uid; open = false }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) { Text("Ask about this topic") }
    if (open) {
        val account = uid
        if (account == null) {
            AlertDialog(onDismissRequest = { open = false }, title = { Text("Sign in to chat") },
                text = { Text("Sign in to save your topic conversations.") }, confirmButton = { TextButton(onClick = { open = false }) { Text("OK") } })
        } else key(account, topicId) {
            val owner = remember { object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() } }
            DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
            val vm = remember(owner) {
                ViewModelProvider(owner, object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T = TopicChatViewModel(account, topicId, title) as T
                })[TopicChatViewModel::class.java]
            }
            val state by vm.state.collectAsStateWithLifecycle()
            TopicChatWindow(title, state, vm::retry, vm::send) { open = false }
        }
    }
}

@Composable
private fun TopicChatWindow(title: String, state: TutorState, onRetry: () -> Unit, onSend: (String) -> Unit, onDismiss: () -> Unit) {
    var draft by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size, state.reply.length) {
        if (state.messages.isNotEmpty() || state.reply.isNotEmpty())
            listState.scrollToItem(state.messages.size + if (state.reply.isNotEmpty()) 1 else 0)
    }
    LaunchedEffect(state.busy, state.error) {
        if (!state.busy && state.error == null && state.messages.lastOrNull()?.role == "model") draft = ""
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val dialogView = LocalView.current
        DisposableEffect(dialogView) {
            val window = (dialogView.parent as? DialogWindowProvider)?.window
            window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR)
            if (android.os.Build.VERSION.SDK_INT >= 30 && window != null) {
                window.attributes = window.attributes.apply { setFitInsetsTypes(0) }
            }
            window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            onDispose { }
        }
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Topic tutor", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
                if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        Text(if (state.context?.notes?.isNotBlank() == true) "AI help using this topic's lesson notes. Check important answers against your course material."
                            else "AI explanation · Detailed lesson notes aren't available for this topic.", style = MaterialTheme.typography.bodySmall)
                        Text("Questions and topic context are sent to Google Gemini. Conversations are saved to your account.", style = MaterialTheme.typography.bodySmall)
                        Text("Limit: 5 requests per minute across all topics on this device.", style = MaterialTheme.typography.bodySmall)
                        if (state.messages.isEmpty()) {
                            listOf("Explain this simply", "Show a solved example", "Help me write a 5-mark answer", "Quiz me on this topic").forEach { prompt ->
                                OutlinedButton(onClick = { draft = prompt }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text(prompt) }
                            }
                        }
                    }
                    items(state.messages, key = { it.id }) { message ->
                        Surface(shape = MaterialTheme.shapes.medium,
                            color = if (message.role == "user") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp)) {
                                Text(if (message.role == "user") "You" else "AI tutor", style = MaterialTheme.typography.labelMedium)
                                if (message.role == "model") TutorMarkdown(message.text) else Text(message.text, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                    if (state.reply.isNotEmpty()) item { TutorMarkdown(state.reply, Modifier.padding(14.dp)) }
                    else if (state.busy) item { Text("Thinking…") }
                    state.error?.let { error -> item {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onRetry, enabled = !state.busy && !state.loading) { Text("Retry") }
                    } }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = draft, onValueChange = { draft = it.take(2000) }, label = { Text("Ask about this topic") },
                    maxLines = 2, enabled = !state.busy, modifier = Modifier.weight(1f))
                Button(onClick = { onSend(draft.trim()) }, enabled = state.canSend && !state.loading && !state.busy && draft.isNotBlank()) {
                    Text(if (state.busy) "Wait" else "Send")
                }
                }
            }
          }
        }
    }
}
