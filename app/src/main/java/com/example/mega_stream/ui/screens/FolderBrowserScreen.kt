package com.example.mega_stream.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import androidx.tv.foundation.lazy.grid.*
import com.example.mega_stream.core.engine.MegaManager
import com.example.mega_stream.core.engine.SharedMediaItem
import com.example.mega_stream.core.engine.CacheManager
import com.example.mega_stream.ui.components.FolderImageCard
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FolderBrowserScreen(
    folderName: String,
    folderUrl: String,
    onMediaSelected: (String, Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mediaItems by remember { mutableStateOf(emptyList<SharedMediaItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }

    // Logic to fetch items AND pre-download thumbnails for the grid
    LaunchedEffect(Unit) {
        isLoading = true
        val items = MegaManager.listSharedFolder(folderUrl)
        mediaItems = items
        isLoading = false
        
        if (items.isNotEmpty()) {
            delay(300)
            try { focusRequester.requestFocus() } catch (e: Exception) {}
            
            // BACKGROUND DOWNLOAD: Fetch grid images so they aren't black
            scope.launch {
                val folderDir = CacheManager.getFolderCacheDir(context, folderUrl)
                items.forEach { item ->
                    val handleId = item.handle.split("#")[0]
                    val file = File(folderDir, "dl_$handleId.jpg")
                    if (!file.exists()) {
                        MegaManager.downloadFile(item.handle, folderDir.absolutePath)
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 32.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = folderName, 
                    style = MaterialTheme.typography.displayMedium, 
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(color = Color.White)
                }
            } else if (mediaItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No images found in this folder.", color = Color.Gray)
                }
            } else {
                TvLazyVerticalGrid(
                    columns = TvGridCells.Fixed(4),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(mediaItems) { index, item ->
                        FolderImageCard(
                            item = item,
                            folderUrl = folderUrl,
                            modifier = Modifier
                                .then(if (index == 0) Modifier.focusRequester(focusRequester) else Modifier),
                            onClick = { onMediaSelected(item.handle, index) }
                        )
                    }
                }
            }
        }
    }
}
