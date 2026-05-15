package com.example.meshtracker_v1.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_REFRESH_INTERVAL    = intPreferencesKey("refresh_interval_seconds")
        val KEY_ONLINE_THRESHOLD    = intPreferencesKey("online_threshold_minutes")
        val KEY_DEFAULT_ONLINE_FILTER = booleanPreferencesKey("default_online_filter")
        val KEY_DEFAULT_GPS_FILTER  = booleanPreferencesKey("default_gps_filter")
        val KEY_MAP_TYPE            = intPreferencesKey("map_type")
        val KEY_HISTORY_MAX_POINTS        = intPreferencesKey("history_max_points")
        val KEY_HISTORY_MIN_DIST_M        = intPreferencesKey("history_min_distance_m")
        val KEY_SHOW_ALL_TRACKS           = booleanPreferencesKey("show_all_tracks")
        val KEY_EXPECTED_BROADCAST_INTERVAL = intPreferencesKey("expected_broadcast_interval")

        const val DEFAULT_REFRESH_INTERVAL           = 5
        const val DEFAULT_ONLINE_THRESHOLD           = 5   // minutes
        const val DEFAULT_MAP_TYPE                   = 0   // NORMAL
        const val DEFAULT_HISTORY_MAX_POINTS         = 50
        const val DEFAULT_HISTORY_MIN_DIST_M         = 20
        const val DEFAULT_SHOW_ALL_TRACKS            = true
        const val DEFAULT_EXPECTED_BROADCAST_INTERVAL = 30  // seconds
    }

    val refreshIntervalSeconds: Flow<Int> = context.dataStore.data
        .map { it[KEY_REFRESH_INTERVAL] ?: DEFAULT_REFRESH_INTERVAL }

    val onlineThresholdMinutes: Flow<Int> = context.dataStore.data
        .map { it[KEY_ONLINE_THRESHOLD] ?: DEFAULT_ONLINE_THRESHOLD }

    val defaultOnlineFilter: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_DEFAULT_ONLINE_FILTER] ?: false }

    val defaultGpsFilter: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_DEFAULT_GPS_FILTER] ?: false }

    val mapType: Flow<Int> = context.dataStore.data
        .map { it[KEY_MAP_TYPE] ?: DEFAULT_MAP_TYPE }

    val historyMaxPoints: Flow<Int> = context.dataStore.data
        .map { it[KEY_HISTORY_MAX_POINTS] ?: DEFAULT_HISTORY_MAX_POINTS }

    val historyMinDistanceM: Flow<Int> = context.dataStore.data
        .map { it[KEY_HISTORY_MIN_DIST_M] ?: DEFAULT_HISTORY_MIN_DIST_M }

    val showAllTracks: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_SHOW_ALL_TRACKS] ?: DEFAULT_SHOW_ALL_TRACKS }

    val expectedBroadcastInterval: Flow<Int> = context.dataStore.data
        .map { it[KEY_EXPECTED_BROADCAST_INTERVAL] ?: DEFAULT_EXPECTED_BROADCAST_INTERVAL }

    suspend fun setRefreshInterval(seconds: Int) =
        context.dataStore.edit { it[KEY_REFRESH_INTERVAL] = seconds }

    suspend fun setOnlineThreshold(minutes: Int) =
        context.dataStore.edit { it[KEY_ONLINE_THRESHOLD] = minutes }

    suspend fun setDefaultOnlineFilter(enabled: Boolean) =
        context.dataStore.edit { it[KEY_DEFAULT_ONLINE_FILTER] = enabled }

    suspend fun setDefaultGpsFilter(enabled: Boolean) =
        context.dataStore.edit { it[KEY_DEFAULT_GPS_FILTER] = enabled }

    suspend fun setMapType(type: Int) =
        context.dataStore.edit { it[KEY_MAP_TYPE] = type }

    suspend fun setHistoryMaxPoints(points: Int) =
        context.dataStore.edit { it[KEY_HISTORY_MAX_POINTS] = points }

    suspend fun setHistoryMinDistanceM(distance: Int) =
        context.dataStore.edit { it[KEY_HISTORY_MIN_DIST_M] = distance }

    suspend fun setShowAllTracks(enabled: Boolean) =
        context.dataStore.edit { it[KEY_SHOW_ALL_TRACKS] = enabled }

    suspend fun setExpectedBroadcastInterval(seconds: Int) =
        context.dataStore.edit { it[KEY_EXPECTED_BROADCAST_INTERVAL] = seconds }

    suspend fun resetToDefaults() =
        context.dataStore.edit { it.clear() }
}
