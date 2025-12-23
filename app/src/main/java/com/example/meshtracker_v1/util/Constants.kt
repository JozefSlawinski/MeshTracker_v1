package com.example.meshtracker_v1.util

/**
 * Stałe używane do komunikacji z aplikacją Meshtastic.
 */
object Constants {
    // Akcje broadcastów
    const val ACTION_NODE_CHANGE = "com.geeksville.mesh.NODE_CHANGE"
    const val ACTION_MESH_CONNECTED = "com.geeksville.mesh.MESH_CONNECTED"
    const val ACTION_MESH_DISCONNECTED = "com.geeksville.mesh.MESH_DISCONNECTED"
    
    // Extras dla intentów
    const val EXTRA_NODE_INFO = "com.geeksville.mesh.NodeInfo"
    const val EXTRA_CONNECTED = "com.geeksville.mesh.Connected"
    const val EXTRA_DISCONNECTED = "com.geeksville.mesh.disconnected"
    
    // Stany połączenia
    const val STATE_CONNECTED = "CONNECTED"
    const val STATE_DISCONNECTED = "DISCONNECTED"
    
    // Pakiet aplikacji Meshtastic
    const val MESHTASTIC_PACKAGE = "com.geeksville.mesh"
    
    // Akcja serwisu Meshtastic
    const val MESH_SERVICE_ACTION = "com.geeksville.mesh.service.MeshService"
}

