package com.example.mega_stream.ui.screens

import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.example.mega_stream.R
import com.example.mega_stream.core.storage.DatabaseHelper
import java.io.File

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "PixStreamo",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Cinematic Image Streaming for Android TV",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(64.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.width(280.dp).height(56.dp),
                colors = ButtonDefaults.colors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("GET STARTED", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun OnboardingMenuScreen(
    onSelectUrl: () -> Unit,
    onSelectStorage: () -> Unit,
    onFinish: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.8f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Configuration",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(48.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                ConfigCard(
                    title = "Mega JSON URL",
                    desc = "Set your custom configuration source",
                    iconRes = R.drawable.ic_sync,
                    onClick = onSelectUrl,
                    modifier = Modifier.weight(1f)
                )
                ConfigCard(
                    title = "Media Storage",
                    desc = "Choose where to keep cached images",
                    iconRes = R.drawable.ic_filemanager,
                    onClick = onSelectStorage,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(64.dp))

            Button(
                onClick = onFinish,
                modifier = Modifier.width(320.dp).height(56.dp),
                colors = ButtonDefaults.colors(containerColor = Color.Yellow, contentColor = Color.Black)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("FINISH SETUP", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ConfigCard(
    title: String,
    desc: String,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(180.dp),
        colors = CardDefaults.colors(containerColor = Color(0xFF1A1A1A), focusedContainerColor = Color(0xFF2A2A2A)),
        scale = CardDefaults.scale(focusedScale = 1.05f),
        border = CardDefaults.border(focusedBorder = Border(androidx.compose.foundation.BorderStroke(2.dp, Color.White)))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color.Yellow
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text(text = desc, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SetupUrlScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper.getInstance(context) }
    var urlText by remember { mutableStateOf(dbHelper.getSetting("config_url", "")) }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.width(600.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Enter Config JSON URL",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Leave empty to use the default MEGA source",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // REFINED TEXT FIELD
            androidx.compose.material3.TextField(
                value = urlText,
                onValueChange = { urlText = it },
                placeholder = { Text("https://mega.nz/file/...", color = Color.DarkGray) },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1A1A1A),
                    unfocusedContainerColor = Color(0xFF1A1A1A),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.Yellow,
                    focusedIndicatorColor = Color.Yellow
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    dbHelper.saveSetting("config_url", urlText.trim())
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.colors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("SAVE & GO BACK", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SetupStorageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper.getInstance(context) }
    
    var currentPath by remember { mutableStateOf(File(dbHelper.getSetting("cache_path", Environment.getExternalStorageDirectory().absolutePath))) }
    var subfolders by remember { mutableStateOf(emptyList<File>()) }

    fun loadSubfolders(dir: File) {
        try {
            subfolders = dir.listFiles { file -> file.isDirectory }?.toList()?.sortedBy { it.name } ?: emptyList()
        } catch (e: Exception) { subfolders = emptyList() }
    }

    LaunchedEffect(currentPath) { loadSubfolders(currentPath) }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(48.dp)) {
            // HEADER
            Text(
                text = "Select Storage Location",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Path: ${currentPath.absolutePath}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxSize()) {
                // LEFT SIDE: LIST
                Column(modifier = Modifier.weight(1.5f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // GO UP item
                        item {
                            FolderItem(
                                name = ".. (Go Up)",
                                isParent = true,
                                onClick = { currentPath.parentFile?.let { currentPath = it } }
                            )
                        }

                        items(subfolders) { folder ->
                            FolderItem(
                                name = folder.name,
                                isParent = false,
                                onClick = { currentPath = folder }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(32.dp))

                // RIGHT SIDE: ACTIONS
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            dbHelper.saveSetting("cache_path", currentPath.absolutePath)
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.colors(containerColor = Color.Yellow, contentColor = Color.Black)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("SELECT THIS FOLDER", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            dbHelper.saveSetting("cache_path", context.cacheDir.absolutePath)
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("RESET TO AUTO")
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Simple text indicator that back is available via remote
                    Text(
                        text = "Press BACK on remote to cancel", 
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.DarkGray,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FolderItem(
    name: String,
    isParent: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF1A1A1A),
            focusedContainerColor = Color.White,
            contentColor = Color.White,
            focusedContentColor = Color.Black
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = if (isParent) R.drawable.ic_back else R.drawable.ic_filemanager),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
