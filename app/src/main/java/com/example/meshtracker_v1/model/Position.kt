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
            
            // Debug: List available methods and fields
            try {
                val declaredMethods = position.javaClass.declaredMethods
                val allMethods = position.javaClass.methods
                val fields = position.javaClass.declaredFields
                android.util.Log.d("MeshPosition", "Declared methods: ${declaredMethods.map { it.name }.joinToString(", ")}")
                android.util.Log.d("MeshPosition", "All methods: ${allMethods.map { it.name }.joinToString(", ")}")
                android.util.Log.d("MeshPosition", "Declared fields: ${fields.map { it.name }.joinToString(", ")}")
            } catch (e: Exception) {
                android.util.Log.w("MeshPosition", "Could not list methods/fields: ${e.message}")
            }
            
            return try {
                val lat = try {
                    // Try getMethod first (includes inherited)
                    val method = position.javaClass.getMethod("getLatitude")
                    val value = method.invoke(position)
                    android.util.Log.d("MeshPosition", "getLatitude() returned: $value (type: ${value?.javaClass?.name})")
                    val result = when (value) {
                        is Double -> value
                        is Float -> value.toDouble()
                        is Int -> value / 1e7  // Meshtastic stores as degrees * 1e7
                        is Long -> value / 1e7
                        else -> (value as? Number)?.toDouble() ?: 0.0
                    }
                    android.util.Log.d("MeshPosition", "Converted latitude: $result")
                    result
                } catch (e: NoSuchMethodException) {
                    try {
                        // Try as a field
                        val latField = position.javaClass.getDeclaredField("latitude")
                        latField.isAccessible = true
                        val value = latField.get(position)
                        android.util.Log.d("MeshPosition", "latitude field value: $value (type: ${value?.javaClass?.name})")
                        val result = when (value) {
                            is Double -> value
                            is Float -> value.toDouble()
                            is Int -> value / 1e7  // Meshtastic stores as degrees * 1e7
                            is Long -> value / 1e7
                            else -> (value as? Number)?.toDouble() ?: 0.0
                        }
                        android.util.Log.d("MeshPosition", "Converted latitude from field: $result")
                        result
                    } catch (e2: Exception) {
                        android.util.Log.w("MeshPosition", "Error getting latitude (method and field): ${e.message}, ${e2.message}")
                        0.0
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MeshPosition", "Error getting latitude: ${e.message}")
                    0.0
                }
                
                val lng = try {
                    // Try getMethod first (includes inherited)
                    val method = position.javaClass.getMethod("getLongitude")
                    val value = method.invoke(position)
                    android.util.Log.d("MeshPosition", "getLongitude() returned: $value (type: ${value?.javaClass?.name})")
                    val result = when (value) {
                        is Double -> value
                        is Float -> value.toDouble()
                        is Int -> value / 1e7  // Meshtastic stores as degrees * 1e7
                        is Long -> value / 1e7
                        else -> (value as? Number)?.toDouble() ?: 0.0
                    }
                    android.util.Log.d("MeshPosition", "Converted longitude: $result")
                    result
                } catch (e: NoSuchMethodException) {
                    try {
                        // Try as a field
                        val lngField = position.javaClass.getDeclaredField("longitude")
                        lngField.isAccessible = true
                        val value = lngField.get(position)
                        android.util.Log.d("MeshPosition", "longitude field value: $value (type: ${value?.javaClass?.name})")
                        val result = when (value) {
                            is Double -> value
                            is Float -> value.toDouble()
                            is Int -> value / 1e7  // Meshtastic stores as degrees * 1e7
                            is Long -> value / 1e7
                            else -> (value as? Number)?.toDouble() ?: 0.0
                        }
                        android.util.Log.d("MeshPosition", "Converted longitude from field: $result")
                        result
                    } catch (e2: Exception) {
                        android.util.Log.w("MeshPosition", "Error getting longitude (method and field): ${e.message}, ${e2.message}")
                        0.0
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MeshPosition", "Error getting longitude: ${e.message}")
                    0.0
                }
                
                android.util.Log.d("MeshPosition", "Final parsed lat=$lat, lng=$lng")
                
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

