package com.laralnet.agroai.ui.screens.prompteditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laralnet.agroai.aimodel.application.command.SavePromptTemplateCommand
import com.laralnet.agroai.aimodel.application.handler.SavePromptTemplateHandler
import com.laralnet.agroai.aimodel.application.query.ObservePromptTemplatesQuery
import com.laralnet.agroai.aimodel.domain.model.PromptTemplate
import com.laralnet.agroai.aimodel.domain.model.PromptWarningLevel
import com.laralnet.agroai.aimodel.domain.repository.AIModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PromptEditorUiState(
    val templates: List<PromptTemplate> = emptyList(),
    val selectedId: String? = null,
    val editContent: String = "",
    val isModified: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val savedOk: Boolean = false,
    val showWarningDialog: Boolean = false,
    val showResetDialog: Boolean = false
) {
    val selectedTemplate: PromptTemplate? get() = templates.find { it.id == selectedId }
}

@HiltViewModel
class PromptEditorViewModel @Inject constructor(
    private val observePromptTemplatesQuery: ObservePromptTemplatesQuery,
    private val savePromptTemplateHandler: SavePromptTemplateHandler,
    private val repository: AIModelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PromptEditorUiState())
    val uiState: StateFlow<PromptEditorUiState> = _uiState

    init {
        viewModelScope.launch {
            observePromptTemplatesQuery().collect { templates ->
                if (templates.isEmpty()) {
                    seedDefaults()
                } else {
                    val currentId = _uiState.value.selectedId
                    _uiState.update { state ->
                        val stillSelected = templates.find { it.id == currentId }
                        state.copy(
                            templates = templates,
                            isLoading = false,
                            // Refresh editContent if the selected template was re-saved externally
                            editContent = if (stillSelected != null && !state.isModified)
                                stillSelected.content
                            else
                                state.editContent
                        )
                    }
                }
            }
        }
    }

    private suspend fun seedDefaults() {
        listOf(
            PromptTemplate.photoAnalysisDefault(),
            PromptTemplate.plantationHealthDefault(),
            PromptTemplate.weatherAdjustmentDefault()
        ).forEach { repository.savePromptTemplate(it) }
    }

    fun selectTemplate(id: String) {
        val template = _uiState.value.templates.find { it.id == id } ?: return
        _uiState.update {
            it.copy(
                selectedId = id,
                editContent = template.content,
                isModified = false,
                savedOk = false
            )
        }
    }

    fun onContentChanged(text: String) {
        _uiState.update { state ->
            state.copy(
                editContent = text,
                isModified = text != (state.selectedTemplate?.content ?: "")
            )
        }
    }

    fun savePrompt() {
        val template = _uiState.value.selectedTemplate ?: return
        if (template.warningLevel >= PromptWarningLevel.MEDIUM) {
            _uiState.update { it.copy(showWarningDialog = true) }
        } else {
            confirmSave()
        }
    }

    fun confirmSave() {
        val state = _uiState.value
        val template = state.selectedTemplate ?: return
        _uiState.update { it.copy(showWarningDialog = false, isSaving = true) }
        viewModelScope.launch {
            savePromptTemplateHandler.handle(
                SavePromptTemplateCommand(template.id, state.editContent)
            )
            _uiState.update { it.copy(isSaving = false, isModified = false, savedOk = true) }
        }
    }

    fun dismissSaveDialog() {
        _uiState.update { it.copy(showWarningDialog = false) }
    }

    fun resetPrompt() {
        _uiState.update { it.copy(showResetDialog = true) }
    }

    fun confirmReset() {
        val template = _uiState.value.selectedTemplate ?: return
        _uiState.update { it.copy(showResetDialog = false, isSaving = true) }
        viewModelScope.launch {
            savePromptTemplateHandler.handle(
                SavePromptTemplateCommand(template.id, template.defaultContent)
            )
            _uiState.update { state ->
                state.copy(
                    isSaving = false,
                    editContent = template.defaultContent,
                    isModified = false,
                    savedOk = true
                )
            }
        }
    }

    fun dismissResetDialog() {
        _uiState.update { it.copy(showResetDialog = false) }
    }

    fun clearSavedOk() {
        _uiState.update { it.copy(savedOk = false) }
    }
}
