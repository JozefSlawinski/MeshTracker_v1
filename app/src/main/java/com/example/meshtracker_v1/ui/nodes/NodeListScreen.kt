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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.meshtracker_v1.model.MeshNodeInfo
import com.example.meshtracker_v1.ui.map.MapViewModel

@Composable
fun NodeListScreen(
    viewModel: MapViewModel = hiltViewModel(),
    onNodeClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    val nodes by viewModel.nodes.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    val sortedNodes = nodes.values
        .sortedWith(compareBy<MeshNodeInfo> { !it.isOnline() }.thenBy { it.getDisplayName() })

    val isConnecting = connectionState is MapViewModel.ConnectionState.Connecting ||
            connectionState is MapViewModel.ConnectionState.Reconnecting

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        when {
            isConnecting && sortedNodes.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            sortedNodes.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (connectionState is MapViewModel.ConnectionState.Connected) {
                                "Brak węzłów w sieci"
                            } else {
                                "Brak połączenia z Meshtastic"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "Węzły: ${sortedNodes.size}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(sortedNodes, key = { it.getId() }) { node ->
                        NodeItem(
                            node = node,
                            onClick = { onNodeClick(node.getId()) }
                        )
                    }
                }
            }
        }
    }
}
