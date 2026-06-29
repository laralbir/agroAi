package com.laralnet.agroai.ui.home

import com.laralnet.agroai.aimodel.application.query.ObserveModelsQuery
import com.laralnet.agroai.aimodel.domain.model.AIModel
import com.laralnet.agroai.aimodel.domain.repository.AIModelRepository
import com.laralnet.agroai.location.application.query.GetCurrentLocationQuery
import com.laralnet.agroai.location.domain.model.PlaceResult
import com.laralnet.agroai.location.domain.repository.LocationRepository
import com.laralnet.agroai.plantation.domain.model.Plantation
import com.laralnet.agroai.plantation.domain.repository.PlantationRepository
import com.laralnet.agroai.treatment.application.query.ObserveUpcomingTreatmentsQuery
import com.laralnet.agroai.treatment.domain.model.Treatment
import com.laralnet.agroai.treatment.domain.repository.TreatmentRepository
import com.laralnet.agroai.treatment.domain.model.TreatmentStatus
import com.laralnet.agroai.treatment.domain.model.TreatmentType
import com.laralnet.agroai.ui.screens.home.HomeViewModel
import com.laralnet.agroai.util.MainDispatcherRule
import com.laralnet.agroai.weather.application.handler.RefreshWeatherHandler
import com.laralnet.agroai.weather.application.query.ObserveWeatherQuery
import com.laralnet.agroai.weather.domain.model.WeatherData
import com.laralnet.agroai.weather.domain.repository.WeatherRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HomeViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val plantationRepository: PlantationRepository = mockk()
    private val aiModelRepository: AIModelRepository = mockk()
    private val treatmentRepository: TreatmentRepository = mockk()
    private val locationRepository: LocationRepository = mockk()
    private val weatherRepository: WeatherRepository = mockk()

    @Before
    fun setUp() {
        every { plantationRepository.observeAll() } returns flowOf<List<Plantation>>(emptyList())
        every { aiModelRepository.observeAll() } returns flowOf<List<AIModel>>(emptyList())
        every { treatmentRepository.observeUpcoming() } returns flowOf<List<Treatment>>(emptyList())
        coEvery { locationRepository.getCurrentLocation() } returns null
        every { weatherRepository.observeCachedWeather(any<Double>(), any<Double>()) } returns MutableStateFlow<WeatherData?>(null)
        coEvery { weatherRepository.refreshWeather(any<Double>(), any<Double>()) } just Runs
    }

    private fun viewModel() = HomeViewModel(
        plantationRepository = plantationRepository,
        observeModels = ObserveModelsQuery(aiModelRepository),
        observeUpcomingTreatments = ObserveUpcomingTreatmentsQuery(treatmentRepository),
        getCurrentLocation = GetCurrentLocationQuery(locationRepository),
        refreshWeatherHandler = RefreshWeatherHandler(weatherRepository),
        observeWeatherQuery = ObserveWeatherQuery(weatherRepository)
    )

    private fun treatment(id: String, scheduledAt: Instant) = Treatment(
        id = id,
        plantationId = "p1",
        type = TreatmentType.RIEGO,
        title = "Treatment $id",
        scheduledAt = scheduledAt,
        status = TreatmentStatus.PENDING
    )

    private fun todayAt(hour: Int): Instant {
        val zone = ZoneId.systemDefault()
        return LocalDate.now(zone).atTime(hour, 0).atZone(zone).toInstant()
    }

    private fun daysFromNow(days: Long): Instant {
        val zone = ZoneId.systemDefault()
        return LocalDate.now(zone).plusDays(days).atTime(9, 0).atZone(zone).toInstant()
    }

    @Test
    fun `todayTreatments contains only treatments scheduled for today`() = runTest {
        val today1 = treatment("t1", todayAt(8))
        val today2 = treatment("t2", todayAt(18))
        val tomorrow = treatment("t3", daysFromNow(1))
        every { treatmentRepository.observeUpcoming() } returns flowOf<List<Treatment>>(listOf(today1, today2, tomorrow))

        val vm = viewModel()
        backgroundScope.launch { vm.todayTreatments.collect {} }
        advanceUntilIdle()

        val result = vm.todayTreatments.value
        assertEquals(2, result.size)
        assertTrue(result.any { it.id == "t1" })
        assertTrue(result.any { it.id == "t2" })
        assertTrue(result.none { it.id == "t3" })
    }

    @Test
    fun `todayTreatments is empty when no treatments are today`() = runTest {
        val tomorrow = treatment("t1", daysFromNow(1))
        every { treatmentRepository.observeUpcoming() } returns flowOf<List<Treatment>>(listOf(tomorrow))

        val vm = viewModel()
        advanceUntilIdle()

        assertTrue(vm.todayTreatments.value.isEmpty())
    }

    @Test
    fun `todayTreatments is empty when list is empty`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        assertTrue(vm.todayTreatments.value.isEmpty())
    }

    @Test
    fun `upcomingTreatments excludes today's treatments`() = runTest {
        val todayT = treatment("today", todayAt(10))
        val tomorrowT = treatment("tomorrow", daysFromNow(1))
        every { treatmentRepository.observeUpcoming() } returns flowOf<List<Treatment>>(listOf(todayT, tomorrowT))

        val vm = viewModel()
        backgroundScope.launch { vm.upcomingTreatments.collect {} }
        advanceUntilIdle()

        val result = vm.upcomingTreatments.value
        assertTrue(result.none { it.id == "today" })
        assertTrue(result.any { it.id == "tomorrow" })
    }

    @Test
    fun `upcomingTreatments is capped at 5`() = runTest {
        val treatments = (1..8).map { i -> treatment("t$i", daysFromNow(i.toLong())) }
        every { treatmentRepository.observeUpcoming() } returns flowOf<List<Treatment>>(treatments)

        val vm = viewModel()
        backgroundScope.launch { vm.upcomingTreatments.collect {} }
        advanceUntilIdle()

        assertEquals(5, vm.upcomingTreatments.value.size)
    }

    @Test
    fun `upcomingTreatments is empty when only today's treatments exist`() = runTest {
        val todayT = treatment("t1", todayAt(9))
        every { treatmentRepository.observeUpcoming() } returns flowOf<List<Treatment>>(listOf(todayT))

        val vm = viewModel()
        advanceUntilIdle()

        assertTrue(vm.upcomingTreatments.value.isEmpty())
    }

    @Test
    fun `homeWeather is null when location fails with exception`() = runTest {
        coEvery { locationRepository.getCurrentLocation() } throws SecurityException("no permission")

        val vm = viewModel()
        advanceUntilIdle()

        assertNull(vm.homeWeather.value)
    }

    @Test
    fun `homeWeather is null when location returns null`() = runTest {
        coEvery { locationRepository.getCurrentLocation() } returns null

        val vm = viewModel()
        advanceUntilIdle()

        assertNull(vm.homeWeather.value)
    }

    @Test
    fun `homeWeather emits data when GPS location is obtained`() = runTest {
        val place = PlaceResult("Home", 40.0, -3.0)
        val weather = WeatherData(latitude = 40.0, longitude = -3.0, current = null, forecast = emptyList())
        coEvery { locationRepository.getCurrentLocation() } returns place
        every { weatherRepository.observeCachedWeather(40.0, -3.0) } returns MutableStateFlow<WeatherData?>(weather)

        val vm = viewModel()
        backgroundScope.launch { vm.homeWeather.collect {} }
        advanceUntilIdle()

        assertEquals(weather, vm.homeWeather.value)
    }

    @Test
    fun `hasActiveModel is false when no models are loaded`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(false, vm.hasActiveModel.value)
    }
}
