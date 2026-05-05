package com.example.meshtracker_v1.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.meshtracker_v1.ui.components.ConnectionStatusBar
import com.example.meshtracker_v1.ui.map.MapScreen
import com.example.meshtracker_v1.ui.map.MapViewModel
import com.example.meshtracker_v1.ui.nodes.NodeListScreen

/**
 * Główny ekran aplikacji z nawigacją między mapą a listą węzłów.
 */
@Composable
fun MainScreen(
    viewModel: MapViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(Screen.Map) }
    val connectionState by viewModel.connectionState.collectAsState()
    val nodes by viewModel.nodes.collectAsState()
    val nodeCount = nodes.size
    
    Scaffold(
        modifier = modifier,
        topBar = {
            ConnectionStatusBar(
                connectionState = connectionState,
                nodeCount = nodeCount,
                onRetry = viewModel::retryConnect
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Place, contentDescription = "Map") },
                    label = { Text("Map") },
                    selected = currentScreen == Screen.Map,
                    onClick = { currentScreen = Screen.Map }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Nodes") },
                    label = { Text("Nodes") },
                    selected = currentScreen == Screen.List,
                    onClick = { currentScreen = Screen.List }
                )
            }
        }
    ) { paddingValues ->
        when (currentScreen) {
            Screen.Map -> {
                MapScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Screen.List -> {
                NodeListScreen(
                    viewModel = viewModel,
                    onNodeClick = { nodeId ->
                        // Przełącz na mapę i zaznacz węzeł
                        currentScreen = Screen.Map
                        viewModel.selectNode(nodeId)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Dostępne ekrany w aplikacji.
 */
private enum class Screen {
    Map,
    List
}

