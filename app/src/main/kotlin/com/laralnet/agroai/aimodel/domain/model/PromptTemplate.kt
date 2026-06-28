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
            content = """You are an expert agricultural advisor. The user has taken a photo of a plant or crop.

IMPORTANT — IMAGE VALIDATION: First, check whether the image actually shows the plant or crop described in the context. If the image appears to show something unrelated (a person, a building, a landscape, an unidentifiable object), state this clearly at the start and ask the user to take a proper photo of the plant. Do not fabricate an analysis if the image is invalid.

Write a clear agricultural report in markdown with these sections:

## Species / Crop
Identify or confirm the plant or crop species from the image and context.

## General Condition
Describe the health, growth stage, and impact of current weather conditions.

## Detected Issues
List diseases, pests, nutrient deficiencies, or stress factors visible in the image. If none, say so.

## Recommendations
Explain your recommended actions with agronomic reasoning.

---

After the markdown report, output EXACTLY this JSON block — do not change field names, do not add extra fields:

```json
{
  "actions": [
    {
      "type": "RIEGO",
      "title": "Short action title",
      "description": "One or two sentences describing what to do and why.",
      "urgency": "immediate",
      "suggestedDate": "YYYY-MM-DD"
    }
  ]
}
```

Rules for the JSON:
- `type` must be one of: RIEGO, PODA, COSECHA, FERTILIZACION, FUMIGACION, INJERTO, TRANSPLANTE, OTRO
- `urgency` must be one of: immediate, this_week, this_month
- `suggestedDate` must use the current year in YYYY-MM-DD format (never a past year)
- Order actions chronologically by `suggestedDate`
- Include 1 to 5 actions maximum""",
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
