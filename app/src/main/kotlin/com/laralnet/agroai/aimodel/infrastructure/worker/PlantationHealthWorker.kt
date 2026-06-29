package com.laralnet.agroai.aimodel.infrastructure.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.laralnet.agroai.MainActivity
import com.laralnet.agroai.R
import com.laralnet.agroai.action.application.command.ScheduleActionCommand
import com.laralnet.agroai.action.application.handler.ScheduleActionHandler
import com.laralnet.agroai.action.domain.model.ActionSource
import com.laralnet.agroai.action.domain.model.ActionType
import com.laralnet.agroai.action.domain.repository.PlantationActionRepository
import com.laralnet.agroai.aimodel.domain.model.PromptTemplate
import com.laralnet.agroai.aimodel.domain.repository.AIModelRepository
import com.laralnet.agroai.aimodel.infrastructure.gemma.GemmaInferenceEngine
import com.laralnet.agroai.photoanalysis.application.handler.SaveAnalysisCommand
import com.laralnet.agroai.photoanalysis.application.handler.SaveAnalysisHandler
import com.laralnet.agroai.plantation.domain.model.Plantation
import com.laralnet.agroai.plantation.domain.repository.PlantationRepository
import com.laralnet.agroai.ui.screens.analysis.parsePhotoAnalysisResponse
import com.laralnet.agroai.weather.domain.model.WeatherData
import com.laralnet.agroai.weather.domain.repository.WeatherRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@HiltWorker
class PlantationHealthWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val plantationRepository: PlantationRepository,
    private val weatherRepository: WeatherRepository,
    private val aiModelRepository: AIModelRepository,
    private val gemmaEngine: GemmaInferenceEngine,
    private val saveAnalysisHandler: SaveAnalysisHandler,
    private val actionRepository: PlantationActionRepository,
    private val scheduleActionHandler: ScheduleActionHandler,
    private val dataStore: DataStore<Preferences>
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val model = aiModelRepository.findActive() ?: return Result.success()
        val modelPath = model.filePath ?: return Result.success()

        if (!gemmaEngine.isModelLoaded()) {
            runCatching { gemmaEngine.loadModel(modelPath) }
                .onFailure { return Result.success() }
        }

        val template = aiModelRepository.findPromptTemplate("plantation_health")
            ?: PromptTemplate.plantationHealthDefault()

        val calendarAccount = dataStore.data.first()[KEY_CALENDAR_ACCOUNT]?.ifBlank { null }

        val plantations = plantationRepository.observeAll().first()
        val scheduledActions = mutableListOf<Pair<String, String>>() // plantationName, actionTitle

        for (plantation in plantations) {
            if (plantation.plants.isEmpty()) continue
            val loc = plantation.location
            if (!loc.hasCoordinates) continue

            val weather = weatherRepository
                .observeCachedWeather(loc.latitude!!, loc.longitude!!)
                .first() ?: continue

            val pendingAiActions = actionRepository.observePendingAiByPlantation(plantation.id).first()
            val prompt = buildHealthPrompt(template.content, plantation, weather, pendingAiActions.map { it.title })
            val response = gemmaEngine.generateText(prompt).getOrNull() ?: continue

            // Save analysis record for history
            saveAnalysisHandler.handle(
                SaveAnalysisCommand(
                    plantationId = plantation.id,
                    plantTypeId = null,
                    plantationName = plantation.name,
                    plantTypeName = null,
                    rawResponse = response
                )
            )

            // Parse suggestions and create PlantationAction items
            val result = parsePhotoAnalysisResponse(response)
            if (result.suggestions.isNotEmpty()) {
                // Delete previous AI pending actions for this plantation
                actionRepository.deleteAiPendingByPlantation(plantation.id)

                for (suggestion in result.suggestions.take(5)) {
                    val actionType = mapToActionType(suggestion.type)
                    val scheduledAt = parseSuggestedDate(suggestion.suggestedDate)

                    scheduleActionHandler.handle(
                        ScheduleActionCommand(
                            plantationId = plantation.id,
                            actionType = actionType,
                            title = suggestion.title.ifBlank { actionType.name.lowercase().replaceFirstChar { it.uppercase() } },
                            notes = suggestion.description,
                            scheduledAt = scheduledAt,
                            source = ActionSource.AI,
                            calendarAccountEmail = calendarAccount
                        )
                    ).onSuccess { action ->
                        scheduledActions.add(plantation.name to action.title)
                    }
                }
            }
        }

        if (scheduledActions.isNotEmpty()) {
            postNotification(scheduledActions)
        }

        return Result.success()
    }

    private fun buildHealthPrompt(
        baseTemplate: String,
        plantation: Plantation,
        weather: WeatherData,
        pendingActionTitles: List<String>
    ): String = buildString {
        append(baseTemplate)
        append("\n\nToday's date: ${LocalDate.now(ZoneId.systemDefault())}. All suggestedDate values must use this year or later.\n")

        append("\n### Plantation\n")
        append("Type: ${plantation.type.name} (${plantation.type.defaultIconEmoji})\n")
        val loc = plantation.location
        when {
            loc.municipality.isNotBlank() -> append("Location: ${loc.municipality}, ${loc.province}\n")
            loc.displayAddress.isNotBlank() -> append("Location: ${loc.displayAddress}\n")
        }
        if (plantation.notes.isNotBlank()) append("Notes: ${plantation.notes}\n")

        if (plantation.plants.isNotEmpty()) {
            append("\n### Plants\n")
            plantation.plants.forEach { pt ->
                append("- ${pt.name}")
                if (pt.variety.isNotBlank()) append(" (${pt.variety})")
                if (pt.count > 0) append(", ${pt.count} units")
                append("\n")
            }
        }

        if (pendingActionTitles.isNotEmpty()) {
            append("\n### Already pending actions (do NOT suggest these again)\n")
            pendingActionTitles.forEach { append("- $it\n") }
        }

        append("\n### Current weather\n")
        weather.current?.let { c ->
            append(
                "${c.condition.name.replace('_', ' ')}, ${c.temperatureCelsius}°C " +
                    "(feels like ${c.feelsLikeCelsius}°C), humidity ${c.humidity}%, " +
                    "wind ${c.windSpeedKmh} km/h, precipitation ${c.precipitationMm} mm\n"
            )
        }

        if (weather.forecast.isNotEmpty()) {
            append("\n### Weather forecast (next 15 days)\n")
            val fmt = DateTimeFormatter.ofPattern("MMM dd")
            weather.forecast.take(15).forEach { day ->
                val date = LocalDate.ofInstant(day.date, ZoneId.systemDefault()).format(fmt)
                append(
                    "$date: ${day.condition.name.replace('_', ' ')} " +
                        "${day.maxTempCelsius.toInt()}/${day.minTempCelsius.toInt()}°C, " +
                        "rain ${day.precipitationProbability}%\n"
                )
            }
        }
    }

    private fun postNotification(actions: List<Pair<String, String>>) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_actions_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = if (actions.size == 1) {
            context.getString(R.string.notif_actions_body_single, actions[0].first, actions[0].second)
        } else {
            context.getString(R.string.notif_actions_body_multiple, actions.size)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.notif_actions_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_view,
                context.getString(R.string.notif_actions_view),
                pendingIntent
            )
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val WORK_NAME = "plantation_health_daily"
        private const val WORK_NAME_ONETIME = "plantation_health_onetime"
        const val CHANNEL_ID = "agroai_actions"
        private const val NOTIFICATION_ID = 1002
        private val KEY_CALENDAR_ACCOUNT = stringPreferencesKey("selected_google_account")

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PlantationHealthWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .setInitialDelay(2, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun scheduleOneTime(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_ONETIME,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<PlantationHealthWorker>().build()
            )
        }

        fun mapToActionType(typeStr: String): ActionType {
            val upper = typeStr.uppercase().trim()
            // Direct match
            runCatching { ActionType.valueOf(upper) }.getOrNull()?.let { return it }
            // Legacy TreatmentType → ActionType mapping
            return when (upper) {
                "RIEGO" -> ActionType.REGAR
                "PODA" -> ActionType.PODAR
                "COSECHA" -> ActionType.COSECHAR
                "FERTILIZACION" -> ActionType.FERTILIZAR
                "FUMIGACION" -> ActionType.FUMIGAR
                "INJERTO" -> ActionType.INJERTAR
                "TRANSPLANTE", "TRASPLANTE" -> ActionType.TRASPLANTAR
                else -> ActionType.OTRO
            }
        }

        fun parseSuggestedDate(raw: String?): Instant {
            val today = LocalDate.now(ZoneId.systemDefault())
            val date = raw?.let {
                runCatching { LocalDate.parse(it) }.getOrNull()?.takeIf { d -> !d.isBefore(today) }
            } ?: today.plusDays(3)
            return date.atTime(LocalTime.of(9, 0)).atZone(ZoneId.systemDefault()).toInstant()
        }
    }
}
