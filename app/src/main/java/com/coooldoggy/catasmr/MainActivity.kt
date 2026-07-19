package com.coooldoggy.catasmr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.coooldoggy.catasmr.auth.YouTubeAuthManager
import com.coooldoggy.catasmr.settings.SettingsRepository
import com.coooldoggy.catasmr.ui.home.HomeScreen
import com.coooldoggy.catasmr.ui.settings.ScheduleSettingsScreen
import com.coooldoggy.catasmr.ui.theme.CatAsmrTheme
import kotlinx.coroutines.launch

private sealed class Screen {
    data object Home : Screen()
    data object Settings : Screen()
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
                var screen by remember { mutableStateOf<Screen>(Screen.Home) }
                val scope = rememberCoroutineScope()

                val consentLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartIntentSenderForResult()
                ) { result: ActivityResult ->
                    val data = result.data ?: return@rememberLauncherForActivityResult
                    val outcome = authManager.resultFromIntent(data)
                    if (outcome is YouTubeAuthManager.AuthOutcome.Authorized) {
                        scope.launch { settingsRepository.setAuthorization(true, outcome.accountEmail) }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (screen) {
                        Screen.Home -> HomeScreen(
                            onOpenSettings = { screen = Screen.Settings },
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
                                        is YouTubeAuthManager.AuthOutcome.Failed -> Unit
                                    }
                                }
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                        Screen.Settings -> ScheduleSettingsScreen(
                            onBack = { screen = Screen.Home },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
