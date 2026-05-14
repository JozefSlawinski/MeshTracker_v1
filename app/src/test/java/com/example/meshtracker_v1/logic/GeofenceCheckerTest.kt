package com.example.meshtracker_v1.logic

import com.example.meshtracker_v1.model.ZoneVertex
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testy jednostkowe (JVM) dla [GeofenceChecker].
 * Testują algorytm ray-casting bez zależności od Androida.
 *
 * Układ współrzędnych: lat = oś Y (N↑), lon = oś X (E→)
 */
class GeofenceCheckerTest {

    // ------------------------------------------------------------------ pomocnicze

    /** Kwadrat jednostkowy: (0,0)→(1,0)→(1,1)→(0,1) w układzie (lat, lon). */
    private val unitSquare = listOf(
        ZoneVertex(lat = 0.0, lon = 0.0),
        ZoneVertex(lat = 1.0, lon = 0.0),
        ZoneVertex(lat = 1.0, lon = 1.0),
        ZoneVertex(lat = 0.0, lon = 1.0)
    )

    /** Trójkąt: wierzchołki (0,0), (2,1), (0,2). */
    private val triangle = listOf(
        ZoneVertex(lat = 0.0, lon = 0.0),
        ZoneVertex(lat = 2.0, lon = 1.0),
        ZoneVertex(lat = 0.0, lon = 2.0)
    )

    /** Realistyczny kwadrat wokół centrum Warszawy (≈52.2°N, 21.0°E). */
    private val warsawSquare = listOf(
        ZoneVertex(lat = 52.0, lon = 20.8),
        ZoneVertex(lat = 52.0, lon = 21.2),
        ZoneVertex(lat = 52.3, lon = 21.2),
        ZoneVertex(lat = 52.3, lon = 20.8)
    )

    /** Wielokąt wklęsły w kształcie litery L. */
    private val lShape = listOf(
        ZoneVertex(lat = 0.0, lon = 0.0),
        ZoneVertex(lat = 3.0, lon = 0.0),
        ZoneVertex(lat = 3.0, lon = 1.0),
        ZoneVertex(lat = 1.0, lon = 1.0),
        ZoneVertex(lat = 1.0, lon = 3.0),
        ZoneVertex(lat = 0.0, lon = 3.0)
    )

    private fun contains(lat: Double, lon: Double, polygon: List<ZoneVertex>) =
        GeofenceChecker.contains(lat, lon, polygon)

    // ================================================================== Kwadrat jednostkowy

    @Test
    fun `kwadrat - centrum wewnątrz`() {
        assertTrue(contains(0.5, 0.5, unitSquare))
    }

    @Test
    fun `kwadrat - punkt blisko lewej krawędzi wewnątrz`() {
        assertTrue(contains(0.5, 0.01, unitSquare))
    }

    @Test
    fun `kwadrat - punkt blisko prawej krawędzi wewnątrz`() {
        assertTrue(contains(0.5, 0.99, unitSquare))
    }

    @Test
    fun `kwadrat - punkt powyżej granicy na zewnątrz`() {
        assertFalse(contains(1.5, 0.5, unitSquare))
    }

    @Test
    fun `kwadrat - punkt poniżej granicy na zewnątrz`() {
        assertFalse(contains(-0.5, 0.5, unitSquare))
    }

    @Test
    fun `kwadrat - punkt na lewo na zewnątrz`() {
        assertFalse(contains(0.5, -0.5, unitSquare))
    }

    @Test
    fun `kwadrat - punkt na prawo na zewnątrz`() {
        assertFalse(contains(0.5, 1.5, unitSquare))
    }

    @Test
    fun `kwadrat - punkt daleko na zewnątrz`() {
        assertFalse(contains(100.0, 100.0, unitSquare))
    }

    // ================================================================== Trójkąt

    @Test
    fun `trójkąt - punkt blisko środka ciężkości wewnątrz`() {
        assertTrue(contains(0.8, 1.0, triangle))
    }

    @Test
    fun `trójkąt - punkt na zewnątrz nad wierzchołkiem`() {
        assertFalse(contains(3.0, 1.0, triangle))
    }

    @Test
    fun `trójkąt - punkt w rogu prostokąta ograniczającego ale poza trójkątem`() {
        assertFalse(contains(1.9, 0.1, triangle))
    }

    // ================================================================== Warszawa

    @Test
    fun `Warszawa - centrum wewnątrz`() {
        assertTrue(contains(52.23, 21.01, warsawSquare))
    }

    @Test
    fun `Warszawa - Kraków na zewnątrz`() {
        assertFalse(contains(50.06, 19.94, warsawSquare))
    }

    @Test
    fun `Warszawa - Gdańsk na zewnątrz`() {
        assertFalse(contains(54.35, 18.65, warsawSquare))
    }

    @Test
    fun `Warszawa - punkt tuż za wschodnią granicą na zewnątrz`() {
        assertFalse(contains(52.15, 21.25, warsawSquare))
    }

    @Test
    fun `Warszawa - punkt tuż za zachodnią granicą na zewnątrz`() {
        assertFalse(contains(52.15, 20.75, warsawSquare))
    }

    // ================================================================== Wielokąt wklęsły (L)

    @Test
    fun `L-shape - punkt w dolnej gałęzi wewnątrz`() {
        assertTrue(contains(0.5, 0.5, lShape))
    }

    @Test
    fun `L-shape - punkt w lewej gałęzi wewnątrz`() {
        assertTrue(contains(2.5, 0.5, lShape))
    }

    @Test
    fun `L-shape - punkt we wklęsłym narożniku na zewnątrz`() {
        // Punkt w "pustym" rogu litery L
        assertFalse(contains(2.0, 2.0, lShape))
    }

    @Test
    fun `L-shape - punkt w prawym górnym rogu bounding-box na zewnątrz`() {
        assertFalse(contains(2.5, 2.5, lShape))
    }

    // ================================================================== Przypadki brzegowe

    @Test
    fun `pusta lista wierzchołków zwraca false`() {
        assertFalse(contains(0.5, 0.5, emptyList()))
    }

    @Test
    fun `jeden wierzchołek zwraca false`() {
        assertFalse(contains(0.5, 0.5, listOf(ZoneVertex(0.5, 0.5))))
    }

    @Test
    fun `dwa wierzchołki (linia) zwracają false`() {
        assertFalse(
            contains(
                0.5, 0.5,
                listOf(ZoneVertex(0.0, 0.0), ZoneVertex(1.0, 1.0))
            )
        )
    }

    @Test
    fun `minimalna trójkąt z dokładnie 3 wierzchołkami działa`() {
        val triangle3 = listOf(
            ZoneVertex(0.0, 0.0),
            ZoneVertex(2.0, 0.0),
            ZoneVertex(1.0, 2.0)
        )
        assertTrue(contains(1.0, 0.5, triangle3))
        assertFalse(contains(3.0, 0.5, triangle3))
    }

    @Test
    fun `bardzo mały wielokąt (rzędu metrów) działa poprawnie`() {
        // ~100m x 100m kwadrat w okolicach Warszawy (1° ≈ 111km)
        val tinySquare = listOf(
            ZoneVertex(lat = 52.2300, lon = 21.0000),
            ZoneVertex(lat = 52.2309, lon = 21.0000),
            ZoneVertex(lat = 52.2309, lon = 21.0013),
            ZoneVertex(lat = 52.2300, lon = 21.0013)
        )
        // Centrum kwadratu
        assertTrue(contains(52.2305, 21.0007, tinySquare))
        // Punkt poza kwadratem
        assertFalse(contains(52.2295, 21.0007, tinySquare))
    }

    // ================================================================== computeNodeZoneMap
    // Testy integracyjne (Zone + org.json) należą do androidTest/GeofenceCheckerInstrumentedTest.
    // Tu sprawdzamy tylko zachowanie algorytmu z pustymi danymi przez bezpośrednie contains().

    @Test
    fun `contains - wielokąt z dokładnie 3 wierzchołkami jest akceptowany`() {
        val poly = listOf(
            ZoneVertex(0.0, 0.0),
            ZoneVertex(4.0, 0.0),
            ZoneVertex(2.0, 4.0)
        )
        assertTrue(contains(2.0, 1.0, poly))   // wewnątrz trójkąta
        assertFalse(contains(0.0, 3.0, poly))  // poza trójkątem
    }

    @Test
    fun `contains - algorytm poprawny dla wielokąta wypukłego z 6 wierzchołkami`() {
        // Regularny sześciokąt wpisany w kwadrat 2x2 wokół (1,1)
        val hexagon = listOf(
            ZoneVertex(lat = 0.0, lon = 1.0),
            ZoneVertex(lat = 0.5, lon = 0.0),
            ZoneVertex(lat = 1.5, lon = 0.0),
            ZoneVertex(lat = 2.0, lon = 1.0),
            ZoneVertex(lat = 1.5, lon = 2.0),
            ZoneVertex(lat = 0.5, lon = 2.0)
        )
        assertTrue(contains(1.0, 1.0, hexagon))   // centrum
        assertFalse(contains(0.1, 0.1, hexagon))  // narożnik bounding-box, poza
    }
}
