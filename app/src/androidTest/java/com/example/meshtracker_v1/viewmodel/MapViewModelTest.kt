package com.example.meshtracker_v1.viewmodel

import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.meshtracker_v1.HiltTestActivity
import com.example.meshtracker_v1.fake.FakeMeshRepository
import com.example.meshtracker_v1.model.MeshNodeInfo
import com.example.meshtracker_v1.model.MeshPosition
import com.example.meshtracker_v1.model.MeshUserInfo
import com.example.meshtracker_v1.simulation.NodeSimulator
import com.example.meshtracker_v1.simulation.PredefinedScenarios
import com.example.meshtracker_v1.ui.map.MapViewModel
import com.example.meshtracker_v1.util.MainDispatcherRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MapViewModelTest {

    // ------------------------------------------------------------------ rules

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val mainDispatcherRule = MainDispatcherRule()

    // ------------------------------------------------------------------ injections

    /** Ten sam singleton co ten wstrzyknięty do MapViewModel — możemy nim sterować w testach. */
    @Inject
    lateinit var fakeRepository: FakeMeshRepository

    private lateinit var viewModel: MapViewModel

    // ------------------------------------------------------------------ setup / teardown

    @Before
    fun setUp() {
        hiltRule.inject()

        // Uruchom pustą Activity z @AndroidEntryPoint, żeby Hilt mógł zbudować ViewModel.
        ActivityScenario.launch(HiltTestActivity::class.java).onActivity { activity ->
            viewModel = ViewModelProvider(activity)[MapViewModel::class.java]
        }
    }

    @After
    fun tearDown() {
        fakeRepository.reset()
    }

    // ================================================================== TESTY POŁĄCZENIA

    @Test
    fun `po emitConnected stan połączenia zmienia się na CONNECTED`() = runTest {
        fakeRepository.emitConnected()

        assertEquals(MapViewModel.ConnectionState.CONNECTED, viewModel.connectionState.value)
    }

    @Test
    fun `po emitDisconnected stan połączenia zmienia się na DISCONNECTED`() = runTest {
        fakeRepository.emitConnected()
        fakeRepository.emitDisconnected()

        assertEquals(MapViewModel.ConnectionState.DISCONNECTED, viewModel.connectionState.value)
    }

    // ================================================================== TESTY WĘZŁÓW

    @Test
    fun `po emitNodeChanged węzeł pojawia się w nodes`() = runTest {
        fakeRepository.emitConnected()

        val node = buildNode("!test0001", "Testowy", 52.0797, 21.0178)
        fakeRepository.emitNodeChanged(node)

        assertTrue(viewModel.nodes.value.containsKey("!test0001"))
    }

    @Test
    fun `cztery węzły ze staticCluster pojawiają się w nodes`() = runTest {
        fakeRepository.emitConnected()

        PredefinedScenarios.staticCluster().forEach { route ->
            fakeRepository.emitNodeChanged(route.initialNode)
        }

        assertEquals(4, viewModel.nodes.value.size)
    }

    @Test
    fun `getNodesWithPosition zwraca tylko węzły z prawidłową pozycją`() = runTest {
        fakeRepository.emitConnected()

        // węzeł z pozycją
        fakeRepository.emitNodeChanged(buildNode("!pos0001", "Z pozycją", 52.0797, 21.0178))
        // węzeł BEZ pozycji
        fakeRepository.emitNodeChanged(
            MeshNodeInfo(
                num  = 2,
                user = MeshUserInfo(id = "!pos0002", longName = "Bez pozycji", shortName = "BP"),
                position = null
            )
        )

        assertEquals(1, viewModel.getNodesWithPosition().size)
    }

    @Test
    fun `po updateNodePosition współrzędne węzła zmieniają się`() = runTest {
        fakeRepository.emitConnected()

        val nodeId = "!move001"
        fakeRepository.emitNodeChanged(buildNode(nodeId, "Ruchomy", 52.0797, 21.0178))

        val newLat = 52.0820
        val newLon = 21.0210
        fakeRepository.updateNodePosition(nodeId, newLat, newLon)

        val node = viewModel.nodes.value[nodeId]
        assertNotNull("Węzeł powinien istnieć", node)
        assertEquals(newLat, node!!.position!!.latitude,  0.00001)
        assertEquals(newLon, node.position!!.longitude, 0.00001)
    }

    @Test
    fun `selectNode ustawia selectedNodeId`() = runTest {
        fakeRepository.emitConnected()
        fakeRepository.emitNodeChanged(buildNode("!sel001", "Wybrany", 52.08, 21.01))

        viewModel.selectNode("!sel001")

        assertEquals("!sel001", viewModel.selectedNodeId.value)
    }

    // ================================================================== TESTY SYMULATORA

    @Test
    fun `NodeSimulator - staticCluster dodaje 4 węzły`() = runTest {
        fakeRepository.emitConnected()

        val simulator = NodeSimulator(fakeRepository, scope = this)
        simulator.runScenario(PredefinedScenarios.staticCluster(intervalMs = 500L))

        // Statyczne węzły pojawiają się natychmiast (runRoute emituje initialNode)
        assertEquals(4, viewModel.nodes.value.size)

        simulator.stopAll()
    }

    @Test
    fun `NodeSimulator - movingPatrol zmienia pozycję węzłów w czasie`() = runTest {
        fakeRepository.emitConnected()

        val routes    = PredefinedScenarios.movingPatrol(intervalMs = 500L)
        val simulator = NodeSimulator(fakeRepository, scope = this)
        simulator.runScenario(routes)

        // Zapamiętaj pozycje startowe
        val startPositions = viewModel.nodes.value.mapValues {
            it.value.position?.let { p -> Pair(p.latitude, p.longitude) }
        }

        // Odczekaj 3 kroki
        advanceTimeBy(500L * 3 + 50L)

        // Przynajmniej jeden węzeł powinien się przesunąć
        var movedCount = 0
        viewModel.nodes.value.forEach { (id, node) ->
            val start = startPositions[id] ?: return@forEach
            val cur   = node.position ?: return@forEach
            if (cur.latitude != start.first || cur.longitude != start.second) movedCount++
        }

        assertTrue("Co najmniej jeden węzeł powinien zmienić pozycję", movedCount >= 1)

        simulator.stopAll()
    }

    @Test
    fun `NodeSimulator - convergingToCenter kończy w centrum`() = runTest {
        fakeRepository.emitConnected()

        val routes    = PredefinedScenarios.convergingToCenter(intervalMs = 300L)
        val simulator = NodeSimulator(fakeRepository, scope = this)
        simulator.runScenario(routes)

        // Trasy mają 4 punkty — przejdź przez wszystkie
        advanceTimeBy(300L * 5)

        // Węzły !aabb0002, 0003, 0004 powinny być w centrum (52.0797, 21.0178)
        listOf("!aabb0002", "!aabb0003", "!aabb0004").forEach { nodeId ->
            val node = viewModel.nodes.value[nodeId]
            assertNotNull("Węzeł $nodeId powinien istnieć", node)
            assertEquals("Węzeł $nodeId powinien być w centrum (lat)",
                52.0797, node!!.position!!.latitude, 0.0001)
            assertEquals("Węzeł $nodeId powinien być w centrum (lon)",
                21.0178, node.position!!.longitude, 0.0001)
        }

        simulator.stopAll()
    }

    // ================================================================== helpers

    private fun buildNode(
        id: String,
        name: String,
        lat: Double,
        lon: Double,
        alt: Int = 100
    ) = MeshNodeInfo(
        num      = id.hashCode(),
        user     = MeshUserInfo(id = id, longName = name, shortName = name.take(4).uppercase()),
        position = MeshPosition(
            latitude  = lat,
            longitude = lon,
            altitude  = alt,
            time      = (System.currentTimeMillis() / 1000).toInt()
        ),
        lastHeard = (System.currentTimeMillis() / 1000).toInt()
    )
}
