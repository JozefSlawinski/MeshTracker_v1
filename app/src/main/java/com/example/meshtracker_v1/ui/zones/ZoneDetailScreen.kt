package com.example.meshtracker_v1.ui.zones

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.meshtracker_v1.model.ZoneEvent
import com.example.meshtracker_v1.model.ZoneEventType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ekran szczegółów strefy geofencingu — karta informacyjna + log ENTER/EXIT.
 *
 * @param zoneId    identyfikator strefy (UUID)
 * @param onBack    nawigacja wstecz (do mapy)
 * @param modifier  opcjonalny modifier zewnętrzny
 * @param viewModel hiltViewModel — wstrzyknięty automatycznie
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneDetailScreen(
    zoneId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ZoneViewModel = hiltViewModel()
) {
    val allZones by viewModel.allZones.collectAsState()
    val zone = allZones.find { it.id == zoneId }

    val events by viewModel.getEventsForZone(zoneId).collectAsState(initial = emptyList())
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(zone?.name ?: "Strefa") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Wróć")
                    }
                },
                actions = {
                    if (events.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Wyczyść log zdarzeń",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->

        // ---- Strefa nie istnieje ----
        if (zone == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Strefa nie istnieje lub została usunięta.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        // ---- Treść ----
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Karta informacji o strefie
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Nagłówek karty: kolor + nazwa + toggle chip
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(zone.colorArgb))
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = zone.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            AssistChip(
                                onClick = { viewModel.toggleActive(zone) },
                                label = {
                                    Text(if (zone.isActive) "Aktywna" else "Nieaktywna")
                                }
                            )
                        }

                        HorizontalDivider()

                        InfoRow("Wierzchołki", zone.vertices().size.toString())
                        InfoRow(
                            label = "Monitorowane węzły",
                            value = zone.watchedNodeIds().let { ids ->
                                if (ids.isEmpty()) "—" else ids.joinToString(", ")
                            }
                        )
                        InfoRow("Zdarzeń w logu", events.size.toString())
                    }
                }
            }

            // Nagłówek logu
            item {
                Text(
                    text = "Log zdarzeń (${events.size})",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Brak zdarzeń
            if (events.isEmpty()) {
                item {
                    Text(
                        text = "Brak zdarzeń. Log zostanie uzupełniony gdy monitorowany " +
                                "węzeł wejdzie lub opuści strefę.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Wiersze zdarzeń (najnowsze pierwsze — Room sortuje po id DESC)
            items(events, key = { it.id }) { event ->
                ZoneEventRow(event)
            }
        }
    }

    // ---- Dialog potwierdzenia czyszczenia logu ----
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Wyczyść log") },
            text = { Text("Usunąć wszystkie zdarzenia dla strefy '${zone?.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearEvents(zoneId)
                        showClearDialog = false
                    }
                ) { Text("Usuń") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Anuluj") }
            }
        )
    }
}

// ---- Wiersz informacyjny (label + wartość) ----

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

// ---- Wiersz zdarzenia ENTER/EXIT ----

@Composable
private fun ZoneEventRow(event: ZoneEvent) {
    val isEnter = event.eventType == ZoneEventType.ENTER.name
    val containerColor = if (isEnter)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.errorContainer

    val label = if (isEnter) "▶ WEJŚCIE" else "◀ WYJŚCIE"

    val dateStr = remember(event.timestampSeconds) {
        SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault())
            .format(Date(event.timestampSeconds * 1000L))
    }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.width(80.dp)
            )
            Text(
                text = event.nodeName,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = dateStr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
