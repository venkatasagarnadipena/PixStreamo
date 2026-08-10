package com.example.mega_stream.data

import android.content.Context
import android.util.Log
import com.example.mega_stream.data.local.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

object ConfigFetcher {
    private val client = OkHttpClient()

    suspend fun fetchAndSync(context: Context, url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext false
            
            val jsonString = response.body?.string() ?: return@withContext false
            val dbHelper = DatabaseHelper(context)
            val folderPairs = mutableListOf<Pair<String, String>>()

            try {
                if (jsonString.trim().startsWith("[")) {
                    val array = JSONArray(jsonString)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        folderPairs.add(Pair(obj.getString("name"), obj.getString("url")))
                    }
                } else {
                    val obj = JSONObject(jsonString)
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        folderPairs.add(Pair(key, obj.getString(key)))
                    }
                }
            } catch (e: Exception) {
                Log.e("ConfigFetcher", "JSON Parsing failed", e)
                return@withContext false
            }

            if (folderPairs.isNotEmpty()) {
                dbHelper.mergeFolders(folderPairs)
                dbHelper.saveSetting("config_url", url)
                return@withContext true
            }
            false
        } catch (e: Exception) {
            Log.e("ConfigFetcher", "Fetch error", e)
            false
        }
    }
}
