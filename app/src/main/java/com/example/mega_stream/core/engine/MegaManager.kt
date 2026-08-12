package com.example.mega_stream.core.engine

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.mega_stream.core.network.PixLog
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

data class SharedMediaItem(
    val handle: String,
    val name: String,
    val type: String,
    val fa: String? = null
)

object MegaManager {
    @Volatile
    private var isInitialized = false
    private val mutex = Mutex()

    fun init(context: Context) {
        if (isInitialized) return
        try {
            val startTime = System.currentTimeMillis()
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }
            isInitialized = true
            PixLog.perf("MegaManager", "EngineInitTime", "${System.currentTimeMillis() - startTime}ms")
        } catch (e: Exception) {
            PixLog.e("MegaManager", "Python Init Error", e)
        }
    }

    /**
     * PRIORITY QUEUE: All calls to Python are now strictly sequential.
     * This prevents 512MB devices from crashing the Python engine with multiple requests.
     */
    suspend fun listSharedFolder(url: String): List<SharedMediaItem> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val startTime = System.currentTimeMillis()
            PixLog.d("MegaManager", "Fetching folder: ${PixLog.mask(url)}")
            
            try {
                val py = Python.getInstance()
                val module = py.getModule("mega_manager")
                val resultJson = module.callAttr("list_shared_folder", url).toString()
                val result = JSONObject(resultJson)
                
                if (result.getString("status") != "success") {
                    return@withLock emptyList<SharedMediaItem>()
                }
                
                val nodesArray = result.getJSONArray("nodes")
                val itemsList = mutableListOf<SharedMediaItem>()
                for (i in 0 until nodesArray.length()) {
                    val nodeObj = nodesArray.getJSONObject(i)
                    itemsList.add(SharedMediaItem(
                        handle = nodeObj.getString("h"),
                        name = nodeObj.getString("name"),
                        type = nodeObj.getString("type"),
                        fa = nodeObj.optString("fa", "")
                    ))
                }
                
                PixLog.perf("MegaManager", "FolderFetchTime", "${System.currentTimeMillis() - startTime}ms")
                itemsList
            } catch (e: Exception) {
                PixLog.e("MegaManager", "listSharedFolder failed", e)
                emptyList<SharedMediaItem>()
            }
        }
    }

    suspend fun downloadFile(url: String, destPath: String, forceFilename: String? = null): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val startTime = System.currentTimeMillis()
            try {
                val py = Python.getInstance()
                val module = py.getModule("mega_manager")
                val resultJson = module.callAttr("download_file", url, destPath, forceFilename).toString()
                val result = JSONObject(resultJson)
                val success = result.getString("status") == "success"
                
                if (success) {
                    val handleId = if (url.contains("#")) url.split("#")[0] else url
                    CacheManager.notifyFileReady(handleId)
                    PixLog.perf("MegaManager", "FileDownloadTime", "${System.currentTimeMillis() - startTime}ms")
                }
                success
            } catch (e: Exception) {
                PixLog.e("MegaManager", "downloadFile failed", e)
                false
            }
        }
    }
}
