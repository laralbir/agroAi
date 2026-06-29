package com.laralnet.agroai.weather.infrastructure.api

import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.laralnet.agroai.weather.domain.model.CurrentWeather
import com.laralnet.agroai.weather.domain.model.DailyForecast
import com.laralnet.agroai.weather.domain.model.WeatherCondition
import com.laralnet.agroai.weather.domain.model.WeatherData
import com.laralnet.agroai.weather.domain.repository.WeatherRepository
import com.laralnet.agroai.weather.infrastructure.persistence.dao.WeatherDao
import com.laralnet.agroai.weather.infrastructure.persistence.entity.WeatherEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject

class OpenMeteoWeatherRepository @Inject constructor(
    private val api: OpenMeteoApiService,
    private val dao: WeatherDao
) : WeatherRepository {

    private val gson = GsonBuilder()
        .registerTypeAdapter(Instant::class.java, InstantEpochMillisAdapter)
        .create()

    override suspend fun fetchWeather(latitude: Double, longitude: Double): Result<WeatherData> =
        runCatching { parseResponse(api.getForecast(latitude = latitude, longitude = longitude)) }

    override fun observeCachedWeather(latitude: Double, longitude: Double): Flow<WeatherData?> =
        dao.observe(locationKey(latitude, longitude)).map { entity ->
            entity?.toDomain()
        }

    override suspend fun refreshWeather(latitude: Double, longitude: Double) {
        fetchWeather(latitude, longitude).getOrNull()?.let { data ->
            // Use query coordinates as key — Open-Meteo snaps lat/lon to its grid,
            // so response.latitude/longitude differ from the query. Always key by the
            // query coords so observeCachedWeather() can find the entry.
            dao.upsert(data.toEntity(queryLat = latitude, queryLon = longitude))
        }
    }

    override suspend fun refreshWeatherIfStale(latitude: Double, longitude: Double, maxAgeMs: Long) {
        val cached = dao.getOnce(locationKey(latitude, longitude))
        val ageMs = if (cached != null) System.currentTimeMillis() - cached.fetchedAt else Long.MAX_VALUE
        // Valid forecast JSON has dates stored as Long: "date":1234567890000
        // Legacy entries (pre Instant-adapter fix) have "date":{} or "date":{"epochSecond":…}
        // — those get filtered as EPOCH and produce empty forecasts.
        // Treat any cache with non-empty forecastJson that lacks a numeric date as stale.
        val hasValidForecast = cached?.forecastJson?.let { json ->
            json != "[]" && json.contains(Regex(""""date"\s*:\s*\d"""))
        } == true
        if (ageMs > maxAgeMs || !hasValidForecast) refreshWeather(latitude, longitude)
    }

    private fun parseResponse(response: OpenMeteoResponse): WeatherData {
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

        return WeatherData(
            latitude = response.latitude,
            longitude = response.longitude,
            fetchedAt = Instant.now(),
            current = current,
            forecast = forecast
        )
    }

    private fun WeatherData.toEntity(queryLat: Double = latitude, queryLon: Double = longitude): WeatherEntity = WeatherEntity(
        id = locationKey(queryLat, queryLon),
        latitude = queryLat,
        longitude = queryLon,
        fetchedAt = fetchedAt.toEpochMilli(),
        currentJson = current?.let { gson.toJson(it) },
        forecastJson = gson.toJson(forecast)
    )

    private fun WeatherEntity.toDomain(): WeatherData {
        val listType = object : TypeToken<List<DailyForecast>>() {}.type
        val forecast: List<DailyForecast> = gson.fromJson(forecastJson, listType)
        return WeatherData(
            latitude = latitude,
            longitude = longitude,
            fetchedAt = Instant.ofEpochMilli(fetchedAt),
            current = currentJson?.let { gson.fromJson(it, CurrentWeather::class.java) },
            // Filter sentinel EPOCH entries produced by the legacy-object fallback in the adapter
            forecast = forecast.filter { it.date != Instant.EPOCH }
        )
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

    companion object {
        fun locationKey(lat: Double, lon: Double) =
            "%.2f_%.2f".format(Locale.ROOT, lat, lon)

        // Gson cannot reliably serialize/deserialize java.time.Instant via reflection on Android
        // (private final fields → always reads back as epoch 0 = Jan 1, 1970).
        // Storing as epoch millis (Long) is safe and compact.
        private object InstantEpochMillisAdapter : TypeAdapter<Instant>() {
            override fun write(out: JsonWriter, value: Instant?) {
                if (value == null) out.nullValue() else out.value(value.toEpochMilli())
            }

            override fun read(reader: JsonReader): Instant {
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull()
                    return Instant.EPOCH
                }
                // Legacy entries were written as a JSON object {epochSecond:…, nano:…}; return
                // EPOCH as sentinel so toDomain() can filter them out. Never return null here —
                // DailyForecast.date is non-nullable and Gson bypasses Kotlin null-safety via
                // Unsafe, which causes a NPE when the field is accessed in the UI.
                return if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                    reader.skipValue()
                    Instant.EPOCH
                } else {
                    Instant.ofEpochMilli(reader.nextLong())
                }
            }
        }
    }
}
