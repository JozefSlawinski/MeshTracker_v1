package com.example.meshtracker_v1.ui.nodes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.meshtracker_v1.ui.map.MapViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NodeListScreen(
    viewModel: MapViewModel = hiltViewModel(),
    onNodeClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    val filteredNodes by viewModel.filteredNodes.collectAsState()
    val totalNodes by viewModel.nodes.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val onlineThresholdSeconds by viewModel.onlineThresholdSeconds.collectAsState()
    val filterState by viewModel.filterState.collectAsState()

    val isConnecting = connectionState is MapViewModel.ConnectionState.Connecting ||
            connectionState is MapViewModel.ConnectionState.Reconnecting

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        when {
            isConnecting && totalNodes.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {

                    // -------------------------------------------------------- SearchBar
                    OutlinedTextField(
                        value = filterState.searchQuery,
                        onValueChange = { query ->
                            viewModel.updateFilter(filterState.copy(searchQuery = query))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        placeholder = { Text("Szukaj węzła…") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (filterState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    viewModel.updateFilter(filterState.copy(searchQuery = ""))
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Wyczyść")
                                }
                            }
                        },
                        singleLine = true
                    )

                    // -------------------------------------------------------- Chipy filtrów
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filterState.showOnlineOnly,
                            onClick = {
                                viewModel.updateFilter(
                                    filterState.copy(showOnlineOnly = !filterState.showOnlineOnly)
                                )
                            },
                            label = { Text("Tylko online") }
                        )
                        FilterChip(
                            selected = filterState.showWithGpsOnly,
                            onClick = {
                                viewModel.updateFilter(
                                    filterState.copy(showWithGpsOnly = !filterState.showWithGpsOnly)
                                )
                            },
                            label = { Text("Ma GPS") }
                        )
                        if (filterState.isActive) {
                            TextButton(onClick = { viewModel.clearFilter() }) {
                                Text("Wyczyść filtry")
                            }
                        }
                    }

                    // -------------------------------------------------------- Licznik wyników
                    val totalCount = totalNodes.size
                    val filteredCount = filteredNodes.size
                    val counterText = if (filterState.isActive) {
                        "Wyniki: $filteredCount / $totalCount węzłów"
                    } else {
                        "Węzły: $totalCount"
                    }
                    Text(
                        text = counterText,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    // -------------------------------------------------------- Lista
                    when {
                        filteredNodes.isEmpty() && filterState.isActive -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Brak węzłów spełniających kryteria",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    TextButton(onClick = { viewModel.clearFilter() }) {
                                        Text("Wyczyść filtry")
                                    }
                                }
                            }
                        }

                        filteredNodes.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
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

                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredNodes, key = { it.getId() }) { node ->
                                    NodeItem(
                                        node = node,
                                        onlineThresholdSeconds = onlineThresholdSeconds,
                                        onClick = { onNodeClick(node.getId()) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
