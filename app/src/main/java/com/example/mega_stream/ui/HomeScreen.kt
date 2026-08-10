package com.example.mega_stream.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import androidx.tv.foundation.lazy.grid.*
import com.example.mega_stream.data.local.DatabaseHelper
import com.example.mega_stream.data.local.Folder
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HeaderButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Button(
        onClick = onClick,
        modifier = modifier.onFocusChanged { isFocused = it.isFocused },
        scale = ButtonDefaults.scale(focusedScale = 1.1f),
        colors = ButtonDefaults.colors(
            containerColor = Color(0xFF1E1E1E),
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black
        ),
        border = ButtonDefaults.border(focusedBorder = Border(androidx.compose.foundation.BorderStroke(2.dp, Color.White))),
        shape = ButtonDefaults.shape(RoundedCornerShape(12.dp)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center // Content alignment inside button
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isFocused) Color.Black else Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    onFolderSelected: (Folder) -> Unit,
    onSettingsSelected: () -> Unit,
    onSyncSelected: () -> Unit,
    onCompleteReset: () -> Unit
) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    var folders by remember { mutableStateOf(emptyList<Folder>()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        folders = dbHelper.getAllFolders()
        Log.d("MegaHomeScreen", "Folders in DB: ${folders.size}")
        if (folders.isNotEmpty()) {
            delay(1000) // Stabilize
            Log.d("MegaHomeScreen", "Attempting requestFocus()")
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                Log.e("MegaHomeScreen", "requestFocus failed", e)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)) // DARKER PREMIUM BLACK
            .onKeyEvent { 
                Log.d("MegaHomeScreen", "Root Box Key Event: $it")
                false 
            }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 32.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Your Folders", style = MaterialTheme.typography.displayMedium, color = Color.White)
                
                Row(
                    modifier = Modifier.wrapContentWidth(), // Ensure it doesn't take full width
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sync Button
                    HeaderButton(
                        text = "Sync",
                        icon = Icons.Default.Refresh,
                        onClick = onSyncSelected
                    )

                    // Settings Button
                    HeaderButton(
                        text = "Setup",
                        icon = Icons.Default.Settings,
                        onClick = onSettingsSelected
                    )
                    
                    // Reset Button
                    HeaderButton(
                        text = "Reset",
                        icon = Icons.Default.Delete,
                        onClick = onCompleteReset
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp)) // MOVED FOLDERS SLIGHTLY DOWNWARD

            if (folders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No folders found. Check your JSON link.", color = Color.Gray)
                }
            } else {
                TvLazyVerticalGrid(
                    columns = TvGridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp), // ADDED PADDING TO PREVENT CROPPING
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(folders, key = { _, folder -> folder.id }) { index, folder ->
                        Card(
                            onClick = { 
                                Log.d("MegaHomeScreen", "CLICKED: ${folder.name}")
                                onFolderSelected(folder) 
                            },
                            modifier = Modifier
                                .aspectRatio(1.5f)
                                .onFocusChanged { 
                                    if (it.isFocused) Log.d("MegaHomeScreen", "FOCUSED: ${folder.name} (index $index)")
                                }
                                .then(if (index == 0) Modifier.focusRequester(focusRequester) else Modifier),
                            scale = CardDefaults.scale(focusedScale = 1.1f), // SLIGHTLY REDUCED SCALE TO PREVENT OVERLAP
                            border = CardDefaults.border(
                                focusedBorder = Border(androidx.compose.foundation.BorderStroke(3.dp, Color.White))
                            ),
                            colors = CardDefaults.colors(
                                containerColor = Color(0xFF1E1E1E), // CLEANER CARD COLOR
                                focusedContainerColor = Color(0xFF2A2A2A)
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.List,
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
