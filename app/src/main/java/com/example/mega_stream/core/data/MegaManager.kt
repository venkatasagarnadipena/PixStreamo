package com.example.mega_stream.core.data

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import org.json.JSONObject

data class SharedMediaItem(
    val handle: String,
    val name: String,
    val type: String, // "video" or "image"
    val fa: String? = null
)

object MegaManager {
    private var isInitialized = false

    fun init(context: Context) {
        if (!isInitialized) {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }
            isInitialized = true
        }
    }

    fun listSharedFolder(url: String): List<SharedMediaItem> {
        val py = Python.getInstance()
        val module = py.getModule("mega_manager")
        val resultJson = module.callAttr("list_shared_folder", url).toString()
        val result = JSONObject(resultJson)
        
        if (result.getString("status") != "success") return emptyList()
        
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
        return itemsList
    }

    fun downloadFile(url: String, destPath: String): Boolean {
        val py = Python.getInstance()
        val module = py.getModule("mega_manager")
        val resultJson = module.callAttr("download_file", url, destPath).toString()
        val result = JSONObject(resultJson)
        return result.getString("status") == "success"
    }
}
