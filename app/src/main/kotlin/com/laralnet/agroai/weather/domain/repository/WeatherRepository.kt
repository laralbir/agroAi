package com.laralnet.agroai.weather.domain.repository

import com.laralnet.agroai.weather.domain.model.WeatherData
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    suspend fun fetchWeather(latitude: Double, longitude: Double): Result<WeatherData>
    fun observeCachedWeather(latitude: Double, longitude: Double): Flow<WeatherData?>
    suspend fun refreshWeather(latitude: Double, longitude: Double)
}
