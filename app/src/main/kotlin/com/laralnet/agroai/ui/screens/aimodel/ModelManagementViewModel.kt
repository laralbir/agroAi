package com.laralnet.agroai.ui.screens.aimodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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
import com.laralnet.agroai.aimodel.infrastructure.download.WorkManagerModelDownloader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelManagementViewModel @Inject constructor(
    private val observeModels: ObserveModelsQuery,
    private val downloadHandler: DownloadModelHandler,
    private val setActiveHandler: SetActiveModelHandler,
    private val deleteHandler: DeleteModelHandler,
    private val workManager: WorkManager,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    data class ModelRowState(
        val variant: ModelVariant,
        val model: AIModel?,
        val downloadProgress: Int = 0,
        val statusMessage: String? = null
    ) {
        val downloadState: DownloadState get() = model?.downloadState ?: DownloadState.NOT_DOWNLOADED
        val isActive: Boolean get() = model?.isActive == true
        val isAvailable: Boolean get() = variant.downloadUrl.isNotBlank()
        /** Shown below the card: live status during download, persisted error after failure. */
        val displayStatus: String? get() = statusMessage ?: model?.lastError
    }

    data class UiState(
        val rows: List<ModelRowState> = emptyList(),
        val hfToken: String = "",
        val pendingWarningVariant: ModelVariant? = null,
        val noTokenDialogVariant: ModelVariant? = null,
        val error: String? = null
    ) {
        val hasHfToken: Boolean get() = hfToken.isNotBlank()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeModels()
                .flatMapLatest { buildRowsFlow(it) }
                .collect { rows -> _uiState.update { it.copy(rows = rows) } }
        }
        dataStore.data
            .map { prefs -> prefs[WorkManagerModelDownloader.KEY_HF_TOKEN] ?: "" }
            .onEach { token -> _uiState.update { it.copy(hfToken = token) } }
            .launchIn(viewModelScope)
    }

    private fun buildRowsFlow(models: List<AIModel>): Flow<List<ModelRowState>> {
        val downloading = models.filter { it.downloadState == DownloadState.DOWNLOADING }
        if (downloading.isEmpty()) return flowOf(buildRows(models, emptyMap(), emptyMap()))

        val workFlows = downloading.map { model ->
            workManager.getWorkInfosForUniqueWorkFlow("model_download_${model.id}")
                .map { infos ->
                    val info = infos.firstOrNull()
                    val progress = info?.progress
                    val progressPct = progress?.getInt(ModelDownloadWorker.KEY_PROGRESS, 0) ?: 0
                    val logFromProgress = progress?.getString(ModelDownloadWorker.KEY_STATUS)
                    // If WorkInfo finished (FAILED/CANCELLED) but DB model still shows
                    // DOWNLOADING (e.g. Worker crashed before it could update Room),
                    // surface the error from outputData so the panel appears.
                    val logFromOutput = if (info != null && info.state.isFinished && logFromProgress == null) {
                        info.outputData.getString(ModelDownloadWorker.KEY_ERROR)
                            ?: "Worker terminado — estado ${info.state.name}"
                    } else null
                    Triple(model.id, progressPct, logFromProgress ?: logFromOutput)
                }
        }
        return combine(workFlows) { triples ->
            buildRows(
                models,
                triples.associate { it.first to it.second },
                triples.associate { it.first to it.third }
            )
        }
    }

    private fun buildRows(
        models: List<AIModel>,
        progresses: Map<String, Int>,
        statuses: Map<String, String?>
    ): List<ModelRowState> =
        ModelVariant.entries.map { variant ->
            val model = models.find { it.variant == variant }
            val progress = if (model?.downloadState == DownloadState.DOWNLOADING) {
                progresses[model.id] ?: model.downloadProgressPercent
            } else {
                model?.downloadProgressPercent ?: 0
            }
            val status = if (model?.downloadState == DownloadState.DOWNLOADING) {
                statuses[model.id]
            } else null
            ModelRowState(variant, model, progress, status)
        }

    fun onDownloadClick(variant: ModelVariant) {
        if (!_uiState.value.hasHfToken) {
            _uiState.update { it.copy(noTokenDialogVariant = variant) }
            return
        }
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

    /** Save token from the inline no-token dialog and continue with the pending download. */
    fun onTokenSavedAndDownload(token: String) {
        val variant = _uiState.value.noTokenDialogVariant ?: return
        _uiState.update { it.copy(noTokenDialogVariant = null) }
        viewModelScope.launch {
            dataStore.edit { it[WorkManagerModelDownloader.KEY_HF_TOKEN] = token }
            if (variant.approximateSizeGb >= 12.0) {
                _uiState.update { it.copy(pendingWarningVariant = variant) }
            } else {
                startDownload(variant)
            }
        }
    }

    fun dismissNoTokenDialog() = _uiState.update { it.copy(noTokenDialogVariant = null) }

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
