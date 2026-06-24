package com.laralnet.agroai.aimodel.infrastructure.oauth

import com.google.gson.annotations.SerializedName

data class HuggingFaceTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Long = 0L,
    @SerializedName("refresh_token") val refreshToken: String = "",
    @SerializedName("scope") val scope: String = ""
)

data class HuggingFaceUserInfoResponse(
    @SerializedName("sub") val sub: String = "",
    @SerializedName("preferred_username") val preferredUsername: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("picture") val picture: String = "",
    @SerializedName("email") val email: String = ""
)
