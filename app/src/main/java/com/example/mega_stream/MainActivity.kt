package com.example.mega_stream

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.example.mega_stream.core.engine.CacheManager
import com.example.mega_stream.core.engine.MegaManager
import com.example.mega_stream.core.storage.DatabaseHelper
import com.example.mega_stream.ui.screens.*
import com.example.mega_stream.ui.theme.Mega_streamTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.decorView.isFocusable = true
        window.decorView.isFocusableInTouchMode = true
        window.decorView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES

        // PERFORMANCE FIX: Init MegaManager in a background thread to prevent blocking Main/UI thread
        CoroutineScope(Dispatchers.IO).launch {
            MegaManager.init(applicationContext)
        }
        
        setContent {
            Mega_streamTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        window.decorView.requestFocus()
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper.getInstance(context) }
    
    val startDest = if (dbHelper.isFirstLaunch()) "welcome" else "splash"

    NavHost(
        navController = navController, 
        startDestination = startDest,
        enterTransition = { fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.95f, animationSpec = tween(500)) },
        exitTransition = { fadeOut(animationSpec = tween(500)) },
        popEnterTransition = { fadeIn(animationSpec = tween(500)) },
        popExitTransition = { fadeOut(animationSpec = tween(500)) + scaleOut(targetScale = 0.95f, animationSpec = tween(500)) }
    ) {
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
            SplashScreen(onSyncComplete = {
                navController.navigate("home") { popUpTo("splash") { inclusive = true } }
            })
        }

        composable("sync_portal") {
            SyncScreen(
                onSyncComplete = {
                    navController.navigate("splash") { popUpTo("home") { inclusive = true } }
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable("home") {
            HomeScreen(
                onFolderSelected = { folder ->
                    val encodedName = URLEncoder.encode(folder.name, StandardCharsets.UTF_8.toString())
                    val encodedUrl = URLEncoder.encode(folder.url, StandardCharsets.UTF_8.toString())
                    navController.navigate("browser/$encodedName/$encodedUrl")
                },
                onSettingsSelected = { navController.navigate("onboarding_menu") },
                onSyncSelected = { navController.navigate("sync_portal") },
                onCompleteReset = {
                    dbHelper.resetAllData()
                    CacheManager.deleteAllCache(context)
                    navController.navigate("welcome") { popUpTo(0) { inclusive = true } }
                }
            )
        }
        
        composable(
            route = "browser/{name}/{url}",
            arguments = listOf(
                navArgument("name") { type = NavType.StringType },
                navArgument("url") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val name = URLDecoder.decode(backStackEntry.arguments?.getString("name") ?: "", StandardCharsets.UTF_8.toString())
            val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString())
            
            FolderBrowserScreen(
                folderName = name, 
                folderUrl = url, 
                onMediaSelected = { handle, index ->
                    val encodedHandle = URLEncoder.encode(handle, StandardCharsets.UTF_8.toString())
                    val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                    navController.navigate("player/$encodedHandle/$index/$encodedUrl")
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = "player/{handle}/{index}/{folderUrl}",
            arguments = listOf(
                navArgument("handle") { type = NavType.StringType },
                navArgument("index") { type = NavType.IntType },
                navArgument("folderUrl") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val handle = URLDecoder.decode(backStackEntry.arguments?.getString("handle") ?: "", StandardCharsets.UTF_8.toString())
            val index = backStackEntry.arguments?.getInt("index") ?: 0
            val folderUrl = URLDecoder.decode(backStackEntry.arguments?.getString("folderUrl") ?: "", StandardCharsets.UTF_8.toString())
            
            PlayerScreen(
                initialHandle = handle,
                initialIndex = index,
                folderUrl = folderUrl,
                onBack = { navController.popBackStack() },
                onHome = {
                    navController.navigate("home") { popUpTo("home") { inclusive = true } }
                }
            )
        }
    }
}
