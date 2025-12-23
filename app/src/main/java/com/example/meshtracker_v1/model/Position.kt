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
            if (position == null) return null
            
            return try {
                val lat = position.javaClass.getMethod("getLatitude").invoke(position) as? Double ?: 0.0
                val lng = position.javaClass.getMethod("getLongitude").invoke(position) as? Double ?: 0.0
                val alt = (position.javaClass.getMethod("getAltitude").invoke(position) as? Int) ?: 0
                val time = (position.javaClass.getMethod("getTime").invoke(position) as? Int) ?: 0
                val satellites = (position.javaClass.getMethod("getSatellitesInView").invoke(position) as? Int) ?: 0
                val speed = (position.javaClass.getMethod("getGroundSpeed").invoke(position) as? Int) ?: 0
                val track = (position.javaClass.getMethod("getGroundTrack").invoke(position) as? Int) ?: 0
                val precision = (position.javaClass.getMethod("getPrecisionBits").invoke(position) as? Int) ?: 0
                
                MeshPosition(lat, lng, alt, time, satellites, speed, track, precision)
            } catch (e: Exception) {
                null
            }
        }
    }
}

