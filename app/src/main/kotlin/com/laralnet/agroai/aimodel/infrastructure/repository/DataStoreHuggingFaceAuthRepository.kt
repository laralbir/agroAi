package com.laralnet.agroai.aimodel.infrastructure.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.laralnet.agroai.aimodel.domain.model.HuggingFaceCredential
import com.laralnet.agroai.aimodel.domain.repository.HuggingFaceAuthRepository
import com.laralnet.agroai.aimodel.infrastructure.oauth.HuggingFaceOAuthCallbackChannel
import com.laralnet.agroai.aimodel.infrastructure.oauth.HuggingFaceOAuthConfig
import com.laralnet.agroai.aimodel.infrastructure.oauth.HuggingFaceTokenService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class DataStoreHuggingFaceAuthRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val tokenService: HuggingFaceTokenService,
    private val callbackChannel: HuggingFaceOAuthCallbackChannel,
    @Named("applicationScope") private val applicationScope: CoroutineScope
) : HuggingFaceAuthRepository {

    private companion object {
        val KEY_ACCESS_TOKEN = stringPreferencesKey("hf_access_token")
        val KEY_REFRESH_TOKEN = stringPreferencesKey("hf_refresh_token")
        val KEY_EXPIRES_AT = longPreferencesKey("hf_token_expires_at")
        val KEY_USERNAME = stringPreferencesKey("hf_username")
        val KEY_AVATAR_URL = stringPreferencesKey("hf_avatar_url")
        // Legacy manual PAT key (kept for backward compatibility)
        val KEY_LEGACY_TOKEN = stringPreferencesKey("hf_token")
        const val TAG = "HFAuthRepository"
    }

    private val _oauthError = MutableStateFlow<String?>(null)
    val oauthError: StateFlow<String?> = _oauthError.asStateFlow()

    init {
        applicationScope.launch {
            callbackChannel.callbacks.collect { callback ->
                processOAuthCallback(callback.code, callback.codeVerifier)
            }
        }
    }

    override fun observeCredential(): Flow<HuggingFaceCredential?> = dataStore.data.map { prefs ->
        val accessToken = prefs[KEY_ACCESS_TOKEN] ?: prefs[KEY_LEGACY_TOKEN] ?: ""
        if (accessToken.isBlank()) return@map null
        HuggingFaceCredential(
            accessToken = accessToken,
            refreshToken = prefs[KEY_REFRESH_TOKEN] ?: "",
            expiresAt = prefs[KEY_EXPIRES_AT] ?: 0L,
            username = prefs[KEY_USERNAME] ?: "",
            avatarUrl = prefs[KEY_AVATAR_URL] ?: ""
        )
    }

    override suspend fun saveCredential(credential: HuggingFaceCredential) {
        dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = credential.accessToken
            prefs[KEY_REFRESH_TOKEN] = credential.refreshToken
            prefs[KEY_EXPIRES_AT] = credential.expiresAt
            prefs[KEY_USERNAME] = credential.username
            prefs[KEY_AVATAR_URL] = credential.avatarUrl
            prefs.remove(KEY_LEGACY_TOKEN)
        }
    }

    override suspend fun saveManualToken(token: String) {
        dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = token
            prefs.remove(KEY_REFRESH_TOKEN)
            prefs[KEY_EXPIRES_AT] = 0L
            prefs.remove(KEY_USERNAME)
            prefs.remove(KEY_AVATAR_URL)
            prefs.remove(KEY_LEGACY_TOKEN)
        }
    }

    override suspend fun clearCredential() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
            prefs.remove(KEY_EXPIRES_AT)
            prefs.remove(KEY_USERNAME)
            prefs.remove(KEY_AVATAR_URL)
            prefs.remove(KEY_LEGACY_TOKEN)
        }
    }

    override suspend fun getValidAccessToken(): String? {
        val prefs = dataStore.data.first()
        val accessToken = prefs[KEY_ACCESS_TOKEN] ?: prefs[KEY_LEGACY_TOKEN]
        if (accessToken.isNullOrBlank()) return null

        val expiresAt = prefs[KEY_EXPIRES_AT] ?: 0L
        val isExpired = expiresAt > 0L && System.currentTimeMillis() > expiresAt
        if (!isExpired) return accessToken

        val refreshToken = prefs[KEY_REFRESH_TOKEN] ?: return null
        return runCatching {
            val response = tokenService.refreshToken(
                refreshToken = refreshToken,
                clientId = HuggingFaceOAuthConfig.CLIENT_ID
            )
            val newExpiresAt = if (response.expiresIn > 0)
                System.currentTimeMillis() + response.expiresIn * 1000
            else 0L
            dataStore.edit { p ->
                p[KEY_ACCESS_TOKEN] = response.accessToken
                if (response.refreshToken.isNotBlank()) p[KEY_REFRESH_TOKEN] = response.refreshToken
                p[KEY_EXPIRES_AT] = newExpiresAt
            }
            response.accessToken
        }.onFailure { e ->
            Log.e(TAG, "Token refresh failed: ${e.message}")
        }.getOrNull()
    }

    private suspend fun processOAuthCallback(code: String, codeVerifier: String) {
        runCatching {
            val tokenResponse = tokenService.exchangeCode(
                code = code,
                redirectUri = HuggingFaceOAuthConfig.REDIRECT_URI,
                clientId = HuggingFaceOAuthConfig.CLIENT_ID,
                codeVerifier = codeVerifier
            )
            val userInfo = runCatching {
                tokenService.getUserInfo("Bearer ${tokenResponse.accessToken}")
            }.getOrNull()

            val expiresAt = if (tokenResponse.expiresIn > 0)
                System.currentTimeMillis() + tokenResponse.expiresIn * 1000
            else 0L

            saveCredential(
                HuggingFaceCredential(
                    accessToken = tokenResponse.accessToken,
                    refreshToken = tokenResponse.refreshToken,
                    expiresAt = expiresAt,
                    username = userInfo?.preferredUsername ?: userInfo?.name ?: "",
                    avatarUrl = userInfo?.picture ?: ""
                )
            )
            Log.i(TAG, "OAuth completed — user: ${userInfo?.preferredUsername}")
            _oauthError.value = null
        }.onFailure { e ->
            Log.e(TAG, "OAuth code exchange failed: ${e.message}")
            _oauthError.value = e.message ?: "Authentication failed"
        }
    }
}
