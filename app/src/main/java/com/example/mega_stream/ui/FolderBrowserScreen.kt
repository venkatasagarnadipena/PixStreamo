package com.example.mega_stream.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import androidx.tv.foundation.lazy.grid.*
import com.example.mega_stream.data.MegaManager
import com.example.mega_stream.data.SharedMediaItem
import com.example.mega_stream.data.StreamingWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FolderBrowserScreen(
    folderUrl: String,
    folderName: String,
    onMediaSelected: (String, Int) -> Unit,
    onBack: () -> Unit
) {
    var mediaItems by remember { mutableStateOf(emptyList<SharedMediaItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    LaunchedEffect(folderUrl) {
        isLoading = true
        mediaItems = withContext(Dispatchers.IO) {
            MegaManager.listSharedFolder(folderUrl).filter { it.type == "image" }
        }
        isLoading = false
        StreamingWorker.initFolder(context, folderUrl, mediaItems)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .padding(48.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = folderName, style = MaterialTheme.typography.displayMedium, color = Color.White)
            Button(onClick = onBack) { Text("Back") }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(color = Color.White)
            }
        } else {
            TvLazyVerticalGrid(
                columns = TvGridCells.Fixed(4),
                contentPadding = PaddingValues(top = 24.dp, bottom = 64.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(mediaItems, key = { _, item -> item.handle }) { index, item ->
                    StreamingImageCard(
                        item = item,
                        folderUrl = folderUrl,
                        onMediaSelected = {
                            StreamingWorker.jumpTo(index)
                            onMediaSelected(item.handle, index)
                        },
                        onFocused = {}
                    )
                }
            }
        }
    }
}
