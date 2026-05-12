package com.example.meshtracker_v1.ui.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meshtracker_v1.data.AppPreferences
import com.example.meshtracker_v1.model.MeshNodeInfo
import com.example.meshtracker_v1.repository.MeshRepository
import com.example.meshtracker_v1.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.meshtracker_v1.ui.nodes.NodeFilterState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel dla ekranu mapy.
 * Zależy wyłącznie od [MeshRepository] — wymienialnego w testach na FakeMeshRepository.
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val meshRepository: MeshRepository,
    private val appPreferences: AppPreferences
) : ViewModel(), MeshRepository.MeshEventListener {

    companion object {
        private const val TAG = "MapViewModel"
        private const val RECONNECT_INTERVAL_SECONDS = 30
    }

    private val _nodes = MutableStateFlow<Map<String, MeshNodeInfo>>(emptyMap())
    val nodes: StateFlow<Map<String, MeshNodeInfo>> = _nodes.asStateFlow()

    private val _selectedNodeId = MutableStateFlow<String?>(null)
    val selectedNodeId: StateFlow<String?> = _selectedNodeId.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var periodicRefreshJob: Job? = null
    private var connectionCheckJob: Job? = null
    private var reconnectJob: Job? = null

    val onlineThresholdSeconds: StateFlow<Int> = appPreferences.onlineThresholdMinutes
        .map { it * 60 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 300)

    val mapType: StateFlow<Int> = appPreferences.mapType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferences.DEFAULT_MAP_TYPE)

    private val refreshIntervalSeconds: StateFlow<Int> = appPreferences.refreshIntervalSeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferences.DEFAULT_REFRESH_INTERVAL)

    private val _filterState = MutableStateFlow(NodeFilterState())
    val filterState: StateFlow<NodeFilterState> = _filterState.asStateFlow()

    val filteredNodes: StateFlow<List<MeshNodeInfo>> = combine(
        _nodes, _filterState, onlineThresholdSeconds
    ) { nodesMap, filter, threshold ->
        nodesMap.values
            .filter { node ->
                val matchesQuery = filter.searchQuery.isEmpty() ||
                        node.getDisplayName().contains(filter.searchQuery, ignoreCase = true) ||
                        node.getId().contains(filter.searchQuery, ignoreCase = true)
                val matchesOnline = !filter.showOnlineOnly || node.isOnline(threshold)
                val matchesGps = !filter.showWithGpsOnly || node.hasValidPosition()
                matchesQuery && matchesOnline && matchesGps
            }
            .sortedWith(
                compareBy<MeshNodeInfo> { !it.isOnline(threshold) }.thenBy { it.getDisplayName() }
            )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateFilter(filter: NodeFilterState) { _filterState.value = filter }
    fun clearFilter() { _filterState.value = NodeFilterState() }

    init {
        // Załaduj domyślne filtry z ustawień przy pierwszym uruchomieniu
        viewModelScope.launch {
            val onlineDefault = appPreferences.defaultOnlineFilter.first()
            val gpsDefault = appPreferences.defaultGpsFilter.first()
            if (onlineDefault || gpsDefault) {
                _filterState.value = NodeFilterState(
                    showOnlineOnly = onlineDefault,
                    showWithGpsOnly = gpsDefault
                )
            }
        }
        meshRepository.addListener(this)
        if (!meshRepository.isMeshtasticInstalled()) {
            Log.w(TAG, "Meshtastic app not installed")
            _connectionState.value = ConnectionState.MeshtasticNotInstalled
        } else {
            val connected = meshRepository.connect()
            if (!connected) {
                Log.w(TAG, "Failed to start connection to Meshtastic service")
            }
        }
    }

    // ------------------------------------------------------------------ MeshEventListener

    override fun onServiceConnected() {
        Log.d(TAG, "Service connected")
        viewModelScope.launch {
            val radioState = meshRepository.getConnectionState()
            when {
                radioState == Constants.STATE_CONNECTED -> {
                    _connectionState.value = ConnectionState.Connected
                    refreshNodes()
                    startPeriodicRefresh()
                }
                radioState != null -> {
                    // AIDL dostępne, ale radio jeszcze nie połączone
                    _connectionState.value = ConnectionState.Connecting
                    startConnectionCheck()
                }
                else -> {
                    // Brak AIDL (broadcast-only mode) — czekamy na broadcast lub węzeł
                    Log.d(TAG, "AIDL unavailable — waiting for broadcast to determine state")
                    _connectionState.value = ConnectionState.Connecting
                }
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
            // Jeśli węzeł dotarł, radio musi być połączone — naprawia "already connected" race
            val state = _connectionState.value
            if (state is ConnectionState.Connecting || state is ConnectionState.Disconnected) {
                Log.d(TAG, "Node received while not Connected — inferring radio is up")
                _connectionState.value = ConnectionState.Connected
                startPeriodicRefresh()
            }

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
            refreshNodes()
            startPeriodicRefresh()
        }
    }

    override fun onMeshDisconnected() {
        Log.d(TAG, "Radio disconnected (broadcast)")
        viewModelScope.launch {
            stopAllJobs()
            if (meshRepository.isConnected()) {
                startReconnecting()
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
        stopAllJobs()
        if (!meshRepository.isMeshtasticInstalled()) {
            Log.w(TAG, "retryConnect: Meshtastic not installed")
            _connectionState.value = ConnectionState.MeshtasticNotInstalled
            return
        }
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
                val delayMs = refreshIntervalSeconds.value * 1_000L
                kotlinx.coroutines.delay(delayMs)
                if (_connectionState.value is ConnectionState.Connected
                    && meshRepository.isConnected()) {
                    refreshNodes()
                } else break
            }
        }
    }

    private fun startReconnecting() {
        reconnectJob?.cancel()
        reconnectJob = viewModelScope.launch {
            var attempt = 1
            while (true) {
                Log.d(TAG, "Reconnect attempt $attempt — waiting ${RECONNECT_INTERVAL_SECONDS}s")
                for (remaining in RECONNECT_INTERVAL_SECONDS downTo 1) {
                    _connectionState.value = ConnectionState.Reconnecting(remaining)
                    kotlinx.coroutines.delay(1_000)
                    if (_connectionState.value !is ConnectionState.Reconnecting) return@launch
                }
                if (!meshRepository.isConnected()) {
                    _connectionState.value = ConnectionState.Disconnected("Utracono połączenie z serwisem")
                    return@launch
                }
                val state = meshRepository.getConnectionState()
                if (state == Constants.STATE_CONNECTED) {
                    _connectionState.value = ConnectionState.Connected
                    refreshNodes()
                    startPeriodicRefresh()
                    return@launch
                }
                attempt++
            }
        }
    }

    private fun stopAllJobs() {
        periodicRefreshJob?.cancel()
        periodicRefreshJob = null
        connectionCheckJob?.cancel()
        connectionCheckJob = null
        reconnectJob?.cancel()
        reconnectJob = null
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
