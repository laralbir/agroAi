package com.laralnet.agroai.ui.location

import android.location.Location
import app.cash.turbine.test
import com.laralnet.agroai.location.infrastructure.gps.GpsLocationProvider
import com.laralnet.agroai.location.infrastructure.nominatim.NominatimAddress
import com.laralnet.agroai.location.infrastructure.nominatim.NominatimApiService
import com.laralnet.agroai.location.infrastructure.nominatim.NominatimPlace
import com.laralnet.agroai.ui.screens.plantation.wizard.LocationPickerViewModel
import com.laralnet.agroai.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.osmdroid.util.GeoPoint

@OptIn(ExperimentalCoroutinesApi::class)
class LocationPickerViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val nominatim: NominatimApiService = mockk()
    private val gpsProvider: GpsLocationProvider = mockk()
    private val viewModel = LocationPickerViewModel(nominatim, gpsProvider)

    private fun fakePlace(city: String = "Sevilla", lat: String = "37.38", lon: String = "-5.97") =
        NominatimPlace(
            placeId = 1L,
            lat = lat,
            lon = lon,
            displayName = "$city, Andalucía, España",
            address = NominatimAddress(city = city, state = "Andalucía", countryCode = "es")
        )

    @Test
    fun `initial state has null marker and empty search`() {
        val state = viewModel.state.value
        assertNull(state.markerPosition)
        assertTrue(state.searchQuery.isEmpty())
        assertTrue(state.searchResults.isEmpty())
    }

    @Test
    fun `onSearchQueryChange with less than 3 chars does not trigger search`() = runTest {
        viewModel.onSearchQueryChange("Se")
        advanceTimeBy(600)
        coVerify(exactly = 0) { nominatim.search(any()) }
    }

    @Test
    fun `onSearchQueryChange triggers search after debounce`() = runTest {
        coEvery { nominatim.search(any()) } returns listOf(fakePlace())

        viewModel.onSearchQueryChange("Sevilla")
        advanceTimeBy(600)

        coVerify(exactly = 1) { nominatim.search("Sevilla") }
    }

    @Test
    fun `onSearchQueryChange populates searchResults on success`() = runTest {
        coEvery { nominatim.search(any()) } returns listOf(fakePlace("Sevilla"), fakePlace("Huelva"))

        viewModel.onSearchQueryChange("Andalucía")
        advanceTimeBy(600)

        assertEquals(2, viewModel.state.value.searchResults.size)
    }

    @Test
    fun `onPlaceSelected updates markerPosition and clears results`() = runTest {
        coEvery { nominatim.search(any()) } returns listOf(fakePlace())
        viewModel.onSearchQueryChange("Sevilla")
        advanceTimeBy(600)

        viewModel.onPlaceSelected(fakePlace())

        val state = viewModel.state.value
        assertNotNull(state.markerPosition)
        assertEquals(37.38, state.markerPosition!!.latitude, 0.01)
        assertTrue(state.searchResults.isEmpty())
    }

    @Test
    fun `onPlaceSelected sets resolved location municipality`() = runTest {
        viewModel.onPlaceSelected(fakePlace("Córdoba"))

        assertEquals("Córdoba", viewModel.state.value.resolvedLocation?.municipality)
    }

    @Test
    fun `onMapTap updates marker and triggers reverse geocoding`() = runTest {
        coEvery { nominatim.reverse(any(), any()) } returns fakePlace("Granada")

        viewModel.onMapTap(GeoPoint(37.18, -3.6))

        coVerify { nominatim.reverse(37.18, -3.6) }
        assertEquals(37.18, viewModel.state.value.markerPosition!!.latitude, 0.01)
    }

    @Test
    fun `onUseGps sets marker from GPS coordinates`() = runTest {
        val fakeAndroidLocation = mockk<Location>(relaxed = true)
        io.mockk.every { fakeAndroidLocation.latitude } returns 36.72
        io.mockk.every { fakeAndroidLocation.longitude } returns -4.42
        coEvery { gpsProvider.getCurrentLocation() } returns Result.success(fakeAndroidLocation)
        coEvery { nominatim.reverse(any(), any()) } returns fakePlace("Málaga")

        viewModel.onUseGps()

        assertEquals(36.72, viewModel.state.value.markerPosition!!.latitude, 0.01)
    }

    @Test
    fun `onUseGps sets error on GPS failure`() = runTest {
        coEvery { gpsProvider.getCurrentLocation() } returns Result.failure(RuntimeException("No GPS"))

        viewModel.onUseGps()

        assertNotNull(viewModel.state.value.error)
    }

    @Test
    fun `clearError removes error from state`() = runTest {
        coEvery { gpsProvider.getCurrentLocation() } returns Result.failure(RuntimeException("err"))
        viewModel.onUseGps()
        assertNotNull(viewModel.state.value.error)

        viewModel.clearError()

        assertNull(viewModel.state.value.error)
    }
}
