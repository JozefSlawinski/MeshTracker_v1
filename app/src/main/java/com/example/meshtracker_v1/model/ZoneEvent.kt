package com.example.meshtracker_v1.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Typ zdarzenia geofencingu. */
enum class ZoneEventType { ENTER, EXIT }

/**
 * Zdarzenie wejścia lub wyjścia węzła ze strefy.
 * Klucz obcy do Zone z CASCADE delete — usunięcie strefy usuwa jej log.
 */
@Entity(
    tableName = "zone_events",
    foreignKeys = [
        ForeignKey(
            entity = Zone::class,
            parentColumns = ["id"],
            childColumns = ["zoneId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("zoneId")]
)
data class ZoneEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val zoneId: String,
    val nodeId: String,
    val nodeName: String,         // snapshot nazwy węzła w chwili zdarzenia
    val eventType: String,        // ZoneEventType.name: "ENTER" lub "EXIT"
    val timestampSeconds: Int,
    val lat: Double,              // pozycja węzła w chwili zdarzenia
    val lon: Double
) {
    fun type(): ZoneEventType = ZoneEventType.valueOf(eventType)
}
