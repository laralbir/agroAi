package com.laralnet.agroai.ui.screens.analysis

import com.laralnet.agroai.aimodel.infrastructure.gemma.PhotoAnalysisResult
import com.laralnet.agroai.aimodel.infrastructure.gemma.TreatmentSuggestion
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class ActionsWrapperJson(
    val actions: List<ActionJson> = emptyList()
)

@Serializable
internal data class ActionJson(
    val type: String = "OTRO",
    val title: String = "",
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
    val suggestions = parseActionsBlock(response)
    return PhotoAnalysisResult(
        species = "",
        issues = emptyList(),
        generalCondition = "",
        suggestions = suggestions,
        rawResponse = response
    )
}

/** Extract the ```json {actions:[...]} ``` block at the end of the response. */
private fun parseActionsBlock(text: String): List<TreatmentSuggestion> {
    val json = extractActionsJson(text) ?: return emptyList()
    return runCatching {
        lenientJson.decodeFromString<ActionsWrapperJson>(json).actions.map { a ->
            TreatmentSuggestion(
                type = a.type.uppercase().trim(),
                title = a.title.trim(),
                description = a.description.trim(),
                urgency = a.urgency.trim(),
                suggestedDate = a.suggestedDate?.trim()
            )
        }
    }.getOrElse { emptyList() }
}

/**
 * Finds the JSON object that contains an "actions" key.
 * Searches markdown code blocks first, then raw braces.
 */
private fun extractActionsJson(text: String): String? {
    // 1. Try markdown code blocks: ```json … ``` or ``` … ```
    val codeBlockRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
    codeBlockRegex.findAll(text).forEach { match ->
        val candidate = match.groupValues.getOrNull(1)?.trim() ?: return@forEach
        if (candidate.contains("\"actions\"")) return candidate
    }

    // 2. Try every { } block in the text looking for one with "actions"
    var pos = 0
    while (pos < text.length) {
        val start = text.indexOf('{', pos)
        if (start < 0) break
        val candidate = extractBraceBlock(text, start) ?: break
        if (candidate.contains("\"actions\"")) return candidate
        pos = start + 1
    }
    return null
}

private fun extractBraceBlock(text: String, start: Int): String? {
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
