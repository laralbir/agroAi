package com.laralnet.agroai.weather.application.query

import com.laralnet.agroai.weather.domain.model.AlertSeverity
import com.laralnet.agroai.weather.domain.model.DailyForecast
import com.laralnet.agroai.weather.domain.model.WeatherAlert
import com.laralnet.agroai.weather.domain.model.WeatherAlertType
import com.laralnet.agroai.weather.domain.model.WeatherCondition
import com.laralnet.agroai.weather.domain.model.WeatherData
import com.laralnet.agroai.weather.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

class ObserveWeatherQuery @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    operator fun invoke(latitude: Double, longitude: Double): Flow<WeatherData?> =
        weatherRepository.observeCachedWeather(latitude, longitude)
}

class GetWeatherAlertsQuery @Inject constructor() {

    operator fun invoke(weather: WeatherData?, scheduledAt: Instant): List<WeatherAlert> {
        if (weather == null) return emptyList()
        val zone = ZoneId.systemDefault()
        val scheduledDate = scheduledAt.atZone(zone).toLocalDate()
        return weather.forecast
            .filter { day ->
                val dayDate = day.date.atZone(zone).toLocalDate()
                dayDate == scheduledDate && day.condition in DANGEROUS_CONDITIONS
            }
            .map { it.toAlert() }
    }

    private fun DailyForecast.toAlert(): WeatherAlert = WeatherAlert(
        id = UUID.randomUUID().toString(),
        type = condition.toAlertType(),
        severity = condition.toSeverity(),
        startAt = date,
        endAt = date.plusSeconds(86_400),
        description = condition.name
    )

    private fun WeatherCondition.toAlertType(): WeatherAlertType = when (this) {
        WeatherCondition.FROST -> WeatherAlertType.FROST
        WeatherCondition.HEAVY_RAIN -> WeatherAlertType.HEAVY_RAIN
        WeatherCondition.STORM -> WeatherAlertType.STORM
        WeatherCondition.HAIL -> WeatherAlertType.HAIL
        WeatherCondition.SNOW -> WeatherAlertType.SNOW
        else -> WeatherAlertType.HEAVY_RAIN
    }

    private fun WeatherCondition.toSeverity(): AlertSeverity = when (this) {
        WeatherCondition.STORM, WeatherCondition.HAIL -> AlertSeverity.HIGH
        WeatherCondition.FROST, WeatherCondition.SNOW -> AlertSeverity.MEDIUM
        else -> AlertSeverity.LOW
    }

    companion object {
        private val DANGEROUS_CONDITIONS = setOf(
            WeatherCondition.FROST,
            WeatherCondition.HEAVY_RAIN,
            WeatherCondition.STORM,
            WeatherCondition.HAIL,
            WeatherCondition.SNOW
        )
    }
}
