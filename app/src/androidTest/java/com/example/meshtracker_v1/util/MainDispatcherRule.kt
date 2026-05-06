package com.example.meshtracker_v1.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit Rule zastępująca Dispatchers.Main na czas testu.
 *
 * Konieczna, ponieważ [com.example.meshtracker_v1.ui.map.MapViewModel]
 * uruchamia coroutiny na viewModelScope (Main dispatcher).
 *
 * Domyślnie używa [UnconfinedTestDispatcher], który wykonuje coroutiny natychmiast
 * (bez potrzeby wywoływania advanceUntilIdle()).
 */
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description?) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description?) {
        Dispatchers.resetMain()
    }
}
