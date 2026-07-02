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
    modifier: Modifier = Modifier,
    onlineThresholdSeconds: Int = 300
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (node.isOnline(onlineThresholdSeconds)) {
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
                    text = if (node.isOnline(onlineThresholdSeconds)) "● Online" else "○ Offline",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (node.isOnline(onlineThresholdSeconds)) {
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
                    0 -> "Klient (CLIENT)"
                    5 -> "Tracker (TRACKER)"
                    else -> "Rola: $role"
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
            val position = node.position
            if (node.hasValidPosition() && position != null) {
                Text(
                    text = "Pozycja: ${String.format("%.6f", position.latitude)}, ${String.format("%.6f", position.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (position.altitude > 0) {
                    Text(
                        text = "Wysokość: ${position.altitude} m n.p.m.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (position.groundSpeed > 0) {
                    Text(
                        text = "Prędkość: ${position.groundSpeed} m/s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (position.groundTrack > 0) {
                    Text(
                        text = "Kierunek: ${position.groundTrack}°",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Wiek pozycji GPS
                if (position.time > 0) {
                    val ageSeconds = (System.currentTimeMillis() / 1000 - position.time).toInt()
                    val ageText = when {
                        ageSeconds < 0    -> "brak czasu GPS"
                        ageSeconds < 60   -> "${ageSeconds}s temu"
                        ageSeconds < 3600 -> "${ageSeconds / 60} min temu"
                        else              -> "${ageSeconds / 3600} h temu"
                    }
                    Text(
                        text = "Pozycja: $ageText",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (ageSeconds > 600) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Ostrzeżenie o obniżonej precyzji GPS
                if (position.precisionBits in 1..27) {
                    val approxMeters = (Math.pow(2.0, (32 - position.precisionBits).toDouble()) * 1e-7 * 111_000).toInt()
                    Text(
                        text = "⚠ Obniżona precyzja GPS (~$approxMeters m)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Text(
                    text = "Brak pozycji GPS",
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

                val satellites = node.position?.satellitesInView ?: 0
                if (satellites > 0) {
                    Text(
                        text = "🛰 $satellites",
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
                val now = Date()
                val isToday = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).run {
                    format(lastHeardDate) == format(now)
                }
                val dateFormat = if (isToday) {
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                } else {
                    SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
                }
                Text(
                    text = "Ostatni kontakt: ${dateFormat.format(lastHeardDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (node.hopsAway > 0) {
                Text(
                    text = "Przeskoków: ${node.hopsAway}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

