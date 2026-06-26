package com.laralnet.agroai.location.domain.model

data class PlaceResult(
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val municipality: String = "",
    val province: String = "",
    val country: String = ""
)
