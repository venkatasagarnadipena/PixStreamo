package com.example.mega_stream.core.network

import android.content.Context
import android.util.Log
import com.example.mega_stream.core.storage.DatabaseHelper
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import kotlinx.html.*
import java.net.NetworkInterface

class LocalWebServer(private val appContext: Context) {
    private var server: NettyApplicationEngine? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start(onUrlReceived: (String) -> Unit) {
        if (server != null) return

        server = embeddedServer(Netty, port = 8888, host = "0.0.0.0") {
            routing {
                get("/") {
                    call.respondHtml {
                        head {
                            title { +"PixStreamo Web Portal" }
                            meta(name = "viewport", content = "width=device-width, initial-scale=1")
                            style {
                                unsafe {
                                    +"""
                                        body { font-family: sans-serif; background: #0A0A0A; color: white; text-align: center; padding: 40px 20px; }
                                        .container { max-width: 500px; margin: auto; background: #1A1A1A; padding: 30px; border-radius: 20px; border: 1px solid #333; }
                                        input { width: 100%; padding: 15px; margin: 20px 0; border-radius: 10px; border: 1px solid #444; background: #111; color: white; font-size: 16px; box-sizing: border-box; }
                                        button { width: 100%; padding: 15px; border-radius: 10px; border: none; background: #ffea00; color: black; font-weight: bold; font-size: 18px; cursor: pointer; }
                                        h1 { color: #ffea00; }
                                        p { color: #888; }
                                    """.trimIndent()
                                }
                            }
                        }
                        body {
                            div("container") {
                                h1 { +"PixStreamo" }
                                p { +"Paste your Mega.nz URL below to sync with your TV." }
                                form(action = "/sync", method = FormMethod.post) {
                                    input(type = InputType.url, name = "url") { 
                                        placeholder = "https://mega.nz/file/..."
                                        attributes["required"] = "true"
                                    }
                                    button(type = ButtonType.submit) { +"Send to TV" }
                                }
                            }
                        }
                    }
                }

                post("/sync") {
                    try {
                        val params = call.receiveParameters()
                        val receivedUrl = params["url"] ?: ""
                        Log.d("LocalWebServer", "Received URL: $receivedUrl")
                        
                        if (receivedUrl.startsWith("https://mega.nz/")) {
                            val dbHelper = DatabaseHelper.getInstance(appContext)
                            withContext(Dispatchers.IO) {
                                dbHelper.saveSetting("config_url", receivedUrl.trim())
                            }
                            
                            // CRITICAL: Switch to Main thread for the UI callback
                            withContext(Dispatchers.Main) {
                                onUrlReceived(receivedUrl.trim())
                            }
                            
                            call.respondHtml {
                                body {
                                    style { unsafe { +"body { background: #0A0A0A; color: white; text-align: center; padding-top: 100px; font-family: sans-serif; }" } }
                                    h2 { +"✅ URL Sent Successfully!" }
                                    p { +"Your TV is now updating. You can close this page." }
                                }
                            }
                        } else {
                            call.respondText("Invalid Mega URL", status = HttpStatusCode.BadRequest)
                        }
                    } catch (e: Exception) {
                        Log.e("LocalWebServer", "Sync Error", e)
                        call.respondText("Server Error: ${e.message}", status = HttpStatusCode.InternalServerError)
                    }
                }
            }
        }
        
        scope.launch {
            try {
                server?.start(wait = false)
                Log.d("LocalWebServer", "Server started on port 8888")
            } catch (e: Exception) {
                Log.e("LocalWebServer", "Failed to start server", e)
            }
        }
    }

    fun stop() {
        scope.launch {
            try {
                server?.stop(200, 500)
                server = null
                Log.d("LocalWebServer", "Server stopped successfully")
            } catch (e: Exception) {
                Log.e("LocalWebServer", "Error stopping server", e)
            }
        }
    }

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && (addr.hostAddress?.indexOf(':') ?: -1) < 0) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LocalWebServer", "Error getting IP", e)
        }
        return null
    }
}
