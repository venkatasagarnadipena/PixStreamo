package com.example.mega_stream.ui

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
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.example.mega_stream.data.local.DatabaseHelper
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.activity.compose.BackHandler
import java.io.File
import android.os.Environment

/**
 * POLISHED WELCOME SCREEN
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Welcome to PixStreamo",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "The ultimate media streaming experience for Android TV.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.LightGray
            )
            Spacer(modifier = Modifier.height(64.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.focusRequester(focusRequester),
                scale = ButtonDefaults.scale(focusedScale = 1.1f),
                border = ButtonDefaults.border(focusedBorder = Border(androidx.compose.foundation.BorderStroke(3.dp, Color.Yellow)))
            ) {
                Text("Start Configuration")
            }
        }
    }
}

/**
 * MAIN ONBOARDING MENU: Card-based selection
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun OnboardingMenuScreen(
    onSelectUrl: () -> Unit,
    onSelectStorage: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    val currentUrl = dbHelper.getSetting("config_url", "Default Link Active")
    val currentStorage = dbHelper.getSetting("storage_path", "AUTO (Recommended)")
    
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(modifier = Modifier.fillMaxSize().padding(48.dp)) {
        Column {
            Text("Initial Configuration", style = MaterialTheme.typography.displaySmall, color = Color.White)
            Text("Customize your experience or use defaults", color = Color.Gray)
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                // URL CONFIG CARD
                ConfigCard(
                    title = "Media Source",
                    subtitle = currentUrl,
                    emoji = "🔗",
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    onClick = onSelectUrl
                )
                
                // STORAGE CONFIG CARD
                ConfigCard(
                    title = "Storage Location",
                    subtitle = currentStorage,
                    emoji = "💾",
                    modifier = Modifier.weight(1f),
                    onClick = onSelectStorage
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = onFinish,
                modifier = Modifier.align(Alignment.CenterHorizontally).width(300.dp),
                scale = ButtonDefaults.scale(focusedScale = 1.1f),
                border = ButtonDefaults.border(focusedBorder = Border(androidx.compose.foundation.BorderStroke(3.dp, Color.Green)))
            ) {
                Text("Finish Setup & Launch")
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ConfigCard(
    title: String,
    subtitle: String,
    emoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(200.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(androidx.compose.foundation.BorderStroke(4.dp, Color.Yellow))
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isFocused) Color.DarkGray else Color(0xFF1A1A1A)
        )
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text(emoji, style = MaterialTheme.typography.displaySmall)
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.Gray, maxLines = 1)
        }
    }
}

/**
 * URL INPUT SCREEN
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SetupUrlScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    var urlText by remember { mutableStateOf(dbHelper.getSetting("config_url", "")) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    BackHandler { onBack() }

    Box(modifier = Modifier.fillMaxSize().padding(64.dp), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.width(600.dp)) {
            Text("Enter Config JSON URL", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Leave empty to use the default MEGA source", color = Color.Gray)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Using standard TextField from material3 but wrapped for TV focus
            androidx.compose.material3.TextField(
                value = urlText,
                onValueChange = { urlText = it },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                placeholder = { Text("https://mega.nz/file/...") },
                singleLine = true,
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = Color.DarkGray,
                    unfocusedContainerColor = Color.Black,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.Gray
                )
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = {
                    dbHelper.saveSetting("config_url", urlText.trim())
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save & Go Back")
            }
        }
    }
}

/**
 * MODERN FOLDER PICKER SCREEN
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SetupStorageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    
    var currentDir by remember { mutableStateOf(Environment.getExternalStorageDirectory()) }
    var fileList by remember { mutableStateOf(emptyList<File>()) }
    
    val focusRequester = remember { FocusRequester() }

    // Load directory content
    LaunchedEffect(currentDir) {
        val files = currentDir.listFiles()?.filter { it.isDirectory && !it.isHidden }?.sortedBy { it.name.lowercase() } ?: emptyList()
        fileList = files
        focusRequester.requestFocus()
    }

    BackHandler {
        if (currentDir.parentFile != null && currentDir.absolutePath != "/storage") {
            currentDir = currentDir.parentFile!!
        } else {
            onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(48.dp)) {
        Column {
            Text("Select Storage Location", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text("Current Path: ${currentDir.absolutePath}", color = Color.Yellow, style = MaterialTheme.typography.bodySmall)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                // DIR LIST
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FolderItem(
                            name = ".. (Go Up)",
                            isFocusedDefault = true,
                            onClick = {
                                if (currentDir.parentFile != null) currentDir = currentDir.parentFile!!
                            }
                        )
                    }
                    items(fileList) { file ->
                        FolderItem(
                            name = file.name,
                            onClick = { currentDir = file }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(48.dp))
                
                // ACTIONS
                Column(modifier = Modifier.width(250.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = {
                            dbHelper.saveSetting("storage_path", currentDir.absolutePath)
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.colors(containerColor = Color.Green, contentColor = Color.Black)
                    ) {
                        Text("Select This Folder")
                    }
                    
                    Button(
                        onClick = {
                            dbHelper.saveSetting("storage_path", "AUTO")
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset to AUTO")
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                        Text("Back to Menu")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FolderItem(name: String, isFocusedDefault: Boolean = false, onClick: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (isFocusedDefault) focusRequester.requestFocus()
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(androidx.compose.foundation.BorderStroke(2.dp, Color.Yellow))),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color(0xFF121212), focusedContainerColor = Color.DarkGray)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("📁", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.width(16.dp))
            Text(name, color = Color.White)
        }
    }
}
