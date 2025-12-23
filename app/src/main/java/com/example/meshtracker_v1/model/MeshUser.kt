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
            if (user == null) return null
            
            return try {
                val id = user.javaClass.getMethod("getId").invoke(user) as? String ?: ""
                val longName = user.javaClass.getMethod("getLongName").invoke(user) as? String ?: ""
                val shortName = user.javaClass.getMethod("getShortName").invoke(user) as? String ?: ""
                val hwModelString = try {
                    user.javaClass.getMethod("getHwModelString").invoke(user) as? String
                } catch (e: Exception) {
                    null
                }
                val isLicensed = (user.javaClass.getMethod("isLicensed").invoke(user) as? Boolean) ?: false
                val role = (user.javaClass.getMethod("getRole").invoke(user) as? Int) ?: 0
                
                MeshUserInfo(id, longName, shortName, hwModelString, isLicensed, role)
            } catch (e: Exception) {
                null
            }
        }
    }
}

