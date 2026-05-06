package com.example.meshtracker_v1.simulation

import com.example.meshtracker_v1.model.MeshNodeInfo
import com.example.meshtracker_v1.model.MeshPosition
import com.example.meshtracker_v1.model.MeshUserInfo

/**
 * Pojedynczy punkt geograficzny używany w trasach symulacji.
 *
 * @param lat szerokość geograficzna (stopnie dziesiętne)
 * @param lon długość geograficzna (stopnie dziesiętne)
 * @param alt wysokość w metrach n.p.m.
 */
data class GeoPoint(
    val lat: Double,
    val lon: Double,
    val alt: Int = 0
)

/**
 * Trasa symulowanego węzła — lista punktów GPS do przejścia w kolejności.
 *
 * @param nodeId  unikalny ID węzła (format Meshtastic: "!xxxxxxxx")
 * @param nodeName  długa nazwa wyświetlana
 * @param shortName  krótka nazwa (maks. 4 znaki)
 * @param waypoints  lista punktów do przejścia; jeśli jeden — węzeł jest statyczny
 * @param intervalMs  czas między kolejnymi krokami w milisekundach
 * @param loop  czy po osiągnięciu ostatniego punktu wrócić do pierwszego
 */
data class WaypointRoute(
    val nodeId: String,
    val nodeName: String,
    val shortName: String = nodeName.take(4).uppercase(),
    val waypoints: List<GeoPoint>,
    val intervalMs: Long = 2_000L,
    val loop: Boolean = false
) {
    init {
        require(waypoints.isNotEmpty()) { "WaypointRoute wymaga co najmniej jednego punktu" }
    }

    /**
     * Buduje [MeshNodeInfo] dla pierwszego punktu trasy.
     * Używane do inicjalnego dodania węzła przed uruchomieniem symulatora.
     */
    val initialNode: MeshNodeInfo
        get() {
            val first = waypoints.first()
            return MeshNodeInfo(
                num = nodeId.hashCode(),
                user = MeshUserInfo(
                    id = nodeId,
                    longName = nodeName,
                    shortName = shortName
                ),
                position = MeshPosition(
                    latitude = first.lat,
                    longitude = first.lon,
                    altitude = first.alt,
                    time = (System.currentTimeMillis() / 1000).toInt()
                ),
                lastHeard = (System.currentTimeMillis() / 1000).toInt()
            )
        }

    /**
     * Buduje [MeshNodeInfo] dla danego punktu trasy.
     */
    fun nodeAtWaypoint(index: Int): MeshNodeInfo {
        val wp = waypoints[index % waypoints.size]
        return MeshNodeInfo(
            num = nodeId.hashCode(),
            user = MeshUserInfo(
                id = nodeId,
                longName = nodeName,
                shortName = shortName
            ),
            position = MeshPosition(
                latitude = wp.lat,
                longitude = wp.lon,
                altitude = wp.alt,
                time = (System.currentTimeMillis() / 1000).toInt()
            ),
            lastHeard = (System.currentTimeMillis() / 1000).toInt()
        )
    }
}
