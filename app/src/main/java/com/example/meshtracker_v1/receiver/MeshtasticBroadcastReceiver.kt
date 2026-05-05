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
                handleNodeChange(context, intent)
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
    private fun handleNodeChange(context: Context, intent: Intent) {
        try {
            Log.d(TAG, "Handling NODE_CHANGE broadcast")
            Log.d(TAG, "Intent extras: ${intent.extras?.keySet()}")
            
            // Pobierz NodeInfo z intent używając ClassLoader z aplikacji Meshtastic
            // Problem: Bundle jest już zdeserializowany przez system, więc musimy użyć Thread context ClassLoader
            val nodeInfoObj = try {
                // Najpierw załaduj ClassLoader z aplikacji Meshtastic
                val meshtasticContext = context.createPackageContext(
                    Constants.MESHTASTIC_PACKAGE,
                    Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY
                )
                val meshtasticClassLoader = meshtasticContext.classLoader
                
                Log.d(TAG, "Loaded Meshtastic ClassLoader: $meshtasticClassLoader")
                
                // Ustaw Thread context ClassLoader PRZED jakimkolwiek dostępem do Bundle
                val originalClassLoader = Thread.currentThread().contextClassLoader
                try {
                    Thread.currentThread().contextClassLoader = meshtasticClassLoader
                    Log.d(TAG, "Set Thread context ClassLoader")
                    
                    // Załaduj klasę NodeInfo używając ClassLoader z Meshtastic
                    val nodeInfoClass = meshtasticClassLoader.loadClass("org.meshtastic.core.model.NodeInfo")
                    Log.d(TAG, "Found NodeInfo class: $nodeInfoClass")
                    
                    // Pobierz Bundle z intent
                    val bundle = intent.extras ?: return
                    
                    // Ustaw ClassLoader dla Bundle
                    bundle.classLoader = meshtasticClassLoader
                    
                    // Spróbuj pobrać NodeInfo używając Bundle.getParcelable()
                    val bundleResult = try {
                        val getParcelableMethod = android.os.Bundle::class.java.getMethod(
                            "getParcelable",
                            String::class.java,
                            Class::class.java
                        )
                        getParcelableMethod.invoke(bundle, Constants.EXTRA_NODE_INFO, nodeInfoClass) as? Any
                    } catch (e: Exception) {
                        Log.w(TAG, "Error using Bundle.getParcelable: ${e.message}")
                        null
                    }
                    
                    if (bundleResult != null) {
                        Log.d(TAG, "Got NodeInfo from Bundle.getParcelable: true")
                        bundleResult
                    } else {
                        // Alternatywa: spróbuj użyć Intent.getParcelableExtra
                        try {
                            val method = Intent::class.java.getMethod(
                                "getParcelableExtra",
                                String::class.java,
                                Class::class.java
                            )
                            val result = method.invoke(intent, Constants.EXTRA_NODE_INFO, nodeInfoClass) as? Any
                            Log.d(TAG, "Got NodeInfo from Intent.getParcelableExtra: ${result != null}")
                            result
                        } catch (e: Exception) {
                            Log.w(TAG, "Error using Intent.getParcelableExtra: ${e.message}")
                            null
                        }
                    }
                } finally {
                    // Przywróć oryginalny ClassLoader
                    Thread.currentThread().contextClassLoader = originalClassLoader
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error using Meshtastic ClassLoader: ${e.message}", e)
                null
            }
            
            if (nodeInfoObj == null) {
                Log.w(TAG, "NodeInfo is null in broadcast. Available extras: ${intent.extras?.keySet()}")
                // Spróbuj znaleźć NodeInfo w innych extras
                intent.extras?.keySet()?.forEach { key ->
                    Log.d(TAG, "Extra key: $key, value type: ${intent.extras?.get(key)?.javaClass?.name}")
                }
                return
            }
            
            Log.d(TAG, "NodeInfo object type: ${nodeInfoObj.javaClass.name}")
            
            // Konwertuj do MeshNodeInfo
            val meshNodeInfo = MeshNodeInfo.fromMeshtasticNodeInfo(nodeInfoObj)
            
            if (meshNodeInfo == null) {
                Log.e(TAG, "Failed to convert NodeInfo to MeshNodeInfo")
                return
            }
            
            Log.d(TAG, "Node changed: ${meshNodeInfo.getDisplayName()} (${meshNodeInfo.getId()})")
            Log.d(TAG, "Node has position: ${meshNodeInfo.hasValidPosition()}")
            if (meshNodeInfo.position != null) {
                val pos = meshNodeInfo.position!!
                Log.d(TAG, "Node position: lat=${pos.latitude}, lng=${pos.longitude}, time=${pos.time}, satellites=${pos.satellitesInView}, precisionBits=${pos.precisionBits}")
                Log.d(TAG, "Position valid: ${pos.isValid()}, inRange: ${pos.isInRange()}")
                Log.d(TAG, "SPEED: ${pos.groundSpeed} m/s (raw: ${pos.groundSpeed})")
                Log.d(TAG, "HEADING: ${pos.groundTrack}° (raw: ${pos.groundTrack})")
            }
            listener.onNodeChanged(meshNodeInfo)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling node change", e)
            e.printStackTrace()
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

