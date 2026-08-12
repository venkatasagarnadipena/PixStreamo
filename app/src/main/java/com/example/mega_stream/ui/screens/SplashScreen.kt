package com.example.mega_stream.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.example.mega_stream.core.network.ConfigFetcher
import com.example.mega_stream.core.storage.DatabaseHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SplashScreen(onSyncComplete: () -> Unit) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper.getInstance(context) }
    
    var progressValue by remember { mutableStateOf(0f) }
    
    val animatedProgress by animateFloatAsState(
        targetValue = progressValue,
        animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
        label = "splash_progress"
    )
    
    LaunchedEffect(Unit) {
        val foldersExist = dbHelper.getAllFolders().isNotEmpty()
        
        if (foldersExist) {
            // FAST START: Enter app immediately, update silently in background
            progressValue = 1f
            ConfigFetcher(context).startAsyncImport() // Lifecycle-safe background sync
            delay(1200)
            onSyncComplete()
        } else {
            // FIRST RUN: Must wait for first import to finish
            progressValue = 0.2f
            delay(500)
            progressValue = 0.5f
            
            // Block UI only for the first-ever data fetch
            ConfigFetcher(context).fetchAndSync() 
            
            progressValue = 1f
            delay(1000)
            onSyncComplete()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "PixStreamo",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Box(
                modifier = Modifier
                    .width(300.dp)
                    .height(6.dp)
                    .background(Color.White.copy(alpha = 0.1f), MaterialTheme.shapes.small)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .background(Color.White, MaterialTheme.shapes.small)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Loading your library...",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
        }
    }
}
