package com.example.meshtracker_v1.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.meshtracker_v1.model.MeshNodeInfo
import com.example.meshtracker_v1.util.Constants

/**
 * BroadcastReceiver do odbierania broadcastów z aplikacji Meshtastic.
 */
class MeshtasticBroadcastReceiver(
    private val listener: MeshtasticReceiverListener
) : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "MeshtasticReceiver"
    }
    
    /**
     * Listener dla zdarzeń z Meshtastic.
     */
    interface MeshtasticReceiverListener {
        /**
         * Wywoływane gdy zmienia się węzeł (pojawienie się, zniknięcie, aktualizacja pozycji).
         */
        fun onNodeChanged(nodeInfo: MeshNodeInfo)
        
        /**
         * Wywoływane gdy radio Meshtastic łączy się.
         */
        fun onMeshConnected()
        
        /**
         * Wywoływane gdy radio Meshtastic rozłącza się.
         */
        fun onMeshDisconnected()
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent == null) {
            return
        }
        
        val action = intent.action ?: return
        
        Log.d(TAG, "Received broadcast: $action")
        
        when (action) {
            Constants.ACTION_NODE_CHANGE -> {
                handleNodeChange(intent)
            }
            Constants.ACTION_MESH_CONNECTED -> {
                handleMeshConnected(intent)
            }
            Constants.ACTION_MESH_DISCONNECTED -> {
                handleMeshDisconnected(intent)
            }
            else -> {
                Log.d(TAG, "Unhandled action: $action")
            }
        }
    }
    
    /**
     * Obsługuje zmianę węzła.
     */
    private fun handleNodeChange(intent: Intent) {
        try {
            // Pobierz NodeInfo z intent (jako Parcelable)
            // Używamy reflection, ponieważ klasa może nie być dostępna w czasie kompilacji
            val nodeInfoObj = try {
                // Spróbuj użyć getParcelableExtra z reflection
                val method = Intent::class.java.getMethod(
                    "getParcelableExtra",
                    String::class.java,
                    Class::class.java
                )
                // Szukamy klasy NodeInfo z pakietu org.meshtastic.core.model
                val nodeInfoClass = Class.forName("org.meshtastic.core.model.NodeInfo")
                method.invoke(intent, Constants.EXTRA_NODE_INFO, nodeInfoClass) as? Any
            } catch (e: Exception) {
                // Fallback: użyj starszej metody (dla Android < 33)
                try {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<android.os.Parcelable>(Constants.EXTRA_NODE_INFO)
                } catch (e2: Exception) {
                    Log.e(TAG, "Error getting NodeInfo from intent", e2)
                    null
                }
            }
            
            if (nodeInfoObj == null) {
                Log.d(TAG, "NodeInfo is null in broadcast")
                return
            }
            
            // Konwertuj do MeshNodeInfo
            val meshNodeInfo = MeshNodeInfo.fromMeshtasticNodeInfo(nodeInfoObj)
            
            if (meshNodeInfo == null) {
                Log.w(TAG, "Failed to convert NodeInfo")
                return
            }
            
            Log.d(TAG, "Node changed: ${meshNodeInfo.getDisplayName()} (${meshNodeInfo.getId()})")
            listener.onNodeChanged(meshNodeInfo)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling node change", e)
        }
    }
    
    /**
     * Obsługuje połączenie z radiem.
     */
    private fun handleMeshConnected(intent: Intent) {
        try {
            val state = intent.getStringExtra(Constants.EXTRA_CONNECTED)
            if (Constants.STATE_CONNECTED.equals(state, ignoreCase = true)) {
                Log.d(TAG, "Mesh radio connected")
                listener.onMeshConnected()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling mesh connected", e)
        }
    }
    
    /**
     * Obsługuje rozłączenie z radiem.
     */
    private fun handleMeshDisconnected(intent: Intent) {
        try {
            Log.d(TAG, "Mesh radio disconnected")
            listener.onMeshDisconnected()
        } catch (e: Exception) {
            Log.e(TAG, "Error handling mesh disconnected", e)
        }
    }
}

