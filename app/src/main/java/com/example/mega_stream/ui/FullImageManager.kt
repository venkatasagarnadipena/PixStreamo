package com.example.mega_stream.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mega_stream.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import android.view.KeyEvent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FullMediaScreen(
    initialHandle: String, 
    initialIndex: Int,
    folderUrl: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    // BACKEND ENGINE STATE
    val currentIndex by StreamingWorker.currentIndex.collectAsState()
    val isSlideshowActive by StreamingWorker.isSlideshowActive.collectAsState()
    
    var mediaItems by remember { mutableStateOf(emptyList<SharedMediaItem>()) }
    var currentFile by remember { mutableStateOf<File?>(null) }
    var isWaitingForFile by remember { mutableStateOf(true) }

    val focusRequester = remember { FocusRequester() }
    val folderCacheDir = remember(folderUrl) { CacheManager.getFolderCacheDir(context, folderUrl) }

    // INITIALIZATION
    LaunchedEffect(folderUrl) {
        val result = withContext(Dispatchers.IO) {
            try { MegaManager.listSharedFolder(folderUrl) } catch (e: Exception) { null }
        }
        if (result != null) {
            mediaItems = result
            StreamingWorker.initFolder(context, folderUrl, result, initialIndex)
            // Ensure slideshow is OFF by default as requested
            StreamingWorker.setSlideshowActive(false)
        }
    }

    // REACTIVE RENDERER
    LaunchedEffect(currentIndex, mediaItems) {
        if (mediaItems.isEmpty()) return@LaunchedEffect
        
        val item = mediaItems[currentIndex]
        val handleId = item.handle.split("#")[0]
        val file = File(folderCacheDir, "dl_$handleId.jpg")

        if (file.exists() && file.length() > 1024) {
            currentFile = file
            isWaitingForFile = false
        } else {
            isWaitingForFile = true
            currentFile = null
            CacheManager.fileReadyEvents
                .filter { it == handleId }
                .collect {
                    if (file.exists()) {
                        currentFile = file
                        isWaitingForFile = false
                    }
                }
        }
        
        // Re-claim focus whenever index changes
        try { focusRequester.requestFocus() } catch (e: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // BLACK BACKGROUND for letterboxing
            .focusRequester(focusRequester)
            .focusable() 
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT -> { StreamingWorker.previous(); true }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> { StreamingWorker.next(); true }
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            if (!isSlideshowActive) StreamingWorker.toggleSlideshow()
                            true
                        }
                        KeyEvent.KEYCODE_BACK -> { onBack(); true }
                        else -> false
                    }
                } else false
            },
        contentAlignment = Alignment.Center
    ) {
        // CONTENT DISPLAY: Maintains aspect ratio without stretching
        Crossfade(targetState = currentFile, label = "image_fade") { file ->
            if (isWaitingForFile || file == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(file)
                        .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                        .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit // PRESERVE ASPECT RATIO (No stretching)
                )
            }
        }

        // ANIMATED SLIDE-DOWN OVERLAY
        AnimatedVisibility(
            visible = !isSlideshowActive,
            enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(600)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(600)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 40.dp) // Slightly higher than bottom edge
                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(40.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // START SLIDESHOW CALL-TO-ACTION
                    Button(
                        onClick = { StreamingWorker.toggleSlideshow() },
                        colors = ButtonDefaults.colors(containerColor = Color.Yellow, contentColor = Color.Black),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("▶ START SLIDESHOW", style = MaterialTheme.typography.labelLarge)
                    }
                    
                    Spacer(modifier = Modifier.width(32.dp))
                    
                    Text(
                        text = "${currentIndex + 1} / ${mediaItems.size}", 
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                    
                    Spacer(modifier = Modifier.width(32.dp))
                    
                    Text(text = "← PREV", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "NEXT →", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
