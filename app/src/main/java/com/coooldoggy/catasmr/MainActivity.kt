package com.coooldoggy.catasmr

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.coooldoggy.catasmr.auth.YouTubeAuthManager
import com.coooldoggy.catasmr.settings.SettingsRepository
import com.coooldoggy.catasmr.ui.home.HomeScreen
import com.coooldoggy.catasmr.ui.settings.ScheduleSettingsScreen
import com.coooldoggy.catasmr.ui.streaming.ConnectToDeviceScreen
import com.coooldoggy.catasmr.ui.theme.CatAsmrTheme
import kotlinx.coroutines.launch

private enum class Tab {
    HOME,
    REMOTE,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private lateinit var authManager: YouTubeAuthManager
    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authManager = YouTubeAuthManager(applicationContext)
        settingsRepository = SettingsRepository(applicationContext)
        enableEdgeToEdge()

        setContent {
            CatAsmrTheme {
                var selectedTab by remember { mutableStateOf(Tab.HOME) }
                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                val consentLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartIntentSenderForResult()
                ) { result: ActivityResult ->
                    val data = result.data
                    if (data == null) {
                        Toast.makeText(context, context.getString(com.coooldoggy.catasmr.R.string.youtube_sign_in_cancelled), Toast.LENGTH_SHORT).show()
                        return@rememberLauncherForActivityResult
                    }
                    when (val outcome = authManager.resultFromIntent(data)) {
                        is YouTubeAuthManager.AuthOutcome.Authorized -> {
                            scope.launch { settingsRepository.setAuthorization(true, outcome.accountEmail) }
                        }
                        is YouTubeAuthManager.AuthOutcome.Failed -> {
                            Toast.makeText(context, context.getString(com.coooldoggy.catasmr.R.string.youtube_sign_in_failed, outcome.message), Toast.LENGTH_LONG)
                                .show()
                        }
                        is YouTubeAuthManager.AuthOutcome.NeedsConsent -> Unit
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == Tab.HOME,
                                onClick = { selectedTab = Tab.HOME },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Home") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == Tab.REMOTE,
                                onClick = { selectedTab = Tab.REMOTE },
                                icon = { Icon(Icons.Default.Phone, contentDescription = "Remote") },
                                label = { Text("Remote") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == Tab.SETTINGS,
                                onClick = { selectedTab = Tab.SETTINGS },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") }
                            )
                        }
                    }
                ) { innerPadding ->
                    when (selectedTab) {
                        Tab.HOME -> HomeScreen(
                            onOpenSettings = { selectedTab = Tab.SETTINGS },
                            onOpenRemoteViewer = { selectedTab = Tab.REMOTE },
                            onOpenPreview = { },
                            onSignIn = {
                                scope.launch {
                                    when (val outcome = authManager.authorize()) {
                                        is YouTubeAuthManager.AuthOutcome.Authorized -> {
                                            settingsRepository.setAuthorization(true, outcome.accountEmail)
                                        }
                                        is YouTubeAuthManager.AuthOutcome.NeedsConsent -> {
                                            consentLauncher.launch(
                                                IntentSenderRequest.Builder(outcome.pendingIntent.intentSender).build()
                                            )
                                        }
                                        is YouTubeAuthManager.AuthOutcome.Failed -> {
                                            Toast.makeText(
                                                context,
                                                context.getString(com.coooldoggy.catasmr.R.string.youtube_sign_in_failed, outcome.message),
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                        )
                        Tab.REMOTE -> ConnectToDeviceScreen(
                            onBack = { selectedTab = Tab.HOME },
                            onConnect = { deviceName, ipAddress, port ->
                                Toast.makeText(context, "Connecting to $deviceName...", Toast.LENGTH_SHORT).show()
                            },
                            onConnectCloud = { pairingCode ->
                                Toast.makeText(context, "Connecting to cloud...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                        )
                        Tab.SETTINGS -> ScheduleSettingsScreen(
                            onBack = { selectedTab = Tab.HOME },
                            onSignIn = {
                                scope.launch {
                                    when (val outcome = authManager.authorize()) {
                                        is YouTubeAuthManager.AuthOutcome.Authorized -> {
                                            settingsRepository.setAuthorization(true, outcome.accountEmail)
                                        }
                                        is YouTubeAuthManager.AuthOutcome.NeedsConsent -> {
                                            consentLauncher.launch(
                                                IntentSenderRequest.Builder(outcome.pendingIntent.intentSender).build()
                                            )
                                        }
                                        is YouTubeAuthManager.AuthOutcome.Failed -> {
                                            Toast.makeText(
                                                context,
                                                context.getString(com.coooldoggy.catasmr.R.string.youtube_sign_in_failed, outcome.message),
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                        )
                    }
                }
            }
        }
    }
}
