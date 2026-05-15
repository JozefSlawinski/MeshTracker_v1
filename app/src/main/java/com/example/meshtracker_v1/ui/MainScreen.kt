package com.example.meshtracker_v1.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.meshtracker_v1.ui.components.ConnectionStatusBar
import com.example.meshtracker_v1.ui.map.MapScreen
import com.example.meshtracker_v1.ui.map.MapViewModel
import com.example.meshtracker_v1.ui.nodes.NodeDetailScreen
import com.example.meshtracker_v1.ui.nodes.NodeListScreen
import com.example.meshtracker_v1.ui.settings.SettingsScreen
import com.example.meshtracker_v1.ui.zones.ZoneDetailScreen
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * Główny ekran aplikacji — zarządza nawigacją między ekranami.
 *
 * Ekrany bez dolnej belki nawigacyjnej:
 *  - [Screen.NodeDetail] — szczegóły węzła
 *  - [Screen.ZoneDetail] — szczegóły strefy geofencingu
 */
@Composable
fun MainScreen(
    viewModel: MapViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Map) }
    val connectionState by viewModel.connectionState.collectAsState()
    val nodes by viewModel.nodes.collectAsState()

    // Kamera mapy żyje w MainScreen — przeżywa przełączanie zakładek
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(52.0, 19.0), 6f)
    }

    /** True gdy bieżący ekran to ekran szczegółów (bez nav bar i status bar). */
    val isDetailScreen = currentScreen is Screen.NodeDetail || currentScreen is Screen.ZoneDetail

    Scaffold(
        modifier = modifier,
        topBar = {
            if (currentScreen != Screen.Settings && !isDetailScreen) {
                ConnectionStatusBar(
                    connectionState = connectionState,
                    nodeCount = nodes.size,
                    onRetry = viewModel::retryConnect
                )
            }
        },
        bottomBar = {
            if (!isDetailScreen) {
                NavigationBar {
                    NavigationBarItem(
                        icon     = { Icon(Icons.Default.Place, contentDescription = "Mapa") },
                        label    = { Text("Mapa") },
                        selected = currentScreen == Screen.Map,
                        onClick  = { currentScreen = Screen.Map }
                    )
                    NavigationBarItem(
                        icon     = { Icon(Icons.Default.List, contentDescription = "Węzły") },
                        label    = { Text("Węzły") },
                        selected = currentScreen == Screen.List,
                        onClick  = { currentScreen = Screen.List }
                    )
                    NavigationBarItem(
                        icon     = { Icon(Icons.Default.Settings, contentDescription = "Ustawienia") },
                        label    = { Text("Ustawienia") },
                        selected = currentScreen == Screen.Settings,
                        onClick  = { currentScreen = Screen.Settings }
                    )
                }
            }
        }
    ) { paddingValues ->
        when (currentScreen) {

            // ---- Mapa ----
            Screen.Map -> MapScreen(
                viewModel  = viewModel,
                cameraPositionState = cameraPositionState,
                onNavigateToZoneDetail = { zoneId ->
                    currentScreen = Screen.ZoneDetail(zoneId)
                },
                modifier      = Modifier.fillMaxSize(),
                contentPadding = paddingValues
            )

            // ---- Lista węzłów ----
            Screen.List -> NodeListScreen(
                viewModel = viewModel,
                onNodeClick = { nodeId ->
                    currentScreen = Screen.NodeDetail(nodeId)
                },
                modifier       = Modifier.fillMaxSize(),
                contentPadding = paddingValues
            )

            // ---- Szczegóły węzła ----
            is Screen.NodeDetail -> {
                val screen = currentScreen as Screen.NodeDetail
                NodeDetailScreen(
                    nodeId     = screen.nodeId,
                    onBack     = { currentScreen = Screen.List },
                    onShowOnMap = { nodeId ->
                        currentScreen = Screen.Map
                        viewModel.selectNode(nodeId)
                    },
                    viewModel      = viewModel,
                    modifier       = Modifier.fillMaxSize(),
                    contentPadding = paddingValues
                )
            }

            // ---- Szczegóły strefy ----
            is Screen.ZoneDetail -> {
                val screen = currentScreen as Screen.ZoneDetail
                ZoneDetailScreen(
                    zoneId   = screen.zoneId,
                    onBack   = { currentScreen = Screen.Map },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // ---- Ustawienia ----
            Screen.Settings -> SettingsScreen(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = paddingValues
            )
        }
    }
}

// ------------------------------------------------------------------ ekrany nawigacji

sealed class Screen {
    object Map      : Screen()
    object List     : Screen()
    object Settings : Screen()
    data class NodeDetail(val nodeId: String) : Screen()
    data class ZoneDetail(val zoneId: String) : Screen()
}
