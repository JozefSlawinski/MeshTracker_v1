package com.example.meshtracker_v1.service

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.meshtracker_v1.logic.GeofenceChecker
import com.example.meshtracker_v1.model.MeshNodeInfo
import com.example.meshtracker_v1.model.Zone
import com.example.meshtracker_v1.model.ZoneEventType
import com.example.meshtracker_v1.receiver.MeshtasticBroadcastReceiver
import com.example.meshtracker_v1.repository.ZoneRepository
import com.example.meshtracker_v1.util.Constants
import com.example.meshtracker_v1.util.NotificationChannels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * Foreground Service monitorujący węzły Meshtastic względem zdefiniowanych stref.
 *
 * Cykl życia:
 *  - Start: [ZoneViewModel] wywołuje [start] gdy istnieje ≥1 aktywna strefa.
 *  - Stop: [ZoneViewModel] wywołuje [stop] gdy wszystkie strefy są nieaktywne/usunięte.
 *  - Self-stop: serwis zatrzymuje się sam gdy Room zwróci pustą listę aktywnych stref.
 *
 * Działanie:
 *  1. Rejestruje [MeshtasticBroadcastReceiver] — odbiera ACTION_NODE_CHANGE.
 *  2. Kolekcjonuje aktywne strefy z Room (Flow — zawsze aktualne).
 *  3. Na każdy update węzła: sprawdza pozycję względem stref, wykrywa ENTER/EXIT,
 *     zapisuje [com.example.meshtracker_v1.model.ZoneEvent] i wysyła powiadomienie.
 */
@AndroidEntryPoint
class ZoneMonitorService : Service(),
    MeshtasticBroadcastReceiver.MeshtasticReceiverListener {

    companion object {
        private const val TAG = "ZoneMonitorService"

        fun start(context: Context) {
            val intent = Intent(context, ZoneMonitorService::class.java)
            ContextCompat.startForegroundService(context, intent)
            Log.d(TAG, "start() called")
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ZoneMonitorService::class.java))
            Log.d(TAG, "stop() called")
        }
    }

    // ------------------------------------------------------------------ DI

    @Inject lateinit var zoneRepository: ZoneRepository

    // ------------------------------------------------------------------ Stan wewnętrzny

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    /** Aktywne strefy — aktualizowane przez Flow z Room, odczytywane z wątku IO. */
    @Volatile private var activeZones: List<Zone> = emptyList()

    /**
     * Poprzedni stan rozmieszczenia: nodeId → zbiór zoneId w których węzeł był przy ostatnim
     * sprawdzeniu. Używany do wykrywania ENTER (pojawił się) i EXIT (zniknął).
     */
    private val previousNodeZoneMap = ConcurrentHashMap<String, Set<String>>()

    private val receiver = MeshtasticBroadcastReceiver(this)

    // ------------------------------------------------------------------ Lifecycle

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")

        // Foreground notification — musi być wywołane natychmiast po onCreate
        startForegroundCompat(buildForegroundNotification(0))

        // Rejestruj receiver na broadcasty Meshtastic (z innego pakietu → EXPORTED)
        val filter = IntentFilter().apply {
            addAction(Constants.ACTION_NODE_CHANGE)
            addAction(Constants.ACTION_MESH_CONNECTED)
            addAction(Constants.ACTION_MESH_DISCONNECTED)
        }
        ContextCompat.registerReceiver(
            this, receiver, filter, ContextCompat.RECEIVER_EXPORTED
        )

        // Nasłuchuj stref z Room — reaguj na każdą zmianę
        serviceScope.launch {
            zoneRepository.allZones.collect { zones ->
                val actives = zones.filter { it.isActive }
                activeZones = actives
                Log.d(TAG, "Aktywne strefy: ${actives.size}")
                updateForegroundNotification(actives.size)
                if (actives.isEmpty()) {
                    Log.d(TAG, "Brak aktywnych stref — serwis zatrzymuje się")
                    stopSelf()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        try { unregisterReceiver(receiver) } catch (_: Exception) {}
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ------------------------------------------------------------------ MeshtasticReceiverListener

    override fun onNodeChanged(nodeInfo: MeshNodeInfo) {
        serviceScope.launch { checkNodeAgainstZones(nodeInfo) }
    }

    override fun onMeshConnected() { /* no-op — serwis nie zarządza połączeniem */ }
    override fun onMeshDisconnected() { /* no-op */ }

    // ------------------------------------------------------------------ Logika geofencingu

    private suspend fun checkNodeAgainstZones(node: MeshNodeInfo) {
        if (!node.hasValidPosition()) return
        val pos = node.position ?: return

        val zones = activeZones
        if (zones.isEmpty()) return

        val nodeId = node.getId()
        val nodeName = node.getDisplayName()

        // Oblicz które strefy zawierają węzeł TERAZ (tylko strefy go monitorujące)
        val currentZones = mutableSetOf<String>()
        for (zone in zones) {
            if (nodeId !in zone.watchedNodeIds()) continue
            val vertices = zone.vertices()
            if (GeofenceChecker.contains(pos.latitude, pos.longitude, vertices)) {
                currentZones.add(zone.id)
            }
        }

        val previousZones = previousNodeZoneMap[nodeId] ?: emptySet()

        // ENTER — strefa w current ale nie w previous
        for (zoneId in currentZones - previousZones) {
            val zone = zones.find { it.id == zoneId } ?: continue
            Log.i(TAG, "ENTER: $nodeName → ${zone.name}")
            zoneRepository.recordEvent(zoneId, nodeId, nodeName, ZoneEventType.ENTER)
            fireAlertNotification(zone, nodeName, ZoneEventType.ENTER)
        }

        // EXIT — strefa w previous ale nie w current
        for (zoneId in previousZones - currentZones) {
            val zone = zones.find { it.id == zoneId } ?: continue
            Log.i(TAG, "EXIT: $nodeName ← ${zone.name}")
            zoneRepository.recordEvent(zoneId, nodeId, nodeName, ZoneEventType.EXIT)
            fireAlertNotification(zone, nodeName, ZoneEventType.EXIT)
        }

        // Aktualizuj stan
        if (currentZones.isEmpty()) {
            previousNodeZoneMap.remove(nodeId)
        } else {
            previousNodeZoneMap[nodeId] = currentZones
        }
    }

    // ------------------------------------------------------------------ Powiadomienia

    private fun fireAlertNotification(
        zone: Zone,
        nodeName: String,
        type: ZoneEventType
    ) {
        // Sprawdź uprawnienie POST_NOTIFICATIONS (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "POST_NOTIFICATIONS brak uprawnień — pomijam powiadomienie")
                return
            }
        }

        val (title, text) = when (type) {
            ZoneEventType.ENTER -> "Węzeł wszedł do strefy" to "$nodeName → ${zone.name}"
            ZoneEventType.EXIT  -> "Węzeł opuścił strefę"  to "$nodeName ← ${zone.name}"
        }

        // Otwórz aplikację po tapnięciu
        val tapIntent = packageManager
            .getLaunchIntentForPackage(packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, tapIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                    android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NotificationChannels.CHANNEL_ZONE_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // ID oparty na haślu strefa+węzeł — nie zalewa powiadomieniami przy wielu zdarzeniach
        val notificationId = (zone.id + nodeName).hashCode()
        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }

    private fun buildForegroundNotification(activeZoneCount: Int): Notification {
        val text = when {
            activeZoneCount == 0  -> "Ładowanie stref…"
            activeZoneCount == 1  -> "Monitoruję 1 strefę"
            activeZoneCount in 2..4 -> "Monitoruję $activeZoneCount strefy"
            else                  -> "Monitoruję $activeZoneCount stref"
        }
        return NotificationCompat.Builder(this, NotificationChannels.CHANNEL_ZONE_SERVICE)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("MeshTracker — Geofencing")
            .setContentText(text)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateForegroundNotification(count: Int) {
        NotificationManagerCompat.from(this).notify(
            NotificationChannels.NOTIFICATION_ID_FOREGROUND,
            buildForegroundNotification(count)
        )
    }

    /** Wywołuje startForeground() z typem serwisu na API 29+. */
    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationChannels.NOTIFICATION_ID_FOREGROUND,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(
                NotificationChannels.NOTIFICATION_ID_FOREGROUND,
                notification
            )
        }
    }
}
