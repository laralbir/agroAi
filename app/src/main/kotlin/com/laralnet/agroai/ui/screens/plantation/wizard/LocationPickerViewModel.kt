package com.laralnet.agroai.ui.screens.plantation.wizard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laralnet.agroai.location.infrastructure.gps.GpsLocationProvider
import com.laralnet.agroai.location.infrastructure.nominatim.NominatimApiService
import com.laralnet.agroai.location.infrastructure.nominatim.NominatimPlace
import com.laralnet.agroai.plantation.domain.model.Location
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

data class LocationPickerState(
    val markerPosition: GeoPoint? = null,
    val resolvedLocation: Location? = null,
    val searchQuery: String = "",
    val searchResults: List<NominatimPlace> = emptyList(),
    val isSearching: Boolean = false,
    val isGpsLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LocationPickerViewModel @Inject constructor(
    private val nominatim: NominatimApiService,
    private val gpsProvider: GpsLocationProvider
) : ViewModel() {

    private val _state = MutableStateFlow(LocationPickerState())
    val state: StateFlow<LocationPickerState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query, searchResults = emptyList()) }
        if (query.length < 3) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500) // debounce — also respects Nominatim 1 req/s policy
            _state.update { it.copy(isSearching = true) }
            nominatim.runCatching { search(query) }
                .onSuccess { results -> _state.update { it.copy(searchResults = results, isSearching = false) } }
                .onFailure { _state.update { it.copy(isSearching = false, error = it.message) } }
        }
    }

    fun onPlaceSelected(place: NominatimPlace) {
        _state.update {
            it.copy(
                markerPosition = GeoPoint(place.latitude, place.longitude),
                resolvedLocation = place.toLocation(),
                searchResults = emptyList(),
                searchQuery = place.address.resolvedMunicipality.ifBlank { place.displayName }
            )
        }
    }

    fun onMapTap(point: GeoPoint) {
        _state.update { it.copy(markerPosition = point, resolvedLocation = null) }
        viewModelScope.launch {
            nominatim.runCatching { reverse(point.latitude, point.longitude) }
                .onSuccess { place -> _state.update { it.copy(resolvedLocation = place.toLocation()) } }
        }
    }

    fun onUseGps() = viewModelScope.launch {
        _state.update { it.copy(isGpsLoading = true, error = null) }
        gpsProvider.getCurrentLocation()
            .onSuccess { loc ->
                val point = GeoPoint(loc.latitude, loc.longitude)
                _state.update { it.copy(markerPosition = point, isGpsLoading = false) }
                nominatim.runCatching { reverse(loc.latitude, loc.longitude) }
                    .onSuccess { place -> _state.update { it.copy(resolvedLocation = place.toLocation()) } }
            }
            .onFailure { e ->
                _state.update { it.copy(isGpsLoading = false, error = e.message) }
            }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    private fun NominatimPlace.toLocation() = Location(
        latitude = latitude,
        longitude = longitude,
        address = address.streetAddress,
        municipality = address.resolvedMunicipality,
        province = address.state ?: address.county ?: "",
        country = address.countryCode?.uppercase() ?: "ES"
    )
}
