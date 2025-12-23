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
            if (nodeInfo == null) {
                android.util.Log.w("MeshNodeInfo", "fromMeshtasticNodeInfo: nodeInfo is null")
                return null
            }
            
            android.util.Log.d("MeshNodeInfo", "Parsing NodeInfo, type: ${nodeInfo.javaClass.name}")
            
            // Debug: List all available methods
            try {
                val methods = nodeInfo.javaClass.declaredMethods
                android.util.Log.d("MeshNodeInfo", "Available methods: ${methods.map { it.name }.joinToString(", ")}")
            } catch (e: Exception) {
                android.util.Log.w("MeshNodeInfo", "Could not list methods: ${e.message}")
            }
            
            return try {
                // Try different ways to get the node number
                val num = try {
                    // Try getNum() method
                    (nodeInfo.javaClass.getMethod("getNum").invoke(nodeInfo) as? Int) ?: 0
                } catch (e: NoSuchMethodException) {
                    try {
                        // Try num field
                        val numField = nodeInfo.javaClass.getDeclaredField("num")
                        numField.isAccessible = true
                        (numField.get(nodeInfo) as? Int) ?: 0
                    } catch (e2: Exception) {
                        try {
                            // Try getNodeNum() method
                            (nodeInfo.javaClass.getMethod("getNodeNum").invoke(nodeInfo) as? Int) ?: 0
                        } catch (e3: Exception) {
                            // If all else fails, try to get it from user ID or use 0
                            android.util.Log.w("MeshNodeInfo", "Could not get node num, trying user ID: ${e.message}")
                            // We'll get the user first and see if we can derive num from there
                            0 // Will be set later if possible
                        }
                    }
                }
                android.util.Log.d("MeshNodeInfo", "Node num: $num")
                
                val userObj = try {
                    nodeInfo.javaClass.getMethod("getUser").invoke(nodeInfo)
                } catch (e: Exception) {
                    android.util.Log.w("MeshNodeInfo", "Error getting user: ${e.message}")
                    null
                }
                val user = MeshUserInfo.fromMeshtasticUser(userObj)
                android.util.Log.d("MeshNodeInfo", "User: ${user?.getDisplayName() ?: "null"}")
                
                // If num is still 0 and we have a user ID, try to derive a num from it
                val finalNum = if (num == 0 && user?.id != null) {
                    // Use hash of user ID as a fallback node number
                    val hash = user.id.hashCode()
                    Math.abs(hash) % 1000000 // Keep it reasonable
                } else {
                    num
                }
                
                val positionObj = try {
                    nodeInfo.javaClass.getMethod("getPosition").invoke(nodeInfo)
                } catch (e: Exception) {
                    android.util.Log.w("MeshNodeInfo", "Error getting position object: ${e.message}")
                    null
                }
                android.util.Log.d("MeshNodeInfo", "Position object: ${if (positionObj != null) positionObj.javaClass.name else "null"}")
                val position = MeshPosition.fromMeshtasticPosition(positionObj)
                android.util.Log.d("MeshNodeInfo", "Parsed position: ${if (position != null) "${position.latitude}, ${position.longitude} (valid: ${position.isValid()}, inRange: ${position.isInRange()})" else "null"}")
                
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
                    val method = nodeInfo.javaClass.getMethod("getLastHeard")
                    val value = method.invoke(nodeInfo)
                    val result = when (value) {
                        is Int -> value
                        is Long -> value.toInt()
                        else -> (value as? Number)?.toInt() ?: 0
                    }
                    android.util.Log.d("MeshNodeInfo", "lastHeard: $result (type: ${value?.javaClass?.name})")
                    result
                } catch (e: NoSuchMethodException) {
                    try {
                        // Try alternative method names
                        val method = nodeInfo.javaClass.getMethod("getLastHeardTime")
                        val value = method.invoke(nodeInfo)
                        val result = when (value) {
                            is Int -> value
                            is Long -> value.toInt()
                            else -> (value as? Number)?.toInt() ?: 0
                        }
                        android.util.Log.d("MeshNodeInfo", "lastHeard (via getLastHeardTime): $result")
                        result
                    } catch (e2: Exception) {
                        android.util.Log.w("MeshNodeInfo", "Could not get lastHeard: ${e.message}, ${e2.message}")
                        0
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MeshNodeInfo", "Error getting lastHeard: ${e.message}")
                    0
                }
                
                // Log online status calculation
                val currentTime = System.currentTimeMillis() / 1000
                val timeDiff = if (lastHeard > 0) currentTime - lastHeard else -1
                android.util.Log.d("MeshNodeInfo", "Online check: lastHeard=$lastHeard, currentTime=$currentTime, diff=$timeDiff seconds, isOnline=${lastHeard > 0 && timeDiff < 300}")
                
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
                
                val result = MeshNodeInfo(finalNum, user, position, snr, rssi, lastHeard, batteryLevel, channel, hopsAway)
                android.util.Log.d("MeshNodeInfo", "Successfully created MeshNodeInfo: ${result.getDisplayName()}")
                result
            } catch (e: Exception) {
                android.util.Log.e("MeshNodeInfo", "Error parsing NodeInfo", e)
                e.printStackTrace()
                null
            }
        }
    }
}

