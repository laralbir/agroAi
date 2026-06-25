package com.laralnet.agroai.weather.infrastructure.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApiService {

    @GET("forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m,wind_direction_10m,apparent_temperature,weather_code",
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum,weather_code,wind_speed_10m_max,uv_index_max",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 7
    ): OpenMeteoResponse
}

data class OpenMeteoResponse(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timezone: String = "",
    val current: OpenMeteoCurrent = OpenMeteoCurrent(),
    val daily: OpenMeteoDaily = OpenMeteoDaily()
)

data class OpenMeteoCurrent(
    val time: String = "",
    @SerializedName("temperature_2m") val temperatureCelsius: Double = 0.0,
    @SerializedName("relative_humidity_2m") val relativeHumidity: Int = 0,
    val precipitation: Double = 0.0,
    @SerializedName("wind_speed_10m") val windSpeedKmh: Double = 0.0,
    @SerializedName("wind_direction_10m") val windDirectionDeg: Int = 0,
    @SerializedName("apparent_temperature") val apparentTemperature: Double = 0.0,
    @SerializedName("weather_code") val weatherCode: Int = 0
)

data class OpenMeteoDaily(
    val time: List<String> = emptyList(),
    @SerializedName("temperature_2m_max") val tempMax: List<Double> = emptyList(),
    @SerializedName("temperature_2m_min") val tempMin: List<Double> = emptyList(),
    @SerializedName("precipitation_probability_max") val precipProbability: List<Int> = emptyList(),
    @SerializedName("precipitation_sum") val precipSum: List<Double> = emptyList(),
    @SerializedName("weather_code") val weatherCode: List<Int> = emptyList(),
    @SerializedName("wind_speed_10m_max") val windSpeedMax: List<Double> = emptyList(),
    @SerializedName("uv_index_max") val uvIndexMax: List<Double> = emptyList()
)
