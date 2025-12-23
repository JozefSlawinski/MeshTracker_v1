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
                    _connectionState.value = ConnectionState.CONNECTED
                    refreshNodes()
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
            val nodesList = meshServiceManager.getNodes()
            if (nodesList != null) {
                val nodesMap = nodesList.associateBy { it.getId() }
                _nodes.value = nodesMap
                Log.d(TAG, "Refreshed ${nodesList.size} nodes")
            } else {
                Log.w(TAG, "Failed to get nodes")
            }
        }
    }
    
    /**
     * Aktualizuje węzeł (wywoływane z BroadcastReceiver).
     */
    override fun onNodeChanged(nodeInfo: MeshNodeInfo) {
        viewModelScope.launch {
            val currentNodes = _nodes.value.toMutableMap()
            currentNodes[nodeInfo.getId()] = nodeInfo
            _nodes.value = currentNodes
            Log.d(TAG, "Node updated: ${nodeInfo.getDisplayName()}")
        }
    }
    
    /**
     * Wywoływane gdy radio Meshtastic łączy się.
     */
    override fun onMeshConnected() {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.CONNECTED
            refreshNodes()
        }
    }
    
    /**
     * Wywoływane gdy radio Meshtastic rozłącza się.
     */
    override fun onMeshDisconnected() {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.DISCONNECTED
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

