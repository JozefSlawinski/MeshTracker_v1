package com.example.meshtracker_v1.simulation

/**
 * Gotowe scenariusze symulacji — okolice Piaseczna (52.08°N, 21.02°E).
 *
 * Każdy scenariusz zwraca listę [WaypointRoute], którą możesz przekazać
 * bezpośrednio do [NodeSimulator.runScenario].
 */
object PredefinedScenarios {

    // ------------------------------------------------------------------ Scenariusz 1
    /**
     * Cztery statyczne węzły rozmieszczone wokół centrum Piaseczna.
     * Idealne do testowania renderowania markerów i listy węzłów.
     *
     * @param intervalMs jak często węzły "odświeżają" swoją pozycję (w milisekundach)
     */
    fun staticCluster(intervalMs: Long = 3_000L): List<WaypointRoute> = listOf(
        WaypointRoute(
            nodeId    = "!aabb0001",
            nodeName  = "Alfa",
            shortName = "ALF",
            // Centrum Piaseczna, okolice Rynku
            waypoints = listOf(GeoPoint(52.0810, 21.0155, 102)),
            intervalMs = intervalMs
        ),
        WaypointRoute(
            nodeId    = "!aabb0002",
            nodeName  = "Bravo",
            shortName = "BRV",
            // Okolice ul. Puławskiej
            waypoints = listOf(GeoPoint(52.0785, 21.0200, 98)),
            intervalMs = intervalMs
        ),
        WaypointRoute(
            nodeId    = "!aabb0003",
            nodeName  = "Charlie",
            shortName = "CHR",
            // Park miejski
            waypoints = listOf(GeoPoint(52.0820, 21.0210, 105)),
            intervalMs = intervalMs
        ),
        WaypointRoute(
            nodeId    = "!aabb0004",
            nodeName  = "Delta",
            shortName = "DLT",
            // Okolice dworca PKP
            waypoints = listOf(GeoPoint(52.0770, 21.0150, 96)),
            intervalMs = intervalMs
        )
    )

    // ------------------------------------------------------------------ Scenariusz 2
    /**
     * Dwa węzły patrolujące trasy po centrum Piaseczna (ruch zapętlony).
     * Idealny do testowania animacji markerów i śledzenia zmiany pozycji.
     *
     * @param intervalMs czas między krokami (domyślnie 2 s)
     */
    fun movingPatrol(intervalMs: Long = 2_000L): List<WaypointRoute> = listOf(
        WaypointRoute(
            nodeId    = "!aabb0001",
            nodeName  = "Patrol Alfa",
            shortName = "PA1",
            waypoints = listOf(
                GeoPoint(52.0810, 21.0120), // start: okolice dworca PKP
                GeoPoint(52.0815, 21.0145),
                GeoPoint(52.0820, 21.0165),
                GeoPoint(52.0825, 21.0185),
                GeoPoint(52.0820, 21.0205),
                GeoPoint(52.0810, 21.0215), // park przy ul. Kościuszki
                GeoPoint(52.0800, 21.0205),
                GeoPoint(52.0795, 21.0185),
                GeoPoint(52.0795, 21.0160),
                GeoPoint(52.0800, 21.0140)
            ),
            intervalMs = intervalMs,
            loop = true
        ),
        WaypointRoute(
            nodeId    = "!aabb0002",
            nodeName  = "Patrol Bravo",
            shortName = "PB2",
            waypoints = listOf(
                GeoPoint(52.0780, 21.0230), // start: ul. Kościuszki / Puławska
                GeoPoint(52.0775, 21.0210),
                GeoPoint(52.0770, 21.0190),
                GeoPoint(52.0765, 21.0170),
                GeoPoint(52.0765, 21.0150),
                GeoPoint(52.0770, 21.0130),
                GeoPoint(52.0778, 21.0115),
                GeoPoint(52.0783, 21.0135),
                GeoPoint(52.0783, 21.0160),
                GeoPoint(52.0780, 21.0200)
            ),
            intervalMs = intervalMs,
            loop = true
        )
    )

    // ------------------------------------------------------------------ Scenariusz 3
    /**
     * Jeden węzeł w centrum + trasy dla pozostałych trzech zbiegające się do centrum.
     * Przydatny do testów "meet-point" i zasięgu sieci mesh.
     *
     * @param intervalMs czas między krokami
     */
    fun convergingToCenter(intervalMs: Long = 1_500L): List<WaypointRoute> {
        val center = GeoPoint(52.0797, 21.0178, 100) // Rynek Piaseczna
        return listOf(
            WaypointRoute(
                nodeId    = "!aabb0001",
                nodeName  = "Centrum",
                shortName = "CTR",
                waypoints = listOf(center),
                intervalMs = intervalMs
            ),
            WaypointRoute(
                nodeId    = "!aabb0002",
                nodeName  = "Nord",
                shortName = "NRD",
                waypoints = listOf(
                    GeoPoint(52.0870, 21.0178),
                    GeoPoint(52.0850, 21.0178),
                    GeoPoint(52.0830, 21.0178),
                    center
                ),
                intervalMs = intervalMs
            ),
            WaypointRoute(
                nodeId    = "!aabb0003",
                nodeName  = "Est",
                shortName = "EST",
                waypoints = listOf(
                    GeoPoint(52.0797, 21.0320),
                    GeoPoint(52.0797, 21.0290),
                    GeoPoint(52.0797, 21.0240),
                    center
                ),
                intervalMs = intervalMs
            ),
            WaypointRoute(
                nodeId    = "!aabb0004",
                nodeName  = "Sud",
                shortName = "SUD",
                waypoints = listOf(
                    GeoPoint(52.0720, 21.0178),
                    GeoPoint(52.0745, 21.0178),
                    GeoPoint(52.0770, 21.0178),
                    center
                ),
                intervalMs = intervalMs
            )
        )
    }
}
