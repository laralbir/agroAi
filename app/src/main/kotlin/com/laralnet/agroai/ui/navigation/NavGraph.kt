package com.laralnet.agroai.ui.navigation

import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import com.laralnet.agroai.plantation.domain.model.Location
import com.laralnet.agroai.ui.screens.home.HomeScreen
import com.laralnet.agroai.ui.screens.plantation.detail.PlantationDetailScreen
import com.laralnet.agroai.ui.screens.plantation.list.PlantationListScreen
import com.laralnet.agroai.ui.screens.plantation.wizard.LocationPickerScreen
import com.laralnet.agroai.ui.screens.plantation.wizard.PlantationWizardScreen
import com.laralnet.agroai.ui.screens.plantation.wizard.PlantationWizardViewModel
import com.laralnet.agroai.ui.screens.settings.SettingsScreen

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
    data object PhotoAnalysis : Screen("analysis")
    data object Calendar : Screen("calendar")
    data object Settings : Screen("settings")
    data object ModelManagement : Screen("models")
}

private const val KEY_PICKED_LAT = "picked_lat"
private const val KEY_PICKED_LON = "picked_lon"
private const val KEY_PICKED_ADDRESS = "picked_address"
private const val KEY_PICKED_MUNICIPALITY = "picked_municipality"
private const val KEY_PICKED_PROVINCE = "picked_province"

private data class BottomNavItem(
    val screen: Screen,
    val labelRes: Int,
    val icon: @Composable () -> Unit
)

@Composable
fun AgroAINavGraph(
    navController: NavHostController = rememberNavController()
) {
    val bottomItems = listOf(
        BottomNavItem(Screen.Home, R.string.nav_home) {
            Icon(Icons.Default.Home, contentDescription = null)
        },
        BottomNavItem(Screen.Plantations, R.string.nav_plantations) {
            Icon(Icons.Default.Agriculture, contentDescription = null)
        },
        BottomNavItem(Screen.PhotoAnalysis, R.string.nav_analysis) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
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
                                navController.navigate(item.screen.route) {
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
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
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
                        navController.navigate(Screen.PhotoAnalysis.route)
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
                    onPlantationCreated = { id ->
                        navController.popBackStack()
                        navController.navigate(Screen.PlantationDetail.route(id))
                    },
                    onOpenMapPicker = { navController.navigate(Screen.LocationPicker.route) },
                    viewModel = wizardViewModel
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
            composable(Screen.PhotoAnalysis.route) {
                PhotoAnalysisScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToModels = {
                        navController.navigate(Screen.ModelManagement.route)
                    }
                )
            }
            composable(Screen.ModelManagement.route) {
                ModelManagementScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
