package com.laralnet.agroai.plantation.domain.model

data class Location(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String = "",
    val municipality: String = "",
    val province: String = "",
    val country: String = "ES",
    val municipalityCode: String = ""  // AEMET municipality code (INE code)
) {
    val hasCoordinates: Boolean
        get() = latitude != null && longitude != null

    val displayAddress: String
        get() = buildString {
            if (address.isNotBlank()) append(address)
            if (municipality.isNotBlank()) {
                if (isNotEmpty()) append(", ")
                append(municipality)
            }
            if (province.isNotBlank()) {
                if (isNotEmpty()) append(", ")
                append(province)
            }
        }
}
