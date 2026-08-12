package com.example.mega_stream.core.network

import android.content.Context
import android.util.Log
import com.example.mega_stream.core.storage.DatabaseHelper
import com.example.mega_stream.core.engine.MegaManager
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ConfigFetcher(private val context: Context) {
    private val dbHelper = DatabaseHelper.getInstance(context)

    companion object {
        const val DEFAULT_JSON_URL = "https://mega.nz/file/AhQR3AxC#ZNvUcirmWJeqlTQAjsODb0L0teZL87vdNGOxf_l-NxY"
    }

    /**
     * HIGH PERFORMANCE SYNC: Only blocks until DB is updated.
     * The heavy node pre-scanning happens in a fire-and-forget job.
     */
    suspend fun fetchAndSync(): Boolean = withContext(Dispatchers.IO) {
        val userUrl = dbHelper.getSetting("config_url", DEFAULT_JSON_URL)
        val finalUrl = if (userUrl.isEmpty()) DEFAULT_JSON_URL else userUrl
        
        Log.d("ConfigFetcher", "Starting sync from: $finalUrl")
        
        try {
            val cacheDir = context.cacheDir
            val expectedFile = File(cacheDir, "config.json")
            if (expectedFile.exists()) expectedFile.delete()

            // Fetch the central config JSON
            val success = MegaManager.downloadFile(finalUrl, cacheDir.absolutePath)
            
            if (success && expectedFile.exists()) {
                val body = expectedFile.readText().trim()
                Log.d("ConfigFetcher", "Downloaded JSON content (Raw): $body")
                
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
                    Log.e("ConfigFetcher", "Parsing failed: ${e.message}")
                }

                if (folderPairs.isNotEmpty()) {
                    // CRITICAL: Update DB immediately
                    dbHelper.mergeFolders(folderPairs)
                    Log.d("ConfigFetcher", "DB updated with ${folderPairs.size} folders.")
                    
                    // PERFORMANCE: Pre-scan in background without blocking the caller (Splash Screen)
                    CoroutineScope(Dispatchers.IO).launch {
                        folderPairs.forEach { folder ->
                            try {
                                Log.d("ConfigFetcher", "Background pre-scanning: ${folder.first}")
                                MegaManager.listSharedFolder(folder.second)
                            } catch (e: Exception) { /* ignore */ }
                        }
                    }
                    
                    expectedFile.delete()
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            Log.e("ConfigFetcher", "Sync Error: ${e.message}")
        }
        
        return@withContext dbHelper.getAllFolders().isNotEmpty()
    }
}
