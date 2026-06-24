package com.laralnet.agroai.account.domain.model

data class GoogleAccount(
    val email: String,
    val displayName: String,
    val photoUri: String? = null,
    val isSelected: Boolean = false
)
