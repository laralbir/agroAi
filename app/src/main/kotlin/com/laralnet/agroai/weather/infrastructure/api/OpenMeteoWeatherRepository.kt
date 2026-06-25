package com.laralnet.agroai.weather.infrastructure.api

import com.laralnet.agroai.weather.domain.model.CurrentWeather
import com.laralnet.agroai.weather.domain.model.DailyForecast
import com.laralnet.agroai.weather.domain.model.WeatherCondition
import com.laralnet.agroai.weather.domain.model.WeatherData
import com.laralnet.agroai.weather.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class OpenMeteoWeatherRepository @Inject constructor(
    private val api: OpenMeteoApiService
) : WeatherRepository {

    override suspend fun fetchWeather(latitude: Double, longitude: Double): Result<WeatherData> =
        runCatching {
            val response = api.getForecast(latitude = latitude, longitude = longitude)
            val timezone = runCatching { ZoneId.of(response.timezone) }.getOrDefault(ZoneId.systemDefault())

            val current = response.current.let { c ->
                CurrentWeather(
                    temperatureCelsius = c.temperatureCelsius,
                    humidity = c.relativeHumidity,
                    windSpeedKmh = c.windSpeedKmh,
                    windDirection = degreesToCompass(c.windDirectionDeg),
                    precipitationMm = c.precipitation,
                    condition = wmoCodeToCondition(c.weatherCode),
                    feelsLikeCelsius = c.apparentTemperature
                )
            }

            val daily = response.daily
            val forecast = daily.time.indices.map { i ->
                DailyForecast(
                    date = LocalDate.parse(daily.time[i]).atStartOfDay(timezone).toInstant(),
                    maxTempCelsius = daily.tempMax.getOrElse(i) { 0.0 },
                    minTempCelsius = daily.tempMin.getOrElse(i) { 0.0 },
                    precipitationProbability = daily.precipProbability.getOrElse(i) { 0 },
                    precipitationMm = daily.precipSum.getOrElse(i) { 0.0 },
                    condition = wmoCodeToCondition(daily.weatherCode.getOrElse(i) { 0 }),
                    windMaxKmh = daily.windSpeedMax.getOrElse(i) { 0.0 },
                    uvIndex = daily.uvIndexMax.getOrElse(i) { 0.0 }.toInt()
                )
            }

            WeatherData(
                latitude = latitude,
                longitude = longitude,
                fetchedAt = Instant.now(),
                current = current,
                forecast = forecast
            )
        }

    override fun observeCachedWeather(latitude: Double, longitude: Double): Flow<WeatherData?> =
        flowOf(null)

    override suspend fun refreshWeather(latitude: Double, longitude: Double) {
        fetchWeather(latitude, longitude)
    }

    private fun wmoCodeToCondition(code: Int): WeatherCondition = when (code) {
        0, 1 -> WeatherCondition.CLEAR
        2 -> WeatherCondition.PARTLY_CLOUDY
        3 -> WeatherCondition.OVERCAST
        45, 48 -> WeatherCondition.FOG
        51, 53, 61 -> WeatherCondition.LIGHT_RAIN
        55, 63, 80, 81 -> WeatherCondition.MODERATE_RAIN
        57, 65, 66, 67, 82 -> WeatherCondition.HEAVY_RAIN
        56 -> WeatherCondition.FROST
        71, 73, 75, 77, 85, 86 -> WeatherCondition.SNOW
        95 -> WeatherCondition.STORM
        96, 99 -> WeatherCondition.HAIL
        else -> WeatherCondition.PARTLY_CLOUDY
    }

    private fun degreesToCompass(degrees: Int): String {
        val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        return dirs[((degrees + 22) % 360) / 45]
    }
}
