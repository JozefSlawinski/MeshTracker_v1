package com.example.meshtracker_v1

import android.app.Application
import com.example.meshtracker_v1.util.NotificationChannels
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MeshTrackerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Kanały powiadomień muszą być utworzone przed pierwszym notify() / startForeground()
        NotificationChannels.createAll(this)
    }
}
