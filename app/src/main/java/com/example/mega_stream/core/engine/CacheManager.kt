package com.example.mega_stream.core.engine

import android.content.Context
import android.os.Environment
import androidx.core.content.ContextCompat
import com.example.mega_stream.core.storage.DatabaseHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File

object CacheManager {
    private val _fileReadyEvents = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val fileReadyEvents = _fileReadyEvents.asSharedFlow()

    fun notifyFileReady(handleId: String) {
        _fileReadyEvents.tryEmit(handleId)
    }

    fun getFolderCacheDir(context: Context, folderUrl: String): File {
        val folderHash = folderUrl.hashCode().toString()
        val baseDir = getOptimalCacheDir(context)
        val folderDir = File(baseDir, "folder_$folderHash")
        if (!folderDir.exists()) folderDir.mkdirs()
        return folderDir
    }

    fun getOptimalCacheDir(context: Context): File {
        val dbHelper = DatabaseHelper.getInstance(context)
        val userPath = dbHelper.getSetting("storage_path", "AUTO")
        
        if (userPath != "AUTO" && userPath.isNotEmpty()) {
            val userDir = File(userPath)
            if (userDir.exists() && userDir.canWrite()) {
                val finalDir = File(userDir, "mega_stream_v3")
                if (!finalDir.exists()) finalDir.mkdirs()
                return finalDir
            }
        }

        val externalFilesDirs = ContextCompat.getExternalFilesDirs(context, null)
        for (dir in externalFilesDirs) {
            if (dir != null) {
                val path = File(dir, "mega_stream_v3")
                if (!path.exists()) path.mkdirs()
                return path
            }
        }
        val internalCache = File(context.cacheDir, "mega_stream_v3")
        if (!internalCache.exists()) internalCache.mkdirs()
        return internalCache
    }

    fun clearFolderCache(folderDir: File) {
        if (StreamingWorker.getActiveFolderUrl().hashCode().toString() in folderDir.name) {
            return
        }
        if (folderDir.exists()) {
            folderDir.deleteRecursively()
        }
    }

    fun deleteAllCache(context: Context) {
        try {
            val baseDir = getOptimalCacheDir(context)
            if (baseDir.exists()) baseDir.deleteRecursively()
        } catch (e: Exception) {}
    }

    fun pruneCacheExcept(folderDir: File, keepHandles: Set<String>) {
        if (!folderDir.exists()) return
        val files = folderDir.listFiles()?.filter { it.isFile && it.name.startsWith("dl_") } ?: return
        val keepIds = keepHandles.map { it.split("#")[0] }.toSet()

        for (file in files) {
            val fileId = file.name.removePrefix("dl_").removeSuffix(".jpg")
            if (fileId !in keepIds) {
                file.delete()
            }
        }
    }
}
