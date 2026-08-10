package com.example.mega_stream

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mega_stream.data.MegaManager
import com.example.mega_stream.data.local.DatabaseHelper
import com.example.mega_stream.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MegaManager.init(this)
        
        setContent {
            AppNavigation()
        }
    }

    @Composable
    fun AppNavigation() {
        val navController = rememberNavController()
        val context = this@MainActivity
        val dbHelper = remember { DatabaseHelper(context) }

        NavHost(navController = navController, startDestination = if (dbHelper.isFirstLaunch()) "welcome" else "splash") {
            composable("welcome") { WelcomeScreen(onContinue = { navController.navigate("onboarding_menu") }) }
            composable("onboarding_menu") {
                OnboardingMenuScreen(
                    onSelectUrl = { navController.navigate("setup_url") },
                    onSelectStorage = { navController.navigate("setup_storage") },
                    onFinish = {
                        dbHelper.completeSetup()
                        navController.navigate("splash") { popUpTo("welcome") { inclusive = true } }
                    }
                )
            }
            composable("setup_url") { SetupUrlScreen(onBack = { navController.popBackStack() }) }
            composable("setup_storage") { SetupStorageScreen(onBack = { navController.popBackStack() }) }

            composable("splash") {
                SplashScreen(
                    onSyncComplete = {
                        navController.navigate("home") { popUpTo("splash") { inclusive = true } }
                    }
                )
            }

            composable("home") {
                HomeScreen(
                    onFolderSelected = { folder -> 
                        val encodedUrl = java.net.URLEncoder.encode(folder.url, "UTF-8")
                        navController.navigate("folder/$encodedUrl/${folder.name}") 
                    },
                    onSettingsSelected = { navController.navigate("onboarding_menu") },
                    onSyncSelected = { navController.navigate("sync_mobile") },
                    onCompleteReset = {
                        dbHelper.resetAllData()
                        navController.navigate("welcome") { popUpTo("home") { inclusive = true } }
                    }
                )
            }

            composable("sync_mobile") {
                SyncScreen(
                    onSyncComplete = { navController.navigate("splash") { popUpTo("home") { inclusive = true } } },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("folder/{url}/{name}") { backStackEntry ->
                val url = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", "UTF-8")
                val name = backStackEntry.arguments?.getString("name") ?: ""
                FolderBrowserScreen(
                    folderUrl = url,
                    folderName = name,
                    onMediaSelected = { handle, index -> 
                        val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                        navController.navigate("player/$encodedUrl/$handle/$index") 
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("player/{url}/{handle}/{index}") { backStackEntry ->
                val url = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", "UTF-8")
                val handle = backStackEntry.arguments?.getString("handle") ?: ""
                val index = backStackEntry.arguments?.getString("index")?.toIntOrNull() ?: 0
                FullMediaScreen(
                    folderUrl = url,
                    initialIndex = index,
                    initialHandle = handle,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
