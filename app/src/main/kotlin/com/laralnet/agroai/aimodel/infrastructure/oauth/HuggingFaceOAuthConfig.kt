package com.laralnet.agroai.aimodel.infrastructure.oauth

import com.laralnet.agroai.BuildConfig

object HuggingFaceOAuthConfig {
    val CLIENT_ID: String = BuildConfig.HF_CLIENT_ID
    const val REDIRECT_URI = "agroai://oauth-callback-huggingface"
    const val BASE_URL = "https://huggingface.co/"
    const val AUTHORIZATION_PATH = "oauth/authorize"
    const val TOKEN_PATH = "oauth/token"
    const val USERINFO_PATH = "oauth/userinfo"
    const val SCOPE = "openid profile read-repos"
}
