package com.example.mega_stream.ui.components

import android.util.Log
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
import com.example.mega_stream.core.engine.SharedMediaItem
import com.example.mega_stream.core.engine.CacheManager
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.flow.filter
import java.io.File

/**
 * CLEAN REACTIVE IMAGE CARD:
 * Automatically listens for the "File Ready" signal and refreshes itself.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FolderImageCard(
    item: SharedMediaItem,
    folderUrl: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val handleId = item.handle.split("#")[0]
    
    // Check local cache folder
    val folderDir = remember(folderUrl) { CacheManager.getFolderCacheDir(context, folderUrl) }
    val cacheFile = remember(handleId) { File(folderDir, "dl_$handleId.jpg") }

    // REACTIVE STATE: Automatically changes to true when the background download finishes
    var isImageReady by remember { mutableStateOf(cacheFile.exists()) }

    // Observer: Listen for the specific file signal from the background engine
    LaunchedEffect(handleId) {
        if (!isImageReady) {
            CacheManager.fileReadyEvents
                .filter { it == handleId }
                .collect {
                    Log.d("FolderImageCard", "Signal received for $handleId - Refreshing Card.")
                    isImageReady = true
                }
        }
    }

    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1.5f),
        scale = CardDefaults.scale(focusedScale = 1.1f),
        border = CardDefaults.border(
            focusedBorder = Border(androidx.compose.foundation.BorderStroke(3.dp, Color.White))
        ),
        colors = CardDefaults.colors(
            containerColor = Color(0xFF1E1E1E),
            focusedContainerColor = Color(0xFF2A2A2A)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // Dark placeholder while waiting
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A)))

            if (isImageReady) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(cacheFile) 
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Spinning indicator for each card that is currently downloading
                androidx.compose.material3.CircularProgressIndicator(
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

/**
 * High-performance Player Image Card for horizontal strip
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StreamingImageCard(
    item: SharedMediaItem,
    folderUrl: String,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val handleId = item.handle.split("#")[0]
    val folderDir = CacheManager.getFolderCacheDir(context, folderUrl)
    val cacheFile = File(folderDir, "dl_$handleId.jpg")
    
    var isReady by remember { mutableStateOf(cacheFile.exists()) }

    LaunchedEffect(handleId) {
        if (!isReady) {
            CacheManager.fileReadyEvents
                .filter { it == handleId }
                .collect { isReady = true }
        }
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .width(160.dp)
            .height(90.dp)
            .onFocusChanged { if (it.isFocused) onFocus() },
        scale = CardDefaults.scale(focusedScale = 1.1f),
        border = CardDefaults.border(
            focusedBorder = Border(androidx.compose.foundation.BorderStroke(2.dp, Color.White))
        )
    ) {
        if (isReady) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(cacheFile)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A)))
        }
    }
}
