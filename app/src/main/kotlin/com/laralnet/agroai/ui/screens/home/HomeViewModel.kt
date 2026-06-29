package com.laralnet.agroai.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laralnet.agroai.aimodel.application.query.GetWorkerNextRunQuery
import com.laralnet.agroai.aimodel.application.query.ObserveModelsQuery
import com.laralnet.agroai.location.application.query.GetCurrentLocationQuery
import com.laralnet.agroai.plantation.domain.model.Plantation
import com.laralnet.agroai.plantation.domain.repository.PlantationRepository
import com.laralnet.agroai.treatment.application.query.ObserveUpcomingTreatmentsQuery
import com.laralnet.agroai.treatment.domain.model.Treatment
import com.laralnet.agroai.weather.application.handler.RefreshWeatherHandler
import com.laralnet.agroai.weather.application.query.ObserveWeatherQuery
import com.laralnet.agroai.weather.domain.model.WeatherData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val plantationRepository: PlantationRepository,
    observeModels: ObserveModelsQuery,
    observeUpcomingTreatments: ObserveUpcomingTreatmentsQuery,
    private val getCurrentLocation: GetCurrentLocationQuery,
    private val refreshWeatherHandler: RefreshWeatherHandler,
    private val observeWeatherQuery: ObserveWeatherQuery,
    private val getWorkerNextRunQuery: GetWorkerNextRunQuery
) : ViewModel() {

    val plantations: StateFlow<List<Plantation>> = plantationRepository
        .observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hasActiveModel: StateFlow<Boolean> = observeModels()
        .map { models -> models.any { it.isActive } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val allUpcoming: Flow<List<Treatment>> = observeUpcomingTreatments()

    val todayTreatments: StateFlow<List<Treatment>> = allUpcoming
        .map { all ->
            val zone = ZoneId.systemDefault()
            val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant()
            val tomorrowStart = todayStart.plusSeconds(86_400)
            all.filter { it.scheduledAt >= todayStart && it.scheduledAt < tomorrowStart }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingTreatments: StateFlow<List<Treatment>> = allUpcoming
        .map { all ->
            val zone = ZoneId.systemDefault()
            val tomorrowStart = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant()
            all.filter { it.scheduledAt >= tomorrowStart }.take(5)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _homeLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    private val _homeLocationName = MutableStateFlow<String?>(null)
    val homeLocationName: StateFlow<String?> = _homeLocationName.asStateFlow()

    private val _isWeatherLoading = MutableStateFlow(true)
    val isWeatherLoading: StateFlow<Boolean> = _isWeatherLoading.asStateFlow()

    val homeWeather: StateFlow<WeatherData?> = _homeLocation
        .flatMapLatest { loc ->
            if (loc == null) flowOf(null)
            else observeWeatherQuery(loc.first, loc.second)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ms until next scheduled periodic worker run; null if not scheduled or unknown
    val workerNextRunMs: StateFlow<Long?> = getWorkerNextRunQuery()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // cached weather keyed by plantation id (no new network calls — Room only)
    val weatherByPlantation: StateFlow<Map<String, WeatherData?>> = plantations
        .flatMapLatest { list ->
            val withCoords = list.filter { it.location.hasCoordinates }
            if (withCoords.isEmpty()) return@flatMapLatest flowOf(emptyMap())
            val weatherFlows = withCoords.map { p ->
                observeWeatherQuery(p.location.latitude!!, p.location.longitude!!)
                    .map { weather -> p.id to weather }
            }
            combine(weatherFlows) { entries -> entries.toMap() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        viewModelScope.launch {
            try {
                val place = getCurrentLocation().getOrNull()
                if (place != null) {
                    _homeLocation.value = Pair(place.latitude, place.longitude)
                    _homeLocationName.value = place.municipality.ifBlank { place.displayName }
                    refreshWeatherHandler.handleIfStale(place.latitude, place.longitude)
                } else {
                    // Fallback: first plantation with GPS coordinates
                    plantationRepository.observeAll()
                        .firstOrNull()
                        ?.firstOrNull { it.location.hasCoordinates }
                        ?.location
                        ?.let { loc ->
                            _homeLocation.value = Pair(loc.latitude!!, loc.longitude!!)
                            _homeLocationName.value = loc.municipality.ifBlank { loc.displayAddress }
                            refreshWeatherHandler.handleIfStale(loc.latitude!!, loc.longitude!!)
                        }
                }
            } finally {
                _isWeatherLoading.value = false
            }
        }
    }
}
