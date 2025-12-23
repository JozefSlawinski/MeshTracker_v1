package com.example.meshtracker_v1.model

import com.google.android.gms.maps.model.LatLng

/**
 * Wrapper dla pozycji GPS z Meshtastic.
 * Używa klasy z pakietu org.meshtastic.core.model.Position.
 */
data class MeshPosition(
    val latitude: Double,
    val longitude: Double,
    val altitude: Int,
    val time: Int = 0,
    val satellitesInView: Int = 0,
    val groundSpeed: Int = 0,
    val groundTrack: Int = 0,
    val precisionBits: Int = 0
) {
    /**
     * Sprawdza czy pozycja jest prawidłowa (nie jest 0,0).
     */
    fun isValid(): Boolean {
        return latitude != 0.0 || longitude != 0.0
    }
    
    /**
     * Sprawdza czy pozycja jest w prawidłowym zakresie geograficznym.
     */
    fun isInRange(): Boolean {
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }
    
    /**
     * Konwertuje pozycję do LatLng dla Google Maps.
     */
    fun toLatLng(): LatLng {
        return LatLng(latitude, longitude)
    }
    
    companion object {
        /**
         * Tworzy MeshPosition z org.meshtastic.core.model.Position.
         * Używa reflection, jeśli klasa nie jest dostępna bezpośrednio.
         */
        fun fromMeshtasticPosition(position: Any?): MeshPosition? {
            if (position == null) {
                android.util.Log.d("MeshPosition", "Position object is null")
                return null
            }
            
            android.util.Log.d("MeshPosition", "Parsing position, type: ${position.javaClass.name}")
            
            // Debug: List available methods
            try {
                val methods = position.javaClass.declaredMethods
                android.util.Log.d("MeshPosition", "Available methods: ${methods.map { it.name }.joinToString(", ")}")
            } catch (e: Exception) {
                android.util.Log.w("MeshPosition", "Could not list methods: ${e.message}")
            }
            
            return try {
                val lat = try {
                    val method = position.javaClass.getMethod("getLatitude")
                    val value = method.invoke(position)
                    when (value) {
                        is Double -> value
                        is Float -> value.toDouble()
                        else -> (value as? Number)?.toDouble() ?: 0.0
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MeshPosition", "Error getting latitude: ${e.message}")
                    0.0
                }
                
                val lng = try {
                    val method = position.javaClass.getMethod("getLongitude")
                    val value = method.invoke(position)
                    when (value) {
                        is Double -> value
                        is Float -> value.toDouble()
                        else -> (value as? Number)?.toDouble() ?: 0.0
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MeshPosition", "Error getting longitude: ${e.message}")
                    0.0
                }
                
                android.util.Log.d("MeshPosition", "Parsed lat=$lat, lng=$lng")
                
                val alt = try {
                    val method = position.javaClass.getMethod("getAltitude")
                    val value = method.invoke(position)
                    when (value) {
                        is Int -> value
                        is Long -> value.toInt()
                        else -> (value as? Number)?.toInt() ?: 0
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MeshPosition", "Error getting altitude: ${e.message}")
                    0
                }
                
                val time = try {
                    val method = position.javaClass.getMethod("getTime")
                    val value = method.invoke(position)
                    when (value) {
                        is Int -> value
                        is Long -> value.toInt()
                        else -> (value as? Number)?.toInt() ?: 0
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MeshPosition", "Error getting time: ${e.message}")
                    0
                }
                
                val satellites = try {
                    val method = position.javaClass.getMethod("getSatellitesInView")
                    val value = method.invoke(position)
                    when (value) {
                        is Int -> value
                        is Long -> value.toInt()
                        else -> (value as? Number)?.toInt() ?: 0
                    }
                } catch (e: Exception) {
                    0
                }
                
                val speed = try {
                    val method = position.javaClass.getMethod("getGroundSpeed")
                    val value = method.invoke(position)
                    when (value) {
                        is Int -> value
                        is Long -> value.toInt()
                        else -> (value as? Number)?.toInt() ?: 0
                    }
                } catch (e: Exception) {
                    0
                }
                
                val track = try {
                    val method = position.javaClass.getMethod("getGroundTrack")
                    val value = method.invoke(position)
                    when (value) {
                        is Int -> value
                        is Long -> value.toInt()
                        else -> (value as? Number)?.toInt() ?: 0
                    }
                } catch (e: Exception) {
                    0
                }
                
                val precision = try {
                    val method = position.javaClass.getMethod("getPrecisionBits")
                    val value = method.invoke(position)
                    when (value) {
                        is Int -> value
                        is Long -> value.toInt()
                        else -> (value as? Number)?.toInt() ?: 0
                    }
                } catch (e: Exception) {
                    0
                }
                
                val result = MeshPosition(lat, lng, alt, time, satellites, speed, track, precision)
                android.util.Log.d("MeshPosition", "Created MeshPosition: valid=${result.isValid()}, inRange=${result.isInRange()}")
                result
            } catch (e: Exception) {
                android.util.Log.e("MeshPosition", "Error parsing position", e)
                null
            }
        }
    }
}

