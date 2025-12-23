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
            
            // Debug: List all available methods (including inherited)
            try {
                val declaredMethods = nodeInfo.javaClass.declaredMethods
                val allMethods = nodeInfo.javaClass.methods
                android.util.Log.d("MeshNodeInfo", "Declared methods: ${declaredMethods.map { it.name }.joinToString(", ")}")
                android.util.Log.d("MeshNodeInfo", "All methods (including inherited): ${allMethods.map { it.name }.joinToString(", ")}")
                
                // Also list fields
                val fields = nodeInfo.javaClass.declaredFields
                android.util.Log.d("MeshNodeInfo", "Declared fields: ${fields.map { it.name }.joinToString(", ")}")
            } catch (e: Exception) {
                android.util.Log.w("MeshNodeInfo", "Could not list methods/fields: ${e.message}")
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
                    // Try getMethod first (includes inherited)
                    nodeInfo.javaClass.getMethod("getUser").invoke(nodeInfo)
                } catch (e: NoSuchMethodException) {
                    try {
                        // Try as a field
                        val userField = nodeInfo.javaClass.getDeclaredField("user")
                        userField.isAccessible = true
                        userField.get(nodeInfo)
                    } catch (e2: Exception) {
                        android.util.Log.w("MeshNodeInfo", "Error getting user (method and field): ${e.message}, ${e2.message}")
                        null
                    }
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
                    // Try getMethod first (includes inherited)
                    android.util.Log.d("MeshNodeInfo", "Trying getPosition() method...")
                    val result = nodeInfo.javaClass.getMethod("getPosition").invoke(nodeInfo)
                    android.util.Log.d("MeshNodeInfo", "getPosition() returned: $result")
                    result
                } catch (e: NoSuchMethodException) {
                    android.util.Log.d("MeshNodeInfo", "getPosition() method not found, trying field access...")
                    try {
                        // Try as a field
                        val positionField = nodeInfo.javaClass.getDeclaredField("position")
                        positionField.isAccessible = true
                        val result = positionField.get(nodeInfo)
                        android.util.Log.d("MeshNodeInfo", "position field value: $result")
                        result
                    } catch (e2: NoSuchFieldException) {
                        android.util.Log.w("MeshNodeInfo", "position field not found either. Trying alternative field names...")
                        // Try alternative field names
                        try {
                            val fields = nodeInfo.javaClass.declaredFields
                            android.util.Log.d("MeshNodeInfo", "Available fields: ${fields.map { it.name }.joinToString(", ")}")
                            // Try common alternatives
                            val altNames = listOf("pos", "location", "gpsPosition", "coordinates")
                            var found: Any? = null
                            for (name in altNames) {
                                try {
                                    val field = nodeInfo.javaClass.getDeclaredField(name)
                                    field.isAccessible = true
                                    found = field.get(nodeInfo)
                                    if (found != null) {
                                        android.util.Log.d("MeshNodeInfo", "Found position in field '$name': $found")
                                        break
                                    }
                                } catch (e3: Exception) {
                                    // Continue trying
                                }
                            }
                            found
                        } catch (e3: Exception) {
                            android.util.Log.w("MeshNodeInfo", "Error getting position (method and field): ${e.message}, ${e2.message}, ${e3.message}")
                            null
                        }
                    } catch (e2: Exception) {
                        android.util.Log.w("MeshNodeInfo", "Error accessing position field: ${e.message}, ${e2.message}")
                        null
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MeshNodeInfo", "Error getting position object: ${e.message}", e)
                    null
                }
                android.util.Log.d("MeshNodeInfo", "Position object: ${if (positionObj != null) "${positionObj.javaClass.name} = $positionObj" else "null"}")
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
                    // Try getMethod first (includes inherited)
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
                    } catch (e2: NoSuchMethodException) {
                        try {
                            // Try as a field
                            val lastHeardField = nodeInfo.javaClass.getDeclaredField("lastHeard")
                            lastHeardField.isAccessible = true
                            val value = lastHeardField.get(nodeInfo)
                            val result = when (value) {
                                is Int -> value
                                is Long -> value.toInt()
                                else -> (value as? Number)?.toInt() ?: 0
                            }
                            android.util.Log.d("MeshNodeInfo", "lastHeard (via field): $result")
                            result
                        } catch (e3: Exception) {
                            android.util.Log.w("MeshNodeInfo", "Could not get lastHeard (method and field): ${e.message}, ${e2.message}, ${e3.message}")
                            0
                        }
                    } catch (e2: Exception) {
                        android.util.Log.w("MeshNodeInfo", "Error getting lastHeard (getLastHeardTime): ${e.message}, ${e2.message}")
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

