package com.example.meshtracker_v1.model

/**
 * Wrapper dla informacji o węźle Meshtastic.
 * Używa klasy z pakietu org.meshtastic.core.model.NodeInfo.
 */
data class MeshNodeInfo(
    val num: Int,
    val user: MeshUserInfo?,
    val position: MeshPosition?,
    val snr: Float = Float.MAX_VALUE,
    val rssi: Int = Int.MAX_VALUE,
    val lastHeard: Int = 0,
    val batteryLevel: Int = 0,
    val channel: Int = 0,
    val hopsAway: Int = 0
) {
    /**
     * Sprawdza czy węzeł ma prawidłową pozycję.
     */
    fun hasValidPosition(): Boolean {
        return position != null && position.isValid() && position.isInRange()
    }
    
    /**
     * Sprawdza czy węzeł jest online (ostatnio słyszany w ciągu ostatnich 5 minut).
     */
    fun isOnline(): Boolean {
        if (lastHeard == 0) return false
        val currentTime = System.currentTimeMillis() / 1000
        val timeDiff = currentTime - lastHeard
        return timeDiff < 300 // 5 minut
    }
    
    /**
     * Zwraca nazwę wyświetlaną węzła.
     */
    fun getDisplayName(): String {
        return user?.getDisplayName() ?: "Node $num"
    }
    
    /**
     * Zwraca ID węzła.
     */
    fun getId(): String {
        return user?.id ?: "node_$num"
    }
    
    companion object {
        /**
         * Tworzy MeshNodeInfo z org.meshtastic.core.model.NodeInfo.
         * Używa reflection, jeśli klasa nie jest dostępna bezpośrednio.
         */
        fun fromMeshtasticNodeInfo(nodeInfo: Any?): MeshNodeInfo? {
            if (nodeInfo == null) return null
            
            return try {
                val num = (nodeInfo.javaClass.getMethod("getNum").invoke(nodeInfo) as? Int) ?: 0
                
                val userObj = try {
                    nodeInfo.javaClass.getMethod("getUser").invoke(nodeInfo)
                } catch (e: Exception) {
                    null
                }
                val user = MeshUserInfo.fromMeshtasticUser(userObj)
                
                val positionObj = try {
                    nodeInfo.javaClass.getMethod("getPosition").invoke(nodeInfo)
                } catch (e: Exception) {
                    null
                }
                val position = MeshPosition.fromMeshtasticPosition(positionObj)
                
                val snr = try {
                    (nodeInfo.javaClass.getMethod("getSnr").invoke(nodeInfo) as? Float) ?: Float.MAX_VALUE
                } catch (e: Exception) {
                    Float.MAX_VALUE
                }
                
                val rssi = try {
                    (nodeInfo.javaClass.getMethod("getRssi").invoke(nodeInfo) as? Int) ?: Int.MAX_VALUE
                } catch (e: Exception) {
                    Int.MAX_VALUE
                }
                
                val lastHeard = try {
                    (nodeInfo.javaClass.getMethod("getLastHeard").invoke(nodeInfo) as? Int) ?: 0
                } catch (e: Exception) {
                    0
                }
                
                val batteryLevel = try {
                    val deviceMetrics = nodeInfo.javaClass.getMethod("getDeviceMetrics").invoke(nodeInfo)
                    if (deviceMetrics != null) {
                        (deviceMetrics.javaClass.getMethod("getBatteryLevel").invoke(deviceMetrics) as? Int) ?: 0
                    } else {
                        0
                    }
                } catch (e: Exception) {
                    0
                }
                
                val channel = try {
                    (nodeInfo.javaClass.getMethod("getChannel").invoke(nodeInfo) as? Int) ?: 0
                } catch (e: Exception) {
                    0
                }
                
                val hopsAway = try {
                    (nodeInfo.javaClass.getMethod("getHopsAway").invoke(nodeInfo) as? Int) ?: 0
                } catch (e: Exception) {
                    0
                }
                
                MeshNodeInfo(num, user, position, snr, rssi, lastHeard, batteryLevel, channel, hopsAway)
            } catch (e: Exception) {
                null
            }
        }
    }
}

