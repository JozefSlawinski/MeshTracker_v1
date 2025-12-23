package com.example.meshtracker_v1.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.example.meshtracker_v1.model.MeshNodeInfo

/**
 * Ekran mapy wyświetlający węzły Meshtastic.
 */
@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val nodes by viewModel.nodes.collectAsState()
    val selectedNodeId by viewModel.selectedNodeId.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Sprawdź uprawnienia do lokalizacji
    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    
    // Stan kamery (domyślnie centrum Polski)
    val defaultLocation = LatLng(52.0, 19.0)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 6f)
    }
    
    // Filtruj węzły z prawidłową pozycją
    val nodesWithPosition = nodes.values.filter { it.hasValidPosition() }
    
    // Debug: loguj informacje o węzłach
    androidx.compose.runtime.LaunchedEffect(nodes.size, nodesWithPosition.size) {
        android.util.Log.d("MapScreen", "Total nodes: ${nodes.size}, nodes with position: ${nodesWithPosition.size}")
        nodes.values.forEach { node ->
            android.util.Log.d("MapScreen", "Node: ${node.getDisplayName()}, hasPosition: ${node.hasValidPosition()}")
        }
    }
    
    // Aktualizuj kamerę gdy wybrano węzeł
    LaunchedEffect(selectedNodeId) {
        selectedNodeId?.let { nodeId ->
            val node = nodes[nodeId]
            node?.position?.let { position ->
                val latLng = position.toLatLng()
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                )
            }
        }
    }
    
    // Wyświetl komunikat o stanie połączenia
    LaunchedEffect(connectionState) {
        when (connectionState) {
            MapViewModel.ConnectionState.CONNECTED -> {
                snackbarHostState.showSnackbar("Connected to Meshtastic")
            }
            MapViewModel.ConnectionState.DISCONNECTED -> {
                snackbarHostState.showSnackbar("Disconnected from Meshtastic")
            }
            MapViewModel.ConnectionState.CONNECTING -> {
                // Nie wyświetlaj komunikatu dla connecting
            }
        }
    }
    
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = { viewModel.selectNode(null) }
            ) {
                // Wyświetl markery dla wszystkich węzłów
                nodesWithPosition.forEach { node ->
                    val position = node.position!!
                    val isSelected = node.getId() == selectedNodeId
                    
                    Marker(
                        state = MarkerState(position = position.toLatLng()),
                        title = node.getDisplayName(),
                        snippet = buildMarkerSnippet(node),
                        onClick = {
                            viewModel.selectNode(node.getId())
                            true
                        }
                    )
                }
            }
            
            // Wyświetl informacje gdy brak węzłów z pozycją
            if (nodesWithPosition.isEmpty() && connectionState == MapViewModel.ConnectionState.CONNECTED) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (nodes.isEmpty()) {
                                "No nodes found"
                            } else {
                                "No nodes with position found"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (nodes.isNotEmpty()) {
                            Text(
                                text = "Total nodes: ${nodes.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Check the Nodes tab to see all nodes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Wyświetl wskaźnik połączenia
            if (connectionState == MapViewModel.ConnectionState.CONNECTING) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

/**
 * Buduje snippet dla markera.
 */
private fun buildMarkerSnippet(node: MeshNodeInfo): String {
    val parts = mutableListOf<String>()
    
    if (node.isOnline()) {
        parts.add("Online")
    } else {
        parts.add("Offline")
    }
    
    if (node.batteryLevel > 0) {
        parts.add("Battery: ${node.batteryLevel}%")
    }
    
    if (node.snr != Float.MAX_VALUE) {
        parts.add("SNR: ${String.format("%.1f", node.snr)} dB")
    }
    
    return parts.joinToString(" • ")
}

