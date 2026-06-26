package com.laralnet.agroai.ui.screens.plantation.wizard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laralnet.agroai.location.application.query.GetCurrentLocationQuery
import com.laralnet.agroai.location.application.query.ReverseGeocodeQuery
import com.laralnet.agroai.location.application.query.SearchPlacesQuery
import com.laralnet.agroai.location.domain.model.PlaceResult
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
    val searchResults: List<PlaceResult> = emptyList(),
    val isSearching: Boolean = false,
    val isGpsLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LocationPickerViewModel @Inject constructor(
    private val searchPlacesQuery: SearchPlacesQuery,
    private val reverseGeocodeQuery: ReverseGeocodeQuery,
    private val getCurrentLocationQuery: GetCurrentLocationQuery
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
            searchPlacesQuery(query)
                .onSuccess { results -> _state.update { it.copy(searchResults = results, isSearching = false) } }
                .onFailure { e -> _state.update { it.copy(isSearching = false, error = e.message) } }
        }
    }

    fun onPlaceSelected(place: PlaceResult) {
        _state.update {
            it.copy(
                markerPosition = GeoPoint(place.latitude, place.longitude),
                resolvedLocation = place.toLocation(),
                searchResults = emptyList(),
                searchQuery = place.municipality.ifBlank { place.displayName }
            )
        }
    }

    fun onMapTap(point: GeoPoint) {
        _state.update { it.copy(markerPosition = point, resolvedLocation = null) }
        viewModelScope.launch {
            reverseGeocodeQuery(point.latitude, point.longitude)
                .onSuccess { place -> place?.let { _state.update { s -> s.copy(resolvedLocation = it.toLocation()) } } }
        }
    }

    fun onUseGps() = viewModelScope.launch {
        _state.update { it.copy(isGpsLoading = true, error = null) }
        getCurrentLocationQuery()
            .onSuccess { place ->
                place?.let {
                    val point = GeoPoint(it.latitude, it.longitude)
                    _state.update { s -> s.copy(markerPosition = point, isGpsLoading = false, resolvedLocation = it.toLocation()) }
                } ?: _state.update { it.copy(isGpsLoading = false) }
            }
            .onFailure { e ->
                _state.update { it.copy(isGpsLoading = false, error = e.message) }
            }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    private fun PlaceResult.toLocation() = Location(
        latitude = latitude,
        longitude = longitude,
        address = address,
        municipality = municipality,
        province = province,
        country = country
    )
}
