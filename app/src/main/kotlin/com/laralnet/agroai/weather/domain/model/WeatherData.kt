package com.laralnet.agroai.weather.domain.model

import java.time.Instant

data class WeatherData(
    val latitude: Double,
    val longitude: Double,
    val fetchedAt: Instant = Instant.now(),
    val current: CurrentWeather?,
    val forecast: List<DailyForecast>
)

data class CurrentWeather(
    val temperatureCelsius: Double,
    val humidity: Int,
    val windSpeedKmh: Double,
    val windDirection: String,
    val precipitationMm: Double,
    val condition: WeatherCondition,
    val feelsLikeCelsius: Double
)

data class DailyForecast(
    val date: Instant,
    val maxTempCelsius: Double,
    val minTempCelsius: Double,
    val precipitationProbability: Int,
    val precipitationMm: Double,
    val condition: WeatherCondition,
    val windMaxKmh: Double,
    val uvIndex: Int
)

enum class WeatherCondition {
    CLEAR, PARTLY_CLOUDY, CLOUDY, OVERCAST, LIGHT_RAIN, MODERATE_RAIN, HEAVY_RAIN,
    STORM, SNOW, FROST, FOG, HAIL, WINDY
}

data class WeatherAlert(
    val id: String,
    val type: WeatherAlertType,
    val severity: AlertSeverity,
    val startAt: Instant,
    val endAt: Instant,
    val description: String
)

enum class WeatherAlertType { FROST, HEAVY_RAIN, STORM, HEAT_WAVE, STRONG_WIND, HAIL, SNOW, DROUGHT }
enum class AlertSeverity { LOW, MEDIUM, HIGH, EXTREME }
