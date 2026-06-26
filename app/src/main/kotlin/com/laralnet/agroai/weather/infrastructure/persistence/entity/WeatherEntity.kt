package com.laralnet.agroai.weather.infrastructure.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_cache")
data class WeatherEntity(
    @PrimaryKey val id: String,   // "%.2f_%.2f".format(lat, lon)
    val latitude: Double,
    val longitude: Double,
    val fetchedAt: Long,           // epoch millis
    val currentJson: String?,      // JSON of CurrentWeather, nullable if fetch only had daily
    val forecastJson: String       // JSON array of DailyForecast
)
