package com.laralnet.agroai.ui.screens.aimodel

import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
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
import com.laralnet.agroai.aimodel.domain.model.HuggingFaceCredential
import com.laralnet.agroai.aimodel.domain.model.ModelVariant
import com.laralnet.agroai.aimodel.domain.repository.HuggingFaceAuthRepository
import com.laralnet.agroai.aimodel.infrastructure.download.ModelDownloadWorker
import com.laralnet.agroai.aimodel.infrastructure.gemma.GemmaInferenceEngine
import com.laralnet.agroai.aimodel.infrastructure.oauth.HuggingFaceOAuthCallbackChannel
import com.laralnet.agroai.aimodel.infrastructure.oauth.PkceHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ModelManagementViewModel @Inject constructor(
    private val observeModels: ObserveModelsQuery,
    private val downloadHandler: DownloadModelHandler,
    private val setActiveHandler: SetActiveModelHandler,
    private val deleteHandler: DeleteModelHandler,
    private val workManager: WorkManager,
    private val hfAuthRepository: HuggingFaceAuthRepository,
    private val oauthCallbackChannel: HuggingFaceOAuthCallbackChannel,
    private val gemmaEngine: GemmaInferenceEngine,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    companion object {
        private val KEY_LANGUAGE = stringPreferencesKey("language_mode")
    }

    data class ModelTestResult(
        val variantDisplayName: String,
        val filePath: String,
        val fileSizeBytes: Long,
        val prompt: String,
        val response: String? = null,
        val loadTimeMs: Long = 0L,
        val inferenceTimeMs: Long = 0L,
        val error: String? = null,
        val isRunning: Boolean = false
    ) {
        val totalTimeMs: Long get() = loadTimeMs + inferenceTimeMs
        val isSuccess: Boolean get() = !isRunning && error == null && response != null
    }

    data class ModelRowState(
        val variant: ModelVariant,
        val model: AIModel?,
        val downloadProgress: Int = 0,
        val statusMessage: String? = null
    ) {
        val downloadState: DownloadState get() = model?.downloadState ?: DownloadState.NOT_DOWNLOADED
        val isActive: Boolean get() = model?.isActive == true
        val isAvailable: Boolean get() = variant.downloadUrl.isNotBlank()
        val displayStatus: String? get() = statusMessage ?: model?.lastError
    }

    data class UiState(
        val rows: List<ModelRowState> = emptyList(),
        val hfCredential: HuggingFaceCredential? = null,
        val pendingWarningVariant: ModelVariant? = null,
        val oauthDialogVariant: ModelVariant? = null,
        val oauthConnecting: Boolean = false,
        val testResult: ModelTestResult? = null,
        val error: String? = null
    ) {
        val hasHfToken: Boolean get() = hfCredential != null
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _browserLaunchEvent = Channel<Uri>(Channel.BUFFERED)
    val browserLaunchEvent: Flow<Uri> = _browserLaunchEvent.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeModels()
                .flatMapLatest { buildRowsFlow(it) }
                .collect { rows -> _uiState.update { it.copy(rows = rows) } }
        }
        hfAuthRepository.observeCredential()
            .onEach { credential ->
                val wasConnecting = _uiState.value.oauthConnecting
                val pendingVariant = _uiState.value.oauthDialogVariant
                _uiState.update { it.copy(hfCredential = credential, oauthConnecting = false) }
                // Auto-start download when OAuth just completed and a variant was waiting
                if (wasConnecting && credential != null && pendingVariant != null) {
                    _uiState.update { it.copy(oauthDialogVariant = null) }
                    if (pendingVariant.approximateSizeGb >= 12.0) {
                        _uiState.update { it.copy(pendingWarningVariant = pendingVariant) }
                    } else {
                        startDownload(pendingVariant)
                    }
                }
            }
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
            _uiState.update { it.copy(oauthDialogVariant = variant) }
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

    fun onConnectHuggingFace() {
        val verifier = PkceHelper.generateCodeVerifier()
        val state = PkceHelper.generateState()
        oauthCallbackChannel.startFlow(verifier, state)
        val uri = oauthCallbackChannel.buildAuthorizationUri(verifier, state)
        _uiState.update { it.copy(oauthConnecting = true) }
        _browserLaunchEvent.trySend(uri)
    }

    fun dismissOAuthDialog() = _uiState.update {
        it.copy(oauthDialogVariant = null, oauthConnecting = false)
    }

    fun onReconnectHuggingFace(variant: ModelVariant) {
        _uiState.update { it.copy(oauthDialogVariant = variant) }
        onConnectHuggingFace()
    }

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

    fun openTestSheet(filePath: String, variantDisplayName: String) {
        viewModelScope.launch {
            val isLiteRtFormat = filePath.endsWith(".litertlm")
            val defaultPrompt = buildDefaultTestPrompt(usePlainText = isLiteRtFormat)
            _uiState.update {
                it.copy(
                    testResult = ModelTestResult(
                        variantDisplayName = variantDisplayName,
                        filePath = filePath,
                        fileSizeBytes = File(filePath).length(),
                        prompt = defaultPrompt
                    )
                )
            }
        }
    }

    fun updateTestPrompt(prompt: String) {
        _uiState.update { state ->
            state.copy(testResult = state.testResult?.copy(prompt = prompt))
        }
    }

    fun runTest() {
        val current = _uiState.value.testResult ?: return
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    testResult = state.testResult?.copy(
                        response = null,
                        error = null,
                        loadTimeMs = 0L,
                        inferenceTimeMs = 0L,
                        isRunning = true
                    )
                )
            }

            val loadStart = System.currentTimeMillis()
            val loadResult = runCatching { gemmaEngine.loadModel(current.filePath) }
            val loadTimeMs = System.currentTimeMillis() - loadStart

            if (loadResult.isFailure) {
                _uiState.update { state ->
                    state.copy(
                        testResult = state.testResult?.copy(
                            error = loadResult.exceptionOrNull()?.message ?: "Error al cargar el modelo",
                            loadTimeMs = loadTimeMs,
                            isRunning = false
                        )
                    )
                }
                return@launch
            }

            val prompt = _uiState.value.testResult?.prompt ?: current.prompt
            val inferStart = System.currentTimeMillis()
            val response = gemmaEngine.generateText(prompt)
            val inferTimeMs = System.currentTimeMillis() - inferStart

            _uiState.update { state ->
                state.copy(
                    testResult = state.testResult?.copy(
                        response = response.getOrNull(),
                        error = response.exceptionOrNull()?.message,
                        loadTimeMs = loadTimeMs,
                        inferenceTimeMs = inferTimeMs,
                        isRunning = false
                    )
                )
            }
        }
    }

    fun retryTest() = runTest()

    private suspend fun buildDefaultTestPrompt(usePlainText: Boolean = false): String {
        val langPref = dataStore.data.map { it[KEY_LANGUAGE] ?: "SYSTEM" }.first()
        val useSpanish = when (langPref) {
            "SPANISH" -> true
            "ENGLISH" -> false
            else -> Locale.getDefault().language == "es"
        }
        // LiteRT-LM (Gemma 4) handles the chat template internally — send plain text.
        // MediaPipe (Gemma 3) requires the <start_of_turn> template manually.
        return if (usePlainText) {
            if (useSpanish)
                "Eres AgroAI, un asistente agrícola experto. Esto es una prueba de diagnóstico del sistema. " +
                "Lista exactamente 3 señales comunes de estrés hídrico en cultivos. " +
                "Para cada señal, describe el síntoma visual en una oración y un remedio sencillo. " +
                "Formato: lista numerada."
            else
                "You are AgroAI, an expert agricultural assistant. This is a system diagnostics test. " +
                "List exactly 3 common signs of water stress in crops. " +
                "For each sign, give the visual symptom in one sentence and one simple remedy. " +
                "Format as a numbered list."
        } else if (useSpanish) buildString {
            append("<start_of_turn>user\n")
            append("Eres AgroAI, un asistente agrícola experto. ")
            append("Esto es una prueba de diagnóstico del sistema. ")
            append("Lista exactamente 3 señales comunes de estrés hídrico en cultivos. ")
            append("Para cada señal, describe el síntoma visual en una oración y un remedio sencillo. ")
            append("Formato: lista numerada.\n")
            append("<end_of_turn>\n")
            append("<start_of_turn>model\n")
        } else buildString {
            append("<start_of_turn>user\n")
            append("You are AgroAI, an expert agricultural assistant. ")
            append("This is a system diagnostics test. ")
            append("List exactly 3 common signs of water stress in crops. ")
            append("For each sign, give the visual symptom in one sentence and one simple remedy. ")
            append("Format as a numbered list.\n")
            append("<end_of_turn>\n")
            append("<start_of_turn>model\n")
        }
    }

    fun dismissTestResult() = _uiState.update { it.copy(testResult = null) }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
