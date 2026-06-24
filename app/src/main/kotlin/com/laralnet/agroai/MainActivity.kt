package com.laralnet.agroai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.laralnet.agroai.aimodel.infrastructure.oauth.HuggingFaceOAuthCallbackChannel
import com.laralnet.agroai.ui.navigation.AgroAINavGraph
import com.laralnet.agroai.ui.theme.AgroAITheme
import com.laralnet.agroai.ui.screens.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var oauthCallbackChannel: HuggingFaceOAuthCallbackChannel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleOAuthIntent(intent)
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val themeMode by settingsViewModel.themeMode.collectAsState()

            AgroAITheme(themeMode = themeMode) {
                AgroAINavGraph()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthIntent(intent)
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        oauthCallbackChannel.handleRedirectUri(uri)
    }
}
