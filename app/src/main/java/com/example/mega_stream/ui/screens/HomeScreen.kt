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
import androidx.compose.ui.focus.onFocusChanged
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
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    var isAutoSyncing by remember { mutableStateOf(false) }

    fun refreshFolders() {
        folders = dbHelper.getAllFolders()
        Log.d("HomeScreen", "Refreshed folders from DB: ${folders.size}")
    }

    LaunchedEffect(Unit) {
        refreshFolders()
        // If DB is empty, or if we want to force a refresh on every entry to pick up new folders
        // We'll stick to auto-sync on empty for now to save performance, 
        // but user can manual sync anytime.
        if (folders.isEmpty()) {
            isAutoSyncing = true
            scope.launch {
                Log.d("HomeScreen", "Starting automatic sync...")
                ConfigFetcher(context).fetchAndSync()
                refreshFolders()
                isAutoSyncing = false
            }
        }
        
        delay(1000)
        if (folders.isNotEmpty()) {
            try { focusRequester.requestFocus() } catch (e: Exception) {}
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Your Folders", style = MaterialTheme.typography.displayMedium, color = Color.White)
                
                Row(
                    modifier = Modifier.wrapContentWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderButton(
                        text = "Sync",
                        iconRes = R.drawable.ic_sync,
                        onClick = onSyncSelected
                    )

                    HeaderButton(
                        text = "Setup",
                        iconRes = R.drawable.ic_setting,
                        onClick = onSettingsSelected
                    )
                    
                    HeaderButton(
                        text = "Reset",
                        iconRes = R.drawable.ic_reset,
                        onClick = onCompleteReset
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            if (folders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isAutoSyncing) {
                            androidx.compose.material3.CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "Fetching folders from Mega...", color = Color.Gray)
                        } else {
                            Text(text = "No folders found. Check your JSON link or Sync.", color = Color.Gray)
                        }
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
                                .then(if (index == 0) Modifier.focusRequester(focusRequester) else Modifier),
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
