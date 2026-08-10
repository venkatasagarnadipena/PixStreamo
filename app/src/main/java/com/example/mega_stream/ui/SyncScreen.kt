package com.example.mega_stream.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.example.mega_stream.sync.QrGenerator
import com.example.mega_stream.sync.SyncServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SyncScreen(onSyncComplete: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var ipAddress by remember { mutableStateOf<String?>(null) }
    val syncServer = remember { SyncServer(context) }

    LaunchedEffect(Unit) {
        val ip = SyncServer.getLocalIpAddress()
        ipAddress = ip
        if (ip != null) {
            val payload = "pixsync:$ip:8888"
            qrBitmap = withContext(Dispatchers.Default) {
                QrGenerator.generate(payload)
            }
            
            syncServer.start {
                onSyncComplete()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            syncServer.stop()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Sync with Mobile",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Scan the QR code with PixStreamo Mobile app to update configuration",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (qrBitmap != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    // Standard Surface doesn't have .colors() in TvMaterial3 easily for non-clickable
                    modifier = Modifier.size(300.dp).background(Color.White, RoundedCornerShape(16.dp))
                ) {
                    Image(
                        bitmap = qrBitmap!!.asImageBitmap(),
                        contentDescription = "Sync QR Code",
                        modifier = Modifier.fillMaxSize().padding(16.dp)
                    )
                }
            } else {
                Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = if (ipAddress != null) "TV IP: $ipAddress" else "Detecting network...",
                color = Color.DarkGray,
                style = MaterialTheme.typography.labelSmall
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            Button(onClick = onBack) {
                Text("Cancel & Go Back")
            }
        }
    }
}
