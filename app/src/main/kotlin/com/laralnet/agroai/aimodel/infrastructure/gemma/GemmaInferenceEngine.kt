package com.laralnet.agroai.aimodel.infrastructure.gemma

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.VisionModelOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class TreatmentSuggestion(
    val type: String,
    val title: String = "",
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
    // MediaPipe engine — for .task files (Gemma 3 / Gemma 3n)
    private var mediaPipeEngine: LlmInference? = null

    // true when the MediaPipe engine was loaded with VisionModelOptions (image support enabled)
    private var mediaPipeVisionEnabled: Boolean = false

    // LiteRT-LM engine — for .litertlm files (Gemma 4)
    private var liteRtEngine: Engine? = null

    private var currentModelPath: String? = null

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
            // Try loading with vision support first (Gemma 3n multimodal models)
            val visionEngine = tryLoadWithVision(modelPath)
            if (visionEngine != null) {
                mediaPipeEngine = visionEngine
                mediaPipeVisionEnabled = true
            } else {
                mediaPipeEngine = loadTextOnly(modelPath)
                mediaPipeVisionEnabled = false
            }
        }
        currentModelPath = modelPath
    }

    fun isModelLoaded(): Boolean = mediaPipeEngine != null || liteRtEngine != null

    fun supportsImageAnalysis(): Boolean = mediaPipeVisionEnabled

    /**
     * Analyze a photo using the loaded model.
     * - Gemma 3n (.task with vision): real image bytes passed via LlmInferenceSession.addImage()
     * - Gemma 3 text-only (.task without vision): text-only analysis (image not sent)
     * - Gemma 4 (.litertlm): LiteRT-LM 0.13.1 is text-only; image not sent
     */
    fun analyzePhoto(imageUri: Uri, systemPrompt: String, userPrompt: String): Flow<String> =
        callbackFlow {
            if (mediaPipeEngine == null && liteRtEngine == null) {
                close(IllegalStateException("No AI model loaded"))
                return@callbackFlow
            }

            var sessionToClose: LlmInferenceSession? = null

            withContext(Dispatchers.Default) {
                val prompt = buildTextPrompt(systemPrompt, userPrompt)

                when {
                    mediaPipeEngine != null -> {
                        val session = LlmInferenceSession.createFromOptions(
                            mediaPipeEngine!!,
                            LlmInferenceSession.LlmInferenceSessionOptions.builder().build()
                        )
                        sessionToClose = session

                        if (mediaPipeVisionEnabled) {
                            val bitmap = loadAndScaleBitmap(imageUri, maxDimension = 768)
                            if (bitmap != null) {
                                runCatching {
                                    session.addImage(BitmapImageBuilder(bitmap).build())
                                }
                            }
                        }

                        session.addQueryChunk(prompt)
                        session.generateResponseAsync { response, done ->
                            trySend(response)
                            if (done) close()
                        }
                    }

                    liteRtEngine != null -> {
                        // LiteRT-LM 0.13.1 — text-only (no image API available)
                        liteRtEngine!!.createConversation().use { conversation ->
                            conversation.sendMessageAsync(prompt).collect { chunk ->
                                trySend(chunk.toString())
                            }
                        }
                        close()
                    }
                }
            }

            awaitClose { sessionToClose?.close() }
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

    // ── private helpers ──────────────────────────────────────────────────────

    private fun tryLoadWithVision(modelPath: String): LlmInference? = runCatching {
        LlmInference.createFromOptions(
            context, LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .setVisionModelOptions(VisionModelOptions.builder().build())
                .setPreferredBackend(LlmInference.Backend.CPU)
                .build()
        )
    }.getOrNull()

    private fun loadTextOnly(modelPath: String): LlmInference =
        try {
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

    private fun buildTextPrompt(systemContext: String, userContent: String): String = buildString {
        if (systemContext.isNotBlank()) {
            append("<start_of_turn>system\n")
            append(systemContext)
            append("\n<end_of_turn>\n")
        }
        append("<start_of_turn>user\n")
        append(userContent)
        append("\n<end_of_turn>\n")
        append("<start_of_turn>model\n")
    }

    private fun loadAndScaleBitmap(uri: Uri, maxDimension: Int): Bitmap? {
        val raw = runCatching {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull() ?: return null

        val w = raw.width
        val h = raw.height
        if (w <= maxDimension && h <= maxDimension) return raw
        val scale = maxDimension.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(raw, (w * scale).toInt(), (h * scale).toInt(), true)
    }

    private fun releaseAll() {
        mediaPipeEngine?.close()
        mediaPipeEngine = null
        mediaPipeVisionEnabled = false
        liteRtEngine?.close()
        liteRtEngine = null
        currentModelPath = null
    }

    fun release() = releaseAll()
}
