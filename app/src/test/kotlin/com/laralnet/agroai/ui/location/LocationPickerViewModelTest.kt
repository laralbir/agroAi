package com.laralnet.agroai.ui.location

import android.location.Location
import app.cash.turbine.test
import com.laralnet.agroai.location.application.query.GetCurrentLocationQuery
import com.laralnet.agroai.location.application.query.ReverseGeocodeQuery
import com.laralnet.agroai.location.application.query.SearchPlacesQuery
import com.laralnet.agroai.location.domain.model.PlaceResult
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

    private val searchPlacesQuery: SearchPlacesQuery = mockk()
    private val reverseGeocodeQuery: ReverseGeocodeQuery = mockk()
    private val getCurrentLocationQuery: GetCurrentLocationQuery = mockk()

    private fun viewModel() = LocationPickerViewModel(
        searchPlacesQuery = searchPlacesQuery,
        reverseGeocodeQuery = reverseGeocodeQuery,
        getCurrentLocationQuery = getCurrentLocationQuery
    )

    private fun fakePlace(
        municipality: String = "Sevilla",
        lat: Double = 37.38,
        lon: Double = -5.97
    ) = PlaceResult(
        displayName = "$municipality, Andalucía, España",
        latitude = lat,
        longitude = lon,
        municipality = municipality,
        province = "Andalucía",
        country = "España"
    )

    @Test
    fun `initial state has null marker and empty search`() {
        val vm = viewModel()
        val state = vm.state.value
        assertNull(state.markerPosition)
        assertTrue(state.searchQuery.isEmpty())
        assertTrue(state.searchResults.isEmpty())
    }

    @Test
    fun `onSearchQueryChange with less than 3 chars does not trigger search`() = runTest {
        val vm = viewModel()
        vm.onSearchQueryChange("Se")
        advanceTimeBy(600)
        coVerify(exactly = 0) { searchPlacesQuery.invoke(any()) }
    }

    @Test
    fun `onSearchQueryChange triggers search after debounce`() = runTest {
        coEvery { searchPlacesQuery("Sevilla") } returns Result.success(listOf(fakePlace()))

        val vm = viewModel()
        vm.onSearchQueryChange("Sevilla")
        advanceTimeBy(600)

        coVerify(exactly = 1) { searchPlacesQuery("Sevilla") }
    }

    @Test
    fun `onSearchQueryChange populates searchResults on success`() = runTest {
        coEvery { searchPlacesQuery(any()) } returns Result.success(
            listOf(fakePlace("Sevilla"), fakePlace("Huelva"))
        )

        val vm = viewModel()
        vm.onSearchQueryChange("Andalucía")
        advanceTimeBy(600)

        assertEquals(2, vm.state.value.searchResults.size)
    }

    @Test
    fun `onPlaceSelected updates markerPosition and clears results`() = runTest {
        coEvery { searchPlacesQuery(any()) } returns Result.success(listOf(fakePlace()))

        val vm = viewModel()
        vm.onSearchQueryChange("Sevilla")
        advanceTimeBy(600)

        vm.onPlaceSelected(fakePlace())

        val state = vm.state.value
        assertNotNull(state.markerPosition)
        assertEquals(37.38, state.markerPosition!!.latitude, 0.01)
        assertTrue(state.searchResults.isEmpty())
    }

    @Test
    fun `onPlaceSelected sets resolved location municipality`() = runTest {
        val vm = viewModel()
        vm.onPlaceSelected(fakePlace("Córdoba"))

        assertEquals("Córdoba", vm.state.value.resolvedLocation?.municipality)
    }

    @Test
    fun `onMapTap updates marker and triggers reverse geocoding`() = runTest {
        coEvery { reverseGeocodeQuery(any(), any()) } returns Result.success(fakePlace("Granada"))

        val vm = viewModel()
        vm.onMapTap(GeoPoint(37.18, -3.6))

        coVerify { reverseGeocodeQuery(37.18, -3.6) }
        assertEquals(37.18, vm.state.value.markerPosition!!.latitude, 0.01)
    }

    @Test
    fun `onUseGps sets marker from GPS coordinates`() = runTest {
        val gpsPlace = fakePlace("Málaga", 36.72, -4.42)
        coEvery { getCurrentLocationQuery() } returns Result.success(gpsPlace)

        val vm = viewModel()
        vm.onUseGps()

        assertEquals(36.72, vm.state.value.markerPosition!!.latitude, 0.01)
    }

    @Test
    fun `onUseGps sets error on GPS failure`() = runTest {
        coEvery { getCurrentLocationQuery() } returns Result.failure(RuntimeException("No GPS"))

        val vm = viewModel()
        vm.onUseGps()

        assertNotNull(vm.state.value.error)
    }

    @Test
    fun `clearError removes error from state`() = runTest {
        coEvery { getCurrentLocationQuery() } returns Result.failure(RuntimeException("err"))
        val vm = viewModel()
        vm.onUseGps()
        assertNotNull(vm.state.value.error)

        vm.clearError()

        assertNull(vm.state.value.error)
    }
}
