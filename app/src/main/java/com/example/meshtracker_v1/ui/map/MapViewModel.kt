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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

/**
 * ViewModel dla ekranu mapy.
 * Zarządza stanem węzłów i połączeniem z Meshtastic.
 */
class MapViewModel(application: Application) : AndroidViewModel(application),
    MeshtasticBroadcastReceiver.MeshtasticReceiverListener {
    
    companion object {
        private const val TAG = "MapViewModel"
    }
    
    private val meshServiceManager: MeshServiceManager =
        MeshServiceManager.getInstance(application)
    
    private val meshtasticReceiver: MeshtasticBroadcastReceiver =
        MeshtasticBroadcastReceiver(this)
    
    private val _nodes = MutableStateFlow<Map<String, MeshNodeInfo>>(emptyMap())
    val nodes: StateFlow<Map<String, MeshNodeInfo>> = _nodes.asStateFlow()
    
    private val _selectedNodeId = MutableStateFlow<String?>(null)
    val selectedNodeId: StateFlow<String?> = _selectedNodeId.asStateFlow()
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private var isReceiverRegistered = false
    private var periodicRefreshJob: Job? = null
    private var connectionCheckJob: Job? = null
    
    init {
        initialize()
    }
    
    /**
     * Inicjalizuje połączenie z Meshtastic.
     */
    private fun initialize() {
        val context = getApplication<Application>()
        
        // Ustaw listener dla zmian połączenia
        meshServiceManager.setConnectionListener(object : MeshServiceManager.ConnectionListener {
            override fun onServiceConnected() {
                Log.d(TAG, "Service connected")
                viewModelScope.launch {
                    // Sprawdź stan połączenia radia
                    val radioState = meshServiceManager.getConnectionState()
                    Log.d(TAG, "Radio connection state: $radioState")
                    
                    if (radioState == Constants.STATE_CONNECTED) {
                        _connectionState.value = ConnectionState.CONNECTED
                        refreshNodes()
                        // Rozpocznij okresowe odświeżanie
                        startPeriodicRefresh()
                    } else {
                        Log.d(TAG, "Service connected but radio not connected yet, waiting for connection...")
                        _connectionState.value = ConnectionState.CONNECTING
                        // Rozpocznij okresowe sprawdzanie stanu połączenia
                        startConnectionCheck()
                    }
                }
            }
            
            override fun onServiceDisconnected() {
                Log.d(TAG, "Service disconnected")
                viewModelScope.launch {
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
            }
        })
        
        // Zarejestruj BroadcastReceiver
        val filter = IntentFilter().apply {
            addAction(Constants.ACTION_NODE_CHANGE)
            addAction(Constants.ACTION_MESH_CONNECTED)
            addAction(Constants.ACTION_MESH_DISCONNECTED)
        }
        
        try {
            context.registerReceiver(meshtasticReceiver, filter, Context.RECEIVER_EXPORTED)
            isReceiverRegistered = true
            Log.d(TAG, "BroadcastReceiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering receiver", e)
        }
        
        // Połącz z serwisem
        val connected = meshServiceManager.connect()
        if (connected) {
            Log.d(TAG, "Started connecting to Meshtastic service")
        } else {
            Log.w(TAG, "Failed to start connection to Meshtastic service")
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }
    
    /**
     * Odświeża listę wszystkich węzłów.
     */
    fun refreshNodes() {
        viewModelScope.launch {
            Log.d(TAG, "Refreshing nodes...")
            
            // Sprawdź czy serwis jest połączony
            if (!meshServiceManager.isConnected()) {
                Log.w(TAG, "Cannot refresh nodes - service not connected")
                return@launch
            }
            
            // Sprawdź stan połączenia radia
            val radioState = meshServiceManager.getConnectionState()
            Log.d(TAG, "Radio connection state: $radioState")
            
            if (radioState != Constants.STATE_CONNECTED) {
                Log.w(TAG, "Cannot refresh nodes - radio not connected (state: $radioState)")
                return@launch
            }
            
            val nodesList = meshServiceManager.getNodes()
            if (nodesList != null) {
                val currentNodes = _nodes.value.toMutableMap()
                var updatedCount = 0
                var unchangedCount = 0
                
                nodesList.forEach { newNode ->
                    val nodeId = newNode.getId()
                    val oldNode = currentNodes[nodeId]
                    
                    // Porównaj pozycje, aby zobaczyć, czy się zmieniły
                    if (oldNode != null && oldNode.hasValidPosition() && newNode.hasValidPosition()) {
                        val oldPos = oldNode.position!!
                        val newPos = newNode.position!!
                        if (oldPos.latitude == newPos.latitude && oldPos.longitude == newPos.longitude) {
                            unchangedCount++
                            Log.d(TAG, "Node ${newNode.getDisplayName()}: position unchanged in refreshNodes (lat=${newPos.latitude}, lng=${newPos.longitude})")
                        } else {
                            updatedCount++
                            Log.d(TAG, "Node ${newNode.getDisplayName()}: position changed in refreshNodes")
                            Log.d(TAG, "  Old: lat=${oldPos.latitude}, lng=${oldPos.longitude}")
                            Log.d(TAG, "  New: lat=${newPos.latitude}, lng=${newPos.longitude}")
                        }
                    } else if (newNode.hasValidPosition()) {
                        updatedCount++
                        Log.d(TAG, "Node ${newNode.getDisplayName()}: new position from refreshNodes")
                    }
                    
                    // Aktualizuj węzeł (ale nie nadpisuj, jeśli broadcast miał nowszą pozycję)
                    // TODO: Można dodać logikę, aby nie nadpisywać świeżych aktualizacji z broadcastów
                    currentNodes[nodeId] = newNode
                }
                
                _nodes.value = currentNodes
                Log.d(TAG, "Refreshed ${nodesList.size} nodes (${updatedCount} updated, ${unchangedCount} unchanged)")
                if (nodesList.isEmpty()) {
                    Log.w(TAG, "Node list is empty - radio may be connected but no nodes discovered yet")
                }
            } else {
                Log.w(TAG, "Failed to get nodes - getNodes() returned null")
            }
        }
    }
    
    /**
     * Sprawdza stan połączenia radia okresowo (gdy serwis jest połączony ale radio nie).
     */
    private fun startConnectionCheck() {
        // Anuluj poprzedni job jeśli istnieje
        connectionCheckJob?.cancel()
        
        connectionCheckJob = viewModelScope.launch {
            while (_connectionState.value == ConnectionState.CONNECTING && meshServiceManager.isConnected()) {
                kotlinx.coroutines.delay(2000) // Sprawdzaj co 2 sekundy
                
                val radioState = meshServiceManager.getConnectionState()
                Log.d(TAG, "Checking radio connection state: $radioState")
                
                if (radioState == Constants.STATE_CONNECTED) {
                    Log.d(TAG, "Radio connected detected via polling")
                    _connectionState.value = ConnectionState.CONNECTED
                    refreshNodes()
                    startPeriodicRefresh()
                    connectionCheckJob?.cancel()
                    connectionCheckJob = null
                }
            }
        }
    }
    
    /**
     * Odświeża węzły okresowo (co 5 sekund).
     */
    private fun startPeriodicRefresh() {
        // Anuluj poprzedni job jeśli istnieje
        periodicRefreshJob?.cancel()
        
        periodicRefreshJob = viewModelScope.launch {
            while (_connectionState.value == ConnectionState.CONNECTED && meshServiceManager.isConnected()) {
                kotlinx.coroutines.delay(5000) // 5 sekund
                
                // Sprawdź ponownie przed odświeżeniem (serwis mógł się rozłączyć podczas delay)
                if (_connectionState.value == ConnectionState.CONNECTED && meshServiceManager.isConnected()) {
                    refreshNodes()
                } else {
                    Log.d(TAG, "Stopping periodic refresh - service disconnected")
                    break
                }
            }
        }
    }
    
    /**
     * Aktualizuje węzeł (wywoływane z BroadcastReceiver).
     */
    override fun onNodeChanged(nodeInfo: MeshNodeInfo) {
        viewModelScope.launch {
            val currentNodes = _nodes.value.toMutableMap()
            val oldNode = currentNodes[nodeInfo.getId()]
            
            // Loguj szczegóły pozycji przed i po aktualizacji
            if (oldNode != null && oldNode.hasValidPosition() && nodeInfo.hasValidPosition()) {
                val oldPos = oldNode.position!!
                val newPos = nodeInfo.position!!
                Log.d(TAG, "Node ${nodeInfo.getDisplayName()} position update:")
                Log.d(TAG, "  Old: lat=${oldPos.latitude}, lng=${oldPos.longitude}, time=${oldPos.time}, satellites=${oldPos.satellitesInView}, precisionBits=${oldPos.precisionBits}")
                Log.d(TAG, "  New: lat=${newPos.latitude}, lng=${newPos.longitude}, time=${newPos.time}, satellites=${newPos.satellitesInView}, precisionBits=${newPos.precisionBits}")
                if (oldPos.latitude == newPos.latitude && oldPos.longitude == newPos.longitude) {
                    Log.w(TAG, "  WARNING: Position has NOT changed! Same coordinates.")
                    Log.w(TAG, "  This suggests Meshtastic is sending the same cached position.")
                    Log.w(TAG, "  Possible reasons:")
                    Log.w(TAG, "    1. GPS has no new fix (node may be indoors)")
                    Log.w(TAG, "    2. Position hasn't changed significantly (within threshold)")
                    Log.w(TAG, "    3. Meshtastic is caching the last known position")
                } else {
                    val latDiff = Math.abs(newPos.latitude - oldPos.latitude)
                    val lngDiff = Math.abs(newPos.longitude - oldPos.longitude)
                    // Przybliżona odległość w metrach (dla szerokości geograficznej ~52°)
                    val distanceMeters = Math.sqrt(latDiff * latDiff * 111000.0 * 111000.0 + lngDiff * lngDiff * 111000.0 * Math.cos(Math.toRadians(newPos.latitude)) * 111000.0 * Math.cos(Math.toRadians(newPos.latitude)))
                    Log.d(TAG, "  Position changed by: lat=${latDiff}, lng=${lngDiff}, distance≈${String.format("%.1f", distanceMeters)}m")
                }
            } else if (nodeInfo.hasValidPosition()) {
                val newPos = nodeInfo.position!!
                Log.d(TAG, "Node ${nodeInfo.getDisplayName()} new position: lat=${newPos.latitude}, lng=${newPos.longitude}, time=${newPos.time}, satellites=${newPos.satellitesInView}, precisionBits=${newPos.precisionBits}")
            }
            
            currentNodes[nodeInfo.getId()] = nodeInfo
            _nodes.value = currentNodes
            Log.d(TAG, "Node updated: ${nodeInfo.getDisplayName()} (${nodeInfo.getId()})")
            Log.d(TAG, "Total nodes now: ${_nodes.value.size}")
            Log.d(TAG, "Node has position: ${nodeInfo.hasValidPosition()}")
        }
    }
    
    /**
     * Wywoływane gdy radio Meshtastic łączy się.
     */
    override fun onMeshConnected() {
        viewModelScope.launch {
            Log.d(TAG, "Radio connected (from broadcast)")
            _connectionState.value = ConnectionState.CONNECTED
            
            // Sprawdź czy serwis jest połączony przed odświeżeniem
            if (meshServiceManager.isConnected()) {
                refreshNodes()
                // Rozpocznij okresowe odświeżanie jeśli jeszcze nie działa
                startPeriodicRefresh()
            } else {
                Log.w(TAG, "Radio connected but service not connected yet, waiting...")
                _connectionState.value = ConnectionState.CONNECTING
                startConnectionCheck()
            }
        }
    }
    
    /**
     * Wywoływane gdy radio Meshtastic rozłącza się.
     */
    override fun onMeshDisconnected() {
        viewModelScope.launch {
            Log.d(TAG, "Radio disconnected (from broadcast)")
            _connectionState.value = ConnectionState.DISCONNECTED
            // Anuluj okresowe odświeżanie
            periodicRefreshJob?.cancel()
            periodicRefreshJob = null
            
            // Jeśli serwis jest nadal połączony, zacznij sprawdzanie połączenia
            if (meshServiceManager.isConnected()) {
                _connectionState.value = ConnectionState.CONNECTING
                startConnectionCheck()
            }
        }
    }
    
    /**
     * Wybiera węzeł na mapie.
     */
    fun selectNode(nodeId: String?) {
        _selectedNodeId.value = nodeId
    }
    
    /**
     * Pobiera węzeł po ID.
     */
    fun getNode(nodeId: String): MeshNodeInfo? {
        return _nodes.value[nodeId]
    }
    
    /**
     * Pobiera wszystkie węzły z prawidłową pozycją.
     */
    fun getNodesWithPosition(): List<MeshNodeInfo> {
        return _nodes.value.values.filter { it.hasValidPosition() }
    }
    
    override fun onCleared() {
        super.onCleared()
        
        // Anuluj wszystkie joby
        periodicRefreshJob?.cancel()
        periodicRefreshJob = null
        connectionCheckJob?.cancel()
        connectionCheckJob = null
        
        // Wyrejestruj receiver
        if (isReceiverRegistered) {
            try {
                getApplication<Application>().unregisterReceiver(meshtasticReceiver)
                isReceiverRegistered = false
                Log.d(TAG, "BroadcastReceiver unregistered")
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering receiver", e)
            }
        }
        
        // Rozłącz serwis
        meshServiceManager.disconnect()
        Log.d(TAG, "Service disconnected")
    }
    
    /**
     * Stan połączenia z Meshtastic.
     */
    enum class ConnectionState {
        CONNECTED,
        DISCONNECTED,
        CONNECTING
    }
}

