package com.laralnet.agroai.aimodel.domain.model

data class HuggingFaceCredential(
    val accessToken: String,
    val refreshToken: String = "",
    val expiresAt: Long = 0L,      // epoch millis; 0 = never expires (manual PAT)
    val username: String = "",
    val avatarUrl: String = ""
) {
    val isExpired: Boolean
        get() = expiresAt > 0L && System.currentTimeMillis() > expiresAt

    val isOAuthToken: Boolean
        get() = expiresAt > 0L
}
