package com.laralnet.agroai.ui.screens.plantation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laralnet.agroai.plantation.domain.model.Plantation
import com.laralnet.agroai.plantation.domain.repository.PlantationRepository
import com.laralnet.agroai.treatment.domain.model.Treatment
import com.laralnet.agroai.treatment.domain.repository.TreatmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlantationDetailViewModel @Inject constructor(
    private val plantationRepository: PlantationRepository,
    private val treatmentRepository: TreatmentRepository
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

    fun load(id: String) = viewModelScope.launch {
        plantationId.value = id
        _plantation.value = plantationRepository.findById(id)
    }
}
