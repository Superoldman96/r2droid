package top.wsdx233.r2droid.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import top.wsdx233.r2droid.screen.home.HomeScreen
import top.wsdx233.r2droid.screen.install.InstallScreen
import top.wsdx233.r2droid.screen.permission.PermissionScreen
import top.wsdx233.r2droid.screen.project.ProjectScreen
import top.wsdx233.r2droid.ui.theme.R2droidTheme
import top.wsdx233.r2droid.util.PermissionManager
import top.wsdx233.r2droid.util.R2Installer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启动应用时检查并安装
        lifecycleScope.launch {
            R2Installer.checkAndInstall(applicationContext)
        }
        
        enableEdgeToEdge()
        setContent {
            R2droidTheme {
                // 监听全局安装状态
                val installState by R2Installer.installState.collectAsState()
                
                if (installState.isInstalling) {
                    InstallScreen(installState = installState)
                } else {
                    MainAppContent()
                }
            }
        }
    }
}

enum class AppScreen {
    Home,
    Project
}

@Composable
fun MainAppContent() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Check permission state
    var hasPermission by remember { mutableStateOf(PermissionManager.hasStoragePermission(context)) }

    // Re-check permission when app resumes (in case user went to settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = PermissionManager.hasStoragePermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (!hasPermission) {
        PermissionScreen(
            onPermissionGranted = { hasPermission = true }
        )
    } else {
        MainAppNavigation()
    }
}

@Composable
fun MainAppNavigation() {
    var currentScreen by remember { mutableStateOf(AppScreen.Home) }

    when (currentScreen) {
        AppScreen.Home -> {
            HomeScreen(
                onNavigateToProject = { currentScreen = AppScreen.Project }
            )
        }
        AppScreen.Project -> {
            BackHandler {
                currentScreen = AppScreen.Home
            }
            ProjectScreen()
        }
    }
}
