package com.kletaq.app.core.notes

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface ModuleScanState {
    object NoNotes : ModuleScanState
    data class Scanning(val progress: Float, val scannedPages: Int, val totalPages: Int) : ModuleScanState
    data class Ready(val manifest: ModuleOcrManifest, val totalPages: Int) : ModuleScanState
    data class ScanFailed(val error: String) : ModuleScanState
}

/**
 * Centralized, shared state repository for student notes availability and scanning.
 * Provides a single source of truth across LessonBottomSheet, LessonStudyScreen, and AI Tutor.
 */
object ModuleNotesRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val states = mutableMapOf<String, MutableStateFlow<ModuleScanState>>()
    private val mutexes = mutableMapOf<String, Mutex>()
    private val globalLock = Any()

    private fun moduleKey(subjectCode: String, moduleIndex: Int): String {
        val cleanSubject = subjectCode.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
        return "${cleanSubject}_m$moduleIndex"
    }

    private fun getOrCreateStateFlow(key: String): MutableStateFlow<ModuleScanState> {
        synchronized(globalLock) {
            return states.getOrPut(key) { MutableStateFlow(ModuleScanState.NoNotes) }
        }
    }

    private fun getOrCreateMutex(key: String): Mutex {
        synchronized(globalLock) {
            return mutexes.getOrPut(key) { Mutex() }
        }
    }

    /**
     * Gets or observes the current scan state for a subject and module.
     */
    fun getScanState(context: Context, subjectCode: String, moduleIndex: Int): StateFlow<ModuleScanState> {
        val key = moduleKey(subjectCode, moduleIndex)
        val flow = getOrCreateStateFlow(key)

        val notePages = CustomNotesManager.getNotesForModule(context, subjectCode, moduleIndex)
        if (notePages.isEmpty()) {
            flow.value = ModuleScanState.NoNotes
        } else if (flow.value is ModuleScanState.NoNotes) {
            val cached = NotesTextExtractor.loadCachedManifest(context, subjectCode, moduleIndex)
            if (cached != null && cached.pages.size == notePages.size) {
                flow.value = ModuleScanState.Ready(cached, notePages.size)
            } else {
                ensureScanned(context, subjectCode, moduleIndex)
            }
        }
        return flow.asStateFlow()
    }

    /**
     * Triggers OCR scan if not already scanned or in progress.
     */
    fun ensureScanned(context: Context, subjectCode: String, moduleIndex: Int, forceRefresh: Boolean = false) {
        val key = moduleKey(subjectCode, moduleIndex)
        val flow = getOrCreateStateFlow(key)
        val mutex = getOrCreateMutex(key)

        scope.launch {
            if (!forceRefresh && flow.value is ModuleScanState.Ready) {
                return@launch
            }
            if (flow.value is ModuleScanState.Scanning) {
                return@launch
            }

            mutex.withLock {
                val notePages = CustomNotesManager.getNotesForModule(context, subjectCode, moduleIndex)
                if (notePages.isEmpty()) {
                    flow.value = ModuleScanState.NoNotes
                    return@withLock
                }

                if (!forceRefresh) {
                    val cached = NotesTextExtractor.loadCachedManifest(context, subjectCode, moduleIndex)
                    if (cached != null && cached.pages.size == notePages.size) {
                        flow.value = ModuleScanState.Ready(cached, notePages.size)
                        return@withLock
                    }
                }

                flow.value = ModuleScanState.Scanning(0f, 0, notePages.size)

                try {
                    val manifest = NotesTextExtractor.extractTextForModule(
                        context = context,
                        subjectCode = subjectCode,
                        moduleIndex = moduleIndex,
                        onProgress = { current, total ->
                            val progress = if (total > 0) current.toFloat() / total.toFloat() else 0f
                            flow.value = ModuleScanState.Scanning(progress, current, total)
                        }
                    )
                    flow.value = ModuleScanState.Ready(manifest, manifest.pages.size)
                } catch (e: Exception) {
                    flow.value = ModuleScanState.ScanFailed(e.localizedMessage ?: "OCR scan failed")
                }
            }
        }
    }

    /**
     * Called whenever notes are uploaded or deleted in CustomNotesManager to invalidate/re-scan.
     */
    fun notifyNotesChanged(context: Context, subjectCode: String, moduleIndex: Int) {
        val key = moduleKey(subjectCode, moduleIndex)
        val flow = getOrCreateStateFlow(key)
        val notePages = CustomNotesManager.getNotesForModule(context, subjectCode, moduleIndex)
        if (notePages.isEmpty()) {
            flow.value = ModuleScanState.NoNotes
        } else {
            ensureScanned(context, subjectCode, moduleIndex, forceRefresh = true)
        }
    }
}
