package com.example.mega_stream.data

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

object StreamingWorker {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val downloadMutex = Mutex()
    private var downloadJob: Job? = null

    private val _isSlideshowActive = MutableStateFlow(false)
    val isSlideshowActive = _isSlideshowActive.asStateFlow()

    private val _isWindowReady = MutableStateFlow(false)
    val isWindowReady = _isWindowReady.asStateFlow()

    private val _activeFolderUrl = MutableStateFlow("")
    fun getActiveFolderUrl() = _activeFolderUrl.value

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex = _currentIndex.asStateFlow()

    private var mediaItems: List<SharedMediaItem> = emptyList()
    private var cacheDir: File? = null

    val statusLabel = MutableStateFlow("")

    fun toggleSlideshow() {
        _isSlideshowActive.value = !_isSlideshowActive.value
    }

    fun setSlideshowActive(active: Boolean) {
        _isSlideshowActive.value = active
    }

    fun initFolder(context: Context, url: String, items: List<SharedMediaItem>, initialIndex: Int = 0) {
        if (_activeFolderUrl.value == url) {
            _currentIndex.value = initialIndex
            return 
        }

        _activeFolderUrl.value = url
        mediaItems = items
        _currentIndex.value = initialIndex
        _isWindowReady.value = false
        cacheDir = CacheManager.getFolderCacheDir(context, url)

        downloadJob?.cancel()
        downloadJob = scope.launch {
            while (isActive) {
                maintainWindow()
                delay(2000)
            }
        }
    }

    fun jumpTo(index: Int) {
        if (index in mediaItems.indices) {
            _currentIndex.value = index
        }
    }

    private suspend fun maintainWindow() {
        val current = _currentIndex.value
        val total = mediaItems.size
        if (total == 0) return

        val windowSize = 30
        val bufferBehind = 5
        val start = (current - bufferBehind).coerceAtLeast(0)
        val end = (start + windowSize).coerceAtMost(total)

        val windowItems = mediaItems.subList(start, end)
        val windowHandles = windowItems.map { it.handle }.toSet()

        cacheDir?.let { CacheManager.pruneCacheExcept(it, windowHandles) }

        var readyCount = 0
        for (item in windowItems) {
            val file = File(cacheDir, "dl_${item.handle}.jpg")
            if (file.exists() && file.length() > 0) {
                readyCount++
                continue
            }

            downloadMutex.withLock {
                val success = MegaManager.downloadFile(_activeFolderUrl.value, cacheDir!!.absolutePath, item.handle)
                if (success) {
                    readyCount++
                    CacheManager.notifyFileReady(item.handle)
                }
            }
            yield()
        }

        _isWindowReady.value = (readyCount >= windowItems.size || readyCount >= 10)
    }

    fun next() {
        if (mediaItems.isEmpty()) return
        val next = (_currentIndex.value + 1)
        if (next < mediaItems.size) {
            _currentIndex.value = next
        } else {
            _isSlideshowActive.value = false
        }
    }

    fun previous() {
        val prev = (_currentIndex.value - 1)
        if (prev >= 0) {
            _currentIndex.value = prev
        }
    }
}
