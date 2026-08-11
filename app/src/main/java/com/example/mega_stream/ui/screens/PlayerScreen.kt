package com.example.mega_stream.ui.screens

import android.view.KeyEvent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mega_stream.R
import com.example.mega_stream.core.engine.*
import com.example.mega_stream.ui.components.DribbbleLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerScreen(
    initialHandle: String,
    initialIndex: Int,
    folderUrl: String,
    onBack: () -> Unit,
    onHome: () -> Unit = {}
) {
    val context = LocalContext.current

    val currentIndex by StreamingWorker.currentIndex.collectAsState()
    val isSlideshowActive by StreamingWorker.isSlideshowActive.collectAsState()
    val statusLabel by StreamingWorker.statusLabel.collectAsState()
    val isWindowReady by StreamingWorker.isWindowReady.collectAsState()

    var mediaItems by remember { mutableStateOf(emptyList<SharedMediaItem>()) }
    var currentFile by remember { mutableStateOf<File?>(null) }
    var isWaitingForFile by remember { mutableStateOf(true) }

    val focusRequester = remember { FocusRequester() }
    val folderCacheDir = remember(folderUrl) { CacheManager.getFolderCacheDir(context, folderUrl) }

    LaunchedEffect(folderUrl) {
        val result = withContext(Dispatchers.IO) {
            try { MegaManager.listSharedFolder(folderUrl) } catch (e: Exception) { null }
        }
        if (result != null) {
            mediaItems = result
            StreamingWorker.setSlideshowActive(false)
            StreamingWorker.initFolder(context, folderUrl, result, initialIndex)
        }
    }

    LaunchedEffect(currentIndex, mediaItems, statusLabel) {
        if (mediaItems.isEmpty()) return@LaunchedEffect

        if (statusLabel == "END_OF_SHOW") {
            delay(7000)
            onHome()
            return@LaunchedEffect
        }

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

        try { focusRequester.requestFocus() } catch (e: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (statusLabel == "END_OF_SHOW") return@onKeyEvent true
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT -> { StreamingWorker.previous(); true }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> { StreamingWorker.next(); true }
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            StreamingWorker.toggleSlideshow()
                            true
                        }
                        KeyEvent.KEYCODE_BACK -> { onBack(); true }
                        else -> false
                    }
                } else false
            },
        contentAlignment = Alignment.Center
    ) {
        if (statusLabel == "END_OF_SHOW") {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "End of Show",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White
                )
            }
        } else if (isSlideshowActive && !isWindowReady) {
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Show will start soon",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                    DribbbleLoader()
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (isWaitingForFile || currentFile == null) {
                    androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(currentFile)
                            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                            .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                            .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                            .allowHardware(true)
                            .crossfade(300)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !isSlideshowActive && statusLabel != "END_OF_SHOW",
            enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(600)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(600)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 40.dp)
                    .background(Color(0xFF1A1A1A), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .padding(start = 8.dp, end = 24.dp, top = 8.dp, bottom = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Button(
                        onClick = { StreamingWorker.toggleSlideshow() },
                        colors = ButtonDefaults.colors(
                            containerColor = Color.Yellow,
                            contentColor = Color.Black
                        ),
                        shape = ButtonDefaults.shape(CircleShape),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_play),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.Black
                            )
                            Text(
                                text = "START SLIDESHOW",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Text(
                        text = "${currentIndex + 1} / ${mediaItems.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "←",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.offset(y = (-2).dp)
                            )
                            Text(
                                text = "PREV",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "NEXT",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "→",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.offset(y = (-2).dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
