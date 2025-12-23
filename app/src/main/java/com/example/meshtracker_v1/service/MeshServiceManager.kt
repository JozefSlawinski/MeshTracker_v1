package com.example.meshtracker_v1.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.example.meshtracker_v1.model.MeshNodeInfo
import com.example.meshtracker_v1.util.Constants

/**
 * Manager do zarządzania połączeniem z serwisem Meshtastic IMeshService.
 * Singleton pattern.
 */
class MeshServiceManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "MeshServiceManager"
        
        @Volatile
        private var INSTANCE: MeshServiceManager? = null
        
        fun getInstance(context: Context): MeshServiceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MeshServiceManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private var meshService: Any? = null // IMeshService używany przez reflection
    private var isBound = false
    private var connectionListener: ConnectionListener? = null
    
    /**
     * Listener dla zmian stanu połączenia.
     */
    interface ConnectionListener {
        fun onServiceConnected()
        fun onServiceDisconnected()
    }
    
    /**
     * ServiceConnection do obsługi bindingu z serwisem.
     */
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service connected")
            try {
                // Użyj reflection do uzyskania IMeshService.Stub
                val stubClass = Class.forName("org.meshtastic.core.service.IMeshService\$Stub")
                val asInterfaceMethod = stubClass.getMethod("asInterface", IBinder::class.java)
                meshService = asInterfaceMethod.invoke(null, service)
                isBound = true
                connectionListener?.onServiceConnected()
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to service", e)
                isBound = false
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service disconnected")
            meshService = null
            isBound = false
            connectionListener?.onServiceDisconnected()
        }
    }
    
    /**
     * Łączy się z serwisem Meshtastic.
     * @return true jeśli rozpoczęto połączenie, false w przeciwnym razie
     */
    fun connect(): Boolean {
        if (isBound) {
            Log.d(TAG, "Already connected")
            return true
        }
        
        val intent = Intent(Constants.MESH_SERVICE_ACTION).apply {
            setPackage(Constants.MESHTASTIC_PACKAGE)
        }
        
        return try {
            val result = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            if (result) {
                Log.d(TAG, "Binding to service started")
            } else {
                Log.w(TAG, "Failed to bind to service")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error binding to service", e)
            false
        }
    }
    
    /**
     * Rozłącza się z serwisem.
     */
    fun disconnect() {
        if (isBound) {
            try {
                context.unbindService(serviceConnection)
                Log.d(TAG, "Unbound from service")
            } catch (e: Exception) {
                Log.e(TAG, "Error unbinding from service", e)
            }
            isBound = false
            meshService = null
        }
    }
    
    /**
     * Sprawdza czy serwis jest połączony.
     */
    fun isConnected(): Boolean {
        return isBound && meshService != null
    }
    
    /**
     * Ustawia listener dla zmian połączenia.
     */
    fun setConnectionListener(listener: ConnectionListener?) {
        this.connectionListener = listener
    }
    
    /**
     * Pobiera listę wszystkich węzłów w sieci.
     * @return Lista węzłów lub null w przypadku błędu
     */
    fun getNodes(): List<MeshNodeInfo>? {
        if (!isConnected()) {
            Log.w(TAG, "Service not connected")
            return null
        }
        
        return try {
            // Użyj reflection do wywołania getNodes()
            val getNodesMethod = meshService?.javaClass?.getMethod("getNodes")
            val nodes = getNodesMethod?.invoke(meshService) as? List<*> ?: return null
            
            nodes.mapNotNull { nodeInfo ->
                MeshNodeInfo.fromMeshtasticNodeInfo(nodeInfo)
            }
        } catch (e: RemoteException) {
            Log.e(TAG, "Error getting nodes", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error getting nodes", e)
            null
        }
    }
    
    /**
     * Pobiera informacje o własnym węźle.
     * @return ID własnego węzła lub null
     */
    fun getMyNodeID(): String? {
        if (!isConnected()) {
            Log.w(TAG, "Service not connected")
            return null
        }
        
        return try {
            // Użyj reflection do wywołania getMyId()
            val getMyIdMethod = meshService?.javaClass?.getMethod("getMyId")
            getMyIdMethod?.invoke(meshService) as? String
        } catch (e: RemoteException) {
            Log.e(TAG, "Error getting my node ID", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error getting my node ID", e)
            null
        }
    }
    
    /**
     * Sprawdza stan połączenia z radiem.
     * @return Stan połączenia ("CONNECTED" lub "DISCONNECTED") lub null
     */
    fun getConnectionState(): String? {
        if (!isConnected()) {
            Log.w(TAG, "Service not connected")
            return null
        }
        
        return try {
            // Użyj reflection do wywołania connectionState()
            val connectionStateMethod = meshService?.javaClass?.getMethod("connectionState")
            connectionStateMethod?.invoke(meshService) as? String
        } catch (e: RemoteException) {
            Log.e(TAG, "Error getting connection state", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error getting connection state", e)
            null
        }
    }
    
    /**
     * Pobiera referencję do serwisu (do użycia w zaawansowanych scenariuszach).
     * Zwraca Any, ponieważ używamy reflection.
     */
    fun getService(): Any? {
        return meshService
    }
}

