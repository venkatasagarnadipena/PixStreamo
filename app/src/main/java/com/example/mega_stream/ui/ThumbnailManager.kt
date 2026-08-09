package com.example.mega_stream.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.mega_stream.data.CacheManager
import com.example.mega_stream.data.SharedMediaItem
import androidx.compose.ui.focus.onFocusChanged
import java.io.File
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StreamingImageCard(
    item: SharedMediaItem, 
    folderUrl: String, // Pass folder URL for strict directory mapping
    onMediaSelected: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val handleId = item.handle.split("#")[0]
    
    // FOLDER-STRICT DISK CHECK
    val folderDir = remember(folderUrl) { CacheManager.getFolderCacheDir(context, folderUrl) }
    val file = remember(handleId) { File(folderDir, "dl_$handleId.jpg") }
    
    var isReady by remember(handleId) { mutableStateOf(file.exists() && file.length() > 1024) }

    LaunchedEffect(handleId) {
        if (!isReady) {
            CacheManager.fileReadyEvents
                .filter { it == handleId }
                .collect {
                    isReady = true
                }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        onClick = onMediaSelected,
        modifier = modifier
            .aspectRatio(1.5f)
            .onFocusChanged { if (it.isFocused) onFocused() },
        scale = CardDefaults.scale(focusedScale = 1.05f),
        border = CardDefaults.border(focusedBorder = Border(androidx.compose.foundation.BorderStroke(3.dp, Color.Yellow)))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isReady) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(file)
                        .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                        .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                        .crossfade(500)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Gray.copy(alpha = alpha)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
