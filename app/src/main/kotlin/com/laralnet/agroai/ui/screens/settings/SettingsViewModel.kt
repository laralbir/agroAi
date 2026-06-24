package com.laralnet.agroai.ui.screens.settings

import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laralnet.agroai.aimodel.domain.model.HuggingFaceCredential
import com.laralnet.agroai.aimodel.domain.repository.HuggingFaceAuthRepository
import com.laralnet.agroai.aimodel.infrastructure.oauth.HuggingFaceOAuthCallbackChannel
import com.laralnet.agroai.aimodel.infrastructure.oauth.PkceHelper
import com.laralnet.agroai.aimodel.infrastructure.repository.DataStoreHuggingFaceAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ThemeMode { LIGHT, DARK, SYSTEM }
enum class LanguageMode { ENGLISH, SPANISH, SYSTEM }

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val hfAuthRepository: HuggingFaceAuthRepository,
    private val oauthCallbackChannel: HuggingFaceOAuthCallbackChannel
) : ViewModel() {

    companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_LANGUAGE = stringPreferencesKey("language_mode")
        val KEY_AEMET_API_KEY = stringPreferencesKey("aemet_api_key")
        val KEY_SELECTED_ACCOUNT = stringPreferencesKey("selected_google_account")
    }

    val themeMode = dataStore.data
        .map { prefs ->
            ThemeMode.valueOf(prefs[KEY_THEME] ?: ThemeMode.SYSTEM.name)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val languageCode = dataStore.data
        .map { prefs -> prefs[KEY_LANGUAGE] ?: LanguageMode.SYSTEM.name }
        .stateIn(viewModelScope, SharingStarted.Eagerly, LanguageMode.SYSTEM.name)

    val aemetApiKey = dataStore.data
        .map { prefs -> prefs[KEY_AEMET_API_KEY] ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val hfCredential = hfAuthRepository.observeCredential()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val hfOAuthError: Flow<String?> =
        (hfAuthRepository as? DataStoreHuggingFaceAuthRepository)?.oauthError
            ?: kotlinx.coroutines.flow.flowOf(null)

    private val _browserLaunchEvent = Channel<Uri>(Channel.BUFFERED)
    val browserLaunchEvent: Flow<Uri> = _browserLaunchEvent.receiveAsFlow()

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        dataStore.edit { it[KEY_THEME] = mode.name }
    }

    fun setLanguageMode(mode: LanguageMode) = viewModelScope.launch {
        dataStore.edit { it[KEY_LANGUAGE] = mode.name }
    }

    fun setAemetApiKey(key: String) = viewModelScope.launch {
        dataStore.edit { it[KEY_AEMET_API_KEY] = key }
    }

    fun connectHuggingFace() {
        val verifier = PkceHelper.generateCodeVerifier()
        val state = PkceHelper.generateState()
        oauthCallbackChannel.startFlow(verifier, state)
        val uri = oauthCallbackChannel.buildAuthorizationUri(verifier, state)
        _browserLaunchEvent.trySend(uri)
    }

    fun disconnectHuggingFace() = viewModelScope.launch {
        hfAuthRepository.clearCredential()
    }
}
