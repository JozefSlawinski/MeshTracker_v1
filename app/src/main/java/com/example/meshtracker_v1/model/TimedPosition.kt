package com.example.meshtracker_v1.model

data class TimedPosition(
    val latitude: Double,
    val longitude: Double,
    val altitude: Int,
    val timestampSeconds: Int,
    val snr: Float = Float.MAX_VALUE,
    val rssi: Int = Int.MAX_VALUE
)
