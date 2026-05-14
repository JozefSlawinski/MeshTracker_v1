package com.example.meshtracker_v1.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {

    /** Kanał trwałego powiadomienia foreground serwisu (niski priorytet, bez dźwięku). */
    const val CHANNEL_ZONE_SERVICE = "zone_monitor_service"

    /** Kanał alertów ENTER/EXIT (wysoki priorytet, z wibracją). */
    const val CHANNEL_ZONE_ALERTS  = "zone_alerts"

    /** ID powiadomienia foreground — stałe przez cały czas życia serwisu. */
    const val NOTIFICATION_ID_FOREGROUND = 2001

    /** Tworzy oba kanały. Bezpieczne do wywołania wielokrotnie (idempotentne od API 26). */
    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val serviceChannel = NotificationChannel(
            CHANNEL_ZONE_SERVICE,
            "Monitor stref",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Aktywne monitorowanie stref geofencingu w tle"
            setShowBadge(false)
        }

        val alertChannel = NotificationChannel(
            CHANNEL_ZONE_ALERTS,
            "Alerty stref",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Węzeł wszedł lub opuścił strefę"
            enableLights(true)
            enableVibration(true)
        }

        manager.createNotificationChannels(listOf(serviceChannel, alertChannel))
    }
}
