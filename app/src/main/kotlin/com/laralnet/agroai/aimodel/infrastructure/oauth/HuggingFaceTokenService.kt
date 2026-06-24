package com.laralnet.agroai.aimodel.infrastructure.oauth

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface HuggingFaceTokenService {

    @POST("oauth/token")
    @FormUrlEncoded
    suspend fun exchangeCode(
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("client_id") clientId: String,
        @Field("code_verifier") codeVerifier: String
    ): HuggingFaceTokenResponse

    @POST("oauth/token")
    @FormUrlEncoded
    suspend fun refreshToken(
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("refresh_token") refreshToken: String,
        @Field("client_id") clientId: String
    ): HuggingFaceTokenResponse

    @GET("oauth/userinfo")
    suspend fun getUserInfo(
        @Header("Authorization") bearer: String
    ): HuggingFaceUserInfoResponse
}
