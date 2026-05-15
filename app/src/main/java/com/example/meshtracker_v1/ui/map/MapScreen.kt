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
import androidx.compose.ui.geometry.Offset
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
    val myNodeId        by viewModel.myNodeId.collectAsState()
    val showAllTracks   by viewModel.showAllTracks.collectAsState()

    val allZones        by zoneViewModel.allZones.collectAsState()
    val drawingState    by zoneViewModel.drawingState.collectAsState()

    val googleMapType = when (mapTypeIndex) {
        1 -> MapType.SATELLITE
        2 -> MapType.TERRAIN
        3 -> MapType.HYBRID
        else -> MapType.NORMAL
    }

    val context = LocalContext.current
    val iconCache = remember { mutableMapOf<MarkerKey, BitmapDescriptor>() }

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

            // ---- Historia pozycji węzłów ----
            if (showAllTracks) {
                nodeHistory.forEach { (nodeId, history) ->
                    if (history.size < 2) return@forEach
                    val isSelected = nodeId == selectedNodeId
                    val node = nodes[nodeId]
                    val isTracker = node?.user?.role == 5
                    val color = when {
                        isSelected  -> ComposeColor(0xFF2196F3.toInt())
                        isTracker   -> ComposeColor(0xFF4CAF50.toInt()).copy(alpha = 0.85f)
                        else        -> ComposeColor(0xFF9E9E9E.toInt()).copy(alpha = 0.5f)
                    }
                    val width = when {
                        isSelected -> 6f
                        isTracker  -> 5f
                        else       -> 3f
                    }
                    Polyline(
                        points = history.map { LatLng(it.latitude, it.longitude) },
                        color  = color,
                        width  = width,
                        zIndex = if (isSelected) 0.6f else 0.5f
                    )
                }
            } else {
                val historyPoints = selectedNodeId?.let { nodeHistory[it] }
                if (historyPoints != null && historyPoints.size >= 2) {
                    Polyline(
                        points = historyPoints.map { LatLng(it.latitude, it.longitude) },
                        color  = ComposeColor(0xFF2196F3.toInt()),
                        width  = 6f,
                        zIndex = 0.5f
                    )
                }
            }

            // ---- Markery węzłów ----
            nodesWithPosition.forEach { node ->
                val position = node.position ?: return@forEach
                val nodeId   = node.getId()
                val isSelected   = nodeId == selectedNodeId
                val inZone       = nodeId in nodesInZones
                // Węzeł lokalny (BLE) nie ma pomiaru RF → snr = Float.MAX_VALUE.
                // Używamy tego jako wskaźnika zamiast porównania ID (getMyId() może nie istnieć w AIDL).
                val isLocalNode  = node.snr == Float.MAX_VALUE
                val isMoving     = position.groundTrack > 0

                Marker(
                    state   = MarkerState(position = position.toLatLng()),
                    title   = node.getDisplayName(),
                    snippet = buildMarkerSnippet(node),
                    icon    = getMarkerIcon(node, isSelected, inZone, isLocalNode, iconCache),
                    // Węzeł w ruchu: anchor = środek kółka; statyczny: anchor = czubek pina
                    anchor  = if (isMoving) Offset(0.5f, 0.5f) else Offset(0.5f, 1.0f),
                    zIndex  = if (isSelected) 1f else 0f,
                    onClick = {
                        viewModel.selectNode(nodeId)
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

// ================================================================== Marker helpers

/**
 * Klucz cache markera — wszystkie właściwości wizualne wpływające na wygląd Bitmapy.
 * Przy 2-5 węzłach cache jest trywialnie mały.
 */
private data class MarkerKey(
    val initials: String,
    val fillColor: Int,
    val heading: Int,        // 0 = statyczny; >0 = kierunek ruchu w stopniach
    val selected: Boolean,
    val snrColor: Int,       // kolor dota SNR (android.graphics.Color int)
    val isLocalNode: Boolean // węzeł podłączony bezpośrednio przez BT/USB
)

/** Wyciąga 1-2 znaki identyfikatora: shortName węzła lub prefix node ID. */
private fun getInitials(node: MeshNodeInfo): String {
    val short = node.user?.shortName?.trim()
    if (!short.isNullOrEmpty()) return short.take(2).uppercase()
    return node.getId().removePrefix("!").take(2).uppercase()
}

/**
 * Kolor wypełnienia kółka markera.
 * Kolejność priorytetów: zaznaczony → offline → w strefie → rola.
 */
private fun getMarkerFillColor(
    node: MeshNodeInfo,
    selected: Boolean,
    inZone: Boolean
): Int = when {
    selected         -> Color.parseColor("#FFC107")   // żółty — zaznaczony
    !node.isOnline() -> Color.parseColor("#9E9E9E")   // szary — offline
    inZone           -> Color.parseColor("#E91E63")   // magenta — w strefie
    else -> when (node.user?.role) {
        0    -> Color.parseColor("#F44336")  // CLIENT      → czerwony
        2, 3 -> Color.parseColor("#2196F3")  // ROUTER/RC   → niebieski
        5    -> Color.parseColor("#4CAF50")  // TRACKER     → zielony
        else -> Color.parseColor("#03A9F4")  // pozostałe   → jasnoniebieski
    }
}

/**
 * Kolor dota SNR:
 *  ≥ 5 dB  → zielony (doskonały)
 *  ≥ 0 dB  → jasnozielony (dobry)
 *  ≥ −5 dB → pomarańczowy (słaby)
 *  < −5 dB → czerwony (bardzo słaby)
 *  brak    → szary
 */
private fun getSnrDotColor(snr: Float): Int = when {
    snr == Float.MAX_VALUE -> Color.parseColor("#9E9E9E")   // brak danych
    snr >= 5f  -> Color.parseColor("#4CAF50")               // doskonały
    snr >= 0f  -> Color.parseColor("#8BC34A")               // dobry
    snr >= -5f -> Color.parseColor("#FF9800")               // słaby
    else       -> Color.parseColor("#F44336")               // bardzo słaby
}

/**
 * Rysuje kompozytowy marker na Canvas.
 *
 * Węzeł statyczny (heading == 0):
 *   ● dot SNR (prawy górny róg kółka)
 *  ┌──────┐
 *  │  AB  │  kółko z inicjałami; kolor = stan/rola
 *  └──┬───┘
 *     ▼  pin (anchor = czubek)
 *
 * Węzeł w ruchu (heading > 0):
 *   Strzałka wychodząca z krawędzi kółka w kierunku ruchu (tylko strzałka się obraca,
 *   kółko i inicjały pozostają w pionie).
 *   ● dot SNR (prawy górny róg kółka, pozycja stała)
 *   Anchor = środek kółka (0.5f, 0.5f).
 */
private fun createCompositeMarker(
    initials: String,
    fillColor: Int,
    heading: Int,
    selected: Boolean,
    snrColor: Int,
    isLocalNode: Boolean = false
): BitmapDescriptor {
    // ---- wymiary (px przy gęstości ekranu ~2.5 MDPI) ----
    val circleR   = if (selected) 34f else 26f    // promień kółka
    val pinH      = if (selected) 26f else 20f    // wysokość pina (tylko statyczny)
    val snrR      = if (selected) 9f  else 7f     // promień dota SNR
    val arrowLen  = if (selected) 42f else 32f    // długość strzałki poza kółkiem
    val arrowBase = circleR * 0.55f               // szerokość podstawy grotu strzałki
    val ringW     = 5f                            // grubość białej obwódki (selected)

    val paint = Paint().apply { isAntiAlias = true }

    return if (heading == 0) {
        // ---- STATYCZNY: kółko + pin ----
        val w = ((circleR + snrR + ringW + 2) * 2).toInt()
        val h = (circleR * 2 + pinH + ringW * 2 + snrR + 2).toInt()
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val cx = w / 2f
        val cy = snrR + ringW + circleR   // środek kółka (zostawia miejsce na dot SNR nad nim)

        // Biała obwódka (zaznaczony)
        if (selected) {
            paint.color = Color.WHITE
            paint.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, circleR + ringW, paint)
            // Pin obwódka
            val pinPath = Path().apply {
                moveTo(cx - (arrowBase * 0.4f) - ringW, cy + circleR)
                lineTo(cx, h.toFloat())
                lineTo(cx + (arrowBase * 0.4f) + ringW, cy + circleR)
                close()
            }
            canvas.drawPath(pinPath, paint)
        }

        // Pin
        paint.color = fillColor
        paint.style = Paint.Style.FILL
        val pinPath = Path().apply {
            val pw = arrowBase * 0.4f
            moveTo(cx - pw, cy + circleR - 2f)
            lineTo(cx, (cy + circleR + pinH))
            lineTo(cx + pw, cy + circleR - 2f)
            close()
        }
        canvas.drawPath(pinPath, paint)

        // Kółko
        canvas.drawCircle(cx, cy, circleR, paint)

        // Badge w prawym górnym rogu kółka: ikonka telefonu (węzeł lokalny) lub dot SNR
        val badgeX = cx + circleR * 0.68f
        val badgeY = cy - circleR * 0.68f
        if (isLocalNode) {
            drawPhoneBadge(canvas, badgeX, badgeY, snrR + 2f, paint)
        } else {
            paint.color = snrColor
            paint.style = Paint.Style.FILL
            canvas.drawCircle(badgeX, badgeY, snrR, paint)
            paint.color = Color.WHITE
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawCircle(badgeX, badgeY, snrR, paint)
        }

        // Inicjały
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        paint.textSize = circleR * 0.82f
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        paint.textAlign = Paint.Align.CENTER
        val textY = cy - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(initials, cx, textY, paint)

        BitmapDescriptorFactory.fromBitmap(bitmap)

    } else {
        // ---- W RUCHU: kwadrat ze strzałką wychodząca z kółka ----
        val margin = arrowLen + snrR + ringW + 4f
        val s = ((circleR + margin) * 2).toInt()
        val bitmap = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val cx = s / 2f
        val cy = s / 2f

        // Kierunek strzałki: heading 0=N (↑), 90=E (→), 180=S (↓), 270=W (←)
        val rad = Math.toRadians(heading.toDouble())
        val dx = Math.sin(rad).toFloat()
        val dy = (-Math.cos(rad)).toFloat()

        // ---- Strzałka (rysowana PRZED kółkiem, pod nim) ----
        val arrowTipX  = cx + dx * (circleR + arrowLen)
        val arrowTipY  = cy + dy * (circleR + arrowLen)
        val arrowBaseX = cx + dx * circleR
        val arrowBaseY = cy + dy * circleR
        // Prostopadły (do obrócenia w kierunku strzałki)
        val perpX = -dy
        val perpY =  dx

        // Biała obwódka strzałki (selected)
        if (selected) {
            paint.color = Color.WHITE
            paint.style = Paint.Style.FILL
            val outPath = Path().apply {
                moveTo(arrowTipX, arrowTipY)
                lineTo(arrowBaseX + perpX * (arrowBase + ringW), arrowBaseY + perpY * (arrowBase + ringW))
                lineTo(arrowBaseX - perpX * (arrowBase + ringW), arrowBaseY - perpY * (arrowBase + ringW))
                close()
            }
            canvas.drawPath(outPath, paint)
        }

        // Grot strzałki
        paint.color = fillColor
        paint.style = Paint.Style.FILL
        val arrowPath = Path().apply {
            moveTo(arrowTipX, arrowTipY)
            lineTo(arrowBaseX + perpX * arrowBase, arrowBaseY + perpY * arrowBase)
            lineTo(arrowBaseX - perpX * arrowBase, arrowBaseY - perpY * arrowBase)
            close()
        }
        canvas.drawPath(arrowPath, paint)

        // ---- Kółko ----
        if (selected) {
            paint.color = Color.WHITE
            canvas.drawCircle(cx, cy, circleR + ringW, paint)
        }
        paint.color = fillColor
        canvas.drawCircle(cx, cy, circleR, paint)

        // Badge SNR / telefon (stały, prawy górny róg — nie obraca się z heading)
        val snrX = cx + circleR * 0.68f
        val snrY = cy - circleR * 0.68f
        if (isLocalNode) {
            drawPhoneBadge(canvas, snrX, snrY, snrR + 2f, paint)
        } else {
            paint.color = snrColor
            paint.style = Paint.Style.FILL
            canvas.drawCircle(snrX, snrY, snrR, paint)
            paint.color = Color.WHITE
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawCircle(snrX, snrY, snrR, paint)
        }

        // Inicjały
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        paint.textSize = circleR * 0.82f
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        paint.textAlign = Paint.Align.CENTER
        val textY = cy - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(initials, cx, textY, paint)

        BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}

/**
 * Rysuje badge z ikoną telefonu (BLE) zamiast dota SNR dla węzła lokalnego.
 * [cx],[cy] = środek badge, [r] = promień tła.
 */
private fun drawPhoneBadge(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
    // Niebieskie tło
    paint.color = Color.parseColor("#1565C0")
    paint.style = Paint.Style.FILL
    canvas.drawCircle(cx, cy, r, paint)
    // Biała obwódka
    paint.color = Color.WHITE
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.5f
    canvas.drawCircle(cx, cy, r, paint)
    // Obrys telefonu (zaokrąglony prostokąt)
    val w = r * 0.55f
    val h = r * 0.9f
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.5f
    canvas.drawRoundRect(cx - w, cy - h, cx + w, cy + h, w * 0.3f, w * 0.3f, paint)
    // Głośniczek
    paint.strokeWidth = 1.2f
    canvas.drawLine(cx - w * 0.35f, cy - h * 0.78f, cx + w * 0.35f, cy - h * 0.78f, paint)
}

/**
 * Zwraca ikonę markera z cache — rysuje [createCompositeMarker] tylko przy pierwszym
 * wywołaniu dla danej kombinacji właściwości wizualnych.
 */
private fun getMarkerIcon(
    node: MeshNodeInfo,
    selected: Boolean,
    inZone: Boolean,
    isLocalNode: Boolean,
    cache: MutableMap<MarkerKey, BitmapDescriptor>
): BitmapDescriptor {
    val key = MarkerKey(
        initials    = getInitials(node),
        fillColor   = getMarkerFillColor(node, selected, inZone),
        heading     = node.position?.groundTrack ?: 0,
        selected    = selected,
        snrColor    = getSnrDotColor(node.snr),
        isLocalNode = isLocalNode
    )
    return cache.getOrPut(key) {
        createCompositeMarker(
            initials    = key.initials,
            fillColor   = key.fillColor,
            heading     = key.heading,
            selected    = key.selected,
            snrColor    = key.snrColor,
            isLocalNode = key.isLocalNode
        )
    }
}
