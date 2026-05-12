package com.example.meshtracker_v1.repository

import com.example.meshtracker_v1.model.MeshNodeInfo

/**
 * Abstrakcja warstwy danych Meshtastic.
 * Izoluje ViewModel od konkretnej implementacji (serwis / fake).
 */
interface MeshRepository {

    /** @return true jeśli aplikacja Meshtastic jest zainstalowana na urządzeniu. */
    fun isMeshtasticInstalled(): Boolean

    /** Nawiązuje połączenie z serwisem Meshtastic. @return true jeśli binding się rozpoczął. */
    fun connect(): Boolean

    /** Rozłącza się od serwisu. */
    fun disconnect()

    /** @return true jeśli serwis jest zbindowany i dostępny. */
    fun isConnected(): Boolean

    /** @return lista wszystkich węzłów lub null przy błędzie. */
    fun getNodes(): List<MeshNodeInfo>?

    /** @return ID własnego węzła lub null. */
    fun getMyNodeId(): String?

    /** @return stan połączenia radia ("CONNECTED" / "DISCONNECTED") lub null. */
    fun getConnectionState(): String?

    /** Rejestruje odbiorcę zdarzeń. */
    fun addListener(listener: MeshEventListener)

    /** Wyrejestrowuje odbiorcę zdarzeń. */
    fun removeListener(listener: MeshEventListener)

    /**
     * Zunifikowany listener łączący zdarzenia serwisu i radia.
     */
    interface MeshEventListener {
        fun onServiceConnected()
        fun onServiceDisconnected()
        fun onNodeChanged(nodeInfo: MeshNodeInfo)
        fun onMeshConnected()
        fun onMeshDisconnected()
    }
}
