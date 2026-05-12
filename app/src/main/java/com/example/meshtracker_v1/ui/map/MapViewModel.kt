package com.example.meshtracker_v1.ui.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meshtracker_v1.model.MeshNodeInfo
import com.example.meshtracker_v1.repository.MeshRepository
import com.example.meshtracker_v1.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel dla ekranu mapy.
 * Zależy wyłącznie od [MeshRepository] — wymienialnego w testach na FakeMeshRepository.
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val meshRepository: MeshRepository
) : ViewModel(), MeshRepository.MeshEventListener {

    companion object {
        private const val TAG = "MapViewModel"
    }

    private val _nodes = MutableStateFlow<Map<String, MeshNodeInfo>>(emptyMap())
    val nodes: StateFlow<Map<String, MeshNodeInfo>> = _nodes.asStateFlow()

    private val _selectedNodeId = MutableStateFlow<String?>(null)
    val selectedNodeId: StateFlow<String?> = _selectedNodeId.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var periodicRefreshJob: Job? = null
    private var connectionCheckJob: Job? = null

    init {
        meshRepository.addListener(this)
        val connected = meshRepository.connect()
        if (!connected) {
            Log.w(TAG, "Failed to start connection to Meshtastic service")
        }
    }

    // ------------------------------------------------------------------ MeshEventListener

    override fun onServiceConnected() {
        Log.d(TAG, "Service connected")
        viewModelScope.launch {
            val radioState = meshRepository.getConnectionState()
            if (radioState == Constants.STATE_CONNECTED) {
                _connectionState.value = ConnectionState.Connected
                refreshNodes()
                startPeriodicRefresh()
            } else {
                _connectionState.value = ConnectionState.Connecting
                startConnectionCheck()
            }
        }
    }

    override fun onServiceDisconnected() {
        Log.d(TAG, "Service disconnected")
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Disconnected()
            stopAllJobs()
        }
    }

    override fun onNodeChanged(nodeInfo: MeshNodeInfo) {
        viewModelScope.launch {
            val currentNodes = _nodes.value.toMutableMap()
            val oldNode = currentNodes[nodeInfo.getId()]

            if (oldNode != null && oldNode.hasValidPosition() && nodeInfo.hasValidPosition()) {
                val oldPos = oldNode.position!!
                val newPos = nodeInfo.position!!
                if (oldPos.latitude == newPos.latitude && oldPos.longitude == newPos.longitude) {
                    Log.d(TAG, "Node ${nodeInfo.getDisplayName()}: position unchanged")
                } else {
                    val dist = approximateDistanceMeters(oldPos.latitude, oldPos.longitude,
                        newPos.latitude, newPos.longitude)
                    Log.d(TAG, "Node ${nodeInfo.getDisplayName()}: moved ≈${String.format("%.1f", dist)}m")
                }
            } else if (nodeInfo.hasValidPosition()) {
                val p = nodeInfo.position!!
                Log.d(TAG, "Node ${nodeInfo.getDisplayName()}: new position (${p.latitude}, ${p.longitude})")
            }

            currentNodes[nodeInfo.getId()] = nodeInfo
            _nodes.value = currentNodes
            Log.d(TAG, "Total nodes: ${_nodes.value.size}")
        }
    }

    override fun onMeshConnected() {
        Log.d(TAG, "Radio connected (broadcast)")
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Connected
            if (meshRepository.isConnected()) {
                refreshNodes()
                startPeriodicRefresh()
            } else {
                _connectionState.value = ConnectionState.Connecting
                startConnectionCheck()
            }
        }
    }

    override fun onMeshDisconnected() {
        Log.d(TAG, "Radio disconnected (broadcast)")
        viewModelScope.launch {
            stopAllJobs()
            if (meshRepository.isConnected()) {
                _connectionState.value = ConnectionState.Connecting
                startConnectionCheck()
            } else {
                _connectionState.value = ConnectionState.Disconnected()
            }
        }
    }

    // ------------------------------------------------------------------ public API

    fun refreshNodes() {
        viewModelScope.launch {
            if (!meshRepository.isConnected()) {
                Log.w(TAG, "Cannot refresh — service not connected")
                return@launch
            }
            if (meshRepository.getConnectionState() != Constants.STATE_CONNECTED) {
                Log.w(TAG, "Cannot refresh — radio not connected")
                return@launch
            }
            val nodesList = meshRepository.getNodes() ?: run {
                Log.w(TAG, "getNodes() returned null")
                return@launch
            }
            val updated = _nodes.value.toMutableMap()
            nodesList.forEach { updated[it.getId()] = it }
            _nodes.value = updated
            Log.d(TAG, "Refreshed ${nodesList.size} nodes")
        }
    }

    fun selectNode(nodeId: String?) { _selectedNodeId.value = nodeId }

    fun retryConnect() {
        Log.d(TAG, "retryConnect() called")
        _connectionState.value = ConnectionState.Connecting
        val connected = meshRepository.connect()
        if (!connected) {
            Log.w(TAG, "retryConnect: Failed to start connection")
            _connectionState.value = ConnectionState.Disconnected("Nie można połączyć")
        } else {
            startConnectionCheck()
        }
    }

    fun getNode(nodeId: String): MeshNodeInfo? = _nodes.value[nodeId]

    fun getNodesWithPosition(): List<MeshNodeInfo> =
        _nodes.value.values.filter { it.hasValidPosition() }

    // ------------------------------------------------------------------ private

    private fun startConnectionCheck() {
        connectionCheckJob?.cancel()
        connectionCheckJob = viewModelScope.launch {
            while (_connectionState.value is ConnectionState.Connecting
                && meshRepository.isConnected()) {
                kotlinx.coroutines.delay(2_000)
                if (meshRepository.getConnectionState() == Constants.STATE_CONNECTED) {
                    _connectionState.value = ConnectionState.Connected
                    refreshNodes()
                    startPeriodicRefresh()
                    break
                }
            }
        }
    }

    private fun startPeriodicRefresh() {
        periodicRefreshJob?.cancel()
        periodicRefreshJob = viewModelScope.launch {
            while (_connectionState.value is ConnectionState.Connected
                && meshRepository.isConnected()) {
                kotlinx.coroutines.delay(5_000)
                if (_connectionState.value is ConnectionState.Connected
                    && meshRepository.isConnected()) {
                    refreshNodes()
                } else break
            }
        }
    }

    private fun stopAllJobs() {
        periodicRefreshJob?.cancel()
        periodicRefreshJob = null
        connectionCheckJob?.cancel()
        connectionCheckJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAllJobs()
        meshRepository.removeListener(this)
        meshRepository.disconnect()
        Log.d(TAG, "ViewModel cleared")
    }

    // ------------------------------------------------------------------ helpers

    private fun approximateDistanceMeters(
        lat1: Double, lon1: Double, lat2: Double, lon2: Double
    ): Double {
        val dLat = lat2 - lat1
        val dLon = lon2 - lon1
        val cosLat = Math.cos(Math.toRadians((lat1 + lat2) / 2))
        return Math.sqrt(
            dLat * dLat * 111_000.0 * 111_000.0 +
            dLon * dLon * (111_000.0 * cosLat) * (111_000.0 * cosLat)
        )
    }

    // ------------------------------------------------------------------ sealed class

    sealed class ConnectionState {
        object Connected : ConnectionState()
        object Connecting : ConnectionState()
        data class Reconnecting(val retryInSeconds: Int) : ConnectionState()
        data class Disconnected(val reason: String = "Rozłączono") : ConnectionState()
        object MeshtasticNotInstalled : ConnectionState()
    }
}
