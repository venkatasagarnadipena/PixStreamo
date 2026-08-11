package com.example.mega_stream.core.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

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

    private val _isWindowReady = MutableStateFlow(false)
    val isWindowReady = _isWindowReady.asStateFlow()

    private var mediaItems: List<SharedMediaItem> = emptyList()
    private var activeFolderUrl: String = ""
    private var cacheDir: File? = null
    private var lastPrunedIndex: Int = -1

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
                _isWindowReady.value = false 
                cacheDir = CacheManager.getFolderCacheDir(context, url)
                
                Log.d("STREAMING_WORKER", "Initializing Rolling Window for: $url at index $initialIndex")
                startEngineLocked()
            }
        }
    }

    private fun startEngineLocked() {
        engineJob = scope.launch {
            while (isActive) {
                val items = mediaItems
                val currentCacheDir = cacheDir
                
                if (items.isEmpty() || currentCacheDir == null) {
                    delay(1000)
                    continue
                }

                val currentPos = _currentIndex.value
                val windowStart = (currentPos - 5).coerceAtLeast(0)
                val windowEnd = (windowStart + WINDOW_SIZE).coerceAtMost(items.size - 1)
                val windowRange = windowStart..windowEnd

                for (i in windowRange) {
                    if (!isActive) break
                    if (i >= items.size) break
                    
                    val item = items[i]
                    val handleId = item.handle.split("#")[0]
                    val file = File(currentCacheDir, "dl_$handleId.jpg")

                    if (!file.exists() || file.length() < 1024) {
                        _isWindowReady.value = false
                        _statusLabel.value = "Downloading ${i + 1}/${items.size}..."
                        
                        val success = withContext(Dispatchers.IO) {
                            MegaManager.downloadFile(item.handle, currentCacheDir.absolutePath)
                        }
                        
                        if (success && file.exists()) {
                            CacheManager.notifyFileReady(handleId)
                        }
                        yield() 
                    }
                    
                    if (_currentIndex.value != currentPos) break
                }

                if (_currentIndex.value == currentPos) {
                    val isFullyReady = if (items.isEmpty()) true else {
                        windowRange.all { idx ->
                            if (idx >= items.size) true 
                            else {
                                val id = items[idx].handle.split("#")[0]
                                File(currentCacheDir, "dl_$id.jpg").exists() 
                            }
                        }
                    }
                    _isWindowReady.value = isFullyReady
                }

                if (Math.abs(currentPos - lastPrunedIndex) >= 5) {
                    val keepHandles = windowRange.filter { it < items.size }.map { items[it].handle }.toSet()
                    CacheManager.pruneCacheExcept(currentCacheDir, keepHandles)
                    lastPrunedIndex = currentPos
                }

                if (_isSlideshowActive.value) {
                    val statusText = if (_statusLabel.value == "END_OF_SHOW") "END_OF_SHOW" else "Slideshow Active: ${currentPos + 1}"
                    _statusLabel.value = statusText
                    
                    delay(8000) 
                    engineMutex.withLock {
                        if (_isSlideshowActive.value && currentPos == _currentIndex.value) {
                            advanceIndex()
                        }
                    }
                } else {
                    if (_statusLabel.value != "END_OF_SHOW") {
                        _statusLabel.value = "Ready at ${currentPos + 1}"
                    }
                    delay(500)
                    while (isActive && _currentIndex.value == currentPos && !_isSlideshowActive.value && _statusLabel.value != "END_OF_SHOW") {
                        delay(200)
                    }
                }
            }
        }
    }

    private fun advanceIndex() {
        if (mediaItems.isNotEmpty()) {
            val current = _currentIndex.value
            val next = current + 1
            if (next < mediaItems.size) {
                _currentIndex.value = next
            } else {
                _isSlideshowActive.value = false
                _statusLabel.value = "END_OF_SHOW"
            }
        }
    }

    fun toggleSlideshow() {
        if (_statusLabel.value == "END_OF_SHOW") {
            _statusLabel.value = "Resuming..."
            _currentIndex.value = 0
        }
        _isSlideshowActive.value = !_isSlideshowActive.value
    }

    fun jumpTo(index: Int, pause: Boolean) {
        if (mediaItems.isEmpty()) return
        val target = index.coerceIn(0, mediaItems.size - 1)
        
        if (_statusLabel.value == "END_OF_SHOW") {
            _statusLabel.value = "Ready"
        }

        if (_currentIndex.value != target) {
            _currentIndex.value = target
            _isWindowReady.value = false 
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
