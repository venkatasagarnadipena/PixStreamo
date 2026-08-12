package com.example.mega_stream.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.example.mega_stream.core.network.LocalWebServer
import com.example.mega_stream.core.network.QrGenerator
import com.example.mega_stream.ui.components.QrCodeView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SyncScreen(onSyncComplete: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var ipAddress by remember { mutableStateOf<String?>(null) }
    var qrBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val server = remember { LocalWebServer(context) }
    var isSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Log.d("SyncScreen", "Entering Sync Screen, starting server")
        ipAddress = server.getLocalIpAddress()
        
        ipAddress?.let {
            val url = "http://$it:8888"
            qrBitmap = QrGenerator.generateQrCode(url, 400)
        }

        server.start { receivedUrl ->
            // This is called on Main thread from LocalWebServer
            Log.d("SyncScreen", "Sync signal received: $receivedUrl")
            isSuccess = true
            scope.launch {
                delay(2000) // Show success state for 2 seconds
                onSyncComplete() // Navigate away
            }
        }
    }

    // Ensure server stops when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            Log.d("SyncScreen", "Leaving Sync Screen, stopping server")
            server.stop()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        if (isSuccess) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "✅ URL Received!", style = MaterialTheme.typography.displayMedium, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Updating your library...", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Text(text = "Local Web Portal", style = MaterialTheme.typography.displayMedium, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Scan this QR with your phone to paste Mega URLs directly to your TV.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 64.dp)
                )

                Spacer(modifier = Modifier.height(48.dp))

                qrBitmap?.let {
                    QrCodeView(bitmap = it)
                } ?: run {
                    androidx.compose.material3.CircularProgressIndicator(color = Color.White)
                }

                Spacer(modifier = Modifier.height(24.dp))

                ipAddress?.let {
                    Text(text = "Or visit in phone browser: http://$it:8888", color = Color.White.copy(alpha = 0.6f))
                }
                
                // Redundant "Back to Home" button removed per user request.
                // User can use the standard remote BACK button to exit.
            }
        }
    }
}
