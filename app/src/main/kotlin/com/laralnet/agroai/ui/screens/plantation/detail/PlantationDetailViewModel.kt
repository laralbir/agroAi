package com.laralnet.agroai.ui.screens.plantation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laralnet.agroai.plantation.domain.model.Plantation
import com.laralnet.agroai.plantation.domain.repository.PlantationRepository
import com.laralnet.agroai.treatment.domain.model.Treatment
import com.laralnet.agroai.treatment.domain.repository.TreatmentRepository
import com.laralnet.agroai.weather.application.handler.RefreshWeatherHandler
import com.laralnet.agroai.weather.application.query.ObserveWeatherQuery
import com.laralnet.agroai.weather.domain.model.WeatherData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlantationDetailViewModel @Inject constructor(
    private val plantationRepository: PlantationRepository,
    private val treatmentRepository: TreatmentRepository,
    private val observeWeatherQuery: ObserveWeatherQuery,
    private val refreshWeatherHandler: RefreshWeatherHandler
) : ViewModel() {

    private val plantationId = MutableStateFlow<String?>(null)

    private val _plantation = MutableStateFlow<Plantation?>(null)
    val plantation: StateFlow<Plantation?> = _plantation

    val treatments: StateFlow<List<Treatment>> = plantationId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else treatmentRepository.observeByPlantation(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weather: StateFlow<WeatherData?> = _plantation
        .filterNotNull()
        .flatMapLatest { p ->
            if (p.location.hasCoordinates)
                observeWeatherQuery(p.location.latitude!!, p.location.longitude!!)
            else
                flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun load(id: String) = viewModelScope.launch {
        plantationId.value = id
        val p = plantationRepository.findById(id) ?: return@launch
        _plantation.value = p
        if (p.location.hasCoordinates) {
            refreshWeatherHandler.handle(p.location.latitude!!, p.location.longitude!!)
        }
    }
}
