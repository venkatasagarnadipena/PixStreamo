package com.example.mega_stream.sync

import android.content.Context
import android.util.Log
import com.example.mega_stream.data.local.DatabaseHelper
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.NetworkInterface

class SyncServer(private val context: Context) {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start(onUrlReceived: (String) -> Unit) {
        if (serverJob?.isActive == true) return

        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(8888)
                Log.d("SyncServer", "Server started on port 8888")

                while (isActive) {
                    val client = serverSocket?.accept() ?: break
                    handleClient(client, onUrlReceived)
                }
            } catch (e: Exception) {
                Log.e("SyncServer", "Server error", e)
            } finally {
                stop()
            }
        }
    }

    private fun handleClient(socket: Socket, onUrlReceived: (String) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val out = PrintWriter(socket.getOutputStream(), true)

                val receivedLine = reader.readLine()
                if (receivedLine != null && receivedLine.startsWith("https://mega.nz/")) {
                    Log.d("SyncServer", "Received URL: $receivedLine")
                    DatabaseHelper(context).saveSetting("config_url", receivedLine)
                    onUrlReceived(receivedLine)
                    out.println("OK")
                } else {
                    out.println("ERROR: Invalid URL")
                }
            } catch (e: Exception) {
                Log.e("SyncServer", "Client handling error", e)
            } finally {
                socket.close()
                stop()
            }
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
        } catch (e: Exception) {}
        serverSocket = null
        serverJob?.cancel()
        Log.d("SyncServer", "Server stopped")
    }

    companion object {
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
            } catch (e: Exception) {}
            return null
        }
    }
}
