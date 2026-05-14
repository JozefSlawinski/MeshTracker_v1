package com.example.meshtracker_v1.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.meshtracker_v1.model.Zone
import com.example.meshtracker_v1.model.ZoneEvent

/**
 * Baza danych Room dla stref geofencingu.
 * exportSchema = false — brak eksportu schematu (projekt we wczesnej fazie).
 * Przy dodaniu migracji zmień na true i skonfiguruj katalog w build.gradle.kts.
 */
@Database(
    entities = [Zone::class, ZoneEvent::class],
    version = 1,
    exportSchema = false
)
abstract class ZoneDatabase : RoomDatabase() {
    abstract fun zoneDao(): ZoneDao
}
