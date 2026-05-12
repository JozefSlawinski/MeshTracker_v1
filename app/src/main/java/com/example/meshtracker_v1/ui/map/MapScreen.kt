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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.google.maps.android.compose.rememberCameraPositionState
import com.example.meshtracker_v1.model.MeshNodeInfo

/**
 * Ekran mapy wyświetlający węzły Meshtastic.
 */
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    val nodes by viewModel.nodes.collectAsState()
    val selectedNodeId by viewModel.selectedNodeId.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val mapTypeIndex by viewModel.mapType.collectAsState()
    val googleMapType = when (mapTypeIndex) {
        1 -> MapType.SATELLITE
        2 -> MapType.TERRAIN
        3 -> MapType.HYBRID
        else -> MapType.NORMAL
    }
    
    val context = LocalContext.current
    val iconCache = remember { mutableMapOf<Triple<Int, Int, Boolean>, BitmapDescriptor>() }

    // Sprawdź uprawnienia do lokalizacji
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
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
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
    
    // Stan kamery (domyślnie centrum Polski)
    val defaultLocation = LatLng(52.0, 19.0)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 6f)
    }
    
    // Filtruj węzły z prawidłową pozycją
    val nodesWithPosition = nodes.values.filter { it.hasValidPosition() }
    
    // Aktualizuj kamerę gdy wybrano węzeł
    LaunchedEffect(selectedNodeId) {
        selectedNodeId?.let { nodeId ->
            val node = nodes[nodeId]
            node?.position?.let { position ->
                val latLng = position.toLatLng()
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                )
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapClick = { viewModel.selectNode(null) },
            properties = MapProperties(
                isMyLocationEnabled = hasLocationPermission,
                mapType = googleMapType
            )
        ) {
            nodesWithPosition.forEach { node ->
                val position = node.position ?: return@forEach
                val isSelected = node.getId() == selectedNodeId

                Marker(
                    state = MarkerState(position = position.toLatLng()),
                    title = node.getDisplayName(),
                    snippet = buildMarkerSnippet(node),
                    icon = getMarkerIcon(node, isSelected, iconCache),
                    zIndex = if (isSelected) 1f else 0f,
                    onClick = {
                        viewModel.selectNode(node.getId())
                        true
                    }
                )
            }
        }

        if (nodesWithPosition.isEmpty() && connectionState is MapViewModel.ConnectionState.Connected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (nodes.isEmpty()) "Brak węzłów w sieci" else "Brak węzłów z pozycją GPS",
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
    }
}

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
 * Wzór: 2^(32 - precisionBits) * 1e-7 * 111_000 m/° (przy równiku).
 */
private fun precisionToMeters(precisionBits: Int): Int {
    val degrees = Math.pow(2.0, (32 - precisionBits).toDouble()) * 1e-7
    return (degrees * 111_000).toInt()
}

/**
 * Zwraca kolor markera na podstawie roli węzła.
 */
private fun getMarkerHue(node: MeshNodeInfo): Float {
    val role = node.user?.role
    if (role == null) {
        android.util.Log.d("MapScreen", "Node ${node.getDisplayName()} has no user or role, using default color")
        return BitmapDescriptorFactory.HUE_AZURE
    }
    android.util.Log.d("MapScreen", "Node ${node.getDisplayName()} has role: $role")
    return when (role) {
        0 -> BitmapDescriptorFactory.HUE_RED // CLIENT - czerwony
        5 -> BitmapDescriptorFactory.HUE_GREEN // TRACKER - zielony
        else -> BitmapDescriptorFactory.HUE_AZURE
    }
}

/**
 * Tworzy BitmapDescriptor ze strzałką wskazującą kierunek (HEADING).
 * @param heading Kierunek w stopniach (0-360, gdzie 0 to północ)
 * @param color Kolor strzałki
 * @return BitmapDescriptor z obróconą strzałką
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
    val arrowWidth = size * 0.15f

    canvas.save()
    canvas.translate(centerX, centerY)
    canvas.rotate(heading.toFloat()) // 0° = Północ, 90° = Wschód, 180° = Południe
    canvas.translate(-centerX, -centerY)

    // Białe obramowanie dla zaznaczonego węzła
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
 * Zwraca ikonę markera z cache — strzałkę jeśli dostępny jest heading, w przeciwnym razie domyślny marker.
 */
private fun getMarkerIcon(
    node: MeshNodeInfo,
    selected: Boolean,
    cache: MutableMap<Triple<Int, Int, Boolean>, BitmapDescriptor>
): BitmapDescriptor {
    val heading = node.position?.groundTrack ?: 0

    if (heading > 0) {
        val color = when (node.user?.role) {
            0 -> Color.RED
            5 -> Color.GREEN
            else -> Color.BLUE
        }
        return cache.getOrPut(Triple(heading, color, selected)) {
            createArrowIcon(heading, color, selected)
        }
    }

    // Domyślny marker — zaznaczony dostaje większą wersję przez hue YELLOW
    return if (selected) {
        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
    } else {
        BitmapDescriptorFactory.defaultMarker(getMarkerHue(node))
    }
}

