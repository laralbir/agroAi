package com.laralnet.agroai.weather.application.handler

import com.laralnet.agroai.weather.domain.repository.WeatherRepository
import javax.inject.Inject

class RefreshWeatherHandler @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    suspend fun handle(latitude: Double, longitude: Double): Result<Unit> =
        runCatching { weatherRepository.refreshWeather(latitude, longitude) }
}
