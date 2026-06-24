package com.laralnet.agroai.aimodel.domain.repository

import com.laralnet.agroai.aimodel.domain.model.HuggingFaceCredential
import kotlinx.coroutines.flow.Flow

interface HuggingFaceAuthRepository {
    fun observeCredential(): Flow<HuggingFaceCredential?>
    suspend fun saveCredential(credential: HuggingFaceCredential)
    suspend fun saveManualToken(token: String)
    suspend fun clearCredential()
    /** Returns a valid access token, refreshing via OAuth if the current one is expired. */
    suspend fun getValidAccessToken(): String?
}
