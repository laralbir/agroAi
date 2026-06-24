package com.laralnet.agroai.weather.domain.repository

import com.laralnet.agroai.weather.domain.model.WeatherData
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    suspend fun fetchWeather(municipalityCode: String): Result<WeatherData>
    fun observeCachedWeather(municipalityCode: String): Flow<WeatherData?>
    suspend fun refreshWeather(municipalityCode: String)
}
