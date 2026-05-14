package com.example.meshtracker_v1.repository

import android.util.Log
import com.example.meshtracker_v1.data.ZoneDao
import com.example.meshtracker_v1.model.Zone
import com.example.meshtracker_v1.model.ZoneEvent
import com.example.meshtracker_v1.model.ZoneEventType
import com.example.meshtracker_v1.model.ZoneVertex
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZoneRepository @Inject constructor(
    private val dao: ZoneDao
) {
    companion object { private const val TAG = "ZoneRepository" }

    // ------------------------------------------------------------------ Obserwacja

    /** Flow wszystkich stref, posortowanych alfabetycznie. */
    val allZones: Flow<List<Zone>> = dao.getAllZones()

    /** Flow zdarzeń dla konkretnej strefy (od najnowszego). */
    fun getEventsForZone(zoneId: String): Flow<List<ZoneEvent>> =
        dao.getEventsForZone(zoneId)

    // ------------------------------------------------------------------ CRUD stref

    suspend fun insertZone(zone: Zone) {
        Log.d(TAG, "insertZone: ${zone.name} (${zone.id})")
        dao.insertZone(zone)
    }

    suspend fun updateZone(zone: Zone) {
        Log.d(TAG, "updateZone: ${zone.name}")
        dao.updateZone(zone)
    }

    suspend fun deleteZone(zoneId: String) {
        Log.d(TAG, "deleteZone: $zoneId (zdarzenia usunięte przez CASCADE)")
        dao.deleteZone(zoneId)
        // ZoneEvent.zoneId ma ForeignKey ON DELETE CASCADE — baza usuwa zdarzenia automatycznie
    }

    suspend fun getZoneById(id: String): Zone? = dao.getZoneById(id)

    /** Pobiera wszystkie aktywne strefy (jednorazowe odczytanie, nie Flow). */
    suspend fun getActiveZones(): List<Zone> = dao.getActiveZones()

    // ------------------------------------------------------------------ Aktywacja

    /** Włącza lub wyłącza strefę bez zmiany innych pól. */
    suspend fun setActive(zone: Zone, active: Boolean) {
        Log.d(TAG, "setActive: ${zone.name} → $active")
        dao.updateZone(zone.copy(isActive = active))
    }

    // ------------------------------------------------------------------ Aktualizacja węzłów

    /** Zastępuje listę monitorowanych węzłów dla danej strefy. */
    suspend fun updateWatchedNodes(zone: Zone, nodeIds: List<String>) {
        val updated = zone.copy(
            watchedNodeIdsJson = Zone.serializeNodeIds(nodeIds)
        )
        dao.updateZone(updated)
        Log.d(TAG, "updateWatchedNodes: ${zone.name} → ${nodeIds.size} węzłów")
    }

    /** Aktualizuje wierzchołki wielokąta. */
    suspend fun updateVertices(zone: Zone, vertices: List<ZoneVertex>) {
        val updated = zone.copy(
            verticesJson = Zone.serializeVertices(vertices)
        )
        dao.updateZone(updated)
        Log.d(TAG, "updateVertices: ${zone.name} → ${vertices.size} wierzchołków")
    }

    // ------------------------------------------------------------------ Log zdarzeń

    /**
     * Zapisuje zdarzenie wejścia/wyjścia węzła ze strefy.
     * @param nodeName snapshot nazwy węzła — przechowywany razem ze zdarzeniem
     */
    suspend fun recordEvent(
        zoneId: String,
        nodeId: String,
        nodeName: String,
        type: ZoneEventType
    ) {
        val event = ZoneEvent(
            zoneId = zoneId,
            nodeId = nodeId,
            nodeName = nodeName,
            eventType = type.name,
            timestampSeconds = (System.currentTimeMillis() / 1000).toInt()
        )
        dao.insertEvent(event)
        Log.d(TAG, "recordEvent: $nodeId ${type.name} strefę $zoneId")
    }

    suspend fun clearEventsForZone(zoneId: String) {
        dao.clearEventsForZone(zoneId)
        Log.d(TAG, "clearEventsForZone: $zoneId")
    }

    suspend fun countEventsForZone(zoneId: String): Int =
        dao.countEventsForZone(zoneId)
}
