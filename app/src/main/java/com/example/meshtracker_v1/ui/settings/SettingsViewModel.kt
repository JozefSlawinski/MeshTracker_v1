package com.example.meshtracker_v1.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meshtracker_v1.data.AppPreferences
import com.example.meshtracker_v1.model.MeshNodeInfo
import com.example.meshtracker_v1.repository.PacketStatsRepository
import com.example.meshtracker_v1.repository.PositionHistoryRepository
import com.example.meshtracker_v1.util.CsvExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences,
    private val positionHistoryRepository: PositionHistoryRepository,
    private val packetStatsRepository: PacketStatsRepository,
    private val csvExporter: CsvExporter
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

    val showAllTracks = prefs.showAllTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferences.DEFAULT_SHOW_ALL_TRACKS)

    val expectedBroadcastInterval = prefs.expectedBroadcastInterval
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferences.DEFAULT_EXPECTED_BROADCAST_INTERVAL)

    private val _exportEvent = Channel<Result<Uri>>(Channel.BUFFERED)
    val exportEvent = _exportEvent.receiveAsFlow()

    fun setRefreshInterval(seconds: Int) = viewModelScope.launch { prefs.setRefreshInterval(seconds) }
    fun setOnlineThreshold(minutes: Int) = viewModelScope.launch { prefs.setOnlineThreshold(minutes) }
    fun setDefaultOnlineFilter(enabled: Boolean) = viewModelScope.launch { prefs.setDefaultOnlineFilter(enabled) }
    fun setDefaultGpsFilter(enabled: Boolean) = viewModelScope.launch { prefs.setDefaultGpsFilter(enabled) }
    fun setMapType(type: Int) = viewModelScope.launch { prefs.setMapType(type) }
    fun setHistoryMaxPoints(points: Int) = viewModelScope.launch { prefs.setHistoryMaxPoints(points) }
    fun setHistoryMinDistanceM(distance: Int) = viewModelScope.launch { prefs.setHistoryMinDistanceM(distance) }
    fun setShowAllTracks(enabled: Boolean) = viewModelScope.launch { prefs.setShowAllTracks(enabled) }
    fun setExpectedBroadcastInterval(seconds: Int) = viewModelScope.launch { prefs.setExpectedBroadcastInterval(seconds) }
    fun resetToDefaults() = viewModelScope.launch { prefs.resetToDefaults() }

    fun exportSession(nodes: Map<String, MeshNodeInfo>) {
        viewModelScope.launch {
            val result = csvExporter.export(nodes = nodes)
            _exportEvent.send(result)
        }
    }

    fun clearAllData() {
        positionHistoryRepository.clearAll()
        packetStatsRepository.resetAll()
    }
}
