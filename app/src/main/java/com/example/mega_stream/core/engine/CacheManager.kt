package com.example.mega_stream.core.engine

import android.content.Context
import com.example.mega_stream.core.network.PixLog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File

object CacheManager {
    private val _fileReadyEvents = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val fileReadyEvents: SharedFlow<String> = _fileReadyEvents.asSharedFlow()

    suspend fun notifyFileReady(handleId: String) {
        _fileReadyEvents.emit(handleId)
    }

    fun getFolderCacheDir(context: Context, folderUrl: String): File {
        val hash = folderUrl.hashCode().toString()
        val dir = File(getOptimalCacheDir(context), "f_$hash")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getOptimalCacheDir(context: Context): File {
        val dbHelper = com.example.mega_stream.core.storage.DatabaseHelper.getInstance(context)
        val savedPath = dbHelper.getSetting("cache_path", "")
        
        if (savedPath.isNotEmpty()) {
            val customDir = File(savedPath, "PixStreamoCache")
            try {
                if (!customDir.exists()) customDir.mkdirs()
                if (customDir.canWrite()) {
                    return customDir
                }
            } catch (e: Exception) {
                PixLog.e("CacheManager", "Storage path failed")
            }
        }
        
        val internalDir = File(context.cacheDir, "PixStreamoCache")
        if (!internalDir.exists()) internalDir.mkdirs()
        return internalDir
    }

    fun clearFolderCache(folderDir: File) {
        try {
            folderDir.listFiles()?.forEach { it.delete() }
            folderDir.delete()
        } catch (e: Exception) {
            PixLog.e("CacheManager", "Clear error")
        }
    }

    fun deleteAllCache(context: Context) {
        val root = getOptimalCacheDir(context)
        root.deleteRecursively()
    }

    fun pruneCacheExcept(folderDir: File, keepHandles: Set<String>) {
        try {
            val files = folderDir.listFiles() ?: return
            val keepIds = keepHandles.map { it.split("#")[0] }.toSet()
            
            files.forEach { file ->
                val fileName = file.name
                if (fileName.startsWith("dl_")) {
                    val id = fileName.removePrefix("dl_").removeSuffix(".jpg")
                    if (!keepIds.contains(id)) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            PixLog.e("CacheManager", "Prune error")
        }
    }
}
