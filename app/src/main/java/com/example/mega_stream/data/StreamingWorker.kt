package com.example.mega_stream.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * ROLLING WINDOW ENGINE:
 * 1. Maintains a 30-image window around the current index.
 * 2. Sequential downloads to respect low-end hardware (1GB RAM).
 * 3. Prunes old images to maintain a small storage footprint.
 */
object StreamingWorker {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val engineMutex = Mutex()
    private var engineJob: Job? = null

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex = _currentIndex.asStateFlow()

    private val _isSlideshowActive = MutableStateFlow(false)
    val isSlideshowActive = _isSlideshowActive.asStateFlow()

    private val _statusLabel = MutableStateFlow("Initializing...")
    val statusLabel = _statusLabel.asStateFlow()

    private var mediaItems: List<SharedMediaItem> = emptyList()
    private var activeFolderUrl: String = ""
    private var cacheDir: File? = null

    private const val WINDOW_SIZE = 30

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
                
                Log.d("STREAMING_WORKER", "Initializing Rolling Window for: $url at index $initialIndex")
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

                val currentPos = _currentIndex.value
                
                // Define the 30-image window to KEEP
                // We keep some previous images too so back-scrolling is smooth
                val windowStart = (currentPos - 5).coerceAtLeast(0)
                val windowEnd = (windowStart + WINDOW_SIZE).coerceAtMost(mediaItems.size - 1)
                val windowRange = windowStart..windowEnd

                // 1. SEQUENTIAL DOWNLOAD OF THE WINDOW
                for (i in windowRange) {
                    if (!isActive) break
                    
                    val item = mediaItems[i]
                    val handleId = item.handle.split("#")[0]
                    val file = File(cacheDir, "dl_$handleId.jpg")

                    if (!file.exists() || file.length() < 1024) {
                        _statusLabel.value = "Downloading ${i + 1}/${mediaItems.size}..."
                        val success = MegaManager.downloadFile(item.handle, cacheDir!!.absolutePath)
                        if (success && file.exists()) {
                            CacheManager.notifyFileReady(handleId)
                        }
                    }
                    
                    // If the user jumped to a new position while we were downloading, pivot immediately
                    if (_currentIndex.value != currentPos) break
                }

                // 2. PRUNING: Only run pruning if we've moved significantly to avoid constant disk IO
                val keepHandles = windowRange.map { mediaItems[it].handle }.toSet()
                CacheManager.pruneCacheExcept(cacheDir!!, keepHandles)

                // 3. SLIDESHOW / WAITING LOGIC
                if (_isSlideshowActive.value) {
                    _statusLabel.value = "Slideshow Active: ${currentPos + 1}"
                    delay(8000) // 8s slide duration
                    engineMutex.withLock {
                        if (_isSlideshowActive.value && currentPos == _currentIndex.value) {
                            advanceIndex()
                        }
                    }
                } else {
                    _statusLabel.value = "Ready at ${currentPos + 1}"
                    // Small delay to prevent tight loop if window is already full
                    delay(500)
                    
                    // Wait for an index change or slideshow activation
                    while (isActive && _currentIndex.value == currentPos && !_isSlideshowActive.value) {
                        delay(200)
                    }
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
        if (_currentIndex.value != target) {
            _currentIndex.value = target
        }
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
