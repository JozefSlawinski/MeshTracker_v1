package com.example.meshtracker_v1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.meshtracker_v1.ui.map.MapScreen
import com.example.meshtracker_v1.ui.theme.MeshTracker_v1Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeshTracker_v1Theme {
                MapScreen(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}