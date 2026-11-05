package com.kletaq.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.kletaq.app.core.navigation.MainNavigation
import com.kletaq.app.core.theme.KletaqTheme
import com.kletaq.app.core.util.LocalNotificationHelper
import com.kletaq.app.data.repository.UserSettingsRepository
import com.kletaq.app.features.flashcards.ui.ImportDeckDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            LocalNotificationHelper.createNotificationChannels(this)
        }
    }

    private var incomingDeckUriState by mutableStateOf<Uri?>(null)
    private var incomingDeckFileNameState by mutableStateOf("deck")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UserSettingsRepository.initialize(applicationContext)
        com.kletaq.app.data.repository.KletaqAcademicRepository.preloadAsync()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        handleIncomingIntent(intent)

        enableEdgeToEdge()
        setContent {
            KletaqTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation()

                    var updateInfo by remember {
                        mutableStateOf<com.kletaq.app.core.update.AppUpdateInfo?>(null)
                    }
                    val context = androidx.compose.ui.platform.LocalContext.current

                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(2500)
                        val result = com.kletaq.app.core.update.AppUpdateManager.checkForUpdate(context)
                        result.onSuccess { info ->
                            if (info.isUpdateAvailable) {
                                updateInfo = info
                            }
                        }
                    }

                    updateInfo?.let { info ->
                        com.kletaq.app.core.update.AppUpdateDialog(
                            updateInfo = info,
                            onDismiss = { updateInfo = null }
                        )
                    }

                    // System "Open with Kletaq" / Deck Import Modal
                    incomingDeckUriState?.let { uri ->
                        ImportDeckDialog(
                            fileUri = uri,
                            fileName = incomingDeckFileNameState,
                            onDismiss = { incomingDeckUriState = null },
                            onImportCompleted = { _, _ ->
                                incomingDeckUriState = null
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val uri: Uri? = when (action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
            }
            else -> null
        }

        if (uri != null) {
            val fileName = queryDisplayName(uri) ?: (uri.lastPathSegment ?: "deck")
            incomingDeckFileNameState = fileName
            incomingDeckUriState = uri
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor: Cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            return cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return uri.lastPathSegment
    }
}
