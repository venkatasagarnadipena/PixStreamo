package com.example.mega_stream.core.network

import android.content.Context
import android.util.Log
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
        private const val TAG = "PIX_SYNC"
        private val syncMutex = Mutex()
        private val globalScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    fun startAsyncImport() {
        globalScope.launch {
            fetchAndSync()
        }
    }

    /**
     * STABLE SYNC: Uses forced filenames to ensure Kotlin can find the JSON.
     */
    suspend fun fetchAndSync(): Boolean = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val userUrl = dbHelper.getSetting("config_url", DEFAULT_JSON_URL)
            val finalUrl = if (userUrl.isEmpty()) DEFAULT_JSON_URL else userUrl
            
            Log.i(TAG, "[SYNC_START] URL: $finalUrl")
            
            try {
                val cacheDir = context.cacheDir
                val configFileName = "config.json"
                val expectedFile = File(cacheDir, configFileName)
                
                if (expectedFile.exists()) expectedFile.delete()

                // Step 1: Download via Python with FORCED filename
                // This fixes the bug where new URLs used their original names (e.g. Validation.json)
                val success = MegaManager.downloadFile(finalUrl, cacheDir.absolutePath, configFileName)
                
                // Step 2: ROBUST FILE CHECK
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
                    Log.i(TAG, "[STEP 2] Success reading JSON. Content: $body")
                    
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
                        Log.e(TAG, "[PARSE_ERROR] ${e.message}")
                    }

                    if (folderPairs.isNotEmpty()) {
                        dbHelper.mergeFolders(folderPairs)
                        Log.i(TAG, "[STEP 3] Saved ${folderPairs.size} folders to DB.")
                        expectedFile.delete()
                        return@withContext true
                    }
                } else {
                    Log.e(TAG, "[SYNC_FAIL] Engine=$success, FileExists=${expectedFile.exists()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "[CRITICAL] Sync Error: ${e.message}")
            }
            
            return@withContext false
        }
    }
}
