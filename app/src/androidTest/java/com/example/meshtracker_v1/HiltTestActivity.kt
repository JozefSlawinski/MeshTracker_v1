package com.example.meshtracker_v1

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Pusta Activity używana wyłącznie w testach instrumentowanych z Hilt.
 * Umożliwia tworzenie ViewModeli w kontekście hiltowego grafu zależności.
 */
@AndroidEntryPoint
class HiltTestActivity : ComponentActivity()
