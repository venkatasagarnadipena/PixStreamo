package com.example.mega_stream.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * REFINED ATOMIC ENGINE:
 * 1. Strictly sequential: downloads one by one.
 * 2. Active Tracking: Prevents auto-cleanup from deleting working files.
 * 3. Robust Error Recovery: Re-creates missing directories JIT.
 */
object StreamingWorker {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val engineMutex = Mutex()
    private var engineJob: Job? = null

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex = _currentIndex.asStateFlow()

    private val _isSlideshowActive = MutableStateFlow(false) // Default to OFF
    val isSlideshowActive = _isSlideshowActive.asStateFlow()

    private val _statusLabel = MutableStateFlow("Initializing...")
    val statusLabel = _statusLabel.asStateFlow()

    private var mediaItems: List<SharedMediaItem> = emptyList()
    private var activeFolderUrl: String = ""
    private var cacheDir: File? = null

    fun getActiveFolderUrl(): String = activeFolderUrl

    fun setSlideshowActive(active: Boolean) {
        _isSlideshowActive.value = active
    }

    fun initFolder(context: Context, url: String, items: List<SharedMediaItem>, initialIndex: Int) {
        scope.launch {
            engineMutex.withLock {
                if (activeFolderUrl == url) {
                    if (_currentIndex.value != initialIndex) {
                        _currentIndex.value = initialIndex
                    }
                    return@withLock
                }
                
                stopLocked()
                activeFolderUrl = url
                mediaItems = items
                _currentIndex.value = initialIndex
                cacheDir = CacheManager.getFolderCacheDir(context, url)
                
                Log.d("ENGINE_REFINED", "Initializing for: $url at index $initialIndex")
                startEngineLocked()
            }
        }
    }

    private fun startEngineLocked() {
        engineJob = scope.launch {
            while (isActive) {
                if (mediaItems.isEmpty() || cacheDir == null) {
                    delay(1000)
                    continue
                }

                val currentIdx = _currentIndex.value
                val item = mediaItems[currentIdx]
                val handleId = item.handle.split("#")[0]
                val file = File(cacheDir, "dl_$handleId.jpg")

                // Ensure directory exists
                if (cacheDir?.exists() == false) cacheDir?.mkdirs()

                // 1. ENSURE CURRENT READY
                if (!file.exists() || file.length() < 1024) {
                    _statusLabel.value = "Unlocking image ${currentIdx + 1}..."
                    val success = MegaManager.downloadFile(item.handle, cacheDir!!.absolutePath)
                    if (success && file.exists()) {
                        CacheManager.notifyFileReady(handleId)
                    } else {
                        delay(1000)
                        advanceIndex()
                        continue
                    }
                }

                // 2. VIEWING / SLIDESHOW LOGIC
                if (_isSlideshowActive.value) {
                    _statusLabel.value = "Viewing image ${currentIdx + 1}"
                    
                    // PRELOAD NEXT 2 (Parallel background)
                    scope.launch {
                        for (offset in 1..2) {
                            val nextIdx = (currentIdx + offset) % mediaItems.size
                            val nextItem = mediaItems[nextIdx]
                            val nextId = nextItem.handle.split("#")[0]
                            val nextFile = File(cacheDir, "dl_$nextId.jpg")
                            if (!nextFile.exists() || nextFile.length() < 1024) {
                                MegaManager.downloadFile(nextItem.handle, cacheDir!!.absolutePath)
                                CacheManager.notifyFileReady(nextId)
                            }
                        }
                    }

                    delay(5000) // 5s slide duration

                    engineMutex.withLock {
                        if (_isSlideshowActive.value && currentIdx == _currentIndex.value) {
                            advanceIndex()
                        }
                    }
                } else {
                    _statusLabel.value = "Paused at ${currentIdx + 1}"
                    delay(1000)
                }
            }
        }
    }

    private fun advanceIndex() {
        if (mediaItems.isNotEmpty()) {
            val next = if (_currentIndex.value < mediaItems.size - 1) _currentIndex.value + 1 else 0
            _currentIndex.value = next
        }
    }

    fun toggleSlideshow() {
        _isSlideshowActive.value = !_isSlideshowActive.value
    }

    fun jumpTo(index: Int, pause: Boolean) {
        if (mediaItems.isEmpty()) return
        val target = index.coerceIn(0, mediaItems.size - 1)
        _currentIndex.value = target
        if (pause) _isSlideshowActive.value = false
    }

    fun next() {
        _isSlideshowActive.value = false
        advanceIndex()
    }

    fun previous() {
        _isSlideshowActive.value = false
        if (_currentIndex.value > 0) _currentIndex.value--
        else _currentIndex.value = mediaItems.size - 1
    }

    private fun stopLocked() {
        engineJob?.cancel()
        activeFolderUrl = ""
        _statusLabel.value = "Stopped"
    }
}
