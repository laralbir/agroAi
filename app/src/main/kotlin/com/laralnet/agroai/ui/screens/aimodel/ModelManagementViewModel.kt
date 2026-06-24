package com.laralnet.agroai.ui.screens.aimodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.laralnet.agroai.aimodel.application.command.DeleteModelCommand
import com.laralnet.agroai.aimodel.application.command.DownloadModelCommand
import com.laralnet.agroai.aimodel.application.command.SetActiveModelCommand
import com.laralnet.agroai.aimodel.application.handler.DeleteModelHandler
import com.laralnet.agroai.aimodel.application.handler.DownloadModelHandler
import com.laralnet.agroai.aimodel.application.handler.SetActiveModelHandler
import com.laralnet.agroai.aimodel.application.query.ObserveModelsQuery
import com.laralnet.agroai.aimodel.domain.model.AIModel
import com.laralnet.agroai.aimodel.domain.model.DownloadState
import com.laralnet.agroai.aimodel.domain.model.ModelVariant
import com.laralnet.agroai.aimodel.infrastructure.download.ModelDownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelManagementViewModel @Inject constructor(
    private val observeModels: ObserveModelsQuery,
    private val downloadHandler: DownloadModelHandler,
    private val setActiveHandler: SetActiveModelHandler,
    private val deleteHandler: DeleteModelHandler,
    private val workManager: WorkManager
) : ViewModel() {

    data class ModelRowState(
        val variant: ModelVariant,
        val model: AIModel?,
        val downloadProgress: Int = 0
    ) {
        val downloadState: DownloadState get() = model?.downloadState ?: DownloadState.NOT_DOWNLOADED
        val isActive: Boolean get() = model?.isActive == true
        val isAvailable: Boolean get() = variant.downloadUrl.isNotBlank()
    }

    data class UiState(
        val rows: List<ModelRowState> = emptyList(),
        val pendingWarningVariant: ModelVariant? = null,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeModels()
                .flatMapLatest { buildRowsFlow(it) }
                .collect { rows -> _uiState.update { it.copy(rows = rows) } }
        }
    }

    private fun buildRowsFlow(models: List<AIModel>): Flow<List<ModelRowState>> {
        val downloading = models.filter { it.downloadState == DownloadState.DOWNLOADING }
        if (downloading.isEmpty()) return flowOf(buildRows(models, emptyMap()))

        val progressFlows = downloading.map { model ->
            workManager.getWorkInfosForUniqueWorkFlow("model_download_${model.id}")
                .map { infos ->
                    model.id to (infos.firstOrNull()?.progress
                        ?.getInt(ModelDownloadWorker.KEY_PROGRESS, 0) ?: 0)
                }
        }
        return combine(progressFlows) { pairs -> buildRows(models, pairs.toMap()) }
    }

    private fun buildRows(models: List<AIModel>, progresses: Map<String, Int>): List<ModelRowState> =
        ModelVariant.entries.map { variant ->
            val model = models.find { it.variant == variant }
            val progress = if (model?.downloadState == DownloadState.DOWNLOADING) {
                progresses[model.id] ?: model.downloadProgressPercent
            } else {
                model?.downloadProgressPercent ?: 0
            }
            ModelRowState(variant, model, progress)
        }

    fun onDownloadClick(variant: ModelVariant) {
        if (variant.approximateSizeGb >= 12.0) {
            _uiState.update { it.copy(pendingWarningVariant = variant) }
        } else {
            startDownload(variant)
        }
    }

    fun onWarningConfirmed() {
        val variant = _uiState.value.pendingWarningVariant ?: return
        _uiState.update { it.copy(pendingWarningVariant = null) }
        startDownload(variant)
    }

    fun onWarningDismissed() = _uiState.update { it.copy(pendingWarningVariant = null) }

    private fun startDownload(variant: ModelVariant) = viewModelScope.launch {
        downloadHandler.handle(DownloadModelCommand(variant))
            .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
    }

    fun onActivate(modelId: String) = viewModelScope.launch {
        setActiveHandler.handle(SetActiveModelCommand(modelId))
            .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
    }

    fun onDelete(modelId: String) = viewModelScope.launch {
        deleteHandler.handle(DeleteModelCommand(modelId))
            .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
