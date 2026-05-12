package com.example.meshtracker_v1.ui.nodes

data class NodeFilterState(
    val searchQuery: String = "",
    val showOnlineOnly: Boolean = false,
    val showWithGpsOnly: Boolean = false,
    val maxDistanceKm: Float? = null
) {
    val isActive: Boolean
        get() = searchQuery.isNotEmpty() || showOnlineOnly || showWithGpsOnly || maxDistanceKm != null

    fun cleared() = NodeFilterState()
}
