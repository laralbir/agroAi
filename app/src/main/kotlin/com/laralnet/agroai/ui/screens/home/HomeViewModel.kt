package com.laralnet.agroai.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laralnet.agroai.aimodel.application.query.ObserveModelsQuery
import com.laralnet.agroai.plantation.domain.model.Plantation
import com.laralnet.agroai.plantation.domain.repository.PlantationRepository
import com.laralnet.agroai.treatment.application.query.ObserveUpcomingTreatmentsQuery
import com.laralnet.agroai.treatment.domain.model.Treatment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    plantationRepository: PlantationRepository,
    observeModels: ObserveModelsQuery,
    observeUpcomingTreatments: ObserveUpcomingTreatmentsQuery
) : ViewModel() {

    val plantations: StateFlow<List<Plantation>> = plantationRepository
        .observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hasActiveModel: StateFlow<Boolean> = observeModels()
        .map { models -> models.any { it.isActive } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val upcomingTreatments: StateFlow<List<Treatment>> = observeUpcomingTreatments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
