package com.example.meshtracker_v1.repository

import com.example.meshtracker_v1.model.PacketStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PacketStatsRepository @Inject constructor() {

    private val _stats = MutableStateFlow<Map<String, PacketStats>>(emptyMap())
    val stats: StateFlow<Map<String, PacketStats>> = _stats.asStateFlow()

    fun record(nodeId: String, timestampSeconds: Int, maxDeltaTHistory: Int = 20) {
        val current = _stats.value.toMutableMap()
        val existing = current[nodeId]

        if (existing == null) {
            current[nodeId] = PacketStats(
                nodeId = nodeId,
                receivedCount = 1,
                lastReceivedSeconds = timestampSeconds,
                sessionStartSeconds = timestampSeconds
            )
        } else {
            val newDeltaTs = existing.deltaTHistory.toMutableList()
            if (existing.lastReceivedSeconds > 0) {
                val deltaT = timestampSeconds - existing.lastReceivedSeconds
                if (deltaT >= 5 && deltaT <= 600) {
                    newDeltaTs.add(deltaT)
                    while (newDeltaTs.size > maxDeltaTHistory) newDeltaTs.removeAt(0)
                }
            }
            current[nodeId] = existing.copy(
                receivedCount = existing.receivedCount + 1,
                lastReceivedSeconds = timestampSeconds,
                deltaTHistory = newDeltaTs
            )
        }

        _stats.value = current
    }

    fun resetAll() { _stats.value = emptyMap() }

    fun resetForNode(nodeId: String) {
        _stats.value = _stats.value.toMutableMap().also { it.remove(nodeId) }
    }
}
