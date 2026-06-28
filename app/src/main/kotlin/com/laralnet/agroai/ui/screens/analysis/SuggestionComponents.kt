package com.laralnet.agroai.ui.screens.analysis

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.laralnet.agroai.R
import com.laralnet.agroai.aimodel.infrastructure.gemma.TreatmentSuggestion
import com.laralnet.agroai.treatment.domain.model.TreatmentType

@Composable
internal fun SuggestionCard(
    suggestion: TreatmentSuggestion,
    onSchedule: (() -> Unit)?
) {
    val type = TreatmentType.entries.firstOrNull { it.name == suggestion.type } ?: TreatmentType.OTRO
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(type.emoji(), style = MaterialTheme.typography.titleLarge)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val titleText = suggestion.title.ifBlank {
                    type.name.lowercase().replaceFirstChar { it.uppercaseChar() }
                }
                Text(titleText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (suggestion.description.isNotBlank()) {
                    Text(
                        suggestion.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 2.dp)) {
                    if (suggestion.urgency.isNotBlank()) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(suggestion.urgency, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    suggestion.suggestedDate?.let { date ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text("📅 $date", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                if (onSchedule != null) {
                    OutlinedButton(
                        onClick = onSchedule,
                        modifier = Modifier.padding(top = 2.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.analysis_schedule_calendar),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

internal fun TreatmentType.emoji() = when (this) {
    TreatmentType.RIEGO -> "💧"
    TreatmentType.PODA -> "✂️"
    TreatmentType.COSECHA -> "🌾"
    TreatmentType.FERTILIZACION -> "🧪"
    TreatmentType.FUMIGACION -> "🌫️"
    TreatmentType.INJERTO -> "🌿"
    TreatmentType.TRANSPLANTE -> "🪴"
    TreatmentType.OTRO -> "📋"
}
