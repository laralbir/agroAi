package com.laralnet.agroai.aimodel.infrastructure.gemma

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
    private var inference: LlmInference? = null
    private var currentModelPath: String? = null

    suspend fun loadModel(modelPath: String) = withContext(Dispatchers.Default) {
        if (currentModelPath == modelPath && inference != null) return@withContext
        inference?.close()
        inference = null
        currentModelPath = null
        // Use the new LiteRT CPU path explicitly. DEFAULT (legacy CPU) rejects newer model
        // signatures; GPU can cause unrecoverable native crashes on some devices/models.
        inference = try {
            LlmInference.createFromOptions(context, LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .setPreferredBackend(LlmInference.Backend.CPU)
                .build())
        } catch (_: Exception) {
            // Fallback for older model formats that only work with the legacy path.
            LlmInference.createFromOptions(context, LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .setPreferredBackend(LlmInference.Backend.DEFAULT)
                .build())
        }
        currentModelPath = modelPath
    }

    fun isModelLoaded(): Boolean = inference != null

    fun analyzePhoto(imageUri: Uri, systemPrompt: String, userPrompt: String): Flow<String> =
        callbackFlow {
            val engine = inference ?: run {
                close(IllegalStateException("Model not loaded"))
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
        runCatching {
            val engine = inference ?: error("Model not loaded")
            engine.generateResponse(prompt)
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

    fun release() {
        inference?.close()
        inference = null
        currentModelPath = null
    }
}
