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
import com.example.meshtracker_v1.ui.nodes.NodeListScreen
import com.example.meshtracker_v1.ui.settings.SettingsScreen

@Composable
fun MainScreen(
    viewModel: MapViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Map) }
    val connectionState by viewModel.connectionState.collectAsState()
    val nodes by viewModel.nodes.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            if (currentScreen != Screen.Settings) {
                ConnectionStatusBar(
                    connectionState = connectionState,
                    nodeCount = nodes.size,
                    onRetry = viewModel::retryConnect
                )
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Place, contentDescription = "Mapa") },
                    label = { Text("Mapa") },
                    selected = currentScreen == Screen.Map,
                    onClick = { currentScreen = Screen.Map }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Węzły") },
                    label = { Text("Węzły") },
                    selected = currentScreen == Screen.List,
                    onClick = { currentScreen = Screen.List }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ustawienia") },
                    label = { Text("Ustawienia") },
                    selected = currentScreen == Screen.Settings,
                    onClick = { currentScreen = Screen.Settings }
                )
            }
        }
    ) { paddingValues ->
        when (currentScreen) {
            Screen.Map -> MapScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize(),
                contentPadding = paddingValues
            )
            Screen.List -> NodeListScreen(
                viewModel = viewModel,
                onNodeClick = { nodeId ->
                    currentScreen = Screen.Map
                    viewModel.selectNode(nodeId)
                },
                modifier = Modifier.fillMaxSize(),
                contentPadding = paddingValues
            )
            Screen.Settings -> SettingsScreen(
                modifier = Modifier.fillMaxSize(),
                contentPadding = paddingValues
            )
        }
    }
}

sealed class Screen {
    object Map : Screen()
    object List : Screen()
    object Settings : Screen()
}
