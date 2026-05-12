package com.example.meshtracker_v1.fake

import com.example.meshtracker_v1.model.MeshNodeInfo
import com.example.meshtracker_v1.model.MeshPosition
import com.example.meshtracker_v1.repository.MeshRepository
import com.example.meshtracker_v1.util.Constants
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Testowy dublet [MeshRepository].
 *
 * Zamiast łączyć się z prawdziwym serwisem Meshtastic, pozwala testom:
 *  - dodawać / usuwać węzły programowo,
 *  - symulować zdarzenia połączenia i rozłączenia,
 *  - emitować aktualizacje pozycji węzłów.
 */
@Singleton
class FakeMeshRepository @Inject constructor() : MeshRepository {

    // ------------------------------------------------------------------ stan wewnętrzny

    private val _nodes = mutableMapOf<String, MeshNodeInfo>()
    private val _listeners = mutableSetOf<MeshRepository.MeshEventListener>()
    private var _isConnected = false
    private var _connectionState = Constants.STATE_DISCONNECTED
    private var _myNodeId = "!fake0000"

    // ------------------------------------------------------------------ MeshRepository

    override fun isMeshtasticInstalled(): Boolean = true  // w testach zawsze "zainstalowane"

    override fun connect(): Boolean = true   // w fake, "połączenie" jest natychmiastowe

    override fun disconnect() {
        _isConnected = false
        _connectionState = Constants.STATE_DISCONNECTED
    }

    override fun isConnected(): Boolean = _isConnected

    override fun getNodes(): List<MeshNodeInfo> = _nodes.values.toList()

    override fun getMyNodeId(): String = _myNodeId

    override fun getConnectionState(): String = _connectionState

    override fun addListener(listener: MeshRepository.MeshEventListener) {
        _listeners.add(listener)
    }

    override fun removeListener(listener: MeshRepository.MeshEventListener) {
        _listeners.remove(listener)
    }

    // ------------------------------------------------------------------ API dla testów / symulatora

    /**
     * Symuluje udane połączenie z serwisem i radiem.
     * Wywołuje [MeshRepository.MeshEventListener.onServiceConnected]
     * oraz [MeshRepository.MeshEventListener.onMeshConnected].
     */
    fun emitConnected() {
        _isConnected = true
        _connectionState = Constants.STATE_CONNECTED
        _listeners.forEach { it.onServiceConnected() }
        _listeners.forEach { it.onMeshConnected() }
    }

    /**
     * Symuluje rozłączenie radia.
     */
    fun emitDisconnected() {
        _isConnected = false
        _connectionState = Constants.STATE_DISCONNECTED
        _listeners.forEach { it.onMeshDisconnected() }
        _listeners.forEach { it.onServiceDisconnected() }
    }

    /**
     * Emituje aktualizację węzła (odpowiednik broadcastu NODE_CHANGE).
     * Węzeł jest zapisywany w wewnętrznej mapie.
     */
    fun emitNodeChanged(node: MeshNodeInfo) {
        _nodes[node.getId()] = node
        _listeners.forEach { it.onNodeChanged(node) }
    }

    /**
     * Dodaje węzeł do wewnętrznej mapy BEZ emitowania zdarzenia.
     * Przydatne do pre-populowania stanu przed testem.
     */
    fun addNodeSilently(node: MeshNodeInfo) {
        _nodes[node.getId()] = node
    }

    /**
     * Usuwa węzeł z wewnętrznej mapy (bez broadcastu).
     */
    fun removeNode(nodeId: String) {
        _nodes.remove(nodeId)
    }

    /**
     * Aktualizuje pozycję istniejącego węzła i emituje zdarzenie.
     */
    fun updateNodePosition(nodeId: String, lat: Double, lon: Double, alt: Int = 0) {
        val existing = _nodes[nodeId] ?: return
        val updated = existing.copy(
            position = MeshPosition(
                latitude = lat,
                longitude = lon,
                altitude = alt,
                time = (System.currentTimeMillis() / 1000).toInt()
            )
        )
        emitNodeChanged(updated)
    }

    /** Czyści stan — przydatne w @After bloku testu. */
    fun reset() {
        _nodes.clear()
        _listeners.clear()
        _isConnected = false
        _connectionState = Constants.STATE_DISCONNECTED
    }
}
