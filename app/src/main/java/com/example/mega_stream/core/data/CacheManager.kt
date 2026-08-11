package com.example.mega_stream.core.data

import android.content.Context
import android.os.Environment
import androidx.core.content.ContextCompat
import java.io.File
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import android.util.Log
import com.example.mega_stream.core.local.DatabaseHelper

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
        val dbHelper = DatabaseHelper(context)
        val userPath = dbHelper.getSetting("storage_path", "AUTO")
        
        if (userPath != "AUTO" && userPath.isNotEmpty()) {
            val userDir = File(userPath)
            if (userDir.exists() && userDir.canWrite()) {
                val finalDir = File(userDir, "mega_stream_v3")
                if (!finalDir.exists()) finalDir.mkdirs()
                return finalDir
            }
            Log.w("CacheManager", "User-defined path $userPath is invalid or not writable. Falling back to AUTO.")
        }

        val externalFilesDirs = ContextCompat.getExternalFilesDirs(context, null)
        for (dir in externalFilesDirs) {
            if (dir != null) {
                val isPrimary = dir.absolutePath.contains("emulated/0")
                if (!isPrimary || Environment.isExternalStorageRemovable(dir)) {
                    val path = File(dir, "mega_stream_v3")
                    if (!path.exists()) path.mkdirs()
                    return path
                }
            }
        }
        val internalCache = File(context.cacheDir, "mega_stream_v3")
        if (!internalCache.exists()) internalCache.mkdirs()
        return internalCache
    }

    fun enforceFolderFIFO(folderDir: File) {
        if (!folderDir.exists()) return
        val files = folderDir.listFiles()?.filter { it.isFile && it.name.startsWith("dl_") }
            ?.sortedBy { it.lastModified() }
        
        if (files != null && files.size > 30) {
            val toDelete = 10 
            for (i in 0 until toDelete) {
                files[i].delete()
            }
        }
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
            if (baseDir.exists()) {
                baseDir.deleteRecursively()
                Log.d("CacheManager", "Full cache purge complete.")
            }
        } catch (e: Exception) {
            Log.e("CacheManager", "Failed to purge all cache", e)
        }
    }

    fun pruneCacheExcept(folderDir: File, keepHandles: Set<String>) {
        if (!folderDir.exists()) return
        
        val files = folderDir.listFiles()?.filter { it.isFile && it.name.startsWith("dl_") } ?: return
        val keepIds = keepHandles.map { it.split("#")[0] }.toSet()

        for (file in files) {
            val fileId = file.name.removePrefix("dl_").removeSuffix(".jpg")
            if (fileId !in keepIds) {
                file.delete()
                Log.d("CacheManager", "Pruned: ${file.name}")
            }
        }
    }
}
