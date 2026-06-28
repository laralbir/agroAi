package com.laralnet.agroai.aimodel.domain.model

import java.util.UUID

enum class PromptWarningLevel { LOW, MEDIUM, HIGH }

data class PromptTemplate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val content: String,
    val systemContext: String = "",
    val isEditable: Boolean = true,
    val isCustomized: Boolean = false,
    val warningLevel: PromptWarningLevel = PromptWarningLevel.LOW,
    val defaultContent: String = content
) {
    companion object {
        fun photoAnalysisDefault(): PromptTemplate = PromptTemplate(
            name = "photo_analysis",
            content = """You are an expert agricultural advisor. The user has taken a photo of a plant or crop and wants your professional assessment.

Based on the plantation context, current weather, and any observations the user describes, write a clear agricultural report in markdown with these sections:

## Species / Crop
Identify or confirm the plant or crop species from the context.

## General Condition
Describe the expected health, typical growth stage for the season, and any concerns given the weather conditions.

## Detected Issues
Based on the context and any observations described, list likely diseases, pests, nutrient deficiencies, or stress factors. If none apparent, say so.

## Recommended Treatments
For each recommended action specify:
- **Action**: what to do
- **Product / dose** (if applicable)
- **Urgency**: immediate / this week / this month
- **Date**: suggest a specific date when possible (YYYY-MM-DD format)

## Additional Notes
Seasonal advice, weather considerations, or other relevant observations.

Do NOT output JSON. Write in plain readable markdown.""",
            warningLevel = PromptWarningLevel.MEDIUM,
            isEditable = true
        ).let { it.copy(defaultContent = it.content) }

        fun weatherAdjustmentDefault(): PromptTemplate = PromptTemplate(
            name = "weather_adjustment",
            content = """You are an agricultural planning assistant.
Given the following scheduled treatment and the weather forecast, evaluate if the treatment should proceed or be rescheduled.
Consider: rain (avoid spraying/fertilizing), frost (avoid planting/harvesting), wind (avoid spraying), heat (avoid heavy watering in midday).
Respond in JSON: {proceed: boolean, reason: string, suggestedReschedule: ISO8601 date or null}""",
            warningLevel = PromptWarningLevel.LOW,
            isEditable = true
        ).let { it.copy(defaultContent = it.content) }
    }
}
