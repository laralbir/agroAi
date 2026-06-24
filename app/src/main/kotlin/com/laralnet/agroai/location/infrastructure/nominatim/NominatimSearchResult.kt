package com.laralnet.agroai.location.infrastructure.nominatim

import com.google.gson.annotations.SerializedName

data class NominatimPlace(
    @SerializedName("place_id") val placeId: Long = 0,
    @SerializedName("lat") val lat: String = "0",
    @SerializedName("lon") val lon: String = "0",
    @SerializedName("display_name") val displayName: String = "",
    @SerializedName("address") val address: NominatimAddress = NominatimAddress()
) {
    val latitude: Double get() = lat.toDoubleOrNull() ?: 0.0
    val longitude: Double get() = lon.toDoubleOrNull() ?: 0.0
}

data class NominatimAddress(
    @SerializedName("road") val road: String? = null,
    @SerializedName("house_number") val houseNumber: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("town") val town: String? = null,
    @SerializedName("village") val village: String? = null,
    @SerializedName("municipality") val municipality: String? = null,
    @SerializedName("county") val county: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("country_code") val countryCode: String? = null,
    @SerializedName("postcode") val postcode: String? = null
) {
    val resolvedMunicipality: String
        get() = city ?: town ?: village ?: municipality ?: county ?: ""

    val streetAddress: String
        get() = listOfNotNull(road, houseNumber).joinToString(" ")
}
