package com.example.meshtracker_v1.ui.map

import android.app.Application
import android.content.Context
import android.content.IntentFilter
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meshtracker_v1.model.MeshNodeInfo
import com.example.meshtracker_v1.receiver.MeshtasticBroadcastReceiver
import com.example.meshtracker_v1.service.MeshServiceManager
import com.example.meshtracker_v1.util.Constants
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MapViewModel(application: Application) : AndroidViewModel(application),
    MeshtasticBroadcastReceiver.MeshtasticReceiverListener {

    companion object {
        private const val TAG = "MapViewModel"
        private const val INITIAL_RECONNECT_DELAY_MS = 2_000L
        private const val MAX_RECONNECT_DELAY_MS = 60_000L
        private const val CONNECTION_CHECK_INTERVAL_MS = 2_000L
    }

    private val meshServiceManager = MeshServiceManager.getInstance(application)
    private val meshtasticReceiver = MeshtasticBroadcastReceiver(this)

    private val _nodes = MutableStateFlow<Map<String, MeshNodeInfo>>(emptyMap())
    val nodes: StateFlow<Map<String, MeshNodeInfo>> = _nodes.asStateFlow()

    private val _selectedNodeId = MutableStateFlow<String?>(null)
    val selectedNodeId: StateFlow<String?> = _selectedNodeId.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Connecting)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val nodesMutex = Mutex()
    private var isReceiverRegistered = false
    private var connectionCheckJob: Job? = null
    private var reconnectJob: Job? = null
    private var reconnectAttemptCount = 0

    init {
        setupServiceListener()
        registerReceiver()
        attemptConnect()
    }

    // ---- Połączenie ----

    private fun setupServiceListener() {
        meshServiceManager.setConnectionListener(object : MeshServiceManager.ConnectionListener {
            override fun onServiceConnected() {
                viewModelScope.launch {
                    val radioState = meshServiceManager.getConnectionState()
                    if (radioState == Constants.STATE_CONNECTED) {
                        onFullyConnected()
                    } else {
                        _connectionState.value = ConnectionState.Connecting
                        startConnectionCheck()
                    }
                }
            }

            override fun onServiceDisconnected() {
                viewModelScope.launch {
                    scheduleReconnect("Serwis Meshtastic rozłączony")
                }
            }
        })
    }

    private fun registerReceiver() {
        val context = getApplication<Application>()
        val filter = IntentFilter().apply {
            addAction(Constants.ACTION_NODE_CHANGE)
            addAction(Constants.ACTION_MESH_CONNECTED)
            addAction(Constants.ACTION_MESH_DISCONNECTED)
        }
        try {
            context.registerReceiver(meshtasticReceiver, filter, Context.RECEIVER_EXPORTED)
            isReceiverRegistered = true
        } catch (e: Exception) {
            Log.e(TAG, "Error registering receiver", e)
        }
    }

    private fun attemptConnect() {
        if (!meshServiceManager.isMeshtasticInstalled()) {
            _connectionState.value = ConnectionState.MeshtasticNotInstalled
            return
        }
        _connectionState.value = ConnectionState.Connecting
        if (!meshServiceManager.connect()) {
            scheduleReconnect("Nie można uruchomić połączenia z Meshtastic")
        }
    }

    private fun scheduleReconnect(reason: String) {
        reconnectJob?.cancel()
        connectionCheckJob?.cancel()

        _connectionState.value = ConnectionState.Disconnected(reason)

        val delayMs = (INITIAL_RECONNECT_DELAY_MS * (1L shl minOf(reconnectAttemptCount, 5)))
            .coerceAtMost(MAX_RECONNECT_DELAY_MS)
        reconnectAttemptCount++

        Log.d(TAG, "Scheduling reconnect in ${delayMs}ms (attempt #$reconnectAttemptCount), reason: $reason")

        reconnectJob = viewModelScope.launch {
            var remaining = (delayMs / 1000).toInt().coerceAtLeast(1)
            while (remaining > 0) {
                _connectionState.value = ConnectionState.Reconnecting(remaining)
                delay(1000)
                remaining--
            }
            attemptConnect()
        }
    }

    private fun onFullyConnected() {
        reconnectAttemptCount = 0
        reconnectJob?.cancel()
        reconnectJob = null
        connectionCheckJob?.cancel()
        connectionCheckJob = null
        _connectionState.value = ConnectionState.Connected
        refreshNodes()
        Log.d(TAG, "Fully connected to Meshtastic")
    }

    /**
     * Pozwala użytkownikowi ręcznie wyzwolić ponowne połączenie.
     */
    fun retryConnect() {
        Log.d(TAG, "Manual retry triggered")
        reconnectJob?.cancel()
        reconnectAttemptCount = 0
        attemptConnect()
    }

    // ---- Odświeżanie węzłów ----

    fun refreshNodes() {
        viewModelScope.launch {
            if (!meshServiceManager.isConnected()) return@launch
            if (meshServiceManager.getConnectionState() != Constants.STATE_CONNECTED) return@launch

            val nodesList = meshServiceManager.getNodes() ?: return@launch

            nodesMutex.withLock {
                val updated = _nodes.value.toMutableMap()
                nodesList.forEach { updated[it.getId()] = it }
                _nodes.value = updated
            }
            Log.d(TAG, "Refreshed ${nodesList.size} nodes")
        }
    }

    // ---- Connection check (serwis podłączony, czekamy na radio) ----

    private fun startConnectionCheck() {
        connectionCheckJob?.cancel()
        connectionCheckJob = viewModelScope.launch {
            while (true) {
                delay(CONNECTION_CHECK_INTERVAL_MS)

                if (!meshServiceManager.isConnected()) {
                    scheduleReconnect("Serwis Meshtastic niedostępny")
                    break
                }

                val radioState = meshServiceManager.getConnectionState()
                if (radioState == Constants.STATE_CONNECTED) {
                    onFullyConnected()
                    break
                }
            }
        }
    }

    // ---- BroadcastReceiver callbacks ----

    override fun onNodeChanged(nodeInfo: MeshNodeInfo) {
        viewModelScope.launch {
            nodesMutex.withLock {
                val updated = _nodes.value.toMutableMap()
                updated[nodeInfo.getId()] = nodeInfo
                _nodes.value = updated
            }
        }
    }

    override fun onMeshConnected() {
        viewModelScope.launch {
            if (meshServiceManager.isConnected()) {
                onFullyConnected()
            } else {
                _connectionState.value = ConnectionState.Connecting
                startConnectionCheck()
            }
        }
    }

    override fun onMeshDisconnected() {
        viewModelScope.launch {
            scheduleReconnect("Radio Meshtastic rozłączone")
        }
    }

    // ---- Helpers ----

    fun selectNode(nodeId: String?) { _selectedNodeId.value = nodeId }
    fun getNode(nodeId: String): MeshNodeInfo? = _nodes.value[nodeId]
    fun getNodesWithPosition(): List<MeshNodeInfo> = _nodes.value.values.filter { it.hasValidPosition() }

    override fun onCleared() {
        super.onCleared()
        reconnectJob?.cancel()
        connectionCheckJob?.cancel()
        if (isReceiverRegistered) {
            try {
                getApplication<Application>().unregisterReceiver(meshtasticReceiver)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering receiver", e)
            }
        }
        meshServiceManager.disconnect()
    }

    // ---- Stan połączenia ----

    sealed class ConnectionState {
        /** Połączono z Meshtastic i radiem. */
        object Connected : ConnectionState()

        /** Trwa łączenie (serwis lub radio). */
        object Connecting : ConnectionState()

        /** Odliczanie do kolejnej próby połączenia. */
        data class Reconnecting(val retryInSeconds: Int) : ConnectionState()

        /** Rozłączono — [reason] opisuje przyczynę. */
        data class Disconnected(val reason: String) : ConnectionState()

        /** Aplikacja Meshtastic nie jest zainstalowana. */
        object MeshtasticNotInstalled : ConnectionState()
    }
}
