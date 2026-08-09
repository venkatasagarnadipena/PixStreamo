package com.example.mega_stream.data

import android.content.Context
import android.os.Environment
import androidx.core.content.ContextCompat
import java.io.File
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import android.util.Log
import com.example.mega_stream.data.local.DatabaseHelper

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

    /**
     * DYNAMIC STORAGE DETECTION:
     * 1. Try user-defined path from Database.
     * 2. Fall back to automatic detection (Removable -> Emulated -> Internal).
     */
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

        // AUTO DETECTION LOGIC
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
}
