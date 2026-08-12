package com.example.mega_stream.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import androidx.tv.foundation.lazy.grid.*
import com.example.mega_stream.R
import com.example.mega_stream.core.storage.DatabaseHelper
import com.example.mega_stream.core.storage.Folder
import com.example.mega_stream.core.network.ConfigFetcher
import com.example.mega_stream.ui.components.HeaderButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.window.Dialog
import com.example.mega_stream.core.engine.CacheManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    onFolderSelected: (Folder) -> Unit,
    onSettingsSelected: () -> Unit,
    onSyncSelected: () -> Unit,
    onCompleteReset: () -> Unit 
) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper.getInstance(context) }
    var folders by remember { mutableStateOf(emptyList<Folder>()) }
    
    // FOCUS REQUERSTERS: Essential for TV D-Pad support
    val syncButtonFocusRequester = remember { FocusRequester() }
    val gridFocusRequester = remember { FocusRequester() }
    
    val scope = rememberCoroutineScope()
    
    var isSyncingFolders by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    fun refreshFolders() {
        val latestFolders = dbHelper.getAllFolders()
        folders = latestFolders
        Log.d("HomeScreen", "UI State Update: ${latestFolders.size} folders found.")
    }

    LaunchedEffect(Unit) {
        refreshFolders()
        
        // CRITICAL FOCUS HANDOFF: 
        // Wait for animations to settle, then aggressively claim focus for the D-Pad.
        delay(1000) 
        if (folders.isEmpty()) {
            Log.d("HomeScreen", "Focusing Sync button (Empty Library)")
            try { syncButtonFocusRequester.requestFocus() } catch (e: Exception) {
                Log.e("HomeScreen", "Focus request failed", e)
            }
        } else {
            Log.d("HomeScreen", "Focusing Grid (Populated Library)")
            try { gridFocusRequester.requestFocus() } catch (e: Exception) {}
        }

        // Silent background check
        isSyncingFolders = true 
        scope.launch {
            ConfigFetcher(context).fetchAndSync()
            refreshFolders()
            isSyncingFolders = false
        }
    }

    // RESET CONFIRMATION DIALOG
    if (showResetDialog) {
        Dialog(onDismissRequest = { showResetDialog = false }) {
            Column(
                modifier = Modifier
                    .width(440.dp)
                    .background(Color(0xFF1A1A1A), MaterialTheme.shapes.medium)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Reset Library?", 
                    style = MaterialTheme.typography.headlineMedium, 
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "This will permanently remove all your folders and cached images. This action cannot be undone.", 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = Color.LightGray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            dbHelper.resetAllData()
                            CacheManager.deleteAllCache(context)
                            folders = emptyList()
                            showResetDialog = false
                            try { syncButtonFocusRequester.requestFocus() } catch (e: Exception) {}
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.colors(
                            containerColor = Color.Red, 
                            contentColor = Color.White,
                            focusedContainerColor = Color.White,
                            focusedContentColor = Color.Red
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "Delete", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    
                    OutlinedButton(
                        onClick = { showResetDialog = false },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.colors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                            focusedContainerColor = Color.White,
                            focusedContentColor = Color.Black
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "Cancel", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 32.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Your Folders", style = MaterialTheme.typography.displayMedium, color = Color.White)
                    
                    if (isSyncingFolders) {
                        Spacer(modifier = Modifier.width(24.dp))
                        androidx.compose.material3.CircularProgressIndicator(
                            color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 3.dp
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.wrapContentWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderButton(
                        text = "Sync", 
                        iconRes = R.drawable.ic_sync, 
                        onClick = onSyncSelected,
                        modifier = Modifier.focusRequester(syncButtonFocusRequester)
                    )
                    HeaderButton(text = "Setup", iconRes = R.drawable.ic_setting, onClick = onSettingsSelected)
                    HeaderButton(
                        text = "Reset", 
                        iconRes = R.drawable.ic_reset, 
                        onClick = { showResetDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            if (folders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Your library is empty. Use 'Sync' to import folders.", color = Color.Gray)
                    }
                }
            } else {
                TvLazyVerticalGrid(
                    columns = TvGridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(folders, key = { _, folder -> folder.id }) { index, folder ->
                        Card(
                            onClick = { onFolderSelected(folder) },
                            modifier = Modifier
                                .aspectRatio(1.5f)
                                .then(if (index == 0) Modifier.focusRequester(gridFocusRequester) else Modifier),
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
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_filemanager),
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = Color.White.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = folder.name,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
