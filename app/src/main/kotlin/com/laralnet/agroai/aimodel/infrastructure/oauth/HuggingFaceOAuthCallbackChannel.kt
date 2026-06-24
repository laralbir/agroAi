package com.laralnet.agroai.aimodel.infrastructure.oauth

import android.net.Uri
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton bridge between MainActivity.onNewIntent (which receives the OAuth redirect)
 * and DataStoreHuggingFaceAuthRepository (which processes the code exchange).
 *
 * Also holds the in-flight PKCE verifier and authorization URI builder.
 */
@Singleton
class HuggingFaceOAuthCallbackChannel @Inject constructor() {

    private var pendingCodeVerifier: String? = null
    private var pendingState: String? = null

    private val _callbacks = Channel<OAuthCallback>(Channel.BUFFERED)
    val callbacks: Flow<OAuthCallback> = _callbacks.receiveAsFlow()

    /** Called by the ViewModel before opening the browser. */
    fun startFlow(codeVerifier: String, state: String) {
        pendingCodeVerifier = codeVerifier
        pendingState = state
    }

    /**
     * Called by MainActivity when the deep-link redirect arrives.
     * Returns true if the URI matched a pending flow and the callback was queued.
     */
    fun handleRedirectUri(uri: Uri): Boolean {
        if (uri.scheme != "agroai" || uri.host != "oauth-callback-huggingface") return false
        val code = uri.getQueryParameter("code") ?: return false
        val verifier = pendingCodeVerifier ?: return false
        pendingCodeVerifier = null
        pendingState = null
        return _callbacks.trySend(OAuthCallback(code, verifier)).isSuccess
    }

    /** Builds the authorization URL that the app opens in the browser. */
    fun buildAuthorizationUri(codeVerifier: String, state: String): Uri {
        val challenge = PkceHelper.generateCodeChallenge(codeVerifier)
        return Uri.parse("${HuggingFaceOAuthConfig.BASE_URL}${HuggingFaceOAuthConfig.AUTHORIZATION_PATH}")
            .buildUpon()
            .appendQueryParameter("client_id", HuggingFaceOAuthConfig.CLIENT_ID)
            .appendQueryParameter("redirect_uri", HuggingFaceOAuthConfig.REDIRECT_URI)
            .appendQueryParameter("scope", HuggingFaceOAuthConfig.SCOPE)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("state", state)
            .build()
    }

    data class OAuthCallback(val code: String, val codeVerifier: String)
}
