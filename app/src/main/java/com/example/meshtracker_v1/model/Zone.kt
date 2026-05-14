package com.example.meshtracker_v1.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Strefa geofencingu — obszar wielokątny monitorujący wybrane węzły.
 * Wierzchołki i lista węzłów przechowywane jako JSON string (brak TypeConverter).
 */
@Entity(tableName = "zones")
data class Zone(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorArgb: Int,
    val isActive: Boolean = true,
    val verticesJson: String = "[]",
    val watchedNodeIdsJson: String = "[]"
) {
    /** Zwraca wierzchołki wielokąta. */
    fun vertices(): List<ZoneVertex> = parseVertices(verticesJson)

    /** Zwraca listę ID węzłów monitorowanych przez tę strefę. */
    fun watchedNodeIds(): List<String> = parseNodeIds(watchedNodeIdsJson)

    companion object {

        /** Tworzy nową strefę z podanymi parametrami. */
        fun create(
            name: String,
            colorArgb: Int,
            vertices: List<ZoneVertex>,
            watchedNodeIds: List<String>
        ): Zone = Zone(
            id = UUID.randomUUID().toString(),
            name = name,
            colorArgb = colorArgb,
            isActive = true,
            verticesJson = serializeVertices(vertices),
            watchedNodeIdsJson = serializeNodeIds(watchedNodeIds)
        )

        fun serializeVertices(vertices: List<ZoneVertex>): String {
            val array = JSONArray()
            vertices.forEach { v ->
                array.put(JSONObject().put("lat", v.lat).put("lon", v.lon))
            }
            return array.toString()
        }

        fun parseVertices(json: String): List<ZoneVertex> = try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                ZoneVertex(lat = obj.getDouble("lat"), lon = obj.getDouble("lon"))
            }
        } catch (_: Exception) { emptyList() }

        fun serializeNodeIds(ids: List<String>): String {
            val array = JSONArray()
            ids.forEach { array.put(it) }
            return array.toString()
        }

        fun parseNodeIds(json: String): List<String> = try {
            val array = JSONArray(json)
            (0 until array.length()).map { array.getString(it) }
        } catch (_: Exception) { emptyList() }

        /** Predefiniowane kolory stref (ARGB packed Int). */
        val PRESET_COLORS: List<Int> = listOf(
            0xFF2196F3.toInt(), // Niebieski
            0xFFE53935.toInt(), // Czerwony
            0xFF43A047.toInt(), // Zielony
            0xFFFF9800.toInt(), // Pomarańczowy
            0xFF9C27B0.toInt()  // Fioletowy
        )
    }
}

/** Pojedynczy wierzchołek wielokąta strefy. */
data class ZoneVertex(val lat: Double, val lon: Double)
