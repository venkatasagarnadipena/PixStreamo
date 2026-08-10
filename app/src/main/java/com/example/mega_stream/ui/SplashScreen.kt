package com.example.mega_stream.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.example.mega_stream.data.CacheManager
import com.example.mega_stream.data.ConfigFetcher
import com.example.mega_stream.data.local.DatabaseHelper
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SplashScreen(onSyncComplete: () -> Unit) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    
    LaunchedEffect(Unit) {
        val savedUrl = dbHelper.getSetting("config_url", "https://mega.nz/file/AhQR3AxC#ZNvUcirmWJeqlTQAjsODb0L0teZL87vdNGOxf_l-NxY")
        val finalUrl = if (savedUrl.isEmpty()) "https://mega.nz/file/AhQR3AxC#ZNvUcirmWJeqlTQAjsODb0L0teZL87vdNGOxf_l-NxY" else savedUrl
        
        ConfigFetcher.fetchAndSync(context, finalUrl)
        delay(2000) 
        onSyncComplete()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "PixStreamo",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White
            )
        }
    }
}
