package com.kletaq.app.features.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kletaq.app.core.ai.*
import com.kletaq.app.core.theme.CardSurface
import com.kletaq.app.core.theme.InkPaperBorder
import com.kletaq.app.core.theme.PurpleAccent
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val config by SecureAiPreferences.configFlow.collectAsState()

    var showAddKeyDialog by remember { mutableStateOf(false) }
    var verifyingKeyId by remember { mutableStateOf<String?>(null) }
    var keyToChangeModel by remember { mutableStateOf<AiKeyEntry?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI Engine & API Keys",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Multi-Key Pooling & Auto-Failover",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddKeyDialog = true },
                containerColor = PurpleAccent,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add API Key", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 88.dp, top = 8.dp)
        ) {
            // 1. Hero Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PurpleAccent.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PurpleAccent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = PurpleAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Bring Your Own AI (BYO-AI)",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "100% Free • Multi-Key Quota Pooling",
                                    fontSize = 11.sp,
                                    color = PurpleAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Connect free API keys from Google AI Studio or Groq to bypass rate limits, unlock 2,000+ token derivations, and get unblocked AI tutoring even when notes aren't attached yet.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = PurpleAccent)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Free Gemini Key", fontSize = 11.sp, color = PurpleAccent, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://console.groq.com/keys"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = PurpleAccent)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Free Groq Key", fontSize = 11.sp, color = PurpleAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. Master Enable Switch
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = BorderStroke(1.dp, InkPaperBorder.ColorLight)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Use Custom AI Keys",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = if (config.isEnabled && config.hasCustomKeys)
                                    "${config.activeKeys.size} custom key(s) active"
                                else
                                    "Using built-in Gemini 3.1 Flash-Lite (5 req/min)",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = config.isEnabled,
                            onCheckedChange = { SecureAiPreferences.setEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PurpleAccent
                            )
                        )
                    }
                }
            }

            // 3. Multi-Key Pooling Strategy (Visible when 2+ keys exist)
            if (config.keys.size >= 2) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        border = BorderStroke(1.dp, InkPaperBorder.ColorLight)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Multi-Key Pooling Strategy",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Combine quotas across multiple accounts to maximize requests.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            KeyPoolingStrategy.entries.forEach { strategy ->
                                val selected = config.strategy == strategy
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { SecureAiPreferences.setStrategy(strategy) },
                                    color = if (selected) PurpleAccent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, if (selected) PurpleAccent else Color.Transparent),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selected,
                                            onClick = { SecureAiPreferences.setStrategy(strategy) },
                                            colors = RadioButtonDefaults.colors(selectedColor = PurpleAccent)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = strategy.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = strategy.description,
                                                fontSize = 11.sp,
                                                color = TextSecondary,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Key List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your API Keys (${config.keys.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = if (config.hasCustomKeys) "🟢 Pool Active" else "⚪ Fallback Active",
                        fontSize = 12.sp,
                        color = if (config.hasCustomKeys) Color(0xFF2E7D32) else TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 5. Key Items
            if (config.keys.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        border = BorderStroke(1.dp, InkPaperBorder.ColorLight)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Key,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = TextSecondary.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No custom API keys added yet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Kletaq is currently using the shared built-in AI tutor. Add a free Google AI Studio key to unlock unlimited high-speed usage!",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showAddKeyDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Your First Key", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                items(config.keys, key = { it.id }) { keyEntry ->
                    KeyEntryCard(
                        entry = keyEntry,
                        isVerifying = verifyingKeyId == keyEntry.id,
                        onToggleActive = { active ->
                            SecureAiPreferences.updateKey(keyEntry.copy(isActive = active))
                        },
                        onTestKey = {
                            scope.launch {
                                verifyingKeyId = keyEntry.id
                                val res = UnifiedAiClient.verifyKey(keyEntry)
                                verifyingKeyId = null
                                if (res.isSuccess) {
                                    snackbarMessage = "✓ ${keyEntry.label} verified successfully!"
                                } else {
                                    snackbarMessage = "✗ Error: ${res.exceptionOrNull()?.message}"
                                }
                            }
                        },
                        onChangeModel = {
                            keyToChangeModel = keyEntry
                        },
                        onDeleteKey = {
                            SecureAiPreferences.removeKey(keyEntry.id)
                        }
                    )
                }
            }

            // 6. Safety Notice
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, InkPaperBorder.ColorLight.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = PurpleAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Hardware Encryption: Your API keys are encrypted locally on this device. They are NEVER sent to Kletaq servers and connect directly to the provider over HTTPS.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }

    if (showAddKeyDialog) {
        AddKeyDialog(
            onDismiss = { showAddKeyDialog = false },
            onKeyAdded = { entry ->
                SecureAiPreferences.addKey(entry)
                showAddKeyDialog = false
                // Auto verify newly added key
                scope.launch {
                    verifyingKeyId = entry.id
                    val res = UnifiedAiClient.verifyKey(entry)
                    verifyingKeyId = null
                    if (res.isSuccess) {
                        snackbarMessage = "✓ Key added and verified!"
                    } else {
                        snackbarMessage = "Key saved, but test ping returned: ${res.exceptionOrNull()?.message}"
                    }
                }
            }
        )
    }

    keyToChangeModel?.let { targetKey ->
        ChangeModelDialog(
            entry = targetKey,
            onDismiss = { keyToChangeModel = null },
            onModelChanged = { newModel ->
                SecureAiPreferences.updateKey(targetKey.copy(model = newModel))
                keyToChangeModel = null
                snackbarMessage = "Model switched to $newModel"
            }
        )
    }

    // Snackbar notification
    snackbarMessage?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(3500)
            snackbarMessage = null
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                shadowElevation = 6.dp
            ) {
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun KeyEntryCard(
    entry: AiKeyEntry,
    isVerifying: Boolean,
    onToggleActive: (Boolean) -> Unit,
    onTestKey: () -> Unit,
    onChangeModel: () -> Unit,
    onDeleteKey: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(
            1.dp,
            if (entry.isActive) PurpleAccent.copy(alpha = 0.35f) else InkPaperBorder.ColorLight
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    !entry.isActive -> Color.Gray
                                    entry.isRateLimitedUntil > System.currentTimeMillis() -> Color(0xFFF57C00) // Orange (Rate-limited)
                                    entry.lastError != null -> Color(0xFFD32F2F) // Red (Error)
                                    else -> Color(0xFF2E7D32) // Green (Ready)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = entry.label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                }

                Switch(
                    checked = entry.isActive,
                    onCheckedChange = onToggleActive,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PurpleAccent
                    ),
                    modifier = Modifier.scale(0.85f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PurpleAccent.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = entry.provider.displayName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleAccent,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onChangeModel() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = entry.model,
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Switch Model",
                            tint = PurpleAccent,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }

                Text(
                    text = entry.maskedKey(),
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Status message
            if (entry.isRateLimitedUntil > System.currentTimeMillis()) {
                val remSeconds = (entry.isRateLimitedUntil - System.currentTimeMillis()) / 1000
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🟡 Rate-limited (Cooling down for ${remSeconds}s, auto-switched to backup)",
                    fontSize = 11.sp,
                    color = Color(0xFFF57C00),
                    fontWeight = FontWeight.Medium
                )
            } else if (entry.lastError != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🔴 ${entry.lastError}",
                    fontSize = 11.sp,
                    color = Color(0xFFD32F2F),
                    maxLines = 1
                )
            } else if (entry.lastLatencyMs > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🟢 Verified • ${entry.lastLatencyMs}ms latency",
                    fontSize = 11.sp,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = InkPaperBorder.ColorLight.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onTestKey,
                    enabled = !isVerifying && entry.isActive,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = PurpleAccent)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pinging…", fontSize = 11.sp)
                    } else {
                        Icon(Icons.Outlined.Speed, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test Ping", fontSize = 11.sp)
                    }
                }

                IconButton(
                    onClick = onDeleteKey,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete key",
                        tint = Color(0xFFD32F2F).copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddKeyDialog(
    onDismiss: () -> Unit,
    onKeyAdded: (AiKeyEntry) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedProvider by remember { mutableStateOf(AiProviderType.GEMINI) }
    var apiKeyText by remember { mutableStateOf("") }
    var keyLabel by remember { mutableStateOf("Google AI Studio (Primary)") }
    var selectedModel by remember { mutableStateOf(AiProviderType.GEMINI.defaultModel) }
    var customBaseUrl by remember { mutableStateOf("") }
    var isKeyVisible by remember { mutableStateOf(false) }

    // Auto-detect provider on paste
    LaunchedEffect(apiKeyText) {
        val detected = AiProviderType.detectProvider(apiKeyText)
        if (apiKeyText.isNotBlank() && selectedProvider != detected) {
            selectedProvider = detected
            selectedModel = detected.defaultModel
            keyLabel = when (detected) {
                AiProviderType.GEMINI -> "Gemini Key"
                AiProviderType.GROQ -> "Groq Llama 3.3"
                AiProviderType.OPENROUTER -> "OpenRouter"
                AiProviderType.OPENAI -> "OpenAI Key"
                else -> keyLabel
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add AI API Key",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                // Provider Selection Chips
                Text(text = "Provider", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                val allProviders = listOf(
                    AiProviderType.GEMINI to "Gemini (Free)",
                    AiProviderType.GROQ to "Groq (Free)",
                    AiProviderType.OPENROUTER to "OpenRouter",
                    AiProviderType.DEEPSEEK to "DeepSeek",
                    AiProviderType.OPENAI to "OpenAI",
                    AiProviderType.CUSTOM_OPENAI to "Custom"
                )
                allProviders.chunked(3).forEach { rowProviders ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowProviders.forEach { (prov, label) ->
                            val isSelected = selectedProvider == prov
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) PurpleAccent else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedProvider = prov
                                        selectedModel = prov.defaultModel
                                        keyLabel = "${prov.displayName} Key"
                                    }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else TextPrimary,
                                    modifier = Modifier
                                        .padding(vertical = 8.dp)
                                        .wrapContentWidth(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                }

                // Help link to get free key
                if (selectedProvider.setupUrl.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(PurpleAccent.copy(alpha = 0.08f))
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(selectedProvider.setupUrl))
                                context.startActivity(intent)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Get 100% Free ${selectedProvider.displayName} Key ↗",
                            fontSize = 11.sp,
                            color = PurpleAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Custom Base URL input if Custom Endpoint
                if (selectedProvider == AiProviderType.CUSTOM_OPENAI) {
                    OutlinedTextField(
                        value = customBaseUrl,
                        onValueChange = { customBaseUrl = it },
                        label = { Text("Base URL (e.g. https://api.together.xyz/v1)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // Key Input
                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = { apiKeyText = it },
                    label = { Text("API Key (${selectedProvider.keyPrefixHint})") },
                    placeholder = { Text(selectedProvider.keyPrefixHint) },
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                Icon(
                                    imageVector = if (isKeyVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = "Toggle visibility",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    apiKeyText = clip.trim()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentPaste,
                                    contentDescription = "Paste",
                                    tint = PurpleAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Key Label
                OutlinedTextField(
                    value = keyLabel,
                    onValueChange = { keyLabel = it },
                    label = { Text("Key Label / Nickname") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Model Selection (Editable + Scrollable Presets)
                Text(text = "Active Model ID", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                OutlinedTextField(
                    value = selectedModel,
                    onValueChange = { selectedModel = it },
                    label = { Text("Model ID (e.g. ${selectedProvider.defaultModel})") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Text(text = "Quick Presets:", fontSize = 11.sp, color = TextSecondary)
                val availableModels = AiProviderType.getAvailableModels(selectedProvider)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableModels.forEach { modelName ->
                        val isSelected = selectedModel == modelName
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) PurpleAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isSelected) PurpleAccent else Color.Transparent),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedModel = modelName }
                        ) {
                            Text(
                                text = modelName,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) PurpleAccent else TextSecondary,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            if (apiKeyText.isNotBlank()) {
                                onKeyAdded(
                                    AiKeyEntry(
                                        label = keyLabel.ifBlank { "${selectedProvider.displayName} Key" },
                                        provider = selectedProvider,
                                        apiKey = apiKeyText.trim(),
                                        model = selectedModel.ifBlank { selectedProvider.defaultModel },
                                        customBaseUrl = customBaseUrl.trim()
                                    )
                                )
                            }
                        },
                        enabled = apiKeyText.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Key", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangeModelDialog(
    entry: AiKeyEntry,
    onDismiss: () -> Unit,
    onModelChanged: (String) -> Unit
) {
    var modelText by remember { mutableStateOf(entry.model) }
    val availableModels = AiProviderType.getAvailableModels(entry.provider)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Switch Model",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Text(
                    text = "Key: ${entry.label} (${entry.provider.displayName})",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = modelText,
                    onValueChange = { modelText = it },
                    label = { Text("Model ID") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Text(text = "Quick Presets:", fontSize = 11.sp, color = TextSecondary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableModels.forEach { preset ->
                        val isSelected = modelText == preset
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) PurpleAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isSelected) PurpleAccent else Color.Transparent),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { modelText = preset }
                        ) {
                            Text(
                                text = preset,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) PurpleAccent else TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            if (modelText.isNotBlank()) {
                                onModelChanged(modelText.trim())
                            }
                        },
                        enabled = modelText.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Apply", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

