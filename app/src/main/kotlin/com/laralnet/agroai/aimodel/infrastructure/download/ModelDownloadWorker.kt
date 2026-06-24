package com.laralnet.agroai.aimodel.infrastructure.download

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.laralnet.agroai.aimodel.domain.model.AIModel
import com.laralnet.agroai.aimodel.domain.model.DownloadState
import com.laralnet.agroai.aimodel.domain.model.ModelVariant
import com.laralnet.agroai.aimodel.domain.repository.AIModelRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

@HiltWorker
class ModelDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: AIModelRepository,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_MODEL_VARIANT = "model_variant"
        const val KEY_MODEL_ID = "model_id"
        const val KEY_PROGRESS = "progress"
        const val KEY_LOG = "log"
        const val KEY_FILE_PATH = "file_path"
        const val KEY_ERROR = "error"
        const val KEY_HF_TOKEN = "hf_token"
        const val NOTIFICATION_ID = 1001

        const val KEY_STATUS = KEY_LOG

        private const val TAG = "ModelDownloadWorker"
        // WorkManager Data hard limit is 10240 bytes; keep log under 8000 chars
        private const val MAX_LOG_CHARS = 8000
    }

    private val log = StringBuilder()
    private var currentProgress = 0

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "doWork() started — Hilt Worker OK")
        val variantName = inputData.getString(KEY_MODEL_VARIANT) ?: return@withContext Result.failure()
        val modelId = inputData.getString(KEY_MODEL_ID) ?: return@withContext Result.failure()
        val variant = runCatching { ModelVariant.valueOf(variantName) }.getOrNull()
            ?: return@withContext Result.failure()

        val modelsDir = applicationContext.getExternalFilesDir("models")
            ?: applicationContext.filesDir.resolve("models").also { it.mkdirs() }
        val destFile = File(modelsDir, "${variant.name.lowercase()}.task")

        Log.d(TAG, "variant=$variantName modelId=$modelId")
        runCatching {
            val hfToken = inputData.getString(KEY_HF_TOKEN).orEmpty()
            val tokenDisplay = if (hfToken.isNotBlank())
                "Bearer hf_***${hfToken.takeLast(4)}"
            else
                "(ninguno — se rechazará)"

            log("=== DESCARGA ${variant.displayName} ===")
            log("Destino: ${destFile.absolutePath}")
            log("")
            log("--- REQUEST ---")
            log("GET ${variant.downloadUrl}")
            log("Authorization: $tokenDisplay")

            val request = Request.Builder()
                .url(variant.downloadUrl)
                .apply { if (hfToken.isNotBlank()) header("Authorization", "Bearer $hfToken") }
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                log("")
                log("--- RESPONSE ---")
                log("HTTP ${response.code} ${response.message}")
                response.headers.forEach { (name, value) ->
                    // Mask any auth-related response headers
                    log("$name: $value")
                }

                if (!response.isSuccessful) {
                    val reason = when (response.code) {
                        401 -> "Token inválido o caducado"
                        403 -> "Acceso denegado — acepta las condiciones de Gemma en HuggingFace"
                        404 -> "Archivo no encontrado — la URL puede haber cambiado"
                        429 -> "Demasiadas solicitudes — espera un momento"
                        else -> "Error inesperado"
                    }
                    // Log response body so the exact server message appears in panel + Logcat
                    val errorBody = runCatching {
                        response.body?.string()?.take(1000)
                    }.getOrNull()
                    log("")
                    log("--- RESPONSE BODY ---")
                    if (!errorBody.isNullOrBlank()) log(errorBody) else log("(vacío)")
                    log("")
                    log("--- ERROR ---")
                    log(reason)
                    Log.e(TAG, "HTTP ${response.code} — URL: ${variant.downloadUrl}")
                    Log.e(TAG, "Body: $errorBody")
                    error("[HTTP ${response.code}] $reason")
                }

                val body = response.body ?: error("Cuerpo de respuesta vacío")
                val contentLength = body.contentLength()
                val totalMbStr = if (contentLength > 0) "${contentLength / 1024 / 1024} MB" else "desconocido (chunked)"

                log("")
                log("--- DESCARGA ---")
                log("Tamaño total: $totalMbStr")

                var bytesRead = 0L
                var lastReportedProgress = -1

                destFile.outputStream().use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesRead += read
                            if (contentLength > 0) {
                                val progress = (bytesRead * 100 / contentLength).toInt()
                                if (progress > lastReportedProgress) {
                                    lastReportedProgress = progress
                                    currentProgress = progress
                                    val dlMb = bytesRead / 1024 / 1024
                                    val totMb = contentLength / 1024 / 1024
                                    flush("$dlMb MB / $totMb MB ($progress%)")
                                    updateProgress(modelId, progress)
                                }
                            } else {
                                val dlMb = bytesRead / 1024 / 1024
                                val prevMb = (bytesRead - read) / 1024 / 1024
                                if (dlMb != prevMb) flush("$dlMb MB descargados…")
                            }
                        }
                    }
                }
            }

            log("")
            log("=== COMPLETADO ===")
            flush()

            val existing = repository.findById(modelId)
            repository.save(
                (existing ?: AIModel(id = modelId, variant = variant, version = variant.gemmaVersion)).copy(
                    filePath = destFile.absolutePath,
                    downloadState = DownloadState.DOWNLOADED,
                    downloadProgressPercent = 100,
                    lastError = null
                )
            )

            if (repository.findActive() == null) {
                repository.setActive(modelId)
                log("Modelo activado automáticamente (ningún modelo activo previo)")
            }

            Result.success(workDataOf(KEY_FILE_PATH to destFile.absolutePath))
        }.getOrElse { e ->
            Log.e(TAG, "=== DOWNLOAD FAILED: ${e.message} ===")
            Log.e(TAG, "Full log:\n${log}")
            destFile.delete()
            val fullLog = log.toString()
            runCatching {
                val existing = repository.findById(modelId)
                repository.save(
                    (existing ?: AIModel(id = modelId, variant = variant, version = variant.gemmaVersion)).copy(
                        filePath = null,
                        downloadState = DownloadState.FAILED,
                        downloadProgressPercent = 0,
                        lastError = fullLog.ifBlank { e.message ?: "Error desconocido" }
                    )
                )
            }.onFailure { dbErr -> Log.e(TAG, "Failed to save FAILED state to DB", dbErr) }
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Error desconocido")))
        }
    }

    /** Append a line to the log and push it via setProgress. */
    private suspend fun log(line: String) {
        log.appendLine(line)
        Log.d(TAG, line)
        flush()
    }

    /** Push current log + optional progress line without appending a permanent entry. */
    private suspend fun flush(progressLine: String? = null) {
        val rawOutput = if (progressLine != null) "$log$progressLine" else log.toString()
        // WorkManager Data is capped at 10240 bytes; truncate to stay safe
        val output = if (rawOutput.length > MAX_LOG_CHARS)
            "…\n" + rawOutput.takeLast(MAX_LOG_CHARS)
        else
            rawOutput
        setProgress(workDataOf(KEY_PROGRESS to currentProgress, KEY_LOG to output))
    }

    private suspend fun updateProgress(modelId: String, progress: Int) {
        val existing = repository.findById(modelId) ?: return
        repository.save(existing.copy(downloadProgressPercent = progress))
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        ForegroundInfo(NOTIFICATION_ID, ModelDownloadNotification.create(applicationContext))
}
