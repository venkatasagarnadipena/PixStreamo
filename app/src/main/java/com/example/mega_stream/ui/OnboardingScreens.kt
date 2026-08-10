package com.example.mega_stream.ui

import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.example.mega_stream.data.local.DatabaseHelper
import java.io.File

/**
 * REUSABLE PREMIUM ACTION BUTTON
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun OnboardingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        scale = ButtonDefaults.scale(focusedScale = 1.05f),
        colors = ButtonDefaults.colors(
            containerColor = if (isPrimary) Color(0xFF1E1E1E) else Color.Transparent,
            contentColor = if (isPrimary) Color.White else Color.Gray,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black
        ),
        border = ButtonDefaults.border(
            focusedBorder = Border(androidx.compose.foundation.BorderStroke(2.dp, Color.White))
        ),
        shape = ButtonDefaults.shape(RoundedCornerShape(12.dp)),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * CINEMATIC WELCOME SCREEN
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "PixStreamo",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Premium media streaming for Android TV",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(64.dp))
            OnboardingButton(
                text = "Start Configuration",
                onClick = onContinue,
                modifier = Modifier.width(300.dp).focusRequester(focusRequester)
            )
        }
    }
}

/**
 * MAIN ONBOARDING MENU
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
    val currentUrl = dbHelper.getSetting("config_url", "Default source active")
    val currentStorage = dbHelper.getSetting("storage_path", "AUTO (Recommended)")

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .padding(56.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("Configuration", style = MaterialTheme.typography.displayMedium, color = Color.White)
            Text("Personalize your experience", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                ConfigCard(
                    title = "Media Source",
                    subtitle = currentUrl,
                    icon = Icons.Default.Share,
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    onClick = onSelectUrl
                )

                ConfigCard(
                    title = "Storage",
                    subtitle = currentStorage,
                    icon = Icons.Default.Home,
                    modifier = Modifier.weight(1f),
                    onClick = onSelectStorage
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            OnboardingButton(
                text = "Finish & Launch",
                onClick = onFinish,
                modifier = Modifier.align(Alignment.CenterHorizontally).width(300.dp)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ConfigCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(180.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(androidx.compose.foundation.BorderStroke(3.dp, Color.White))
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF161616),
            focusedContainerColor = Color(0xFF242424),
            contentColor = Color.White,
            focusedContentColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused) Color.White else Color.Gray,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(text = title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .padding(56.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally // CENTERED CONTENT
        ) {
            Text("Media Source", style = MaterialTheme.typography.displayMedium, color = Color.White)
            Text("Enter your Config JSON URL", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(64.dp))

            TextField(
                value = urlText,
                onValueChange = { urlText = it },
                modifier = Modifier
                    .width(700.dp) // FIXED WIDTH FOR BETTER CENTERING
                    .focusRequester(focusRequester),
                placeholder = { Text("https://mega.nz/file/...") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF121212),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.Gray,
                    focusedIndicatorColor = Color.White,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OnboardingButton(
                    text = "Save & Go Back",
                    onClick = {
                        dbHelper.saveSetting("config_url", urlText.trim())
                        onBack()
                    }
                )
                OnboardingButton(
                    text = "Cancel",
                    onClick = onBack,
                    isPrimary = false
                )
            }
        }
    }
}

/**
 * STORAGE PICKER SCREEN
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SetupStorageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }

    var currentDir by remember { mutableStateOf(File("/storage")) }
    if (!currentDir.exists()) currentDir = File("/")

    var fileList by remember { mutableStateOf(emptyList<File>()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(currentDir) {
        val files = try {
            currentDir.listFiles()?.filter { it.isDirectory && !it.isHidden }?.sortedBy { it.name.lowercase() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        fileList = files
        if (fileList.isNotEmpty() || currentDir.parentFile != null) {
            try { focusRequester.requestFocus() } catch (e: Exception) {}
        }
    }

    BackHandler {
        val parent = currentDir.parentFile
        if (parent != null && currentDir.absolutePath != "/") {
            currentDir = parent
        } else {
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .padding(56.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("Storage Location", style = MaterialTheme.typography.displayMedium, color = Color.White)
            Text("Select where to store media cache", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Current Path: ${currentDir.absolutePath}", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                // FOLDER LIST
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item(key = "up_item") {
                        FolderRowItem(
                            name = ".. (Go Up)",
                            isUpItem = true,
                            isFocusedDefault = true,
                            onClick = {
                                val parent = currentDir.parentFile
                                if (parent != null) currentDir = parent
                            }
                        )
                    }
                    items(fileList, key = { it.absolutePath }) { file ->
                        FolderRowItem(
                            name = file.name,
                            isUpItem = false,
                            onClick = { currentDir = file }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(48.dp))

                // ACTION PANEL
                Column(
                    modifier = Modifier.width(280.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally // CENTERED ACTIONS
                ) {
                    OnboardingButton(
                        text = "Select This Folder",
                        onClick = {
                            dbHelper.saveSetting("storage_path", currentDir.absolutePath)
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OnboardingButton(
                        text = "Reset to AUTO",
                        onClick = {
                            dbHelper.saveSetting("storage_path", "AUTO")
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isPrimary = false
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    OnboardingButton(
                        text = "Back to Menu",
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                        isPrimary = false
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FolderRowItem(
    name: String,
    isUpItem: Boolean = false,
    isFocusedDefault: Boolean = false,
    onClick: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (isFocusedDefault) focusRequester.requestFocus()
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(androidx.compose.foundation.BorderStroke(2.dp, Color.White))
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF141414),
            focusedContainerColor = Color.White,
            contentColor = Color.White,
            focusedContentColor = Color.Black
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isUpItem) Icons.Default.KeyboardArrowUp else Icons.Default.Add,
                contentDescription = null,
                tint = if (isFocused) Color.Black else Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = name, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
