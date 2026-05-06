package com.example.meshtracker_v1.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testy jednostkowe (JVM) dla [MeshPosition].
 * Nie wymagają emulatora — uruchamiają się lokalnie jako zwykłe testy JVM.
 */
class MeshPositionTest {

    // ------------------------------------------------------------------ isValid

    @Test
    fun `isValid - pozycja 0,0 jest nieważna`() {
        assertFalse(MeshPosition(0.0, 0.0, 0).isValid())
    }

    @Test
    fun `isValid - poprawna pozycja Piaseczna jest ważna`() {
        assertTrue(MeshPosition(52.0797, 21.0178, 100).isValid())
    }

    @Test
    fun `isValid - jedna niezerowa współrzędna wystarczy`() {
        assertTrue(MeshPosition(0.0, 21.0178, 0).isValid())
        assertTrue(MeshPosition(52.0797, 0.0, 0).isValid())
    }

    // ------------------------------------------------------------------ isInRange

    @Test
    fun `isInRange - Piaseczno jest w zakresie`() {
        assertTrue(MeshPosition(52.0797, 21.0178, 100).isInRange())
    }

    @Test
    fun `isInRange - lat poza zakresem -90 do 90`() {
        assertFalse(MeshPosition(91.0, 21.0, 0).isInRange())
        assertFalse(MeshPosition(-91.0, 21.0, 0).isInRange())
    }

    @Test
    fun `isInRange - lon poza zakresem -180 do 180`() {
        assertFalse(MeshPosition(52.0, 181.0, 0).isInRange())
        assertFalse(MeshPosition(52.0, -181.0, 0).isInRange())
    }

    @Test
    fun `isInRange - granice zakresu sa akceptowane`() {
        assertTrue(MeshPosition(90.0, 180.0, 0).isInRange())
        assertTrue(MeshPosition(-90.0, -180.0, 0).isInRange())
    }

    // ------------------------------------------------------------------ hasValidPosition (przez MeshNodeInfo)

    @Test
    fun `MeshNodeInfo - hasValidPosition zwraca false gdy position jest null`() {
        val node = MeshNodeInfo(
            num      = 1,
            user     = MeshUserInfo("!abc", "Test", "TST"),
            position = null
        )
        assertFalse(node.hasValidPosition())
    }

    @Test
    fun `MeshNodeInfo - hasValidPosition zwraca true dla prawidłowej pozycji`() {
        val node = MeshNodeInfo(
            num      = 1,
            user     = MeshUserInfo("!abc", "Test", "TST"),
            position = MeshPosition(52.0797, 21.0178, 100)
        )
        assertTrue(node.hasValidPosition())
    }

    @Test
    fun `MeshNodeInfo - hasValidPosition zwraca false dla pozycji 0,0`() {
        val node = MeshNodeInfo(
            num      = 1,
            user     = MeshUserInfo("!abc", "Test", "TST"),
            position = MeshPosition(0.0, 0.0, 0)
        )
        assertFalse(node.hasValidPosition())
    }

    // ------------------------------------------------------------------ isOnline

    @Test
    fun `MeshNodeInfo - isOnline zwraca false gdy lastHeard jest 0`() {
        val node = MeshNodeInfo(num = 1, user = null, position = null, lastHeard = 0)
        assertFalse(node.isOnline())
    }

    @Test
    fun `MeshNodeInfo - isOnline zwraca true gdy lastHeard jest teraz`() {
        val now = (System.currentTimeMillis() / 1000).toInt()
        val node = MeshNodeInfo(num = 1, user = null, position = null, lastHeard = now)
        assertTrue(node.isOnline())
    }

    @Test
    fun `MeshNodeInfo - isOnline zwraca false gdy lastHeard jest starszy niz 5 minut`() {
        val old = ((System.currentTimeMillis() / 1000) - 400).toInt() // 400 s temu
        val node = MeshNodeInfo(num = 1, user = null, position = null, lastHeard = old)
        assertFalse(node.isOnline())
    }

    // ------------------------------------------------------------------ getDisplayName / getId

    @Test
    fun `MeshNodeInfo - getDisplayName zwraca longName gdy dostepna`() {
        val node = MeshNodeInfo(
            num  = 42,
            user = MeshUserInfo("!abc", "Długa nazwa", "DN"),
            position = null
        )
        assertEquals("Długa nazwa", node.getDisplayName())
    }

    @Test
    fun `MeshNodeInfo - getDisplayName zwraca fallback gdy user jest null`() {
        val node = MeshNodeInfo(num = 42, user = null, position = null)
        assertEquals("Node 42", node.getDisplayName())
    }

    @Test
    fun `MeshNodeInfo - getId zwraca user id gdy dostepne`() {
        val node = MeshNodeInfo(
            num  = 42,
            user = MeshUserInfo("!cafebabe", "Test", "TST"),
            position = null
        )
        assertEquals("!cafebabe", node.getId())
    }

    @Test
    fun `MeshNodeInfo - getId zwraca node_num jako fallback`() {
        val node = MeshNodeInfo(num = 42, user = null, position = null)
        assertEquals("node_42", node.getId())
    }

    // ------------------------------------------------------------------ MeshUserInfo

    @Test
    fun `MeshUserInfo - getDisplayName zwraca longName gdy niepusta`() {
        val user = MeshUserInfo("!abc", "Pełna nazwa", "PN")
        assertEquals("Pełna nazwa", user.getDisplayName())
    }

    @Test
    fun `MeshUserInfo - getDisplayName wraca do shortName gdy longName pusta`() {
        val user = MeshUserInfo("!abc", "", "PN")
        assertEquals("PN", user.getDisplayName())
    }
}
