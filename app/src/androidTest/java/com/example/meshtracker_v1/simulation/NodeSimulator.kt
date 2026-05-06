package com.example.meshtracker_v1.simulation

import com.example.meshtracker_v1.fake.FakeMeshRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Silnik symulacji ruchu węzłów Meshtastic.
 *
 * Uruchamia coroutine dla każdej [WaypointRoute] i cyklicznie emituje
 * aktualizacje pozycji przez [FakeMeshRepository].
 *
 * Przykład użycia w teście:
 * ```kotlin
 * val simulator = NodeSimulator(fakeRepository, scope = testScope)
 * simulator.runScenario(PredefinedScenarios.movingPatrol())
 * advanceTimeBy(10_000)
 * simulator.stopAll()
 * ```
 */
class NodeSimulator(
    private val repository: FakeMeshRepository,
    private val scope: CoroutineScope
) {
    private val jobs = mutableListOf<Job>()

    /**
     * Uruchamia symulację dla pojedynczej trasy.
     * @return [Job] pozwalający anulować tylko ten węzeł.
     */
    fun runRoute(route: WaypointRoute): Job {
        // Natychmiast dodaj węzeł w punkcie startowym
        repository.emitNodeChanged(route.initialNode)

        val job = scope.launch {
            if (route.waypoints.size <= 1) {
                // Statyczny węzeł — nie ma czego animować
                return@launch
            }

            var index = 0
            while (isActive) {
                delay(route.intervalMs)
                if (!isActive) break

                index++
                val wrappedIndex = if (route.loop) {
                    index % route.waypoints.size
                } else {
                    if (index >= route.waypoints.size) break
                    index
                }
                repository.emitNodeChanged(route.nodeAtWaypoint(wrappedIndex))
            }
        }
        jobs.add(job)
        return job
    }

    /**
     * Uruchamia symulację dla listy tras (np. pełny scenariusz).
     * @return lista [Job]ów — po jednym na trasę.
     */
    fun runScenario(routes: List<WaypointRoute>): List<Job> =
        routes.map { runRoute(it) }

    /**
     * Anuluje wszystkie aktywne symulacje.
     */
    fun stopAll() {
        jobs.forEach { it.cancel() }
        jobs.clear()
    }
}
