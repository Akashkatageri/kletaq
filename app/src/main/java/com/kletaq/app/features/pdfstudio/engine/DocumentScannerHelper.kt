package com.kletaq.app.features.pdfstudio.engine

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class DocumentScannerHandle(
    val launchScanner: () -> Unit,
    val launchCamera: () -> Unit
)

/**
 * Shared Document Scanner & Camera helper for PDF Studio screens.
 * Uses Google Play Services ML Kit Document Scanner (auto-crop, deskew, filters, multi-page)
 * and falls back seamlessly to the system Camera app via FileProvider if unavailable.
 */
@Composable
fun rememberDocumentScannerLauncher(
    pageLimit: Int = 50,
    onPagesCaptured: (List<Uri>) -> Unit
): DocumentScannerHandle {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (success && uri != null) {
            onPagesCaptured(listOf(uri))
        }
    }

    val launchCameraFallback: () -> Unit = {
        try {
            val scansDir = File(context.cacheDir, "scans").apply { if (!exists()) mkdirs() }
            val photoFile = File(scansDir, "cam_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val result = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
            val scannedUris = result?.pages?.map { it.imageUri }.orEmpty()
            if (scannedUris.isNotEmpty()) {
                // Persist temporary Play Services URIs into app cache so reorder/conversion never loses read grant
                scope.launch {
                    val localUris = copyUrisToLocalScans(context, scannedUris)
                    if (localUris.isNotEmpty()) {
                        onPagesCaptured(localUris)
                    }
                }
            }
        }
    }

    val launchSmartScanner: () -> Unit = {
        val activity = context.findActivity()
        if (activity == null) {
            launchCameraFallback()
        } else {
            try {
                val options = GmsDocumentScannerOptions.Builder()
                    .setGalleryImportAllowed(true)
                    .setPageLimit(pageLimit)
                    .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                    .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                    .build()

                val scanner = GmsDocumentScanning.getClient(options)
                scanner.getStartScanIntent(activity)
                    .addOnSuccessListener { intentSender ->
                        try {
                            scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                        } catch (_: Exception) {
                            launchCameraFallback()
                        }
                    }
                    .addOnFailureListener {
                        // Fall back automatically if Play Services scanner module is unavailable on device
                        launchCameraFallback()
                    }
            } catch (_: Exception) {
                launchCameraFallback()
            }
        }
    }

    return remember(launchSmartScanner, launchCameraFallback) {
        DocumentScannerHandle(
            launchScanner = launchSmartScanner,
            launchCamera = launchCameraFallback
        )
    }
}

private suspend fun copyUrisToLocalScans(context: Context, sourceUris: List<Uri>): List<Uri> =
    withContext(Dispatchers.IO) {
        val scansDir = File(context.cacheDir, "scans").apply { if (!exists()) mkdirs() }
        val result = mutableListOf<Uri>()
        val ts = System.currentTimeMillis()
        sourceUris.forEachIndexed { index, sourceUri ->
            try {
                val destFile = File(scansDir, "scan_${ts}_$index.jpg")
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (destFile.exists() && destFile.length() > 0L) {
                    result.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destFile))
                }
            } catch (_: Exception) {
                // Keep original URI as fallback if copy fails
                result.add(sourceUri)
            }
        }
        result
    }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

