package com.example.meshtracker_v1.ui.nodes

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.meshtracker_v1.model.MeshNodeInfo
import com.example.meshtracker_v1.model.PacketStats
import com.example.meshtracker_v1.model.TimedPosition
import com.example.meshtracker_v1.ui.map.MapViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeDetailScreen(
    nodeId: String,
    onBack: () -> Unit,
    onShowOnMap: (String) -> Unit,
    viewModel: MapViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    val nodes by viewModel.nodes.collectAsState()
    val onlineThresholdSeconds by viewModel.onlineThresholdSeconds.collectAsState()
    val nodeHistoryMap by viewModel.nodeHistory.collectAsState()
    val packetStatsMap by viewModel.packetStats.collectAsState()
    val expectedInterval by viewModel.expectedBroadcastInterval.collectAsState()
    val node = nodes[nodeId]
    val history = nodeHistoryMap[nodeId] ?: emptyList()
    val stats = packetStatsMap[nodeId]

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.exportEvent.collect { result ->
            result.onSuccess { uri ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Eksportuj dane węzła"))
            }
        }
    }

    Scaffold(
        modifier = modifier.padding(contentPadding),
        topBar = {
            TopAppBar(
                title = { Text(node?.getDisplayName() ?: nodeId) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Wstecz")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (node == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Węzeł nie znaleziony",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            NodeDetailContent(
                node = node,
                history = history,
                stats = stats,
                expectedInterval = expectedInterval,
                onlineThresholdSeconds = onlineThresholdSeconds,
                onShowOnMap = { onShowOnMap(nodeId) },
                onResetStats = { viewModel.resetStatsForNode(nodeId) },
                onExport = { viewModel.exportNodeData(nodeId) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

@Composable
private fun NodeDetailContent(
    node: MeshNodeInfo,
    history: List<TimedPosition>,
    stats: PacketStats?,
    expectedInterval: Int,
    onlineThresholdSeconds: Int,
    onShowOnMap: () -> Unit,
    onResetStats: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isOnline = node.isOnline(onlineThresholdSeconds)
    var showResetStatsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---- Header card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isOnline)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = node.getDisplayName(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (node.user?.shortName?.isNotEmpty() == true &&
                            node.user.shortName != node.user.longName) {
                            Text(
                                text = node.user.shortName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Surface(
                        color = if (isOnline) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = if (isOnline) "ONLINE" else "OFFLINE",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isOnline) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.surface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "ID: ${node.getId()}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---- Identity section
        DetailSection(title = "Tożsamość") {
            node.user?.let { user ->
                DetailRow("Rola", roleLabel(user.role))
                DetailRow("Sprzęt", user.getHardwareModel())
                if (user.isLicensed) {
                    DetailRow("Licencja HAM", "Tak")
                }
            }
            DetailRow("Kanał", node.channel.toString())
            DetailRow("Przeskoki", if (node.hopsAway == 0) "Bezpośrednio" else "${node.hopsAway} przeskoki")
        }

        // ---- Signal section
        DetailSection(title = "Sygnał") {
            if (node.snr != Float.MAX_VALUE) {
                val snrLabel = snrQualityLabel(node.snr)
                DetailRow("SNR", "${String.format("%.1f", node.snr)} dB  ($snrLabel)")
            } else {
                DetailRow("SNR", "— (brak pomiaru RF)")
            }
            if (node.rssi != Int.MAX_VALUE) {
                DetailRow("RSSI", "${node.rssi} dBm")
            } else {
                DetailRow("RSSI", "—")
            }
            if (node.batteryLevel > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DetailRow("Bateria", "${node.batteryLevel}%")
                    LinearProgressIndicator(
                        progress = { node.batteryLevel / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = when {
                            node.batteryLevel >= 60 -> MaterialTheme.colorScheme.primary
                            node.batteryLevel >= 25 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }

        // ---- Packet stats section
        DetailSection(title = "Statystyki odbioru") {
            if (stats == null || stats.receivedCount == 0) {
                Text(
                    text = "Brak danych — oczekiwanie na pakiety",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                DetailRow("Odebrano pakietów", stats.receivedCount.toString())
                if (stats.receivedCount < 5) {
                    DetailRow("Avg Δt", "Za mało danych")
                    DetailRow("PDR (est.)", "Za mało danych")
                } else {
                    DetailRow("Avg Δt", "${String.format("%.1f", stats.avgDeltaT)} s")
                    DetailRow("Min / Max Δt", "${stats.minDeltaT} s / ${stats.maxDeltaT} s")
                    val pdr = stats.pdr(expectedInterval)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        DetailRow("PDR (est.)", "${String.format("%.0f", pdr)}%")
                        LinearProgressIndicator(
                            progress = { (pdr / 100.0).toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                            color = when {
                                pdr >= 90.0 -> MaterialTheme.colorScheme.primary
                                pdr >= 70.0 -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
                DetailRow("Oczekiwany interwał", "${expectedInterval} s (z ustawień)")
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { showResetStatsDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Resetuj statystyki")
                }
            }
        }

        // ---- Position section
        if (node.hasValidPosition()) {
            val pos = node.position!!
            DetailSection(title = "Pozycja GPS") {
                DetailRow("Szerokość", String.format("%.6f°", pos.latitude))
                DetailRow("Długość", String.format("%.6f°", pos.longitude))
                if (pos.altitude != 0) {
                    DetailRow("Wysokość", "${pos.altitude} m n.p.m.")
                }
                if (pos.satellitesInView > 0) {
                    DetailRow("Satelity", pos.satellitesInView.toString())
                }
                if (pos.groundSpeed > 0) {
                    DetailRow("Prędkość", "${pos.groundSpeed} m/s")
                }
                if (pos.groundTrack > 0) {
                    DetailRow("Kierunek", "${pos.groundTrack}° ${headingToCardinal(pos.groundTrack)}")
                }
                if (pos.precisionBits in 1..27) {
                    val meters = precisionToMeters(pos.precisionBits)
                    DetailRow("Dokładność", "~$meters m")
                }
                if (pos.time > 0) {
                    val ageSeconds = (System.currentTimeMillis() / 1000 - pos.time).toInt()
                    DetailRow("Wiek GPS", formatAge(ageSeconds))
                }
            }
        }

        // ---- Last heard section
        if (node.lastHeard > 0) {
            DetailSection(title = "Ostatni kontakt") {
                val lastHeardMs = node.lastHeard.toLong() * 1000
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date(lastHeardMs))
                val ageSeconds = (System.currentTimeMillis() / 1000 - node.lastHeard).toInt()
                DetailRow("Czas", dateStr)
                DetailRow("Temu", formatAge(ageSeconds))
            }
        }

        // ---- Position history section
        if (history.isNotEmpty()) {
            val totalDistKm = trackDistanceKm(history)
            val distLabel = if (totalDistKm >= 0.01) " — dystans: ~${String.format("%.2f", totalDistKm)} km" else ""
            DetailSection(title = "Historia pozycji (${history.size})$distLabel") {
                val dateFormat = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
                history.reversed().take(10).forEachIndexed { index, pos ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 3.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                    val dateStr = dateFormat.format(Date(pos.timestampSeconds.toLong() * 1000))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.42f)
                        )
                        Text(
                            text = "${String.format("%.5f", pos.latitude)}, ${String.format("%.5f", pos.longitude)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(0.58f)
                        )
                    }
                }
                if (history.size > 10) {
                    Text(
                        text = "… i ${history.size - 10} wcześniejszych",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // ---- Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (node.hasValidPosition()) {
                Button(
                    onClick = onShowOnMap,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Place, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pokaż na mapie")
                }
            }

            FilledTonalButton(
                onClick = {
                    val pos = node.position
                    val text = buildString {
                        append("Węzeł: ${node.getDisplayName()}\n")
                        append("ID: ${node.getId()}\n")
                        if (pos != null) {
                            append("Pozycja: ${String.format("%.6f", pos.latitude)}, ${String.format("%.6f", pos.longitude)}\n")
                            append("https://maps.google.com/?q=${pos.latitude},${pos.longitude}\n")
                        }
                        append("Stan: ${if (node.isOnline()) "Online" else "Offline"}\n")
                    }
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(intent, "Udostępnij węzeł"))
                },
                modifier = if (node.hasValidPosition()) Modifier.weight(1f) else Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Udostępnij")
            }
        }

        if (history.isNotEmpty()) {
            FilledTonalButton(
                onClick = onExport,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Eksportuj dane CSV")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    if (showResetStatsDialog) {
        AlertDialog(
            onDismissRequest = { showResetStatsDialog = false },
            title = { Text("Resetuj statystyki?") },
            text = { Text("Liczniki pakietów i Δt dla tego węzła zostaną wyzerowane. Historia pozycji pozostanie niezmieniona.") },
            confirmButton = {
                TextButton(onClick = {
                    onResetStats()
                    showResetStatsDialog = false
                }) { Text("Resetuj") }
            },
            dismissButton = {
                TextButton(onClick = { showResetStatsDialog = false }) { Text("Anuluj") }
            }
        )
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.45f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.55f)
        )
    }
}

private fun trackDistanceKm(history: List<TimedPosition>): Double {
    var total = 0.0
    for (i in 1 until history.size) {
        val a = history[i - 1]
        val b = history[i]
        val dLat = b.latitude - a.latitude
        val dLon = b.longitude - a.longitude
        val cosLat = cos(Math.toRadians((a.latitude + b.latitude) / 2))
        total += sqrt(
            dLat * dLat * 111_000.0 * 111_000.0 +
            dLon * dLon * (111_000.0 * cosLat) * (111_000.0 * cosLat)
        )
    }
    return total / 1000.0
}

private fun roleLabel(role: Int): String = when (role) {
    0 -> "CLIENT"
    1 -> "CLIENT_MUTE"
    2 -> "ROUTER"
    3 -> "ROUTER_CLIENT"
    4 -> "REPEATER"
    5 -> "TRACKER"
    6 -> "SENSOR"
    7 -> "TAK"
    else -> "Nieznana ($role)"
}

private fun snrQualityLabel(snr: Float): String = when {
    snr >= 5f -> "Doskonały"
    snr >= 0f -> "Dobry"
    snr >= -5f -> "Słaby"
    else -> "Bardzo słaby"
}

private fun headingToCardinal(degrees: Int): String {
    val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    return directions[((degrees + 22) / 45) % 8]
}

private fun precisionToMeters(precisionBits: Int): Int {
    val degrees = Math.pow(2.0, (32 - precisionBits).toDouble()) * 1e-7
    return (degrees * 111_000).toInt()
}

private fun formatAge(ageSeconds: Int): String = when {
    ageSeconds < 0 -> "brak czasu"
    ageSeconds < 60 -> "${ageSeconds}s"
    ageSeconds < 3600 -> "${ageSeconds / 60} min"
    ageSeconds < 86400 -> "${ageSeconds / 3600} h ${(ageSeconds % 3600) / 60} min"
    else -> "${ageSeconds / 86400} dni"
}
