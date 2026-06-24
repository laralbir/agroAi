package com.laralnet.agroai.ui.screens.plantation.wizard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laralnet.agroai.plantation.application.command.CreatePlantationCommand
import com.laralnet.agroai.plantation.application.handler.CreatePlantationHandler
import com.laralnet.agroai.plantation.domain.model.Location
import com.laralnet.agroai.plantation.domain.model.PlantType
import com.laralnet.agroai.plantation.domain.model.PlantationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class PlantForm(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val variety: String = "",
    val count: String = ""
)

data class PlantationWizardState(
    val name: String = "",
    val type: PlantationType? = null,
    val areaSqMeters: String = "",
    val notes: String = "",
    val address: String = "",
    val municipality: String = "",
    val province: String = "",
    val municipalityCode: String = "",
    val plantForms: List<PlantForm> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val createdId: String? = null
)

@HiltViewModel
class PlantationWizardViewModel @Inject constructor(
    private val createPlantationHandler: CreatePlantationHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlantationWizardState())
    val uiState: StateFlow<PlantationWizardState> = _uiState.asStateFlow()

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    fun setName(value: String) = _uiState.update { it.copy(name = value) }
    fun setArea(value: String) = _uiState.update { it.copy(areaSqMeters = value) }
    fun setNotes(value: String) = _uiState.update { it.copy(notes = value) }
    fun setType(type: PlantationType) = _uiState.update { it.copy(type = type) }
    fun setAddress(value: String) = _uiState.update { it.copy(address = value) }
    fun setMunicipality(value: String) = _uiState.update { it.copy(municipality = value) }
    fun setProvince(value: String) = _uiState.update { it.copy(province = value) }
    fun setMunicipalityCode(value: String) = _uiState.update { it.copy(municipalityCode = value) }

    fun addPlant() = _uiState.update { it.copy(plantForms = it.plantForms + PlantForm()) }

    fun updatePlant(index: Int, form: PlantForm) = _uiState.update {
        it.copy(plantForms = it.plantForms.toMutableList().also { list -> list[index] = form })
    }

    fun removePlant(index: Int) = _uiState.update {
        it.copy(plantForms = it.plantForms.toMutableList().also { list -> list.removeAt(index) })
    }

    fun nextStep() {
        val state = _uiState.value
        if (_currentStep.value < 3) _currentStep.update { it + 1 }
    }

    fun previousStep() {
        if (_currentStep.value > 0) _currentStep.update { it - 1 }
    }

    fun createPlantation() = viewModelScope.launch {
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true, error = null) }

        val command = CreatePlantationCommand(
            name = state.name,
            type = state.type ?: PlantationType.OTRO,
            location = Location(
                address = state.address,
                municipality = state.municipality,
                province = state.province,
                municipalityCode = state.municipalityCode
            ),
            areaSqMeters = state.areaSqMeters.toDoubleOrNull() ?: 0.0,
            plants = state.plantForms.filter { it.name.isNotBlank() }.map { form ->
                PlantType(
                    plantationId = "",
                    name = form.name,
                    variety = form.variety,
                    count = form.count.toIntOrNull() ?: 0
                )
            },
            notes = state.notes
        )

        createPlantationHandler.handle(command)
            .onSuccess { plantation ->
                _uiState.update { it.copy(isLoading = false, createdId = plantation.id) }
            }
            .onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
    }
}
