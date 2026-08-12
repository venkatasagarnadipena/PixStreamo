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
        const val DEFAULT_JSON_URL = "https://mega.nz/file/AhQR3AxC#ZNvUcirmWJeqlTQAjsODb0L0teZL87vdNGOxf_l-NxY"
        private val syncMutex = Mutex()
        private val globalScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        
        // SYNC COOLDOWN: Prevent redundant syncing within 3 minutes
        private var lastSyncTime = 0L
    }

    fun startAsyncImport() {
        globalScope.launch {
            fetchAndSync()
        }
    }

    /**
     * OPTIMIZED SYNC: Checks for cooldown and ensures data integrity.
     */
    suspend fun fetchAndSync(): Boolean = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (now - lastSyncTime < 180000) { // 3 minutes cooldown
            PixLog.d("ConfigFetcher", "Sync skipped (Cooldown active)")
            return@withContext false
        }

        syncMutex.withLock {
            val userUrl = dbHelper.getSetting("config_url", DEFAULT_JSON_URL)
            val finalUrl = if (userUrl.isEmpty()) DEFAULT_JSON_URL else userUrl
            
            PixLog.i("ConfigFetcher", "Sync starting...")
            
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
                        PixLog.e("ConfigFetcher", "Parse Error", e)
                    }

                    if (folderPairs.isNotEmpty()) {
                        // ENSURE ATOMICITY: Use NonCancellable to prevent DB corruption during screen transition
                        withContext(NonCancellable) {
                            dbHelper.mergeFolders(folderPairs)
                        }
                        lastSyncTime = System.currentTimeMillis()
                        expectedFile.delete()
                        return@withContext true
                    }
                }
            } catch (e: Exception) {
                PixLog.e("ConfigFetcher", "Sync Error", e)
            }
            return@withContext false
        }
    }
}
