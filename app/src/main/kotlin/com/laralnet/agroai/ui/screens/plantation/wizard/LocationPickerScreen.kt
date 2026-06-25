package com.laralnet.agroai.ui.screens.plantation.wizard

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.laralnet.agroai.R
import com.laralnet.agroai.plantation.domain.model.Location
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

// Default center: center of Spain
private val SPAIN_CENTER = GeoPoint(40.4168, -3.7038)
private const val DEFAULT_ZOOM = 6.0
private const val DETAIL_ZOOM = 15.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    onNavigateBack: () -> Unit,
    onLocationConfirmed: (Location) -> Unit,
    viewModel: LocationPickerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onUseGps()
    }

    // OSM attribution required by tile usage policy
    val osmAttribution = "© OpenStreetMap contributors"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.location_picker_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    state.resolvedLocation?.let {
                        IconButton(onClick = { onLocationConfirmed(it) }) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = stringResource(R.string.btn_confirm),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Map — fills entire space
            OsmdroidMapView(
                modifier = Modifier.fillMaxSize(),
                markerPosition = state.markerPosition,
                onMapTap = viewModel::onMapTap
            )

            // OSM attribution (required)
            Text(
                text = osmAttribution,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )

            // Search + GPS overlay (top)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Search bar
                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    isSearching = state.isSearching,
                    onGpsClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) viewModel.onUseGps()
                        else locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                    isGpsLoading = state.isGpsLoading
                )

                // Search results dropdown
                AnimatedVisibility(visible = state.searchResults.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        LazyColumn {
                            items(state.searchResults) { place ->
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            place.address.resolvedMunicipality.ifBlank { place.displayName },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            place.displayName,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    },
                                    modifier = Modifier.clickable { viewModel.onPlaceSelected(place) }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }

            // Resolved location chip (bottom)
            state.resolvedLocation?.let { loc ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = loc.municipality.ifBlank { loc.address }.ifBlank { stringResource(R.string.location_selected) },
                                style = MaterialTheme.typography.titleSmall
                            )
                            if (loc.province.isNotBlank()) {
                                Text(
                                    text = loc.province,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            loc.latitude?.let { lat ->
                                loc.longitude?.let { lon ->
                                    Text(
                                        text = "%.5f, %.5f".format(lat, lon),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                        FilledTonalButton(onClick = { onLocationConfirmed(loc) }) {
                            Text(stringResource(R.string.btn_confirm))
                        }
                    }
                }
            }

            // Error snackbar
            state.error?.let { error ->
                LaunchedEffect(error) {
                    // auto-clear after 3s
                    kotlinx.coroutines.delay(3_000)
                    viewModel.clearError()
                }
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = viewModel::clearError) { Text("OK") }
                    }
                ) { Text(error) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isSearching: Boolean,
    onGpsClick: () -> Unit,
    isGpsLoading: Boolean
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 6.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.padding(start = 12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.location_search_hint)) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                modifier = Modifier.weight(1f),
                trailingIcon = if (isSearching) {
                    { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
                } else null
            )
            IconButton(onClick = onGpsClick, enabled = !isGpsLoading) {
                if (isGpsLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        Icons.Default.GpsFixed,
                        contentDescription = stringResource(R.string.location_use_my_location),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun OsmdroidMapView(
    modifier: Modifier = Modifier,
    markerPosition: GeoPoint?,
    onMapTap: (GeoPoint) -> Unit
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val mapView = rememberMapView()

    // Tap overlay
    DisposableEffect(mapView) {
        val overlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                onMapTap(p)
                return true
            }
            override fun longPressHelper(p: GeoPoint) = false
        })
        mapView.overlays.add(0, overlay)
        onDispose { mapView.overlays.remove(overlay) }
    }

    // Marker — recomposed when markerPosition changes
    DisposableEffect(markerPosition) {
        mapView.overlays.removeAll(mapView.overlays.filterIsInstance<Marker>().toSet())
        markerPosition?.let { pos ->
            Marker(mapView).apply {
                position = pos
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                mapView.overlays.add(this)
                mapView.controller.animateTo(pos, DETAIL_ZOOM, 800L)
            }
        }
        mapView.invalidate()
        onDispose { }
    }

    // Lifecycle hooks required by osmdroid
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}

@Composable
private fun rememberMapView(): MapView {
    val context = LocalContext.current
    return remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            controller.setZoom(DEFAULT_ZOOM)
            controller.setCenter(SPAIN_CENTER)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
        }
    }
}
