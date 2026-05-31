package com.example.meshtracker_v1.model

data class PacketStats(
    val nodeId: String,
    val receivedCount: Int = 0,
    val lastReceivedSeconds: Int = 0,
    val deltaTHistory: List<Int> = emptyList(),
    val sessionStartSeconds: Int = 0
) {
    val avgDeltaT: Double
        get() = if (deltaTHistory.isEmpty()) 0.0 else deltaTHistory.average()

    val minDeltaT: Int
        get() = deltaTHistory.minOrNull() ?: 0

    val maxDeltaT: Int
        get() = deltaTHistory.maxOrNull() ?: 0

    fun pdr(expectedIntervalSeconds: Int): Double {
        if (expectedIntervalSeconds <= 0 || avgDeltaT <= 0.0) return 0.0
        return (expectedIntervalSeconds / avgDeltaT).coerceAtMost(1.0) * 100.0
    }
}
