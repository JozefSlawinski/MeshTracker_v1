package com.example.meshtracker_v1.mapper

import com.example.meshtracker_v1.model.MeshNodeInfo
import com.google.android.gms.maps.model.MarkerOptions

/**
 * Mapper do konwersji MeshNodeInfo na MarkerOptions dla Google Maps.
 */
object NodeToMarkerMapper {
    
    /**
     * Konwertuje węzeł na MarkerOptions.
     */
    fun nodeToMarker(nodeInfo: MeshNodeInfo): MarkerOptions {
        val position = nodeInfo.position ?: throw IllegalArgumentException("Node has no position")
        val latLng = position.toLatLng()
        
        val title = nodeInfo.getDisplayName()
        val snippet = buildSnippet(nodeInfo)
        
        return MarkerOptions()
            .position(latLng)
            .title(title)
            .snippet(snippet)
    }
    
    /**
     * Buduje snippet z dodatkowymi informacjami o węźle.
     */
    private fun buildSnippet(nodeInfo: MeshNodeInfo): String {
        val parts = mutableListOf<String>()
        
        // Status online/offline
        if (nodeInfo.isOnline()) {
            parts.add("Online")
        } else {
            parts.add("Offline")
        }
        
        // Bateria
        if (nodeInfo.batteryLevel > 0) {
            parts.add("Battery: ${nodeInfo.batteryLevel}%")
        }
        
        // SNR
        if (nodeInfo.snr != Float.MAX_VALUE) {
            parts.add("SNR: ${String.format("%.1f", nodeInfo.snr)} dB")
        }
        
        // RSSI
        if (nodeInfo.rssi != Int.MAX_VALUE) {
            parts.add("RSSI: ${nodeInfo.rssi} dBm")
        }
        
        // Hops away
        if (nodeInfo.hopsAway > 0) {
            parts.add("Hops: ${nodeInfo.hopsAway}")
        }
        
        return parts.joinToString(" • ")
    }
}

