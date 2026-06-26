package com.laralnet.agroai.weather.infrastructure.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.laralnet.agroai.plantation.domain.repository.PlantationRepository
import com.laralnet.agroai.weather.domain.repository.WeatherRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Locale
import java.util.concurrent.TimeUnit

@HiltWorker
class WeatherRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val plantationRepository: PlantationRepository,
    private val weatherRepository: WeatherRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val plantations = plantationRepository.observeAll().first()
        val seen = mutableSetOf<String>()
        plantations.forEach { plantation ->
            val loc = plantation.location
            if (loc.hasCoordinates) {
                val key = "%.2f_%.2f".format(Locale.ROOT, loc.latitude, loc.longitude)
                if (seen.add(key)) {
                    runCatching {
                        weatherRepository.refreshWeather(loc.latitude!!, loc.longitude!!)
                    }
                }
            }
        }
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "weather_refresh_periodic"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WeatherRefreshWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
