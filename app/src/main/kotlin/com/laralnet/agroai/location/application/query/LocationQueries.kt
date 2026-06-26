package com.laralnet.agroai.location.application.query

import com.laralnet.agroai.location.domain.model.PlaceResult
import com.laralnet.agroai.location.domain.repository.LocationRepository
import javax.inject.Inject

class SearchPlacesQuery @Inject constructor(private val repository: LocationRepository) {
    suspend operator fun invoke(query: String): Result<List<PlaceResult>> =
        runCatching { repository.searchPlaces(query) }
}

class ReverseGeocodeQuery @Inject constructor(private val repository: LocationRepository) {
    suspend operator fun invoke(latitude: Double, longitude: Double): Result<PlaceResult?> =
        runCatching { repository.reverseGeocode(latitude, longitude) }
}

class GetCurrentLocationQuery @Inject constructor(private val repository: LocationRepository) {
    suspend operator fun invoke(): Result<PlaceResult?> =
        runCatching { repository.getCurrentLocation() }
}
