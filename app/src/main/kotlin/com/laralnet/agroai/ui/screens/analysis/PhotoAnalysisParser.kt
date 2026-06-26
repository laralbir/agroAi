package com.laralnet.agroai.ui.screens.analysis

import com.laralnet.agroai.aimodel.infrastructure.gemma.PhotoAnalysisResult
import com.laralnet.agroai.aimodel.infrastructure.gemma.TreatmentSuggestion
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class PhotoAnalysisResultJson(
    val species: String = "",
    val issues: List<String> = emptyList(),
    val generalCondition: String = "",
    val treatments: List<TreatmentSuggestionJson> = emptyList()
)

@Serializable
internal data class TreatmentSuggestionJson(
    val type: String = "",
    val description: String = "",
    val urgency: String = "",
    val suggestedDate: String? = null
)

private val lenientJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

internal fun parsePhotoAnalysisResponse(response: String): PhotoAnalysisResult {
    val json = extractJsonObject(response)
    if (json != null) {
        runCatching {
            val dto = lenientJson.decodeFromString<PhotoAnalysisResultJson>(json)
            return PhotoAnalysisResult(
                species = dto.species,
                issues = dto.issues,
                generalCondition = dto.generalCondition,
                suggestions = dto.treatments.map { t ->
                    TreatmentSuggestion(
                        type = t.type,
                        description = t.description,
                        urgency = t.urgency,
                        suggestedDate = t.suggestedDate
                    )
                },
                rawResponse = response
            )
        }
    }
    // Fallback: wrap raw response as a single suggestion
    return PhotoAnalysisResult(
        species = "",
        issues = emptyList(),
        generalCondition = "",
        suggestions = listOf(
            TreatmentSuggestion(
                type = "OTRO",
                description = response.take(1000),
                urgency = "review",
                suggestedDate = null
            )
        ),
        rawResponse = response
    )
}

/**
 * Extracts the first valid JSON object from text that may contain markdown code fences
 * or surrounding prose.
 */
private fun extractJsonObject(text: String): String? {
    // 1. Try markdown code block: ```json ... ``` or ``` ... ```
    val codeBlockRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
    codeBlockRegex.find(text)?.groupValues?.getOrNull(1)?.trim()?.let { candidate ->
        if (candidate.startsWith("{")) return candidate
    }

    // 2. Try first { ... } in the text
    val start = text.indexOf('{')
    if (start < 0) return null
    var depth = 0
    var inString = false
    var escape = false
    for (i in start until text.length) {
        val c = text[i]
        when {
            escape -> escape = false
            c == '\\' && inString -> escape = true
            c == '"' -> inString = !inString
            !inString && c == '{' -> depth++
            !inString && c == '}' -> {
                depth--
                if (depth == 0) return text.substring(start, i + 1)
            }
        }
    }
    return null
}
