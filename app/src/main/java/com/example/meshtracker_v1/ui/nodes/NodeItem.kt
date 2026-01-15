package com.example.meshtracker_v1.ui.nodes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.meshtracker_v1.model.MeshNodeInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Element listy wyświetlający informacje o węźle.
 */
@Composable
fun NodeItem(
    node: MeshNodeInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (node.isOnline()) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Nazwa węzła i status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = node.getDisplayName(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Status online/offline
                Text(
                    text = if (node.isOnline()) "● Online" else "○ Offline",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (node.isOnline()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            // ID węzła
            Text(
                text = "ID: ${node.getId()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Rola węzła (jeśli dostępna)
            node.user?.role?.let { role ->
                val roleName = when (role) {
                    0 -> "CLIENT"
                    5 -> "TRACKER"
                    else -> "Role: $role"
                }
                Text(
                    text = roleName,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (role) {
                        0 -> MaterialTheme.colorScheme.error // CLIENT - czerwony
                        5 -> MaterialTheme.colorScheme.primary // TRACKER - zielony/primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            // Pozycja (jeśli dostępna)
            if (node.hasValidPosition()) {
                val position = node.position!!
                Text(
                    text = "Position: ${String.format("%.6f", position.latitude)}, ${String.format("%.6f", position.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (position.altitude > 0) {
                    Text(
                        text = "Altitude: ${position.altitude} m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "No position available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            // Informacje o sygnale i baterii
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (node.batteryLevel > 0) {
                    Text(
                        text = "🔋 ${node.batteryLevel}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                if (node.snr != Float.MAX_VALUE) {
                    Text(
                        text = "📡 SNR: ${String.format("%.1f", node.snr)} dB",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                if (node.rssi != Int.MAX_VALUE) {
                    Text(
                        text = "📶 RSSI: ${node.rssi} dBm",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // Ostatnio słyszane
            if (node.lastHeard > 0) {
                val lastHeardDate = Date(node.lastHeard * 1000L)
                val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                Text(
                    text = "Last heard: ${dateFormat.format(lastHeardDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Hops away
            if (node.hopsAway > 0) {
                Text(
                    text = "Hops away: ${node.hopsAway}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

