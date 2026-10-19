package com.reelcounter.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.reelcounter.app.ui.ReelCounterApp
import com.reelcounter.app.ui.rememberReelCounterAppState
import com.reelcounter.core.common.network.NetworkMonitor
import com.reelcounter.core.designsystem.theme.ReelCounterTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val appState = rememberReelCounterAppState(networkMonitor = networkMonitor)
            ReelCounterTheme {
                ReelCounterApp(appState = appState)
            }
        }
    }
}
