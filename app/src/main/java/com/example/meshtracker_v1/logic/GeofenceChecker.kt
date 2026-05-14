package com.example.meshtracker_v1.logic

import com.example.meshtracker_v1.model.MeshNodeInfo
import com.example.meshtracker_v1.model.Zone
import com.example.meshtracker_v1.model.ZoneVertex

/**
 * Algorytm ray-casting do wykrywania czy punkt leży wewnątrz wielokąta.
 *
 * Zasada działania: z badanego punktu wysyłamy poziomy promień w prawo (+∞ lon).
 * Liczymy ile razy promień przecina krawędzie wielokąta.
 * Nieparzysta liczba przecięć → punkt wewnątrz; parzysta → na zewnątrz.
 *
 * Ograniczenie: nie obsługuje wielokątów przecinających antypołudnik (±180° lon).
 * W praktyce aplikacja działa na obszarze Polski/Europy — ograniczenie nieistotne.
 */
object GeofenceChecker {

    /**
     * Sprawdza czy punkt (pointLat, pointLon) leży wewnątrz wielokąta.
     * Zwraca false dla wielokątów z mniej niż 3 wierzchołkami.
     */
    fun contains(
        pointLat: Double,
        pointLon: Double,
        polygon: List<ZoneVertex>
    ): Boolean {
        if (polygon.size < 3) return false

        var inside = false
        var j = polygon.lastIndex

        for (i in polygon.indices) {
            val vi = polygon[i]
            val vj = polygon[j]

            // Czy krawędź (vj → vi) przecina poziomy promień wychodzący z punktu?
            // Warunek: jedna końcówka powyżej, druga poniżej lub równo z pointLat.
            val crossesRay = (vi.lat <= pointLat && vj.lat > pointLat) ||
                             (vj.lat <= pointLat && vi.lat > pointLat)

            if (crossesRay) {
                // Długość geograficzna przecięcia promienia z krawędzią
                val intersectLon = vi.lon +
                    (pointLat - vi.lat) / (vj.lat - vi.lat) * (vj.lon - vi.lon)

                // Przecięcie jest tylko jeśli leży na prawo od punktu
                if (pointLon < intersectLon) {
                    inside = !inside
                }
            }

            j = i
        }

        return inside
    }

    /**
     * Sprawdza czy węzeł Meshtastic leży wewnątrz strefy.
     * Zwraca false jeśli węzeł nie ma ważnej pozycji GPS lub strefa ma < 3 wierzchołków.
     */
    fun nodeInZone(node: MeshNodeInfo, zone: Zone): Boolean {
        if (!node.hasValidPosition()) return false
        val pos = node.position ?: return false
        return contains(pos.latitude, pos.longitude, zone.vertices())
    }

    /**
     * Zwraca podzbiór węzłów aktualnie będących wewnątrz strefy.
     */
    fun filterNodesInZone(
        nodes: Iterable<MeshNodeInfo>,
        zone: Zone
    ): List<MeshNodeInfo> = nodes.filter { nodeInZone(it, zone) }

    /**
     * Dla listy stref zwraca mapę nodeId → Set<zoneId> z aktualnym rozmieszczeniem węzłów.
     * Używane przez ZoneMonitorService do wykrywania zmian stanu.
     */
    fun computeNodeZoneMap(
        nodes: Iterable<MeshNodeInfo>,
        activeZones: List<Zone>
    ): Map<String, Set<String>> {
        val result = mutableMapOf<String, MutableSet<String>>()

        for (zone in activeZones) {
            val vertices = zone.vertices()
            if (vertices.size < 3) continue

            val watchedIds = zone.watchedNodeIds().toSet()

            for (node in nodes) {
                // Sprawdzaj tylko węzły z listy monitorowanych tej strefy
                if (node.getId() !in watchedIds) continue
                if (!node.hasValidPosition()) continue
                val pos = node.position ?: continue

                if (contains(pos.latitude, pos.longitude, vertices)) {
                    result.getOrPut(node.getId()) { mutableSetOf() }.add(zone.id)
                }
            }
        }

        return result
    }
}
