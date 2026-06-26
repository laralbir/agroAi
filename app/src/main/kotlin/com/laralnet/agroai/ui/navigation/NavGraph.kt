package com.laralnet.agroai.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.laralnet.agroai.R
import com.laralnet.agroai.ui.screens.aimodel.ModelManagementScreen
import com.laralnet.agroai.ui.screens.analysis.PhotoAnalysisScreen
import android.net.Uri
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.laralnet.agroai.plantation.domain.model.Location
import com.laralnet.agroai.ui.screens.home.HomeScreen
import com.laralnet.agroai.ui.screens.onboarding.OnboardingScreen
import com.laralnet.agroai.ui.screens.onboarding.OnboardingViewModel
import com.laralnet.agroai.ui.screens.plantation.detail.PlantationDetailScreen
import com.laralnet.agroai.ui.screens.plantation.list.PlantationListScreen
import com.laralnet.agroai.ui.screens.plantation.wizard.LocationPickerScreen
import com.laralnet.agroai.ui.screens.plantation.wizard.PlantationWizardScreen
import com.laralnet.agroai.ui.screens.plantation.wizard.PlantationWizardViewModel
import com.laralnet.agroai.ui.screens.settings.SettingsScreen
import com.laralnet.agroai.ui.screens.calendar.CalendarScreen
import com.laralnet.agroai.ui.screens.treatment.ScheduleTreatmentScreen
import com.laralnet.agroai.ui.screens.treatment.TreatmentDetailScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Plantations : Screen("plantations")
    data object PlantationDetail : Screen("plantation/{id}") {
        fun route(id: String) = "plantation/$id"
    }
    data object PlantationWizard : Screen("plantation/new")
    data object PlantationEdit : Screen("plantation/{id}/edit") {
        fun route(id: String) = "plantation/$id/edit"
    }
    data object LocationPicker : Screen("location_picker")
    data object PhotoAnalysis : Screen("analysis?plantationId={plantationId}") {
        val routeBase = "analysis"
        fun route(plantationId: String? = null) =
            if (plantationId != null) "analysis?plantationId=$plantationId" else "analysis"
    }
    data object Calendar : Screen("calendar")
    data object Settings : Screen("settings")
    data object ModelManagement : Screen("models")
    data object ScheduleTreatment : Screen("plantation/{plantationId}/treatment/new") {
        fun route(plantationId: String) = "plantation/$plantationId/treatment/new"
        fun routeWithPrefill(
            plantationId: String,
            type: String,
            title: String,
            description: String,
            rawAnalysis: String? = null
        ) = "plantation/$plantationId/treatment/new" +
            "?prefillType=${Uri.encode(type)}" +
            "&prefillTitle=${Uri.encode(title.take(120))}" +
            "&prefillDesc=${Uri.encode(description.take(400))}" +
            (rawAnalysis?.take(3000)?.let { "&prefillAnalysis=${Uri.encode(it)}" } ?: "")
    }
    data object TreatmentDetail : Screen("treatment/{treatmentId}") {
        fun route(id: String) = "treatment/$id"
    }
    data object Onboarding : Screen("onboarding")
    /** Silent router — navigates immediately to Onboarding or Home, never shown to the user. */
    data object Router : Screen("router")
}

private const val KEY_PICKED_LAT = "picked_lat"
private const val KEY_PICKED_LON = "picked_lon"
private const val KEY_PICKED_ADDRESS = "picked_address"
private const val KEY_PICKED_MUNICIPALITY = "picked_municipality"
private const val KEY_PICKED_PROVINCE = "picked_province"

private data class BottomNavItem(
    val screen: Screen,
    val labelRes: Int,
    val navigateRoute: String = screen.route,
    val icon: @Composable () -> Unit
)

@Composable
fun AgroAINavGraph(
    navController: NavHostController = rememberNavController(),
    onboardingViewModel: OnboardingViewModel = hiltViewModel()
) {
    val bottomItems = listOf(
        BottomNavItem(Screen.Home, R.string.nav_home) {
            Icon(Icons.Default.Home, contentDescription = null)
        },
        BottomNavItem(Screen.Plantations, R.string.nav_plantations) {
            Icon(Icons.Default.Agriculture, contentDescription = null)
        },
        BottomNavItem(
            screen = Screen.PhotoAnalysis,
            labelRes = R.string.nav_analysis,
            navigateRoute = Screen.PhotoAnalysis.routeBase
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
        },
        BottomNavItem(Screen.Calendar, R.string.nav_calendar) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null)
        },
        BottomNavItem(Screen.Settings, R.string.nav_settings) {
            Icon(Icons.Default.Settings, contentDescription = null)
        }
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            icon = item.icon,
                            label = { Text(stringResource(item.labelRes)) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == item.screen.route
                            } == true,
                            onClick = {
                                navController.navigate(item.navigateRoute) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Router.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Silent router: reads onboarding flag and navigates immediately
            composable(Screen.Router.route) {
                val done by onboardingViewModel.onboardingDone.collectAsState()
                LaunchedEffect(done) {
                    if (done == null) return@LaunchedEffect // still loading DataStore
                    val target = if (done == true) Screen.Home.route else Screen.Onboarding.route
                    navController.navigate(target) {
                        popUpTo(Screen.Router.route) { inclusive = true }
                    }
                }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onCompleted = {
                        onboardingViewModel.markDone()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToPlantations = {
                        navController.navigate(Screen.Plantations.route)
                    },
                    onNavigateToPlantationDetail = { id ->
                        navController.navigate(Screen.PlantationDetail.route(id))
                    },
                    onNavigateToModels = {
                        navController.navigate(Screen.ModelManagement.route)
                    },
                    onNavigateToTreatmentDetail = { id ->
                        navController.navigate(Screen.TreatmentDetail.route(id))
                    }
                )
            }
            composable(Screen.Plantations.route) {
                PlantationListScreen(
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.PlantationDetail.route(id))
                    },
                    onNavigateToWizard = {
                        navController.navigate(Screen.PlantationWizard.route)
                    }
                )
            }
            composable(
                route = Screen.PlantationDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: return@composable
                PlantationDetailScreen(
                    plantationId = id,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAnalysis = {
                        navController.navigate(Screen.PhotoAnalysis.route(id))
                    },
                    onNavigateToEdit = {
                        navController.navigate(Screen.PlantationEdit.route(id))
                    },
                    onNavigateToScheduleTreatment = { pId ->
                        navController.navigate(Screen.ScheduleTreatment.route(pId))
                    },
                    onNavigateToTreatmentDetail = { tId ->
                        navController.navigate(Screen.TreatmentDetail.route(tId))
                    }
                )
            }
            composable(Screen.PlantationWizard.route) { backStack ->
                val wizardViewModel: PlantationWizardViewModel = hiltViewModel()

                // Receive location result posted by LocationPickerScreen
                val savedStateHandle = backStack.savedStateHandle
                LaunchedEffect(Unit) {
                    savedStateHandle.getStateFlow<Double?>(KEY_PICKED_LAT, null).collect { lat ->
                        if (lat == null) return@collect
                        val location = Location(
                            latitude = lat,
                            longitude = savedStateHandle[KEY_PICKED_LON],
                            address = savedStateHandle[KEY_PICKED_ADDRESS] ?: "",
                            municipality = savedStateHandle[KEY_PICKED_MUNICIPALITY] ?: "",
                            province = savedStateHandle[KEY_PICKED_PROVINCE] ?: ""
                        )
                        wizardViewModel.setLocationFromMap(location)
                        savedStateHandle.remove<Double>(KEY_PICKED_LAT)
                    }
                }

                PlantationWizardScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onPlantationSaved = { id ->
                        navController.popBackStack()
                        navController.navigate(Screen.PlantationDetail.route(id))
                    },
                    onOpenMapPicker = { navController.navigate(Screen.LocationPicker.route) },
                    viewModel = wizardViewModel
                )
            }
            composable(
                route = Screen.PlantationEdit.route,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { backStack ->
                val editViewModel: PlantationWizardViewModel = hiltViewModel()

                val savedStateHandle = backStack.savedStateHandle
                LaunchedEffect(Unit) {
                    savedStateHandle.getStateFlow<Double?>(KEY_PICKED_LAT, null).collect { lat ->
                        if (lat == null) return@collect
                        val location = Location(
                            latitude = lat,
                            longitude = savedStateHandle[KEY_PICKED_LON],
                            address = savedStateHandle[KEY_PICKED_ADDRESS] ?: "",
                            municipality = savedStateHandle[KEY_PICKED_MUNICIPALITY] ?: "",
                            province = savedStateHandle[KEY_PICKED_PROVINCE] ?: ""
                        )
                        editViewModel.setLocationFromMap(location)
                        savedStateHandle.remove<Double>(KEY_PICKED_LAT)
                    }
                }

                val id = backStack.arguments?.getString("id") ?: return@composable
                PlantationWizardScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onPlantationSaved = {
                        navController.navigate(Screen.PlantationDetail.route(id)) {
                            popUpTo(Screen.PlantationDetail.route) { inclusive = true }
                        }
                    },
                    onOpenMapPicker = { navController.navigate(Screen.LocationPicker.route) },
                    viewModel = editViewModel
                )
            }
            composable(Screen.LocationPicker.route) {
                LocationPickerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onLocationConfirmed = { location ->
                        // Pass result back to the wizard via savedStateHandle
                        navController.previousBackStackEntry?.savedStateHandle?.apply {
                            set(KEY_PICKED_LAT, location.latitude)
                            set(KEY_PICKED_LON, location.longitude)
                            set(KEY_PICKED_ADDRESS, location.address)
                            set(KEY_PICKED_MUNICIPALITY, location.municipality)
                            set(KEY_PICKED_PROVINCE, location.province)
                        }
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = Screen.PhotoAnalysis.route,
                arguments = listOf(
                    navArgument("plantationId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {
                PhotoAnalysisScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToScheduleTreatment = { plantationId, type, title, description, rawAnalysis ->
                        navController.navigate(
                            Screen.ScheduleTreatment.routeWithPrefill(plantationId, type, title, description, rawAnalysis)
                        )
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToModels = {
                        navController.navigate(Screen.ModelManagement.route)
                    }
                )
            }
            composable(Screen.Calendar.route) {
                CalendarScreen(
                    onNavigateToTreatmentDetail = { id ->
                        navController.navigate(Screen.TreatmentDetail.route(id))
                    }
                )
            }
            composable(Screen.ModelManagement.route) {
                ModelManagementScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.ScheduleTreatment.route +
                    "?prefillType={prefillType}&prefillTitle={prefillTitle}&prefillDesc={prefillDesc}&prefillAnalysis={prefillAnalysis}",
                arguments = listOf(
                    navArgument("plantationId") { type = NavType.StringType },
                    navArgument("prefillType") { type = NavType.StringType; defaultValue = ""; nullable = true },
                    navArgument("prefillTitle") { type = NavType.StringType; defaultValue = ""; nullable = true },
                    navArgument("prefillDesc") { type = NavType.StringType; defaultValue = ""; nullable = true },
                    navArgument("prefillAnalysis") { type = NavType.StringType; defaultValue = ""; nullable = true }
                )
            ) {
                ScheduleTreatmentScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onTreatmentScheduled = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.TreatmentDetail.route,
                arguments = listOf(navArgument("treatmentId") { type = NavType.StringType })
            ) {
                TreatmentDetailScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
