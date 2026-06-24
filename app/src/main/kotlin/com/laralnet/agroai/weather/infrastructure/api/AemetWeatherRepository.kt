package com.laralnet.agroai.weather.infrastructure.api

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.laralnet.agroai.weather.domain.model.DailyForecast
import com.laralnet.agroai.weather.domain.model.WeatherCondition
import com.laralnet.agroai.weather.domain.model.WeatherData
import com.laralnet.agroai.weather.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Named

class AemetWeatherRepository @Inject constructor(
    private val api: AemetApiService,
    private val dataStore: DataStore<Preferences>,
    @Named("aemet_api_key") private val apiKey: String
) : WeatherRepository {

    companion object {
        val AEMET_KEY_PREF = stringPreferencesKey("aemet_api_key")
    }

    override suspend fun fetchWeather(municipalityCode: String): Result<WeatherData> = runCatching {
        val key = dataStore.data.first()[AEMET_KEY_PREF] ?: apiKey
        if (key.isBlank()) error("AEMET API key not configured")

        val response = api.getDailyForecast(municipalityCode, key)
        if (response.estado != 200) error("AEMET error ${response.estado}: ${response.descripcion}")

        WeatherData(
            municipalityCode = municipalityCode,
            municipalityName = municipalityCode,
            current = null,
            forecast = emptyList()
        )
    }

    override fun observeCachedWeather(municipalityCode: String): Flow<WeatherData?> = flowOf(null)

    override suspend fun refreshWeather(municipalityCode: String) {
        fetchWeather(municipalityCode)
    }

    private fun mapSkyState(value: String): WeatherCondition = when (value) {
        "11", "11n" -> WeatherCondition.CLEAR
        "12", "12n", "13", "13n" -> WeatherCondition.PARTLY_CLOUDY
        "14", "14n", "15", "15n", "16", "16n" -> WeatherCondition.CLOUDY
        "17", "17n" -> WeatherCondition.OVERCAST
        "23", "24", "43", "44" -> WeatherCondition.LIGHT_RAIN
        "25", "26", "45", "46" -> WeatherCondition.MODERATE_RAIN
        "27", "47" -> WeatherCondition.HEAVY_RAIN
        "33", "34", "35", "36" -> WeatherCondition.STORM
        "51", "52", "53", "54" -> WeatherCondition.SNOW
        "71", "72", "73", "74" -> WeatherCondition.FOG
        else -> WeatherCondition.PARTLY_CLOUDY
    }
}
