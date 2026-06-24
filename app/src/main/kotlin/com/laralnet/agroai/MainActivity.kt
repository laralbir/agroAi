package com.laralnet.agroai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.laralnet.agroai.ui.navigation.AgroAINavGraph
import com.laralnet.agroai.ui.theme.AgroAITheme
import com.laralnet.agroai.ui.screens.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val languageCode by settingsViewModel.languageCode.collectAsState()

            AgroAITheme(themeMode = themeMode) {
                AgroAINavGraph()
            }
        }
    }
}
