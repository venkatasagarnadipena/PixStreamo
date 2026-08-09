package com.example.mega_stream.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
fun HomeScreen(
    onFolderSelected: (Folder) -> Unit,
    onSettingsSelected: () -> Unit,
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
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Update Config Button
                    IconButton(onClick = onSettingsSelected) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                    
                    // Reset Button
                    Button(
                        onClick = onCompleteReset,
                        colors = ButtonDefaults.colors(containerColor = Color(0xFF1E1E1E), contentColor = Color.Gray),
                        border = ButtonDefaults.border(focusedBorder = Border(androidx.compose.foundation.BorderStroke(2.dp, Color.White)))
                    ) {
                        Text("Complete Reset")
                    }
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
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "📁", style = MaterialTheme.typography.displaySmall)
                                    Text(text = folder.name, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
