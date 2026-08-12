package com.example.mega_stream.core.network

import android.content.Context
import com.example.mega_stream.core.storage.DatabaseHelper
import com.example.mega_stream.core.engine.MegaManager
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ConfigFetcher(private val context: Context) {
    private val dbHelper = DatabaseHelper.getInstance(context)

    companion object {
        // PERMANENTLY REMOVED: Default URL is now empty to ensure no auto-loading
        const val DEFAULT_JSON_URL = ""
        private val syncMutex = Mutex()
        private val globalScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private var lastSyncTime = 0L
    }

    fun startAsyncImport() {
        globalScope.launch {
            fetchAndSync()
        }
    }

    /**
     * PRODUCTION SYNC: Only processes user-provided URLs.
     */
    suspend fun fetchAndSync(): Boolean = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (now - lastSyncTime < 180000) return@withContext false

        syncMutex.withLock {
            val userUrl = dbHelper.getSetting("config_url", "")
            
            // If there is no user-provided URL, we abort immediately.
            if (userUrl.isEmpty()) {
                return@withLock false
            }
            
            val finalUrl = userUrl
            
            try {
                val cacheDir = context.cacheDir
                val configFileName = "config.json"
                val expectedFile = File(cacheDir, configFileName)
                if (expectedFile.exists()) expectedFile.delete()

                val success = MegaManager.downloadFile(finalUrl, cacheDir.absolutePath, configFileName)
                
                var fileFound = false
                repeat(10) { 
                    if (expectedFile.exists() && expectedFile.length() > 5) {
                        fileFound = true
                        return@repeat
                    }
                    delay(500)
                }
                
                if (success && fileFound) {
                    val body = expectedFile.readText().trim()
                    val folderPairs = mutableListOf<Pair<String, String>>()
                    try {
                        if (body.startsWith("[")) {
                            val jsonArray = JSONArray(body)
                            for (i in 0 until jsonArray.length()) {
                                val obj = jsonArray.getJSONObject(i)
                                val name = if (obj.has("folder")) obj.getString("folder") else obj.getString("name")
                                val url = obj.getString("url")
                                folderPairs.add(Pair(name, url))
                            }
                        } else if (body.startsWith("{")) {
                            val jsonObj = JSONObject(body)
                            val keys = jsonObj.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                val value = jsonObj.get(key)
                                if (value is String) {
                                    folderPairs.add(Pair(key, value))
                                } else if (value is JSONObject) {
                                    val url = value.optString("url", "")
                                    if (url.isNotEmpty()) folderPairs.add(Pair(key, url))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // All logs removed for final production
                    }

                    if (folderPairs.isNotEmpty()) {
                        withContext(NonCancellable) {
                            dbHelper.mergeFolders(folderPairs)
                        }
                        lastSyncTime = System.currentTimeMillis()
                        expectedFile.delete()
                        return@withContext true
                    }
                }
            } catch (e: Exception) {
                // All logs removed for final production
            }
            return@withContext false
        }
    }
}
