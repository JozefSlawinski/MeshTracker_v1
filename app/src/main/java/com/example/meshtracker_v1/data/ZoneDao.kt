package com.example.meshtracker_v1.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.meshtracker_v1.model.Zone
import com.example.meshtracker_v1.model.ZoneEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface ZoneDao {

    // ------------------------------------------------------------------ Strefy

    @Query("SELECT * FROM zones ORDER BY name ASC")
    fun getAllZones(): Flow<List<Zone>>

    @Query("SELECT * FROM zones ORDER BY name ASC")
    suspend fun getAllZonesOnce(): List<Zone>

    @Query("SELECT * FROM zones WHERE id = :id")
    suspend fun getZoneById(id: String): Zone?

    @Query("SELECT * FROM zones WHERE isActive = 1")
    suspend fun getActiveZones(): List<Zone>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZone(zone: Zone)

    @Update
    suspend fun updateZone(zone: Zone)

    @Query("DELETE FROM zones WHERE id = :id")
    suspend fun deleteZone(id: String)

    // ------------------------------------------------------------------ Zdarzenia

    @Query("SELECT * FROM zone_events WHERE zoneId = :zoneId ORDER BY timestampSeconds DESC")
    fun getEventsForZone(zoneId: String): Flow<List<ZoneEvent>>

    @Query("SELECT * FROM zone_events WHERE zoneId = :zoneId ORDER BY timestampSeconds ASC")
    suspend fun getEventsOnce(zoneId: String): List<ZoneEvent>

    @Query("SELECT COUNT(*) FROM zone_events WHERE zoneId = :zoneId")
    suspend fun countEventsForZone(zoneId: String): Int

    @Insert
    suspend fun insertEvent(event: ZoneEvent)

    @Query("DELETE FROM zone_events WHERE zoneId = :zoneId")
    suspend fun clearEventsForZone(zoneId: String)
}
