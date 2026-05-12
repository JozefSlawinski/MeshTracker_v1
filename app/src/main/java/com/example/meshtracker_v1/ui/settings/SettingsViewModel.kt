package com.example.meshtracker_v1.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meshtracker_v1.data.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences
) : ViewModel() {

    val refreshInterval = prefs.refreshIntervalSeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferences.DEFAULT_REFRESH_INTERVAL)

    val onlineThreshold = prefs.onlineThresholdMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferences.DEFAULT_ONLINE_THRESHOLD)

    val defaultOnlineFilter = prefs.defaultOnlineFilter
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val defaultGpsFilter = prefs.defaultGpsFilter
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val mapType = prefs.mapType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferences.DEFAULT_MAP_TYPE)

    val historyMaxPoints = prefs.historyMaxPoints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferences.DEFAULT_HISTORY_MAX_POINTS)

    val historyMinDistanceM = prefs.historyMinDistanceM
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferences.DEFAULT_HISTORY_MIN_DIST_M)

    fun setRefreshInterval(seconds: Int) = viewModelScope.launch { prefs.setRefreshInterval(seconds) }
    fun setOnlineThreshold(minutes: Int) = viewModelScope.launch { prefs.setOnlineThreshold(minutes) }
    fun setDefaultOnlineFilter(enabled: Boolean) = viewModelScope.launch { prefs.setDefaultOnlineFilter(enabled) }
    fun setDefaultGpsFilter(enabled: Boolean) = viewModelScope.launch { prefs.setDefaultGpsFilter(enabled) }
    fun setMapType(type: Int) = viewModelScope.launch { prefs.setMapType(type) }
    fun setHistoryMaxPoints(points: Int) = viewModelScope.launch { prefs.setHistoryMaxPoints(points) }
    fun setHistoryMinDistanceM(distance: Int) = viewModelScope.launch { prefs.setHistoryMinDistanceM(distance) }
    fun resetToDefaults() = viewModelScope.launch { prefs.resetToDefaults() }
}
