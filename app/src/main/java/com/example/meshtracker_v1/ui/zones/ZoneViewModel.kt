package com.example.meshtracker_v1.ui.zones

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meshtracker_v1.model.Zone
import com.example.meshtracker_v1.model.ZoneEvent
import com.example.meshtracker_v1.model.ZoneVertex
import com.example.meshtracker_v1.repository.ZoneRepository
import com.example.meshtracker_v1.service.ZoneMonitorService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ZoneViewModel @Inject constructor(
    private val zoneRepository: ZoneRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // ------------------------------------------------------------------ Lista stref

    /** Wszystkie strefy posortowane alfabetycznie — obserwowany Flow z Room. */
    val allZones: StateFlow<List<Zone>> = zoneRepository.allZones
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Start / stop ZoneMonitorService zależnie od tego czy istnieje ≥1 aktywna strefa
        viewModelScope.launch {
            zoneRepository.allZones.collect { zones ->
                if (zones.any { it.isActive }) {
                    ZoneMonitorService.start(context)
                } else {
                    ZoneMonitorService.stop(context)
                }
            }
        }
    }

    // ------------------------------------------------------------------ Zaznaczona strefa

    private val _selectedZoneId = MutableStateFlow<String?>(null)
    val selectedZoneId: StateFlow<String?> = _selectedZoneId.asStateFlow()

    fun selectZone(id: String?) { _selectedZoneId.value = id }

    // ------------------------------------------------------------------ Maszyna stanów rysowania

    private val _drawingState = MutableStateFlow<DrawingState>(DrawingState.Idle)
    val drawingState: StateFlow<DrawingState> = _drawingState.asStateFlow()

    /** Przechodzi do trybu rysowania. Poprzedni stan jest kasowany. */
    fun startDrawing() {
        _drawingState.value = DrawingState.Drawing(emptyList())
    }

    /**
     * Dodaje wierzchołek do aktualnie rysowanego wielokąta.
     * Ignorowany gdy nie jesteśmy w stanie Drawing.
     */
    fun addVertex(lat: Double, lon: Double) {
        val state = _drawingState.value as? DrawingState.Drawing ?: return
        _drawingState.value = state.copy(
            vertices = state.vertices + ZoneVertex(lat, lon)
        )
    }

    /**
     * Usuwa ostatni dodany wierzchołek.
     * Gdy lista wierzchołków jest pusta — wraca do Idle (anuluje rysowanie).
     */
    fun removeLastVertex() {
        val state = _drawingState.value as? DrawingState.Drawing ?: return
        _drawingState.value = if (state.vertices.isEmpty()) {
            DrawingState.Idle
        } else {
            state.copy(vertices = state.vertices.dropLast(1))
        }
    }

    /**
     * Zamyka wielokąt i przechodzi do stanu Confirming (dialog nazwy + węzłów).
     * Wymaga ≥ 3 wierzchołków — inaczej ignorowane.
     */
    fun closePolygon() {
        val state = _drawingState.value as? DrawingState.Drawing ?: return
        if (state.canClose) {
            _drawingState.value = DrawingState.Confirming(state.vertices)
        }
    }

    /** Anuluje rysowanie i wraca do stanu Idle. */
    fun cancelDrawing() {
        _drawingState.value = DrawingState.Idle
    }

    /**
     * Zatwierdza nową strefę: zapisuje do Room i wraca do Idle.
     * Wywołać po wypełnieniu dialogu potwierdzenia.
     */
    fun confirmZone(name: String, colorArgb: Int, watchedNodeIds: List<String>) {
        val state = _drawingState.value as? DrawingState.Confirming ?: return
        viewModelScope.launch {
            val zone = Zone.create(
                name = name.trim().ifEmpty { "Strefa" },
                colorArgb = colorArgb,
                vertices = state.vertices,
                watchedNodeIds = watchedNodeIds
            )
            zoneRepository.insertZone(zone)
            _drawingState.value = DrawingState.Idle
        }
    }

    // ------------------------------------------------------------------ CRUD stref

    /** Włącza lub wyłącza strefę (toggle). */
    fun toggleActive(zone: Zone) = viewModelScope.launch {
        zoneRepository.setActive(zone, !zone.isActive)
    }

    /** Usuwa strefę wraz z jej logiem zdarzeń (CASCADE w bazie). */
    fun deleteZone(zoneId: String) = viewModelScope.launch {
        if (_selectedZoneId.value == zoneId) _selectedZoneId.value = null
        zoneRepository.deleteZone(zoneId)
    }

    /** Aktualizuje listę monitorowanych węzłów dla strefy. */
    fun updateWatchedNodes(zone: Zone, nodeIds: List<String>) = viewModelScope.launch {
        zoneRepository.updateWatchedNodes(zone, nodeIds)
    }

    // ------------------------------------------------------------------ Log zdarzeń

    /** Flow zdarzeń ENTER/EXIT dla konkretnej strefy. */
    fun getEventsForZone(zoneId: String): Flow<List<ZoneEvent>> =
        zoneRepository.getEventsForZone(zoneId)

    /** Usuwa log zdarzeń dla strefy. */
    fun clearEvents(zoneId: String) = viewModelScope.launch {
        zoneRepository.clearEventsForZone(zoneId)
    }

    // ------------------------------------------------------------------ Sealed class

    sealed class DrawingState {

        /** Normalny tryb — brak rysowania. */
        object Idle : DrawingState()

        /**
         * Tryb rysowania — użytkownik dodaje wierzchołki long-pressem.
         * @param vertices wierzchołki dodane do tej pory
         */
        data class Drawing(val vertices: List<ZoneVertex>) : DrawingState() {
            /** True gdy można zamknąć wielokąt (≥ 3 wierzchołki). */
            val canClose: Boolean get() = vertices.size >= 3
            /** True gdy można cofnąć ostatni wierzchołek. */
            val canUndo: Boolean get() = vertices.isNotEmpty()
        }

        /**
         * Wielokąt zamknięty — wyświetlany dialog nazwy i wyboru węzłów.
         * @param vertices zatwierdzone wierzchołki wielokąta
         */
        data class Confirming(val vertices: List<ZoneVertex>) : DrawingState()
    }
}
