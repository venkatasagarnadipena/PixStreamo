package com.example.mega_stream.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import androidx.tv.foundation.lazy.grid.*
import com.example.mega_stream.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FolderBrowserScreen(
    folderName: String, 
    folderUrl: String, 
    onMediaSelected: (String, Int) -> Unit
) {
    val context = LocalContext.current
    var mediaItems by remember { mutableStateOf(emptyList<SharedMediaItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }
    
    val backendIndex by StreamingWorker.currentIndex.collectAsState()

    // 10s Cleanup Worker
    DisposableEffect(folderUrl) {
        onDispose {
            val folderCacheDir = CacheManager.getFolderCacheDir(context, folderUrl)
            @Suppress("OPT_IN_USAGE")
            GlobalScope.launch(Dispatchers.IO) {
                delay(10000)
                CacheManager.clearFolderCache(folderCacheDir)
            }
        }
    }

    LaunchedEffect(folderUrl) {
        val result = withContext(Dispatchers.IO) {
            try {
                MegaManager.listSharedFolder(folderUrl)
            } catch (e: Exception) { null }
        }
        if (result != null) {
            mediaItems = result
            // Initialize engine for this folder
            StreamingWorker.initFolder(context, folderUrl, result, backendIndex)
        }
        isLoading = false
        if (mediaItems.isNotEmpty()) {
            delay(500)
            try { focusRequester.requestFocus() } catch (e: Exception) {}
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = folderName, style = MaterialTheme.typography.headlineMedium, color = Color.White)
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            TvLazyVerticalGrid(
                columns = TvGridCells.Fixed(4),
                contentPadding = PaddingValues(bottom = 64.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(mediaItems, key = { _, item -> item.handle }) { index, item ->
                    StreamingImageCard(
                        item = item,
                        folderUrl = folderUrl,
                        onMediaSelected = { 
                            StreamingWorker.jumpTo(index, pause = false)
                            onMediaSelected(item.handle, index) 
                        },
                        onFocused = { 
                            // Update engine index when scrolling in grid to prioritize visible items
                            StreamingWorker.jumpTo(index, pause = false) 
                        },
                        modifier = Modifier
                    )
                }
            }
        }
    }
}
