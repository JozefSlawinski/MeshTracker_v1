package com.example.meshtracker_v1.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.example.meshtracker_v1.model.MeshNodeInfo
import com.example.meshtracker_v1.ui.zones.ZoneBottomSheet
import com.example.meshtracker_v1.ui.zones.ZoneConfirmDialog
import com.example.meshtracker_v1.ui.zones.ZoneViewModel

/**
 * Ekran mapy wyświetlający węzły Meshtastic i strefy geofencingu.
 *
 * Tryb rysowania: long-press na mapie dodaje wierzchołek; FABs (Zamknij/Cofnij/Anuluj)
 * zarządzają stanem maszyny [ZoneViewModel.DrawingState].
 */
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel(),
    zoneViewModel: ZoneViewModel = hiltViewModel(),
    onNavigateToZoneDetail: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    // ------------------------------------------------------------------ stan z VMów
    val nodes           by viewModel.nodes.collectAsState()
    val selectedNodeId  by viewModel.selectedNodeId.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val mapTypeIndex    by viewModel.mapType.collectAsState()
    val nodeHistory     by viewModel.nodeHistory.collectAsState()
    val activeZones     by viewModel.activeZones.collectAsState()
    val nodesInZones    by viewModel.nodesInZones.collectAsState()

    val allZones        by zoneViewModel.allZones.collectAsState()
    val drawingState    by zoneViewModel.drawingState.collectAsState()

    val googleMapType = when (mapTypeIndex) {
        1 -> MapType.SATELLITE
        2 -> MapType.TERRAIN
        3 -> MapType.HYBRID
        else -> MapType.NORMAL
    }

    val context = LocalContext.current
    val iconCache = remember { mutableMapOf<Triple<Int, Int, Boolean>, BitmapDescriptor>() }

    // Sheet stref widoczny gdy użytkownik tapnie FAB
    var showZoneSheet by remember { mutableStateOf(false) }

    // ------------------------------------------------------------------ uprawnienia lokalizacji
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // ------------------------------------------------------------------ kamera
    val defaultLocation = LatLng(52.0, 19.0)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 6f)
    }

    val nodesWithPosition = nodes.values.filter { it.hasValidPosition() }

    // Rzutowanie stanu rysowania — używane zarówno wewnątrz GoogleMap jak i w FABs/hint
    val drawing = drawingState as? ZoneViewModel.DrawingState.Drawing

    LaunchedEffect(selectedNodeId) {
        selectedNodeId?.let { nodeId ->
            nodes[nodeId]?.position?.let { position ->
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(position.toLatLng(), 15f)
                )
            }
        }
    }

    // ------------------------------------------------------------------ UI
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        // ---- Mapa ----
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapClick = {
                // W trybie Idle: odznacz węzeł. W trybie Drawing: ignoruj (long-press dodaje wierzchołki).
                if (drawingState is ZoneViewModel.DrawingState.Idle) {
                    viewModel.selectNode(null)
                }
            },
            onMapLongClick = { latLng ->
                if (drawingState is ZoneViewModel.DrawingState.Drawing) {
                    zoneViewModel.addVertex(latLng.latitude, latLng.longitude)
                }
            },
            properties = MapProperties(
                isMyLocationEnabled = hasLocationPermission,
                mapType = googleMapType
            )
        ) {
            // ---- Wielokąty stref ----
            activeZones.forEach { zone ->
                val pts = zone.vertices().map { v -> LatLng(v.lat, v.lon) }
                if (pts.size >= 3) {
                    Polygon(
                        points = pts,
                        fillColor   = ComposeColor(zone.colorArgb).copy(alpha = 0.25f),
                        strokeColor = ComposeColor(zone.colorArgb),
                        strokeWidth = 4f,
                        zIndex      = 0.1f
                    )
                }
            }

            // ---- Podgląd rysowanego wielokąta ----
            if (drawing != null && drawing.vertices.isNotEmpty()) {
                val pts = drawing.vertices.map { v -> LatLng(v.lat, v.lon) }

                // Polyline łącząca dodane wierzchołki
                Polyline(
                    points     = pts,
                    color      = ComposeColor(0xFF4CAF50.toInt()),
                    width      = 6f,
                    zIndex     = 0.8f
                )

                // Markery wierzchołków
                drawing.vertices.forEachIndexed { idx, v ->
                    Marker(
                        state = MarkerState(LatLng(v.lat, v.lon)),
                        title = "Punkt ${idx + 1}",
                        icon  = BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_GREEN
                        ),
                        zIndex = 0.9f
                    )
                }
            }

            // ---- Historia pozycji zaznaczonego węzła ----
            val historyPoints = selectedNodeId?.let { nodeHistory[it] }
            if (historyPoints != null && historyPoints.size >= 2) {
                Polyline(
                    points = historyPoints.map { LatLng(it.latitude, it.longitude) },
                    color  = ComposeColor(0xFF2196F3.toInt()),
                    width  = 6f,
                    zIndex = 0.5f
                )
            }

            // ---- Markery węzłów ----
            nodesWithPosition.forEach { node ->
                val position = node.position ?: return@forEach
                val isSelected = node.getId() == selectedNodeId
                val inZone = node.getId() in nodesInZones

                Marker(
                    state   = MarkerState(position = position.toLatLng()),
                    title   = node.getDisplayName(),
                    snippet = buildMarkerSnippet(node),
                    icon    = getMarkerIcon(node, isSelected, inZone, iconCache),
                    zIndex  = if (isSelected) 1f else 0f,
                    onClick = {
                        viewModel.selectNode(node.getId())
                        true
                    }
                )
            }
        }

        // ---- Pusty stan / łączenie ----
        if (nodesWithPosition.isEmpty() &&
            connectionState is MapViewModel.ConnectionState.Connected
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (nodes.isEmpty()) "Brak węzłów w sieci"
                               else "Brak węzłów z pozycją GPS",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (nodes.isNotEmpty()) {
                        Text(
                            text = "Węzły: ${nodes.size} (bez pozycji GPS)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        val isConnecting = connectionState is MapViewModel.ConnectionState.Connecting ||
                connectionState is MapViewModel.ConnectionState.Reconnecting
        if (isConnecting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                CircularProgressIndicator()
            }
        }

        // ---- Podpowiedź rysowania ----
        if (drawing != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
                shape         = MaterialTheme.shapes.medium,
                tonalElevation = 6.dp
            ) {
                Text(
                    text = "Dodano ${drawing.vertices.size} wierzchołków • " +
                           "Przytrzymaj mapę, aby dodać kolejny",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style    = MaterialTheme.typography.labelMedium
                )
            }
        }

        // ---- FABs (prawy dolny róg) ----
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement  = Arrangement.spacedBy(8.dp),
            horizontalAlignment  = Alignment.End
        ) {
            when (val state = drawingState) {

                // Tryb normalny — jeden FAB otwierający listę stref
                is ZoneViewModel.DrawingState.Idle -> {
                    FloatingActionButton(onClick = { showZoneSheet = true }) {
                        Icon(Icons.Default.Place, contentDescription = "Strefy")
                    }
                }

                // Tryb rysowania — trzy ExtendedFABs (Anuluj / Cofnij / Zamknij)
                is ZoneViewModel.DrawingState.Drawing -> {
                    ExtendedFloatingActionButton(
                        onClick = { zoneViewModel.cancelDrawing() },
                        icon    = { Icon(Icons.Default.Close, contentDescription = null) },
                        text    = { Text("Anuluj") }
                    )
                    if (state.canUndo) {
                        ExtendedFloatingActionButton(
                            onClick = { zoneViewModel.removeLastVertex() },
                            icon    = { Icon(Icons.Default.ArrowBack, contentDescription = null) },
                            text    = { Text("Cofnij") }
                        )
                    }
                    if (state.canClose) {
                        ExtendedFloatingActionButton(
                            onClick = { zoneViewModel.closePolygon() },
                            icon    = { Icon(Icons.Default.Done, contentDescription = null) },
                            text    = { Text("Zamknij") }
                        )
                    }
                }

                // Stan Confirming — dialog zakrywa mapę, FABs ukryte
                is ZoneViewModel.DrawingState.Confirming -> { /* brak FABs */ }
            }
        }
    }

    // ------------------------------------------------------------------ overlays poza mapą

    // Sheet z listą stref
    if (showZoneSheet) {
        ZoneBottomSheet(
            zones            = allZones,
            onDismiss        = { showZoneSheet = false },
            onStartDrawing   = { zoneViewModel.startDrawing() },
            onToggleActive   = { zone -> zoneViewModel.toggleActive(zone) },
            onDeleteZone     = { zoneId -> zoneViewModel.deleteZone(zoneId) },
            onNavigateToDetail = { zoneId ->
                showZoneSheet = false
                onNavigateToZoneDetail(zoneId)
            }
        )
    }

    // Dialog po zamknięciu wielokąta
    if (drawingState is ZoneViewModel.DrawingState.Confirming) {
        ZoneConfirmDialog(
            availableNodes = nodes.values.toList(),
            onConfirm = { name, colorArgb, watchedNodeIds ->
                zoneViewModel.confirmZone(name, colorArgb, watchedNodeIds)
            },
            onDismiss = { zoneViewModel.cancelDrawing() }
        )
    }
}

// ------------------------------------------------------------------ Marker helpers

/**
 * Buduje snippet dla markera.
 */
private fun buildMarkerSnippet(node: MeshNodeInfo): String {
    val parts = mutableListOf<String>()

    parts.add(if (node.isOnline()) "Online" else "Offline")

    if (node.batteryLevel > 0) parts.add("Bat: ${node.batteryLevel}%")
    if (node.snr != Float.MAX_VALUE) parts.add("SNR: ${String.format("%.1f", node.snr)} dB")

    node.position?.let { pos ->
        if (pos.groundSpeed > 0) parts.add("${pos.groundSpeed} m/s")
        if (pos.groundTrack > 0) parts.add("${pos.groundTrack}°")

        // Wiek pozycji
        if (pos.time > 0) {
            val ageSeconds = (System.currentTimeMillis() / 1000 - pos.time).toInt()
            parts.add(formatPositionAge(ageSeconds))
        }

        // Ostrzeżenie o obniżonej precyzji
        if (pos.precisionBits in 1..27) {
            parts.add("⚠ precyzja ~${precisionToMeters(pos.precisionBits)} m")
        }
    }

    return parts.joinToString(" • ")
}

/**
 * Formatuje wiek pozycji GPS w czytelny sposób.
 */
private fun formatPositionAge(ageSeconds: Int): String = when {
    ageSeconds < 0    -> "GPS: brak czasu"
    ageSeconds < 60   -> "GPS: ${ageSeconds}s temu"
    ageSeconds < 3600 -> "GPS: ${ageSeconds / 60}min temu"
    else              -> "GPS: ${ageSeconds / 3600}h temu"
}

/**
 * Przybliża maksymalne odchylenie pozycji (w metrach) na podstawie precisionBits.
 */
private fun precisionToMeters(precisionBits: Int): Int {
    val degrees = Math.pow(2.0, (32 - precisionBits).toDouble()) * 1e-7
    return (degrees * 111_000).toInt()
}

/**
 * Zwraca kolor (hue) markera na podstawie roli węzła.
 */
private fun getMarkerHue(node: MeshNodeInfo): Float {
    val role = node.user?.role
    if (role == null) {
        android.util.Log.d("MapScreen", "Node ${node.getDisplayName()} has no user or role, using default color")
        return BitmapDescriptorFactory.HUE_AZURE
    }
    android.util.Log.d("MapScreen", "Node ${node.getDisplayName()} has role: $role")
    return when (role) {
        0 -> BitmapDescriptorFactory.HUE_RED    // CLIENT
        5 -> BitmapDescriptorFactory.HUE_GREEN  // TRACKER
        else -> BitmapDescriptorFactory.HUE_AZURE
    }
}

/**
 * Tworzy BitmapDescriptor ze strzałką wskazującą kierunek (HEADING).
 */
private fun createArrowIcon(
    heading: Int,
    color: Int = Color.BLUE,
    selected: Boolean = false
): BitmapDescriptor {
    val size = if (selected) 130 else 100
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

    val centerX = size / 2f
    val centerY = size / 2f
    val arrowLength = size * 0.35f
    val arrowWidth  = size * 0.15f

    canvas.save()
    canvas.translate(centerX, centerY)
    canvas.rotate(heading.toFloat())
    canvas.translate(-centerX, -centerY)

    if (selected) {
        val outlinePaint = Paint().apply {
            this.color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val outlinePath = Path().apply {
            val o = 6f
            moveTo(centerX, centerY - arrowLength - o)
            lineTo(centerX - arrowWidth / 2 - o, centerY - arrowLength / 3)
            lineTo(centerX, centerY + o)
            lineTo(centerX + arrowWidth / 2 + o, centerY - arrowLength / 3)
            close()
        }
        canvas.drawPath(outlinePath, outlinePaint)
        outlinePaint.apply { this.color = Color.WHITE }
        canvas.drawCircle(centerX, centerY, size * 0.08f + 4f, outlinePaint)
    }

    val paint = Paint().apply {
        this.color = color
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    val path = Path().apply {
        moveTo(centerX, centerY - arrowLength)
        lineTo(centerX - arrowWidth / 2, centerY - arrowLength / 3)
        lineTo(centerX, centerY)
        lineTo(centerX + arrowWidth / 2, centerY - arrowLength / 3)
        close()
    }
    canvas.drawPath(path, paint)
    canvas.drawCircle(centerX, centerY, size * 0.08f, paint)

    canvas.restore()

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

/**
 * Zwraca ikonę markera z cache.
 *
 * @param inZone true gdy węzeł jest wewnątrz co najmniej jednej aktywnej strefy —
 *               węzeł nie-zaznaczony dostaje kolor MAGENTA
 */
private fun getMarkerIcon(
    node: MeshNodeInfo,
    selected: Boolean,
    inZone: Boolean,
    cache: MutableMap<Triple<Int, Int, Boolean>, BitmapDescriptor>
): BitmapDescriptor {
    val heading = node.position?.groundTrack ?: 0

    if (heading > 0) {
        // Węzeł w strefie (i nie zaznaczony) → kolor magenta, inaczej rola-based
        val color = if (inZone && !selected) Color.MAGENTA else when (node.user?.role) {
            0 -> Color.RED
            5 -> Color.GREEN
            else -> Color.BLUE
        }
        return cache.getOrPut(Triple(heading, color, selected)) {
            createArrowIcon(heading, color, selected)
        }
    }

    // Domyślny marker pinezka
    return when {
        selected -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
        inZone   -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)
        else     -> BitmapDescriptorFactory.defaultMarker(getMarkerHue(node))
    }
}
