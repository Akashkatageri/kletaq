package com.studyos.app.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.studyos.app.core.theme.InkPaperBorder
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsSystemDaydream
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.theme.CardSurface
import com.studyos.app.core.theme.PurpleAccent
import com.studyos.app.core.theme.TextPrimary
import com.studyos.app.core.theme.TextSecondary
import com.studyos.app.core.util.HapticFeedbackHelper
import com.studyos.app.data.repository.UserSettingsRepository

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onNavigateToBacklogSubjects: () -> Unit = {},
    onNavigateToStudyCalendar: () -> Unit = {},
    onNavigateToArchivedSemesters: () -> Unit = {}
) {
    val context = LocalContext.current

    // Observe persisted DataStore settings state
    val settingsState by UserSettingsRepository.userSettingsState.collectAsState()

    var isSynced by remember { mutableStateOf(true) }
    var archivedSemestersCount by remember { mutableIntStateOf(1) }

    var showFocusGoalDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPermissionRationaleDialog by remember { mutableStateOf(false) }

    var userProfile by remember { mutableStateOf<com.studyos.app.data.model.UserProfile?>(null) }
    val currentUser = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }

    androidx.compose.runtime.LaunchedEffect(currentUser) {
        if (currentUser != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        userProfile = snapshot.toObject(com.studyos.app.data.model.UserProfile::class.java)
                    }
                }
        }
    }

    val displayUsername = userProfile?.username?.takeIf { it.isNotBlank() }
        ?: currentUser?.displayName?.takeIf { it.isNotBlank() }
        ?: "Learner"

    val subtitleInfo = buildString {
        append("Google")
        userProfile?.let { prof ->
            if (prof.university.isNotBlank()) append(" • ").append(prof.university)
            if (prof.branch.isNotBlank()) append(" • ").append(prof.branch)
            if (prof.semester > 0) append(" • Sem ").append(prof.semester)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // --- 1. HEADER WITH PROFILE CHIP & TOP-RIGHT ICONS ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Profile Chip
                    Surface(
                        shape = InkPaperBorder.MediumShape,
                        color = CardSurface,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🔥", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = displayUsername,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = TextPrimary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "S${userProfile?.semester ?: 1}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Top-Right Action Icons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = CardSurface,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(36.dp)
                    ) {
                        IconButton(onClick = onNavigateToSearch) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = CardSurface,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(36.dp)
                    ) {
                        IconButton(onClick = onNavigateToNotifications) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- 2. ACCOUNT SECTION ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ACCOUNT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    letterSpacing = 0.5.sp
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = InkPaperBorder.HeavyShape,
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = InkPaperBorder.heavyBorder(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = TextPrimary.copy(alpha = 0.12f),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = "👨‍💻", fontSize = 24.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = displayUsername,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = subtitleInfo,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            // Sync Status Badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSynced) Color(0xFFD1FAE5) else Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = if (isSynced) "🟢 Synced" else "🟡 Offline",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSynced) Color(0xFF10B981) else Color(0xFFD97706),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                android.util.Log.d("AuthFlow", "User signed out")
                                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                                onSignOut()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.2f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                        ) {
                            Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log out of Kletaq", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- 3. APPEARANCE SECTION ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "APPEARANCE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PurpleAccent,
                    letterSpacing = 0.5.sp
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = InkPaperBorder.HeavyShape,
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = InkPaperBorder.heavyBorder(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 🌙 Dark Theme Card
                        ThemeSelectionCard(
                            title = "Dark",
                            icon = Icons.Default.Nightlight,
                            isSelected = settingsState.themeMode.lowercase() == "dark",
                            onClick = { UserSettingsRepository.updateThemeMode("dark") },
                            modifier = Modifier.weight(1f)
                        )

                        // ☀️ Light Theme Card
                        ThemeSelectionCard(
                            title = "Light",
                            icon = Icons.Default.WbSunny,
                            isSelected = settingsState.themeMode.lowercase() == "light",
                            onClick = { UserSettingsRepository.updateThemeMode("light") },
                            modifier = Modifier.weight(1f)
                        )

                        // ⚙️ System Default Card
                        ThemeSelectionCard(
                            title = "System",
                            icon = Icons.Default.SettingsSystemDaydream,
                            isSelected = settingsState.themeMode.lowercase() == "system",
                            onClick = { UserSettingsRepository.updateThemeMode("system") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // --- 4. LEARNING SECTION ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "LEARNING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PurpleAccent,
                    letterSpacing = 0.5.sp
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = InkPaperBorder.HeavyShape,
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = InkPaperBorder.heavyBorder(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 📖 Study Calendar
                        SettingActionRow(
                            icon = Icons.Default.MenuBook,
                            title = "Study calendar",
                            subtitle = "Semester dates • Vacation mode",
                            badgeText = "Active",
                            onClick = onNavigateToStudyCalendar
                        )

                        // 🎯 Daily Focus Goal (Saved to DataStore)
                        SettingActionRow(
                            icon = Icons.Default.Schedule,
                            title = "Daily focus goal",
                            subtitle = "Currently ${settingsState.dailyFocusGoalMinutes} minutes",
                            badgeText = "${settingsState.dailyFocusGoalMinutes} min",
                            onClick = { showFocusGoalDialog = true }
                        )

                        // 🏖 Semester Break Mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFECFEFF),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(imageVector = Icons.Default.Umbrella, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = "Semester break mode", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                    Text(text = "Freeze streak during holidays", fontSize = 11.sp, color = TextSecondary)
                                }
                            }

                            Switch(
                                checked = settingsState.semesterBreakMode,
                                onCheckedChange = { UserSettingsRepository.updateSemesterBreakMode(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PurpleAccent)
                            )
                        }

                        // 📚 Archived Semesters
                        SettingActionRow(
                            icon = Icons.Default.Archive,
                            title = "Archived semesters",
                            subtitle = "$archivedSemestersCount previous semester archived",
                            badgeText = "$archivedSemestersCount",
                            onClick = onNavigateToArchivedSemesters
                        )

                        // 📘 Active Backlog Subjects
                        SettingActionRow(
                            icon = Icons.Default.MenuBook,
                            title = "Active backlog subjects",
                            subtitle = "Manage subjects from previous semesters",
                            badgeText = "Manage",
                            onClick = onNavigateToBacklogSubjects
                        )
                    }
                }
            }
        }

        // --- 5. NOTIFICATIONS SECTION ---
        item {
            val isSystemPermissionGranted = com.studyos.app.core.ui.NotificationPermissionHelper.isPermissionGranted(context)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "NOTIFICATIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PurpleAccent,
                    letterSpacing = 0.5.sp
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = InkPaperBorder.HeavyShape,
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = InkPaperBorder.heavyBorder(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // System Permission Status Banner / Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSystemPermissionGranted) Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = if (isSystemPermissionGranted) "🔔" else "🔕", fontSize = 18.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "System Notifications",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (isSystemPermissionGranted) "Status: Enabled in Android" else "Status: Disabled in Android",
                                        fontSize = 11.sp,
                                        color = if (isSystemPermissionGranted) Color(0xFF10B981) else Color(0xFFEF4444)
                                    )
                                }
                            }

                            if (!isSystemPermissionGranted) {
                                Button(
                                    onClick = {
                                        val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        }
                                        context.startActivity(intent)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Enable Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // 📝 1. Exam Reminders (1–2 days before)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFFEF3C7),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = "📝", fontSize = 18.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = "Exam reminders", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                    Text(text = "1–2 days before scheduled exam", fontSize = 11.sp, color = TextSecondary)
                                }
                            }

                            Switch(
                                checked = settingsState.morningReminderEnabled,
                                onCheckedChange = { checked ->
                                    if (checked && !isSystemPermissionGranted) {
                                        showPermissionRationaleDialog = true
                                    }
                                    UserSettingsRepository.updateMorningReminderEnabled(checked)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PurpleAccent)
                            )
                        }

                        // 🔥 2. Streak Expiration Warnings
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFFEE2E2),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = "🔥", fontSize = 18.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = "Streak warnings", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                    Text(text = "Notify if streak will expire tonight", fontSize = 11.sp, color = TextSecondary)
                                }
                            }

                            Switch(
                                checked = settingsState.eveningStreakProtectionEnabled,
                                onCheckedChange = { checked ->
                                    if (checked && !isSystemPermissionGranted) {
                                        showPermissionRationaleDialog = true
                                    }
                                    UserSettingsRepository.updateEveningStreakProtectionEnabled(checked)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PurpleAccent)
                            )
                        }

                        // 📚 3. Revision Reminders (only if pending)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFEEF2FF),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = "📚", fontSize = 18.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = "Revision reminders", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                    Text(text = "Notify when spaced repetition card is due", fontSize = 11.sp, color = TextSecondary)
                                }
                            }

                            Switch(
                                checked = true,
                                onCheckedChange = { checked ->
                                    if (checked && !isSystemPermissionGranted) {
                                        showPermissionRationaleDialog = true
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PurpleAccent)
                            )
                        }
                    }
                }
            }
        }

        // --- 6. SOUND & HAPTICS SECTION ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SOUND & HAPTICS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PurpleAccent,
                    letterSpacing = 0.5.sp
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = InkPaperBorder.HeavyShape,
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = InkPaperBorder.heavyBorder(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 🎵 Sound Effects Toggle (Saved to DataStore)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFD1FAE5),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = "Sound effects", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                    Text(text = "XP sounds • achievement chimes", fontSize = 11.sp, color = TextSecondary)
                                }
                            }

                            Switch(
                                checked = settingsState.soundEffectsEnabled,
                                onCheckedChange = { UserSettingsRepository.updateSoundEffectsEnabled(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PurpleAccent)
                            )
                        }

                        // 📳 Haptic Vibration Toggle (Saved to DataStore & triggers vibration)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFECFEFF),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(imageVector = Icons.Default.Vibration, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = "Haptic vibration", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                    Text(text = "Tactile feedback during study sessions", fontSize = 11.sp, color = TextSecondary)
                                }
                            }

                            Switch(
                                checked = settingsState.hapticFeedbackEnabled,
                                onCheckedChange = { enabled ->
                                    UserSettingsRepository.updateHapticFeedbackEnabled(enabled)
                                    if (enabled) {
                                        HapticFeedbackHelper.vibrate(context)
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PurpleAccent)
                            )
                        }
                    }
                }
            }
        }

        // --- 7. ABOUT SECTION ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ABOUT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PurpleAccent,
                    letterSpacing = 0.5.sp
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = InkPaperBorder.HeavyShape,
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = InkPaperBorder.heavyBorder(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SettingActionRow(
                            icon = Icons.Default.Info,
                            title = "About StudyOS",
                            subtitle = "Version V1.0.0 • Academic Control Center",
                            badgeText = "V1.0.0",
                            onClick = { showAboutDialog = true }
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(28.dp)) }
    }

    // Modal Dialogs
    if (showFocusGoalDialog) {
        ModalBottomSheet(
            onDismissRequest = { showFocusGoalDialog = false },
            containerColor = CardSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "🎯 Daily Focus Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = "Choose target focus minutes per day:", fontSize = 12.sp, color = TextSecondary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15, 30, 45, 60).forEach { mins ->
                        Button(
                            onClick = {
                                UserSettingsRepository.updateDailyFocusGoal(mins)
                                HapticFeedbackHelper.vibrate(context)
                                showFocusGoalDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (settingsState.dailyFocusGoalMinutes == mins) PurpleAccent else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (settingsState.dailyFocusGoalMinutes == mins) Color.White else TextPrimary
                            )
                        ) {
                            Text("${mins}m", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showTimePickerDialog) {
        ModalBottomSheet(
            onDismissRequest = { showTimePickerDialog = false },
            containerColor = CardSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "🌅 Morning Reminder Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)

                listOf("7:00 AM", "8:00 AM", "9:00 AM", "10:00 AM").forEach { t ->
                    Button(
                        onClick = {
                            UserSettingsRepository.updateMorningReminderTime(t)
                            HapticFeedbackHelper.vibrate(context)
                            showTimePickerDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (settingsState.morningReminderTime == t) PurpleAccent else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (settingsState.morningReminderTime == t) Color.White else TextPrimary
                        )
                    ) {
                        Text(t, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showAboutDialog) {
        ModalBottomSheet(
            onDismissRequest = { showAboutDialog = false },
            containerColor = CardSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "🎓 StudyOS V1.0.0", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = PurpleAccent)
                Text(text = "Academic Control Center built for students.", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showAboutDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent, contentColor = Color.White)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    if (showPermissionRationaleDialog) {
        com.studyos.app.core.ui.NotificationPermissionDialog(
            onDismiss = { showPermissionRationaleDialog = false },
            onPermissionResult = { granted ->
                showPermissionRationaleDialog = false
            }
        )
    }
}

@Composable
private fun ThemeSelectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = InkPaperBorder.MediumShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PurpleAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            1.5.dp,
            if (isSelected) PurpleAccent else TextSecondary.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) PurpleAccent else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) PurpleAccent else TextPrimary
            )
        }
    }
}

@Composable
private fun SettingActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    badgeText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = PurpleAccent.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = title, tint = PurpleAccent, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = PurpleAccent.copy(alpha = 0.12f)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurpleAccent,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}
