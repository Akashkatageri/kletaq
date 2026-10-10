package com.kletaq.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.kletaq.app.core.navigation.MainNavigation
import com.kletaq.app.core.theme.KletaqTheme
import com.kletaq.app.core.util.LocalNotificationHelper
import com.kletaq.app.data.repository.UserSettingsRepository
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UserSettingsRepository.initialize(applicationContext)
        com.kletaq.app.data.repository.KletaqAcademicRepository.preloadAsync()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        enableEdgeToEdge()
        setContent {
            KletaqTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation()

                    var updateInfo by androidx.compose.runtime.remember {
                        androidx.compose.runtime.mutableStateOf<com.kletaq.app.core.update.AppUpdateInfo?>(null)
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
                }
            }
        }
    }
}
