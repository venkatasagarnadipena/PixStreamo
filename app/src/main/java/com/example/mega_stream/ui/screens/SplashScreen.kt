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
    val progress = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        // Parallel work: Sync data
        launch {
            ConfigFetcher(context).fetchAndSync()
        }

        // Dedicated animation launch to ensure it's not blocked
        launch {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 3000, easing = LinearEasing)
            )
        }
        
        // Wait for minimum time
        delay(3200) 
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
            
            // Dynamic Progress Bar replacing static icon
            Box(
                modifier = Modifier
                    .width(300.dp)
                    .height(6.dp)
                    .background(Color.White.copy(alpha = 0.1f), MaterialTheme.shapes.small)
            ) {
                // Ensure fillMaxWidth reads the animate value on every frame
                val currentWidth = progress.value
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(currentWidth)
                        .background(Color.White, MaterialTheme.shapes.small)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Loading your experience...",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
        }
    }
}
