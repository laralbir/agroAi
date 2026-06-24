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
            content = """You are an expert agricultural advisor.
Analyze the provided image of a plant, tree, fruit, or plantation.
Identify:
1. The plant/crop species if visible
2. Any visible diseases, pests, or deficiencies
3. The current growth stage
4. Recommended treatments or maintenance actions (be specific with products and dosages when applicable)
5. Urgency level (immediate, this week, this month)

Respond in a structured JSON format with fields: species, issues, treatments (array of {type, description, urgency, suggestedDate}), generalCondition.
Use the same language as the system locale.""",
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
