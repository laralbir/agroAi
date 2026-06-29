package com.laralnet.agroai.ui.screens.action

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laralnet.agroai.action.application.command.CompleteActionCommand
import com.laralnet.agroai.action.application.command.DeleteActionCommand
import com.laralnet.agroai.action.application.handler.CompleteActionHandler
import com.laralnet.agroai.action.application.handler.DeleteActionHandler
import com.laralnet.agroai.action.domain.model.PlantationAction
import com.laralnet.agroai.action.domain.repository.PlantationActionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PlantationActionRepository,
    private val completeActionHandler: CompleteActionHandler,
    private val deleteActionHandler: DeleteActionHandler
) : ViewModel() {

    private val actionId: String = checkNotNull(savedStateHandle["actionId"])

    private val _action = MutableStateFlow<PlantationAction?>(null)
    val action: StateFlow<PlantationAction?> = _action

    private val _navigateUp = MutableStateFlow(false)
    val navigateUp: StateFlow<Boolean> = _navigateUp

    init {
        viewModelScope.launch {
            _action.value = repository.findById(actionId)
        }
    }

    fun complete(notes: String = "") = viewModelScope.launch {
        completeActionHandler.handle(CompleteActionCommand(id = actionId, notes = notes))
        _action.value = repository.findById(actionId)
    }

    fun delete() = viewModelScope.launch {
        deleteActionHandler.handle(DeleteActionCommand(id = actionId))
        _navigateUp.value = true
    }
}
