package com.example.mega_stream.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.example.mega_stream.data.ConfigFetcher
import com.example.mega_stream.data.CacheManager
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SplashScreen(onSyncComplete: () -> Unit) {
    val context = LocalContext.current
    val configFetcher = remember { ConfigFetcher(context) }
    var storageInfo by remember { mutableStateOf("Detecting storage...") }

    LaunchedEffect(Unit) {
        // Show current effective storage path
        val currentCache = CacheManager.getOptimalCacheDir(context)
        storageInfo = "Storage: ${currentCache.parentFile?.absolutePath ?: "Internal"}"
        
        withContext(Dispatchers.IO) {
            configFetcher.fetchAndSync()
        }
        onSyncComplete()
    }

    Box(
        modifier = Modifier.fillMaxSize(), 
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "PixStreamo",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading your configuration...", 
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray
            )
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = storageInfo,
                style = MaterialTheme.typography.labelSmall,
                color = Color.DarkGray
            )
        }
    }
}
