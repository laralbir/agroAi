package com.laralnet.agroai.location.domain.repository

import com.laralnet.agroai.location.domain.model.PlaceResult

interface LocationRepository {
    suspend fun searchPlaces(query: String): List<PlaceResult>
    suspend fun reverseGeocode(latitude: Double, longitude: Double): PlaceResult?
    /** Returns GPS fix + reverse-geocoded address, or null if GPS is unavailable. */
    suspend fun getCurrentLocation(): PlaceResult?
}
