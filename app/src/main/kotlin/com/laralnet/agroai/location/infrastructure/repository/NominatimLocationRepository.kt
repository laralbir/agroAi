package com.laralnet.agroai.location.infrastructure.repository

import com.laralnet.agroai.location.domain.model.PlaceResult
import com.laralnet.agroai.location.domain.repository.LocationRepository
import com.laralnet.agroai.location.infrastructure.gps.GpsLocationProvider
import com.laralnet.agroai.location.infrastructure.nominatim.NominatimApiService
import com.laralnet.agroai.location.infrastructure.nominatim.NominatimPlace
import javax.inject.Inject

class NominatimLocationRepository @Inject constructor(
    private val api: NominatimApiService,
    private val gpsProvider: GpsLocationProvider
) : LocationRepository {

    override suspend fun searchPlaces(query: String): List<PlaceResult> =
        api.search(query).map { it.toPlaceResult() }

    override suspend fun reverseGeocode(latitude: Double, longitude: Double): PlaceResult =
        api.reverse(latitude, longitude).toPlaceResult()

    override suspend fun getCurrentLocation(): PlaceResult? {
        val loc = gpsProvider.getCurrentLocation().getOrNull() ?: return null
        return runCatching { reverseGeocode(loc.latitude, loc.longitude) }.getOrNull()
    }

    private fun NominatimPlace.toPlaceResult() = PlaceResult(
        displayName = displayName,
        latitude = latitude,
        longitude = longitude,
        address = address.streetAddress,
        municipality = address.resolvedMunicipality,
        province = address.state ?: address.county ?: "",
        country = address.countryCode?.uppercase() ?: "ES"
    )
}
