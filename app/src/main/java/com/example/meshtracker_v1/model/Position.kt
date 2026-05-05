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
    
    /**
     * Zwraca kierunek (heading) w stopniach (0-360, gdzie 0 to północ).
     * Alias dla groundTrack.
     */
    val heading: Int
        get() = groundTrack
    
    /**
     * Zwraca prędkość w m/s.
     * Alias dla groundSpeed.
     */
    val speed: Int
        get() = groundSpeed
    
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
                
                // Spróbuj odczytać wszystkie pola pozycji, aby zobaczyć surowe wartości
                fields.forEach { field ->
                    try {
                        field.isAccessible = true
                        val value = field.get(position)
                        android.util.Log.d("MeshPosition", "Field '${field.name}': $value (type: ${field.type.name})")
                    } catch (e: Exception) {
                        // Ignoruj błędy dostępu do pól
                    }
                }
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
                        is Double -> {
                            // Jeśli wartość jest Double, sprawdź czy wygląda na format E7
                            // Wartości w formacie E7 dla Polski (52.0, 19.0) to około 520000000, 190000000
                            // Wartości w stopniach to około 52.0, 19.0
                            if (Math.abs(value) > 1000.0) {
                                android.util.Log.w("MeshPosition", "WARNING: Latitude value ($value) looks like E7 format but is Double! Dividing by 1e7.")
                                value / 1e7
                            } else {
                                value
                            }
                        }
                        is Float -> {
                            val doubleValue = value.toDouble()
                            if (Math.abs(doubleValue) > 1000.0) {
                                android.util.Log.w("MeshPosition", "WARNING: Latitude value ($doubleValue) looks like E7 format but is Float! Dividing by 1e7.")
                                doubleValue / 1e7
                            } else {
                                doubleValue
                            }
                        }
                        is Int -> {
                            android.util.Log.d("MeshPosition", "Latitude is Int, dividing by 1e7: $value -> ${value / 1e7}")
                            value / 1e7  // Meshtastic stores as degrees * 1e7
                        }
                        is Long -> {
                            android.util.Log.d("MeshPosition", "Latitude is Long, dividing by 1e7: $value -> ${value / 1e7}")
                            value / 1e7
                        }
                        else -> {
                            val numValue = (value as? Number)?.toDouble() ?: 0.0
                            if (Math.abs(numValue) > 1000.0) {
                                android.util.Log.w("MeshPosition", "WARNING: Latitude value ($numValue) looks like E7 format! Dividing by 1e7.")
                                numValue / 1e7
                            } else {
                                numValue
                            }
                        }
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
                            is Double -> {
                                if (Math.abs(value) > 1000.0) {
                                    android.util.Log.w("MeshPosition", "WARNING: Latitude field value ($value) looks like E7 format but is Double! Dividing by 1e7.")
                                    value / 1e7
                                } else {
                                    value
                                }
                            }
                            is Float -> {
                                val doubleValue = value.toDouble()
                                if (Math.abs(doubleValue) > 1000.0) {
                                    android.util.Log.w("MeshPosition", "WARNING: Latitude field value ($doubleValue) looks like E7 format but is Float! Dividing by 1e7.")
                                    doubleValue / 1e7
                                } else {
                                    doubleValue
                                }
                            }
                            is Int -> {
                                android.util.Log.d("MeshPosition", "Latitude field is Int, dividing by 1e7: $value -> ${value / 1e7}")
                                value / 1e7  // Meshtastic stores as degrees * 1e7
                            }
                            is Long -> {
                                android.util.Log.d("MeshPosition", "Latitude field is Long, dividing by 1e7: $value -> ${value / 1e7}")
                                value / 1e7
                            }
                            else -> {
                                val numValue = (value as? Number)?.toDouble() ?: 0.0
                                if (Math.abs(numValue) > 1000.0) {
                                    android.util.Log.w("MeshPosition", "WARNING: Latitude field value ($numValue) looks like E7 format! Dividing by 1e7.")
                                    numValue / 1e7
                                } else {
                                    numValue
                                }
                            }
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
                        is Double -> {
                            // Jeśli wartość jest Double, sprawdź czy wygląda na format E7
                            // Wartości w formacie E7 dla Polski (52.0, 19.0) to około 520000000, 190000000
                            // Wartości w stopniach to około 52.0, 19.0
                            if (Math.abs(value) > 1000.0) {
                                android.util.Log.w("MeshPosition", "WARNING: Longitude value ($value) looks like E7 format but is Double! Dividing by 1e7.")
                                value / 1e7
                            } else {
                                value
                            }
                        }
                        is Float -> {
                            val doubleValue = value.toDouble()
                            if (Math.abs(doubleValue) > 1000.0) {
                                android.util.Log.w("MeshPosition", "WARNING: Longitude value ($doubleValue) looks like E7 format but is Float! Dividing by 1e7.")
                                doubleValue / 1e7
                            } else {
                                doubleValue
                            }
                        }
                        is Int -> {
                            android.util.Log.d("MeshPosition", "Longitude is Int, dividing by 1e7: $value -> ${value / 1e7}")
                            value / 1e7  // Meshtastic stores as degrees * 1e7
                        }
                        is Long -> {
                            android.util.Log.d("MeshPosition", "Longitude is Long, dividing by 1e7: $value -> ${value / 1e7}")
                            value / 1e7
                        }
                        else -> {
                            val numValue = (value as? Number)?.toDouble() ?: 0.0
                            if (Math.abs(numValue) > 1000.0) {
                                android.util.Log.w("MeshPosition", "WARNING: Longitude value ($numValue) looks like E7 format! Dividing by 1e7.")
                                numValue / 1e7
                            } else {
                                numValue
                            }
                        }
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
                            is Double -> {
                                if (Math.abs(value) > 1000.0) {
                                    android.util.Log.w("MeshPosition", "WARNING: Longitude field value ($value) looks like E7 format but is Double! Dividing by 1e7.")
                                    value / 1e7
                                } else {
                                    value
                                }
                            }
                            is Float -> {
                                val doubleValue = value.toDouble()
                                if (Math.abs(doubleValue) > 1000.0) {
                                    android.util.Log.w("MeshPosition", "WARNING: Longitude field value ($doubleValue) looks like E7 format but is Float! Dividing by 1e7.")
                                    doubleValue / 1e7
                                } else {
                                    doubleValue
                                }
                            }
                            is Int -> {
                                android.util.Log.d("MeshPosition", "Longitude field is Int, dividing by 1e7: $value -> ${value / 1e7}")
                                value / 1e7  // Meshtastic stores as degrees * 1e7
                            }
                            is Long -> {
                                android.util.Log.d("MeshPosition", "Longitude field is Long, dividing by 1e7: $value -> ${value / 1e7}")
                                value / 1e7
                            }
                            else -> {
                                val numValue = (value as? Number)?.toDouble() ?: 0.0
                                if (Math.abs(numValue) > 1000.0) {
                                    android.util.Log.w("MeshPosition", "WARNING: Longitude field value ($numValue) looks like E7 format! Dividing by 1e7.")
                                    numValue / 1e7
                                } else {
                                    numValue
                                }
                            }
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
                
                // Sprawdź czy pozycja jest w rozsądnym zakresie (może wskazywać na błąd parsowania)
                if (lat != 0.0 && lng != 0.0) {
                    if (lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0) {
                        android.util.Log.w("MeshPosition", "WARNING: Position out of valid range! lat=$lat, lng=$lng")
                    }
                    // Sprawdź czy pozycja wygląda na nieprawidłową (np. bardzo małe wartości mogą wskazywać na błąd konwersji)
                    if (Math.abs(lat) < 0.0001 || Math.abs(lng) < 0.0001) {
                        android.util.Log.w("MeshPosition", "WARNING: Position values are very small, might indicate parsing error!")
                    }
                }
                
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
                    // Spróbuj najpierw getGroundSpeed(), potem getSpeed()
                    val method = try {
                        position.javaClass.getMethod("getGroundSpeed")
                    } catch (e: NoSuchMethodException) {
                        position.javaClass.getMethod("getSpeed")
                    }
                    val value = method.invoke(position)
                    when (value) {
                        is Int -> value
                        is Long -> value.toInt()
                        else -> (value as? Number)?.toInt() ?: 0
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MeshPosition", "Error getting speed: ${e.message}")
                    0
                }
                
                val track = try {
                    // Spróbuj najpierw getGroundTrack(), potem getHeading()
                    val method = try {
                        position.javaClass.getMethod("getGroundTrack")
                    } catch (e: NoSuchMethodException) {
                        try {
                            position.javaClass.getMethod("getHeading")
                        } catch (e2: NoSuchMethodException) {
                            position.javaClass.getMethod("getTrack")
                        }
                    }
                    val value = method.invoke(position)
                    val rawValue = when (value) {
                        is Int -> value
                        is Long -> value.toInt()
                        else -> (value as? Number)?.toInt() ?: 0
                    }
                    
                    android.util.Log.d("MeshPosition", "Raw groundTrack from method: $rawValue")
                    
                    // Meshtastic przechowuje heading w setnych stopniach (pomnożone przez 100000)
                    // Jeśli wartość jest większa niż 360, musimy ją podzielić
                    if (rawValue > 360) {
                        android.util.Log.d("MeshPosition", "GroundTrack value ($rawValue) looks like it's in hundredths of degrees, dividing by 100000")
                        val converted = (rawValue / 100000.0).toInt()
                        android.util.Log.d("MeshPosition", "Converted groundTrack: $rawValue -> $converted degrees")
                        converted
                    } else {
                        android.util.Log.d("MeshPosition", "GroundTrack value ($rawValue) is already in degrees")
                        rawValue
                    }
                    } catch (e: NoSuchMethodException) {
                    // Spróbuj odczytać z pola bezpośrednio
                    try {
                        val trackField = position.javaClass.getDeclaredField("groundTrack")
                        trackField.isAccessible = true
                        val value = trackField.get(position)
                        val rawValue = when (value) {
                            is Int -> value
                            is Long -> value.toInt()
                            else -> (value as? Number)?.toInt() ?: 0
                        }
                        android.util.Log.d("MeshPosition", "Got groundTrack from field: $rawValue (raw value from Meshtastic)")
                        
                        // Meshtastic przechowuje heading w setnych stopniach (pomnożone przez 100000)
                        if (rawValue > 360) {
                            android.util.Log.d("MeshPosition", "GroundTrack field value ($rawValue) looks like it's in hundredths of degrees, dividing by 100000")
                            val converted = (rawValue / 100000.0).toInt()
                            android.util.Log.d("MeshPosition", "Converted groundTrack from field: $rawValue -> $converted degrees")
                            converted
                        } else {
                            android.util.Log.d("MeshPosition", "GroundTrack field value ($rawValue) is already in degrees")
                            rawValue
                        }
                    } catch (e2: Exception) {
                        android.util.Log.w("MeshPosition", "Error getting track/heading (method and field): ${e.message}, ${e2.message}")
                        0
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MeshPosition", "Error getting track/heading: ${e.message}")
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
                android.util.Log.d("MeshPosition", "Created MeshPosition: valid=${result.isValid()}, inRange=${result.isInRange()}, speed=${result.groundSpeed} m/s, heading=${result.groundTrack}°")
                result
            } catch (e: Exception) {
                android.util.Log.e("MeshPosition", "Error parsing position", e)
                null
            }
        }
    }
}

