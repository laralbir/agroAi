package com.laralnet.agroai.aimodel.infrastructure.gemma

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class TreatmentSuggestion(
    val type: String,
    val description: String,
    val urgency: String,
    val suggestedDate: String?
)

data class PhotoAnalysisResult(
    val species: String,
    val issues: List<String>,
    val generalCondition: String,
    val suggestions: List<TreatmentSuggestion>,
    val rawResponse: String
)

@Singleton
class GemmaInferenceEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // MediaPipe engine — for .task files (Gemma 3)
    private var mediaPipeEngine: LlmInference? = null

    // LiteRT-LM engine — for .litertlm files (Gemma 4)
    private var liteRtEngine: Engine? = null

    private var currentModelPath: String? = null

    private val isLiteRtModel: Boolean
        get() = currentModelPath?.endsWith(".litertlm") == true

    suspend fun loadModel(modelPath: String) = withContext(Dispatchers.Default) {
        if (currentModelPath == modelPath && (mediaPipeEngine != null || liteRtEngine != null)) return@withContext
        releaseAll()

        if (modelPath.endsWith(".litertlm")) {
            val config = EngineConfig(
                modelPath = modelPath,
                backend = Backend.CPU(),
                cacheDir = context.cacheDir.path
            )
            val engine = Engine(config)
            engine.initialize()
            liteRtEngine = engine
        } else {
            // Use the new LiteRT CPU path explicitly. DEFAULT (legacy CPU) rejects newer model
            // signatures; GPU can cause unrecoverable native crashes on some devices/models.
            mediaPipeEngine = try {
                LlmInference.createFromOptions(
                    context, LlmInferenceOptions.builder()
                        .setModelPath(modelPath)
                        .setMaxTokens(1024)
                        .setPreferredBackend(LlmInference.Backend.CPU)
                        .build()
                )
            } catch (_: Exception) {
                LlmInference.createFromOptions(
                    context, LlmInferenceOptions.builder()
                        .setModelPath(modelPath)
                        .setMaxTokens(1024)
                        .setPreferredBackend(LlmInference.Backend.DEFAULT)
                        .build()
                )
            }
        }
        currentModelPath = modelPath
    }

    fun isModelLoaded(): Boolean = mediaPipeEngine != null || liteRtEngine != null

    fun analyzePhoto(imageUri: Uri, systemPrompt: String, userPrompt: String): Flow<String> =
        callbackFlow {
            val engine = mediaPipeEngine ?: run {
                close(IllegalStateException("Photo analysis only supported for Gemma 3 (.task) models"))
                return@callbackFlow
            }
            withContext(Dispatchers.Default) {
                val bitmap = loadBitmapFromUri(imageUri)
                val fullPrompt = buildMultimodalPrompt(systemPrompt, userPrompt, bitmap != null)
                engine.generateResponseAsync(fullPrompt) { response, done ->
                    trySend(response)
                    if (done) close()
                }
            }
            awaitClose()
        }

    suspend fun generateText(prompt: String): Result<String> = withContext(Dispatchers.Default) {
        when {
            liteRtEngine != null -> runCatching {
                val sb = StringBuilder()
                liteRtEngine!!.createConversation().use { conversation ->
                    conversation.sendMessageAsync(prompt).collect { message ->
                        sb.append(message.toString())
                    }
                }
                sb.toString()
            }
            mediaPipeEngine != null -> runCatching {
                mediaPipeEngine!!.generateResponse(prompt)
            }
            else -> Result.failure(IllegalStateException("Model not loaded"))
        }
    }

    private fun buildMultimodalPrompt(
        systemContext: String,
        userContent: String,
        hasImage: Boolean
    ): String = buildString {
        if (systemContext.isNotBlank()) {
            append("<start_of_turn>system\n")
            append(systemContext)
            append("\n<end_of_turn>\n")
        }
        append("<start_of_turn>user\n")
        if (hasImage) append("[IMAGE]\n")
        append(userContent)
        append("\n<end_of_turn>\n")
        append("<start_of_turn>model\n")
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    }.getOrNull()

    private fun releaseAll() {
        mediaPipeEngine?.close()
        mediaPipeEngine = null
        liteRtEngine?.close()
        liteRtEngine = null
        currentModelPath = null
    }

    fun release() = releaseAll()
}
