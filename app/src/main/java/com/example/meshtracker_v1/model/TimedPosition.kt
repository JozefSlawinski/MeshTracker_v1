package com.example.meshtracker_v1.model

data class TimedPosition(
    val latitude: Double,
    val longitude: Double,
    val altitude: Int,
    val timestampSeconds: Int
)
