package com.example.meshtracker_v1.repository

import com.example.meshtracker_v1.model.MeshPosition
import com.example.meshtracker_v1.model.TimedPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.sqrt

@Singleton
class PositionHistoryRepository @Inject constructor() {

    private val _history = MutableStateFlow<Map<String, List<TimedPosition>>>(emptyMap())
    val history: StateFlow<Map<String, List<TimedPosition>>> = _history.asStateFlow()

    fun record(nodeId: String, position: MeshPosition, maxPoints: Int, minDistanceM: Int) {
        val current = _history.value
        val nodeHistory = current[nodeId]?.toMutableList() ?: mutableListOf()

        val last = nodeHistory.lastOrNull()
        if (last != null) {
            val dist = approximateDistanceMeters(
                last.latitude, last.longitude,
                position.latitude, position.longitude
            )
            if (dist < minDistanceM) return
        }

        val ts = if (position.time > 0) position.time
                 else (System.currentTimeMillis() / 1000).toInt()

        nodeHistory.add(TimedPosition(position.latitude, position.longitude, position.altitude, ts))

        while (nodeHistory.size > maxPoints) nodeHistory.removeAt(0)

        _history.value = current.toMutableMap().also { it[nodeId] = nodeHistory }
    }

    fun clearAll() { _history.value = emptyMap() }

    private fun approximateDistanceMeters(
        lat1: Double, lon1: Double, lat2: Double, lon2: Double
    ): Double {
        val dLat = lat2 - lat1
        val dLon = lon2 - lon1
        val cosLat = cos(Math.toRadians((lat1 + lat2) / 2))
        return sqrt(
            dLat * dLat * 111_000.0 * 111_000.0 +
            dLon * dLon * (111_000.0 * cosLat) * (111_000.0 * cosLat)
        )
    }
}
