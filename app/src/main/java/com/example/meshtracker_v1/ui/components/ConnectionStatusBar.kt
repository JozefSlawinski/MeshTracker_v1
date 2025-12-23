package com.example.meshtracker_v1.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.meshtracker_v1.ui.map.MapViewModel

/**
 * Pasek statusu wyświetlający stan połączenia z Meshtastic.
 */
@Composable
fun ConnectionStatusBar(
    connectionState: MapViewModel.ConnectionState,
    nodeCount: Int,
    modifier: Modifier = Modifier
) {
    val (statusText, statusColor) = when (connectionState) {
        MapViewModel.ConnectionState.CONNECTED -> {
            "Connected" to MaterialTheme.colorScheme.primary
        }
        MapViewModel.ConnectionState.DISCONNECTED -> {
            "Disconnected" to MaterialTheme.colorScheme.error
        }
        MapViewModel.ConnectionState.CONNECTING -> {
            "Connecting..." to MaterialTheme.colorScheme.secondary
        }
    }
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = statusColor.copy(alpha = 0.1f),
        contentColor = statusColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Wskaźnik statusu (kropka)
                Surface(
                    color = statusColor,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.size(8.dp)
                ) {
                    // Pusta zawartość - tylko kolor
                }
                
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
            }
            
            if (connectionState == MapViewModel.ConnectionState.CONNECTED) {
                Text(
                    text = "$nodeCount node${if (nodeCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

