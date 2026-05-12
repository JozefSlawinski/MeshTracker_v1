package com.example.meshtracker_v1.repository

import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import com.example.meshtracker_v1.model.MeshNodeInfo
import com.example.meshtracker_v1.receiver.MeshtasticBroadcastReceiver
import com.example.meshtracker_v1.service.MeshServiceManager
import com.example.meshtracker_v1.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Produkcyjna implementacja [MeshRepository].
 * Zarządza [MeshServiceManager] i [MeshtasticBroadcastReceiver].
 */
@Singleton
class MeshServiceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val meshServiceManager: MeshServiceManager
) : MeshRepository {

    companion object {
        private const val TAG = "MeshServiceRepository"
    }

    private val listeners = mutableSetOf<MeshRepository.MeshEventListener>()
    private var isReceiverRegistered = false

    /** BroadcastReceiver przekazuje zdarzenia do wszystkich zarejestrowanych listenerów. */
    private val broadcastReceiver = MeshtasticBroadcastReceiver(
        object : MeshtasticBroadcastReceiver.MeshtasticReceiverListener {
            override fun onNodeChanged(nodeInfo: MeshNodeInfo) {
                listeners.forEach { it.onNodeChanged(nodeInfo) }
            }
            override fun onMeshConnected() {
                listeners.forEach { it.onMeshConnected() }
            }
            override fun onMeshDisconnected() {
                listeners.forEach { it.onMeshDisconnected() }
            }
        }
    )

    init {
        // Ustaw listener połączenia raz — na cały czas życia singletona.
        meshServiceManager.setConnectionListener(object : MeshServiceManager.ConnectionListener {
            override fun onServiceConnected() {
                Log.d(TAG, "Service connected")
                listeners.forEach { it.onServiceConnected() }
            }
            override fun onServiceDisconnected() {
                Log.d(TAG, "Service disconnected")
                listeners.forEach { it.onServiceDisconnected() }
            }
        })
    }

    override fun isMeshtasticInstalled(): Boolean = try {
        context.packageManager.getPackageInfo("com.geeksville.mesh", 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    override fun connect(): Boolean {
        registerReceiverIfNeeded()
        return meshServiceManager.connect()
    }

    override fun disconnect() {
        meshServiceManager.disconnect()
        unregisterReceiverIfNeeded()
    }

    override fun isConnected(): Boolean = meshServiceManager.isConnected()

    override fun getNodes(): List<MeshNodeInfo>? = meshServiceManager.getNodes()

    override fun getMyNodeId(): String? = meshServiceManager.getMyNodeID()

    override fun getConnectionState(): String? = meshServiceManager.getConnectionState()

    override fun addListener(listener: MeshRepository.MeshEventListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: MeshRepository.MeshEventListener) {
        listeners.remove(listener)
    }

    // ------------------------------------------------------------------ helpers

    private fun registerReceiverIfNeeded() {
        if (isReceiverRegistered) return
        try {
            val filter = IntentFilter().apply {
                addAction(Constants.ACTION_NODE_CHANGE)
                addAction(Constants.ACTION_MESH_CONNECTED)
                addAction(Constants.ACTION_MESH_DISCONNECTED)
            }
            context.registerReceiver(broadcastReceiver, filter, Context.RECEIVER_EXPORTED)
            isReceiverRegistered = true
            Log.d(TAG, "BroadcastReceiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering receiver", e)
        }
    }

    private fun unregisterReceiverIfNeeded() {
        if (!isReceiverRegistered) return
        try {
            context.unregisterReceiver(broadcastReceiver)
            isReceiverRegistered = false
            Log.d(TAG, "BroadcastReceiver unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
    }
}
