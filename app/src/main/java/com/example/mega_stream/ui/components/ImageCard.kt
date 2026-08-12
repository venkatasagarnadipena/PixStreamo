package com.example.mega_stream.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mega_stream.core.engine.CacheManager
import com.example.mega_stream.core.engine.SharedMediaItem
import kotlinx.coroutines.flow.filter
import java.io.File

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StreamingImageCard(
    item: SharedMediaItem,
    folderUrl: String,
    onMediaSelected: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val handleId = item.handle.split("#")[0]
    val folderCacheDir = remember(folderUrl) { CacheManager.getFolderCacheDir(context, folderUrl) }
    val file = File(folderCacheDir, "dl_$handleId.jpg")

    var currentFile by remember { mutableStateOf<File?>(if (file.exists() && file.length() > 1024) file else null) }

    LaunchedEffect(handleId) {
        if (!file.exists() || file.length() <= 1024) {
            currentFile = null
            CacheManager.fileReadyEvents
                .filter { it == handleId }
                .collect {
                    if (file.exists()) {
                        currentFile = file
                    }
                }
        } else {
            currentFile = file
        }
    }

    Card(
        onClick = onMediaSelected,
        modifier = modifier
            .aspectRatio(1.5f)
            .onFocusChanged { if (it.isFocused) onFocused() },
        scale = CardDefaults.scale(focusedScale = 1.08f),
        border = CardDefaults.border(focusedBorder = Border(androidx.compose.foundation.BorderStroke(3.dp, Color.White))),
        colors = CardDefaults.colors(
            containerColor = Color(0xFF1E1E1E),
            focusedContainerColor = Color(0xFF2A2A2A)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (currentFile != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(currentFile)
                        .crossfade(true)
                        .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                        .size(400, 260)
                        .build(),
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}
