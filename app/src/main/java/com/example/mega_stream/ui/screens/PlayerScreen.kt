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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mega_stream.R
import com.example.mega_stream.core.engine.*
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

    // Logic: End screen only if reached the end WHILE in slideshow mode
    val isEndOfShow = statusLabel == "END_OF_SHOW"
    
    // Logic: Start screen only if slideshow is active and first 30 images not cached yet
    val showIntro = isSlideshowActive && !isWindowReady

    LaunchedEffect(folderUrl) {
        val result = withContext(Dispatchers.IO) {
            try { MegaManager.listSharedFolder(folderUrl) } catch (e: Exception) { null }
        }
        if (result != null) {
            mediaItems = result
            StreamingWorker.initFolder(context, folderUrl, result, initialIndex)
        }
    }

    LaunchedEffect(currentIndex, mediaItems, statusLabel) {
        if (mediaItems.isEmpty()) return@LaunchedEffect

        // Auto-navigate to home only after the End of Show screen has been visible
        if (isEndOfShow) {
            delay(5000)
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
                if (isEndOfShow) return@onKeyEvent true
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
        // --- 1. SLIDESHOW START SCREEN (Only if Slideshow is active and buffering) ---
        AnimatedVisibility(
            visible = showIntro,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = tween(800))
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Starting Slideshow",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    androidx.compose.material3.CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Buffering initial images...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            }
        }

        // --- 2. MAIN PLAYER ---
        if (!showIntro && !isEndOfShow) {
            Crossfade(
                targetState = currentFile,
                animationSpec = tween(durationMillis = 1000),
                label = "image_fade"
            ) { targetFile ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (targetFile != null && !isWaitingForFile) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(targetFile)
                                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                                .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                                .allowHardware(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        androidx.compose.material3.CircularProgressIndicator(color = Color.White)
                    }
                }
            }
        }

        // --- 3. SLIDESHOW END SCREEN (Only triggers if slideshow completes) ---
        AnimatedVisibility(
            visible = isEndOfShow,
            enter = fadeIn(animationSpec = tween(800)) + scaleIn(initialScale = 0.9f),
            exit = fadeOut()
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_reset),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Yellow
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "End of Show",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Returning to Home Screen...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            }
        }

        // --- 4. CONTROLS OVERLAY (Hidden during Slideshow) ---
        AnimatedVisibility(
            visible = !isSlideshowActive && !isEndOfShow,
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
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "${currentIndex + 1} / ${mediaItems.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(text = "← PREV", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Text(text = "NEXT →", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
