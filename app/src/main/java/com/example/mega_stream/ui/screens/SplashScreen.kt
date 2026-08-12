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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SplashScreen(onSyncComplete: () -> Unit) {
    val context = LocalContext.current
    
    // UI-State driven progress (0f to 1f)
    var progressValue by remember { mutableStateOf(0f) }
    
    // Explicit animation state
    val animatedProgress by animateFloatAsState(
        targetValue = progressValue,
        animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing),
        label = "splash_progress"
    )
    
    LaunchedEffect(Unit) {
        // Start background sync immediately (Fire and forget, but update progress on finish)
        val syncJob = launch {
            try {
                ConfigFetcher(context).fetchAndSync()
            } catch (e: Exception) { /* sync failed but we move on */ }
            progressValue = 1f
        }

        // Sequential progress visualization
        progressValue = 0.2f
        delay(800)
        progressValue = 0.5f
        delay(1000)
        progressValue = 0.8f
        
        // HARD LIMIT: Maximum 4 seconds wait time to ensure app entry
        delay(2200) 
        
        // Ensure we are at 100% before leaving
        progressValue = 1f
        
        // Wait for animation to visually fill up before transition
        delay(1600)

        onSyncComplete()
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
            
            // Modern Progress Bar
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
                text = if (animatedProgress < 1f) "Optimizing your experience..." else "Ready!",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
        }
    }
}
