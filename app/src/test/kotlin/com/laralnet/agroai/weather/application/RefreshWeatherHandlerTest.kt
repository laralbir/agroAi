package com.laralnet.agroai.weather.application

import com.laralnet.agroai.weather.application.handler.RefreshWeatherHandler
import com.laralnet.agroai.weather.domain.repository.WeatherRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class RefreshWeatherHandlerTest {

    private val weatherRepository: WeatherRepository = mockk(relaxed = true)
    private val handler = RefreshWeatherHandler(weatherRepository)

    @Test
    fun `handle delegates to weatherRepository refreshWeather`() = runTest {
        val result = handler.handle(40.41, -3.70)

        assertTrue(result.isSuccess)
        coVerify { weatherRepository.refreshWeather(40.41, -3.70) }
    }

    @Test
    fun `handle returns failure when repository throws`() = runTest {
        coEvery { weatherRepository.refreshWeather(any(), any()) } throws RuntimeException("Network error")

        val result = handler.handle(40.41, -3.70)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Network error") == true)
    }

    @Test
    fun `handle passes correct coordinates to repository`() = runTest {
        handler.handle(51.509865, -0.118092)

        coVerify { weatherRepository.refreshWeather(51.509865, -0.118092) }
    }
}
