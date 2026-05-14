package com.example.meshtracker_v1.di

import android.content.Context
import androidx.room.Room
import com.example.meshtracker_v1.data.ZoneDao
import com.example.meshtracker_v1.data.ZoneDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideZoneDatabase(
        @ApplicationContext context: Context
    ): ZoneDatabase = Room.databaseBuilder(
        context,
        ZoneDatabase::class.java,
        "zone_database"
    ).build()

    @Provides
    @Singleton
    fun provideZoneDao(database: ZoneDatabase): ZoneDao = database.zoneDao()
}
