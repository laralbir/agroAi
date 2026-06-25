package com.laralnet.agroai

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.laralnet.agroai.aimodel.infrastructure.oauth.HuggingFaceOAuthCallbackChannel
import com.laralnet.agroai.ui.navigation.AgroAINavGraph
import com.laralnet.agroai.ui.theme.AgroAITheme
import com.laralnet.agroai.ui.screens.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var oauthCallbackChannel: HuggingFaceOAuthCallbackChannel

    override fun attachBaseContext(newBase: Context) {
        val langMode = newBase
            .getSharedPreferences(SettingsViewModel.LOCALE_PREFS, Context.MODE_PRIVATE)
            .getString(SettingsViewModel.LOCALE_KEY, "SYSTEM") ?: "SYSTEM"

        val locale = when (langMode) {
            "ENGLISH" -> Locale.ENGLISH
            "SPANISH" -> Locale("es")
            else -> newBase.resources.configuration.locales[0]
        }
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleOAuthIntent(intent)
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val languageCode by settingsViewModel.languageCode.collectAsState()

            // Recreate the Activity when the user changes the language so that
            // attachBaseContext runs again with the new locale.
            val initialLanguageCode = remember { mutableStateOf<String?>(null) }
            LaunchedEffect(languageCode) {
                val prev = initialLanguageCode.value
                initialLanguageCode.value = languageCode
                if (prev != null && prev != languageCode) {
                    recreate()
                }
            }

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
