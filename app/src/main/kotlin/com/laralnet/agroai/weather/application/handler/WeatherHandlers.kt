package com.laralnet.agroai.weather.application.handler

import com.laralnet.agroai.weather.domain.repository.WeatherRepository
import javax.inject.Inject

class RefreshWeatherHandler @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    /** Always fetches from the network regardless of cache age. */
    suspend fun handle(latitude: Double, longitude: Double): Result<Unit> =
        runCatching { weatherRepository.refreshWeather(latitude, longitude) }

    /** Fetches only if the cached entry is missing or older than [maxAgeMs] (default 1 hour). */
    suspend fun handleIfStale(
        latitude: Double,
        longitude: Double,
        maxAgeMs: Long = WeatherRepository.MAX_CACHE_AGE_MS
    ): Result<Unit> = runCatching { weatherRepository.refreshWeatherIfStale(latitude, longitude, maxAgeMs) }
}
