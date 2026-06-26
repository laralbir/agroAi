package com.laralnet.agroai.weather.infrastructure

import com.laralnet.agroai.weather.domain.model.WeatherCondition
import com.laralnet.agroai.weather.infrastructure.api.OpenMeteoApiService
import com.laralnet.agroai.weather.infrastructure.api.OpenMeteoWeatherRepository
import com.laralnet.agroai.weather.infrastructure.persistence.dao.WeatherDao
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class OpenMeteoParserTest {

    private val api: OpenMeteoApiService = mockk()
    private val dao: WeatherDao = mockk(relaxed = true)
    private val repository = OpenMeteoWeatherRepository(api, dao)

    private fun fakeResponse(
        temperature: Double = 22.0,
        humidity: Int = 60,
        windSpeed: Double = 15.0,
        windDir: Int = 90,
        feelsLike: Double = 20.0,
        precipMm: Double = 0.0,
        weatherCode: Int = 0,
        dailyCodes: List<Int> = listOf(0, 61, 95, 56),
        dailyPrecipProb: List<Int> = listOf(10, 70, 90, 30)
    ) = com.laralnet.agroai.weather.infrastructure.api.OpenMeteoResponse(
        latitude = 40.41,
        longitude = -3.70,
        timezone = "Europe/Madrid",
        current = com.laralnet.agroai.weather.infrastructure.api.OpenMeteoCurrent(
            temperatureCelsius = temperature,
            relativeHumidity = humidity,
            windSpeedKmh = windSpeed,
            windDirectionDeg = windDir,
            apparentTemperature = feelsLike,
            precipitation = precipMm,
            weatherCode = weatherCode
        ),
        daily = com.laralnet.agroai.weather.infrastructure.api.OpenMeteoDaily(
            time = dailyCodes.mapIndexed { i, _ -> "2026-07-0${i + 1}" },
            tempMax = List(dailyCodes.size) { 28.0 },
            tempMin = List(dailyCodes.size) { 14.0 },
            precipProbability = dailyPrecipProb,
            precipSum = List(dailyCodes.size) { 0.0 },
            weatherCode = dailyCodes,
            windSpeedMax = List(dailyCodes.size) { 20.0 },
            uvIndexMax = List(dailyCodes.size) { 6.0 }
        )
    )

    @Test
    fun `fetchWeather maps WMO code 0 to CLEAR`() = runTest {
        coEvery { api.getForecast(any(), any(), any(), any(), any(), any()) } returns fakeResponse(weatherCode = 0)

        val result = repository.fetchWeather(40.41, -3.70)

        assertTrue(result.isSuccess)
        assertEquals(WeatherCondition.CLEAR, result.getOrNull()?.current?.condition)
    }

    @Test
    fun `fetchWeather maps WMO code 95 to STORM`() = runTest {
        coEvery { api.getForecast(any(), any(), any(), any(), any(), any()) } returns fakeResponse(weatherCode = 95)

        val result = repository.fetchWeather(40.41, -3.70)

        assertEquals(WeatherCondition.STORM, result.getOrNull()?.current?.condition)
    }

    @Test
    fun `fetchWeather maps WMO code 56 (freezing drizzle) to FROST`() = runTest {
        coEvery { api.getForecast(any(), any(), any(), any(), any(), any()) } returns fakeResponse(weatherCode = 56)

        val result = repository.fetchWeather(40.41, -3.70)

        assertEquals(WeatherCondition.FROST, result.getOrNull()?.current?.condition)
    }

    @Test
    fun `fetchWeather maps WMO code 65 (heavy rain) to HEAVY_RAIN`() = runTest {
        coEvery { api.getForecast(any(), any(), any(), any(), any(), any()) } returns fakeResponse(weatherCode = 65)

        val result = repository.fetchWeather(40.41, -3.70)

        assertEquals(WeatherCondition.HEAVY_RAIN, result.getOrNull()?.current?.condition)
    }

    @Test
    fun `fetchWeather maps wind direction 90 degrees to E`() = runTest {
        coEvery { api.getForecast(any(), any(), any(), any(), any(), any()) } returns fakeResponse(windDir = 90)

        val result = repository.fetchWeather(40.41, -3.70)

        assertEquals("E", result.getOrNull()?.current?.windDirection)
    }

    @Test
    fun `fetchWeather maps wind direction 0 degrees to N`() = runTest {
        coEvery { api.getForecast(any(), any(), any(), any(), any(), any()) } returns fakeResponse(windDir = 0)

        val result = repository.fetchWeather(40.41, -3.70)

        assertEquals("N", result.getOrNull()?.current?.windDirection)
    }

    @Test
    fun `fetchWeather populates daily forecast list`() = runTest {
        coEvery { api.getForecast(any(), any(), any(), any(), any(), any()) } returns fakeResponse(
            dailyCodes = listOf(0, 61, 95, 56)
        )

        val result = repository.fetchWeather(40.41, -3.70)
        val forecast = result.getOrNull()?.forecast

        assertNotNull(forecast)
        assertEquals(4, forecast!!.size)
        assertEquals(WeatherCondition.CLEAR, forecast[0].condition)
        assertEquals(WeatherCondition.LIGHT_RAIN, forecast[1].condition)
        assertEquals(WeatherCondition.STORM, forecast[2].condition)
        assertEquals(WeatherCondition.FROST, forecast[3].condition)
    }

    @Test
    fun `fetchWeather returns failure on API exception`() = runTest {
        coEvery { api.getForecast(any(), any(), any(), any(), any(), any()) } throws RuntimeException("timeout")

        val result = repository.fetchWeather(40.41, -3.70)

        assertTrue(result.isFailure)
    }

    @Test
    fun `locationKey rounds to 2 decimal places`() {
        assertEquals("40.41_-3.70", OpenMeteoWeatherRepository.locationKey(40.4123, -3.6999))
    }
}
