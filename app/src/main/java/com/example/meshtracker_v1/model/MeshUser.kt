package com.example.meshtracker_v1.model

/**
 * Wrapper dla użytkownika węzła Meshtastic.
 * Używa klasy z pakietu org.meshtastic.core.model.MeshUser.
 */
data class MeshUserInfo(
    val id: String,
    val longName: String,
    val shortName: String,
    val hwModelString: String? = null,
    val isLicensed: Boolean = false,
    val role: Int = 0
) {
    /**
     * Zwraca nazwę wyświetlaną (longName lub shortName).
     */
    fun getDisplayName(): String {
        return longName.ifEmpty { shortName }
    }
    
    /**
     * Zwraca model sprzętu jako String.
     */
    fun getHardwareModel(): String {
        return hwModelString ?: "Unknown"
    }
    
    companion object {
        /**
         * Tworzy MeshUserInfo z org.meshtastic.core.model.MeshUser.
         * Używa reflection, jeśli klasa nie jest dostępna bezpośrednio.
         */
        fun fromMeshtasticUser(user: Any?): MeshUserInfo? {
            if (user == null) {
                android.util.Log.d("MeshUserInfo", "fromMeshtasticUser: user is null")
                return null
            }
            
            android.util.Log.d("MeshUserInfo", "Parsing user object: ${user.javaClass.name}")
            
            return try {
                // List available methods for debugging
                try {
                    val methods = user.javaClass.methods
                    android.util.Log.d("MeshUserInfo", "Available methods: ${methods.map { it.name }.joinToString(", ")}")
                    val fields = user.javaClass.declaredFields
                    android.util.Log.d("MeshUserInfo", "Available fields: ${fields.map { it.name }.joinToString(", ")}")
                } catch (e: Exception) {
                    android.util.Log.w("MeshUserInfo", "Could not list methods/fields: ${e.message}")
                }
                
                // Try different ways to get ID
                val id = try {
                    try {
                        android.util.Log.d("MeshUserInfo", "Trying getId()...")
                        val result = user.javaClass.getMethod("getId").invoke(user) as? String
                        android.util.Log.d("MeshUserInfo", "getId() succeeded: $result")
                        result
                    } catch (e: NoSuchMethodException) {
                        android.util.Log.d("MeshUserInfo", "getId() not found, trying getUserId()...")
                        try {
                            val result = user.javaClass.getMethod("getUserId").invoke(user) as? String
                            android.util.Log.d("MeshUserInfo", "getUserId() succeeded: $result")
                            result
                        } catch (e2: NoSuchMethodException) {
                            android.util.Log.d("MeshUserInfo", "getUserId() not found, trying getIdBytes()...")
                            try {
                                // Try getIdBytes() and convert to string
                                val idBytes = user.javaClass.getMethod("getIdBytes").invoke(user) as? ByteArray
                                val result = idBytes?.let { String(it) }
                                android.util.Log.d("MeshUserInfo", "getIdBytes() succeeded: $result")
                                result
                            } catch (e3: NoSuchMethodException) {
                                android.util.Log.d("MeshUserInfo", "getIdBytes() not found, trying field access...")
                                try {
                                    // Try as a field
                                    val idField = user.javaClass.getDeclaredField("id")
                                    idField.isAccessible = true
                                    val fieldValue = idField.get(user)
                                    val result = when (fieldValue) {
                                        is String -> fieldValue
                                        is ByteArray -> String(fieldValue)
                                        else -> fieldValue?.toString() ?: ""
                                    }
                                    android.util.Log.d("MeshUserInfo", "Field 'id' access succeeded: $result")
                                    result
                                } catch (e4: Exception) {
                                    android.util.Log.d("MeshUserInfo", "Could not get ID (all methods failed): ${e.message}, ${e2.message}, ${e3.message}, ${e4.message}")
                                    ""
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MeshUserInfo", "Unexpected error getting ID: ${e.message}", e)
                    ""
                } ?: ""
                android.util.Log.d("MeshUserInfo", "id: $id")
                
                val longName = try {
                    user.javaClass.getMethod("getLongName").invoke(user) as? String ?: ""
                } catch (e: NoSuchMethodException) {
                    try {
                        val field = user.javaClass.getDeclaredField("longName")
                        field.isAccessible = true
                        (field.get(user) as? String) ?: ""
                    } catch (e2: Exception) {
                        android.util.Log.d("MeshUserInfo", "Could not get longName: ${e.message}, ${e2.message}")
                        ""
                    }
                } ?: ""
                android.util.Log.d("MeshUserInfo", "longName: $longName")
                
                val shortName = try {
                    user.javaClass.getMethod("getShortName").invoke(user) as? String ?: ""
                } catch (e: NoSuchMethodException) {
                    try {
                        val field = user.javaClass.getDeclaredField("shortName")
                        field.isAccessible = true
                        (field.get(user) as? String) ?: ""
                    } catch (e2: Exception) {
                        android.util.Log.d("MeshUserInfo", "Could not get shortName: ${e.message}, ${e2.message}")
                        ""
                    }
                } ?: ""
                android.util.Log.d("MeshUserInfo", "shortName: $shortName")
                
                val hwModelString = try {
                    user.javaClass.getMethod("getHwModelString").invoke(user) as? String
                } catch (e: Exception) {
                    android.util.Log.d("MeshUserInfo", "getHwModelString not available: ${e.message}")
                    null
                }
                
                val isLicensed = try {
                    (user.javaClass.getMethod("isLicensed").invoke(user) as? Boolean) ?: false
                } catch (e: Exception) {
                    android.util.Log.d("MeshUserInfo", "isLicensed not available: ${e.message}")
                    false
                }
                
                val role = try {
                    val roleValue = try {
                        user.javaClass.getMethod("getRole").invoke(user)
                    } catch (e: NoSuchMethodException) {
                        try {
                            val field = user.javaClass.getDeclaredField("role")
                            field.isAccessible = true
                            field.get(user)
                        } catch (e2: Exception) {
                            android.util.Log.d("MeshUserInfo", "Could not get role (method and field): ${e.message}, ${e2.message}")
                            null
                        }
                    }
                    android.util.Log.d("MeshUserInfo", "getRole() returned: $roleValue (type: ${roleValue?.javaClass?.name})")
                    when (roleValue) {
                        is Int -> roleValue
                        is Number -> roleValue.toInt()
                        null -> 0
                        else -> {
                            android.util.Log.w("MeshUserInfo", "Unexpected role type: ${roleValue.javaClass.name}")
                            0
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MeshUserInfo", "Error getting role: ${e.message}", e)
                    0
                }
                
                android.util.Log.d("MeshUserInfo", "Parsed role: $role")
                
                val result = MeshUserInfo(id, longName, shortName, hwModelString, isLicensed, role)
                android.util.Log.d("MeshUserInfo", "Successfully created MeshUserInfo: ${result.getDisplayName()}, role: ${result.role}")
                result
            } catch (e: Exception) {
                android.util.Log.e("MeshUserInfo", "Error parsing user: ${e.message}", e)
                e.printStackTrace()
                null
            }
        }
    }
}

