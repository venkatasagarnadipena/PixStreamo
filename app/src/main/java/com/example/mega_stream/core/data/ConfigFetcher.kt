package com.example.mega_stream.core.data

import android.content.Context
import android.util.Log
import com.example.mega_stream.core.local.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

class ConfigFetcher(private val context: Context) {
    private val dbHelper = DatabaseHelper(context)

    // DEFAULT FALLBACK URL
    private val DEFAULT_JSON_URL = "https://mega.nz/file/AhQR3AxC#ZNvUcirmWJeqlTQAjsODb0L0teZL87vdNGOxf_l-NxY"

    /**
     * DYNAMIC SYNC:
     * Reads URL from DB settings, falls back to hardcoded default if empty/invalid.
     */
    suspend fun fetchAndSync(): Boolean = withContext(Dispatchers.IO) {
        val userUrl = dbHelper.getSetting("config_url", DEFAULT_JSON_URL)
        val finalUrl = if (userUrl.isEmpty()) DEFAULT_JSON_URL else userUrl
        
        Log.d("ConfigFetcher", "Starting sync from: $finalUrl")
        
        try {
            val cacheDir = context.cacheDir
            val expectedFile = File(cacheDir, "config.json")
            if (expectedFile.exists()) expectedFile.delete()

            // Call Python download with the configured URL
            val success = MegaManager.downloadFile(finalUrl, cacheDir.absolutePath)
            
            if (success && expectedFile.exists()) {
                val body = expectedFile.readText()
                val jsonArray = JSONArray(body)
                val folders = mutableListOf<Pair<String, String>>()
                
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    folders.add(Pair(obj.getString("folder"), obj.getString("url")))
                }
                
                dbHelper.mergeFolders(folders)
                expectedFile.delete()
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e("ConfigFetcher", "Sync Error: ${e.message}")
        }
        
        return@withContext dbHelper.getAllFolders().size > 0
    }
}
