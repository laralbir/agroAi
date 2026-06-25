package com.laralnet.agroai.ui.screens.plantation.wizard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laralnet.agroai.plantation.application.command.CreatePlantationCommand
import com.laralnet.agroai.plantation.application.command.UpdatePlantationCommand
import com.laralnet.agroai.plantation.application.handler.CreatePlantationHandler
import com.laralnet.agroai.plantation.application.handler.UpdatePlantationHandler
import com.laralnet.agroai.plantation.domain.model.Location
import com.laralnet.agroai.plantation.domain.model.PlantType
import com.laralnet.agroai.plantation.domain.model.PlantationType
import com.laralnet.agroai.plantation.domain.repository.PlantationRepository
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
    val plantationId: String? = null,
    val name: String = "",
    val type: PlantationType? = null,
    val areaSqMeters: String = "",
    val notes: String = "",
    val address: String = "",
    val municipality: String = "",
    val province: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val plantForms: List<PlantForm> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val savedId: String? = null
) {
    val isEditMode: Boolean get() = plantationId != null
}

@HiltViewModel
class PlantationWizardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val createPlantationHandler: CreatePlantationHandler,
    private val updatePlantationHandler: UpdatePlantationHandler,
    private val plantationRepository: PlantationRepository
) : ViewModel() {

    private val editingId: String? = savedStateHandle["id"]

    private val _uiState = MutableStateFlow(PlantationWizardState())
    val uiState: StateFlow<PlantationWizardState> = _uiState.asStateFlow()

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    init {
        editingId?.let { loadForEdit(it) }
    }

    private fun loadForEdit(id: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        val plantation = plantationRepository.findById(id)
        if (plantation != null) {
            _uiState.update { state ->
                state.copy(
                    plantationId = id,
                    name = plantation.name,
                    type = plantation.type,
                    areaSqMeters = plantation.areaSqMeters.let { a ->
                        if (a == a.toLong().toDouble()) a.toLong().toString() else a.toString()
                    },
                    notes = plantation.notes,
                    address = plantation.location.address,
                    municipality = plantation.location.municipality,
                    province = plantation.location.province,
                    latitude = plantation.location.latitude,
                    longitude = plantation.location.longitude,
                    plantForms = plantation.plants.map { plant ->
                        PlantForm(
                            name = plant.name,
                            variety = plant.variety,
                            count = if (plant.count > 0) plant.count.toString() else ""
                        )
                    },
                    isLoading = false
                )
            }
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun setName(value: String) = _uiState.update { it.copy(name = value) }
    fun setArea(value: String) = _uiState.update { it.copy(areaSqMeters = value) }
    fun setNotes(value: String) = _uiState.update { it.copy(notes = value) }
    fun setType(type: PlantationType) = _uiState.update { it.copy(type = type) }
    fun setAddress(value: String) = _uiState.update { it.copy(address = value) }
    fun setMunicipality(value: String) = _uiState.update { it.copy(municipality = value) }
    fun setProvince(value: String) = _uiState.update { it.copy(province = value) }

    fun setLocationFromMap(location: Location) = _uiState.update {
        it.copy(
            latitude = location.latitude,
            longitude = location.longitude,
            address = location.address,
            municipality = location.municipality,
            province = location.province
        )
    }

    fun addPlant() = _uiState.update { it.copy(plantForms = listOf(PlantForm()) + it.plantForms) }

    fun updatePlant(index: Int, form: PlantForm) = _uiState.update {
        it.copy(plantForms = it.plantForms.toMutableList().also { list -> list[index] = form })
    }

    fun removePlant(index: Int) = _uiState.update {
        it.copy(plantForms = it.plantForms.toMutableList().also { list -> list.removeAt(index) })
    }

    fun nextStep() {
        if (_currentStep.value < 3) _currentStep.update { it + 1 }
    }

    fun previousStep() {
        if (_currentStep.value > 0) _currentStep.update { it - 1 }
    }

    fun save() {
        if (_uiState.value.isEditMode) updatePlantation()
        else createPlantation()
    }

    private fun createPlantation() = viewModelScope.launch {
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true, error = null) }

        val command = CreatePlantationCommand(
            name = state.name,
            type = state.type ?: PlantationType.OTRO,
            location = Location(
                latitude = state.latitude,
                longitude = state.longitude,
                address = state.address,
                municipality = state.municipality,
                province = state.province
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
                _uiState.update { it.copy(isLoading = false, savedId = plantation.id) }
            }
            .onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
    }

    private fun updatePlantation() = viewModelScope.launch {
        val state = _uiState.value
        val id = state.plantationId ?: return@launch
        _uiState.update { it.copy(isLoading = true, error = null) }

        val command = UpdatePlantationCommand(
            id = id,
            name = state.name,
            type = state.type ?: PlantationType.OTRO,
            location = Location(
                latitude = state.latitude,
                longitude = state.longitude,
                address = state.address,
                municipality = state.municipality,
                province = state.province
            ),
            areaSqMeters = state.areaSqMeters.toDoubleOrNull() ?: 0.0,
            plants = state.plantForms.filter { it.name.isNotBlank() }.map { form ->
                PlantType(
                    plantationId = id,
                    name = form.name,
                    variety = form.variety,
                    count = form.count.toIntOrNull() ?: 0
                )
            },
            notes = state.notes,
            googleAccountEmail = null
        )

        updatePlantationHandler.handle(command)
            .onSuccess {
                _uiState.update { it.copy(isLoading = false, savedId = id) }
            }
            .onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
    }
}
