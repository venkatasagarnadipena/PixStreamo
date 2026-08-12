package com.example.mega_stream.ui.screens

import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.example.mega_stream.R
import com.example.mega_stream.core.storage.DatabaseHelper
import java.io.File

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
                text = "Welcome to PixStreamo",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "The ultimate media streaming experience for Android TV.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.focusRequester(focusRequester),
                scale = ButtonDefaults.scale(focusedScale = 1.08f),
                shape = ButtonDefaults.shape(CircleShape),
                colors = ButtonDefaults.colors(containerColor = Color(0xFF1E1E1E), contentColor = Color.White),
                border = ButtonDefaults.border(focusedBorder = Border(BorderStroke(2.dp, Color.White))),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp)
            ) {
                Text("Start Configuration", style = MaterialTheme.typography.labelLarge)
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
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper.getInstance(context) }
    val currentUrl = dbHelper.getSetting("config_url", "Default Link Active")
    val currentStorage = dbHelper.getSetting("storage_path", "AUTO (Recommended)")

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .padding(horizontal = 56.dp, vertical = 48.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("Initial Configuration", style = MaterialTheme.typography.displaySmall, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Customize your experience or use defaults", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                ConfigCard(
                    title = "Media Source",
                    subtitle = currentUrl,
                    iconRes = R.drawable.ic_sync,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    onClick = onSelectUrl
                )

                ConfigCard(
                    title = "Storage Location",
                    subtitle = currentStorage,
                    iconRes = R.drawable.ic_filemanager,
                    modifier = Modifier.weight(1f),
                    onClick = onSelectStorage
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onFinish,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                scale = ButtonDefaults.scale(focusedScale = 1.08f),
                shape = ButtonDefaults.shape(CircleShape),
                colors = ButtonDefaults.colors(containerColor = Color(0xFF1E1E1E), contentColor = Color.White),
                border = ButtonDefaults.border(focusedBorder = Border(BorderStroke(2.dp, Color.White))),
                contentPadding = PaddingValues(horizontal = 40.dp, vertical = 14.dp)
            ) {
                Text("Finish Setup & Launch", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ConfigCard(
    title: String,
    subtitle: String,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(200.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.5.dp, Color.White))
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF161616),
            focusedContainerColor = Color(0xFF242424),
            contentColor = Color.White,
            focusedContentColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = if (isFocused) Color.Yellow else Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(36.dp)
            )

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isFocused) Color.White.copy(alpha = 0.9f) else Color.Gray,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SetupUrlScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper.getInstance(context) }
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
            .padding(64.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = Modifier.width(600.dp)) {
            Text("Enter Config JSON URL", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Leave empty to use the default MEGA source", color = Color.Gray)

            Spacer(modifier = Modifier.height(32.dp))

            TextField(
                value = urlText,
                onValueChange = { urlText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text("https://mega.nz/file/...") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF121212),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.Gray,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    dbHelper.saveSetting("config_url", urlText.trim())
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = ButtonDefaults.shape(CircleShape),
                colors = ButtonDefaults.colors(containerColor = Color(0xFF1E1E1E), contentColor = Color.White),
                border = ButtonDefaults.border(focusedBorder = Border(BorderStroke(2.dp, Color.White))),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text("Save & Go Back", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SetupStorageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper.getInstance(context) }

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
        try { focusRequester.requestFocus() } catch (e: Exception) {}
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
            .padding(48.dp)
    ) {
        Column {
            Text("Select Storage Location", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Path: ${currentDir.absolutePath}", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        FolderItem(
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
                        FolderItem(
                            name = file.name,
                            isUpItem = false,
                            onClick = { currentDir = file }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(48.dp))

                Column(
                    modifier = Modifier.width(260.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            dbHelper.saveSetting("storage_path", currentDir.absolutePath)
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = ButtonDefaults.shape(CircleShape),
                        colors = ButtonDefaults.colors(containerColor = Color(0xFF1E1E1E), contentColor = Color.White),
                        border = ButtonDefaults.border(focusedBorder = Border(BorderStroke(2.dp, Color.White))),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text("Select This Folder", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = {
                            dbHelper.saveSetting("storage_path", "AUTO")
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = ButtonDefaults.shape(CircleShape),
                        colors = ButtonDefaults.colors(containerColor = Color(0xFF141414), contentColor = Color.Gray),
                        border = ButtonDefaults.border(focusedBorder = Border(BorderStroke(2.dp, Color.White))),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text("Reset to AUTO", style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                        shape = ButtonDefaults.shape(CircleShape),
                        border = ButtonDefaults.border(
                            border = Border(BorderStroke(1.dp, Color.Gray)),
                            focusedBorder = Border(BorderStroke(2.dp, Color.White))
                        ),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text("Back to Menu", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FolderItem(
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
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(2.dp, Color.White))),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF141414),
            focusedContainerColor = Color(0xFF262626)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = if (isUpItem) painterResource(id = R.drawable.ic_back) else painterResource(id = R.drawable.ic_filemanager),
                contentDescription = null,
                tint = if (isFocused) Color.Yellow else Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = name,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
