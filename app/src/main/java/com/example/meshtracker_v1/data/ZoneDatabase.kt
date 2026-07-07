package com.example.meshtracker_v1.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.meshtracker_v1.model.Zone
import com.example.meshtracker_v1.model.ZoneEvent

/**
 * Baza danych Room dla stref geofencingu.
 * exportSchema = false — brak eksportu schematu (projekt we wczesnej fazie).
 * Przy dodaniu migracji zmień na true i skonfiguruj katalog w build.gradle.kts.
 */
@Database(
    entities = [Zone::class, ZoneEvent::class],
    version = 2,
    exportSchema = false
)
abstract class ZoneDatabase : RoomDatabase() {
    abstract fun zoneDao(): ZoneDao

    companion object {
        /** Dodaje kolumny lat/lon do zone_events (lokalizacja węzła w chwili zdarzenia). */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE zone_events ADD COLUMN lat REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE zone_events ADD COLUMN lon REAL NOT NULL DEFAULT 0.0")
            }
        }
    }
}
