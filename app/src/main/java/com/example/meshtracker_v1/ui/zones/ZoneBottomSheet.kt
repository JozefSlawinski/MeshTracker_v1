package com.example.meshtracker_v1.ui.zones

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.meshtracker_v1.model.Zone

/**
 * ModalBottomSheet z listą zdefiniowanych stref geofencingu.
 * Każdy wiersz pozwala włączyć/wyłączyć strefę, usunąć ją lub przejść do jej szczegółów.
 * Przycisk „Dodaj strefę" zamyka sheet i startuje tryb rysowania.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneBottomSheet(
    zones: List<Zone>,
    onDismiss: () -> Unit,
    onStartDrawing: () -> Unit,
    onToggleActive: (Zone) -> Unit,
    onDeleteZone: (String) -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ---- Nagłówek ----
            Text(
                text = "Strefy geofencingu",
                style = MaterialTheme.typography.titleMedium
            )

            HorizontalDivider()

            // ---- Lista stref ----
            if (zones.isEmpty()) {
                Text(
                    text = "Brak zdefiniowanych stref.\n" +
                            "Naciśnij 'Dodaj strefę' i narysuj wielokąt long-pressem na mapie.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(zones, key = { it.id }) { zone ->
                        ZoneListItem(
                            zone = zone,
                            onToggle = { onToggleActive(zone) },
                            onDelete = { onDeleteZone(zone.id) },
                            onDetail = { onNavigateToDetail(zone.id) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ---- Przycisk Dodaj ----
            Button(
                onClick = {
                    onDismiss()
                    onStartDrawing()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Dodaj strefę")
            }
        }
    }
}

// ---- Wiersz strefy ----

@Composable
private fun ZoneListItem(
    zone: Zone,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onDetail: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Kolor strefy
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(zone.colorArgb))
            )
            Spacer(Modifier.width(12.dp))

            // Nazwa + info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = zone.name,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = buildString {
                        append("${zone.vertices().size} wierzchołków")
                        val nodeCount = zone.watchedNodeIds().size
                        if (nodeCount > 0) append(" • $nodeCount węzłów")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Toggle aktywności
            Switch(
                checked = zone.isActive,
                onCheckedChange = { onToggle() }
            )

            // Usuń
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Usuń strefę",
                    tint = MaterialTheme.colorScheme.error
                )
            }

            // Szczegóły
            IconButton(onClick = onDetail) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = "Szczegóły strefy"
                )
            }
        }
    }
}
