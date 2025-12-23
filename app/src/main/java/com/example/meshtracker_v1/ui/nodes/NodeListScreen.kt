package com.example.meshtracker_v1.ui.nodes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.meshtracker_v1.model.MeshNodeInfo
import com.example.meshtracker_v1.ui.map.MapViewModel

/**
 * Ekran listy węzłów Meshtastic.
 */
@Composable
fun NodeListScreen(
    viewModel: MapViewModel = viewModel(),
    onNodeClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val nodes by viewModel.nodes.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Filtruj węzły - pokaż wszystkie, nie tylko z pozycją
    val allNodes = nodes.values.toList()
    
    // Sortuj węzły: najpierw online, potem offline, potem po nazwie
    val sortedNodes = allNodes.sortedWith(
        compareBy<MeshNodeInfo> { !it.isOnline() }
            .thenBy { it.getDisplayName() }
    )
    
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
            when {
                connectionState == MapViewModel.ConnectionState.CONNECTING -> {
                    // Wyświetl wskaźnik ładowania
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                sortedNodes.isEmpty() -> {
                    // Brak węzłów
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (connectionState == MapViewModel.ConnectionState.CONNECTED) {
                                    "No nodes found"
                                } else {
                                    "Not connected to Meshtastic"
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                
                else -> {
                    // Lista węzłów
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "Nodes: ${sortedNodes.size}",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        
                        items(sortedNodes, key = { it.getId() }) { node ->
                            NodeItem(
                                node = node,
                                onClick = {
                                    onNodeClick(node.getId())
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

