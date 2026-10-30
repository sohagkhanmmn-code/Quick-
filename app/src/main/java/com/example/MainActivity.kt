package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ai.configureAppCheck
import com.example.ui.JarvisViewModel
import com.example.ui.components.MiniHudOverlay
import com.example.ui.screens.AutomationsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VisionScreen
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisMuted
import com.example.ui.theme.JarvisObsidianDark
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.JarvisWhite
import com.example.ui.theme.MyApplicationTheme

enum class JarvisNavTab(val title: String, val icon: ImageVector) {
    HOME("HUD", Icons.Default.Home),
    TOOLS("Tools", Icons.Default.Build),
    VISION("Vision", Icons.Default.Visibility),
    MEMORY("Memory", Icons.Default.Memory),
    SETTINGS("Config", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: JarvisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var currentTab by remember { mutableStateOf(JarvisNavTab.HOME) }

                // Audio Permission Launcher
                val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        viewModel.startListening()
                    } else {
                        Toast.makeText(this, "Microphone permission is required for voice commands", Toast.LENGTH_SHORT).show()
                    }
                }

                val requestRecordPermission = {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPerm) {
                        viewModel.startListening()
                    } else {
                        recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                // Camera Permission Launcher
                val cameraPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (!isGranted) {
                        Toast.makeText(this, "Camera permission needed for vision input", Toast.LENGTH_SHORT).show()
                    }
                }

                val requestCameraPermission = {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!hasPerm) {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }

                // Handle back button on sub-screens to return to Home HUD
                BackHandler(enabled = currentTab != JarvisNavTab.HOME) {
                    currentTab = JarvisNavTab.HOME
                }

                val isMiniHudActive by viewModel.isMiniHudActive.collectAsState()
                val assistantState by viewModel.assistantState.collectAsState()
                val audioAmp by viewModel.audioAmplitude.collectAsState()
                val transcript by viewModel.userTranscript.collectAsState()
                val jarvisSpeech by viewModel.jarvisSpeechText.collectAsState()
                val isTorchOn by viewModel.isTorchOn.collectAsState()
                val isPowerSavingMode by viewModel.isPowerSavingMode.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = JarvisObsidianDark,
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .border(1.dp, JarvisBorder),
                            containerColor = JarvisSurfaceDark,
                            contentColor = JarvisCyan
                        ) {
                            JarvisNavTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.title,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = JarvisObsidianDark,
                                        selectedTextColor = JarvisCyan,
                                        indicatorColor = JarvisCyan,
                                        unselectedIconColor = JarvisMuted,
                                        unselectedTextColor = JarvisMuted
                                    ),
                                    modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Crossfade(targetState = currentTab, label = "ScreenCrossfade") { tab ->
                            when (tab) {
                                JarvisNavTab.HOME -> HomeScreen(
                                    viewModel = viewModel,
                                    onRequestRecordPermission = requestRecordPermission
                                )
                                JarvisNavTab.TOOLS -> AutomationsScreen(
                                    viewModel = viewModel
                                )
                                JarvisNavTab.VISION -> VisionScreen(
                                    viewModel = viewModel,
                                    onRequestCameraPermission = requestCameraPermission
                                )
                                JarvisNavTab.MEMORY -> MemoryScreen(
                                    viewModel = viewModel
                                )
                                JarvisNavTab.SETTINGS -> SettingsScreen(
                                    viewModel = viewModel
                                )
                            }
                        }

                        // Floating Mini-HUD Overlay
                        MiniHudOverlay(
                            isVisible = isMiniHudActive,
                            state = assistantState,
                            audioAmplitude = audioAmp,
                            transcript = transcript,
                            jarvisResponse = jarvisSpeech,
                            isTorchOn = isTorchOn,
                            isPowerSavingMode = isPowerSavingMode,
                            onToggleTorch = { viewModel.toggleTorch() },
                            onMicClick = requestRecordPermission,
                            onStopClick = { viewModel.interruptBargeIn() },
                            onClose = { viewModel.toggleMiniHud() },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }
}
