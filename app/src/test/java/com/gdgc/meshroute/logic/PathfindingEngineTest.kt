package com.gdgc.meshroute.logic

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PathfindingEngineTest {

    private lateinit var engine: PathfindingEngine

    @Before
    fun setup() {
        engine = PathfindingEngine()
        
        // Setup a mock building:
        // R101 --(1)-- HallA --(1)-- ExitMain
        //  |            |
        // (1)          (5)  <-- Hallway B is "longer" or harder to traverse
        //  |            |
        // HallB --(1)-- ExitEmergency
        
        val adjacencies = mapOf(
            "R101" to listOf("HallA" to 1, "HallB" to 1),
            "HallA" to listOf("R101" to 1, "ExitMain" to 1, "HallB" to 5),
            "HallB" to listOf("R101" to 1, "ExitEmergency" to 1, "HallA" to 5),
            "ExitMain" to listOf("HallA" to 1),
            "ExitEmergency" to listOf("HallB" to 1)
        )
        engine.setupFloorplan(adjacencies)
    }

    @Test
    fun `test shortest path to nearest exit`() {
        val start = "R101"
        val exits = listOf("ExitMain", "ExitEmergency")
        val blocked = emptyList<String>()

        val path = engine.findNearestSafeExit(start, exits, blocked)
        
        // Should find ExitMain or ExitEmergency (both 2 steps away)
        assertNotNull(path)
        assertEquals(3, path?.size)
        assertTrue(path!!.contains("R101"))
        assertTrue(path.last() == "ExitMain" || path.last() == "ExitEmergency")
    }

    @Test
    fun `test rerouting when one exit is blocked`() {
        val start = "R101"
        val exits = listOf("ExitMain", "ExitEmergency")
        
        // Block HallA (leads to ExitMain)
        val blocked = listOf("HallA")

        val path = engine.findNearestSafeExit(start, exits, blocked)
        
        // Must go through HallB to ExitEmergency
        assertNotNull(path)
        assertEquals(listOf("R101", "HallB", "ExitEmergency"), path)
    }

    @Test
    fun `test no path found when all exits are blocked`() {
        val start = "R101"
        val exits = listOf("ExitMain", "ExitEmergency")
        
        // Block both hallways
        val blocked = listOf("HallA", "HallB")

        val path = engine.findNearestSafeExit(start, exits, blocked)
        
        assertNull("Should return null when trapped", path)
    }

    @Test
    fun `test Dijkstra chooses shortest weighted path`() {
        // R101 --(1)-- HallA --(1)-- ExitMain
        //  |            |
        // (1)          (10)  <-- Make HallA -> HallB very "expensive"
        //  |            |
        // HallB --(1)-- ExitEmergency
        
        engine.setupFloorplan(mapOf(
            "R101" to listOf("HallA" to 1, "HallB" to 1),
            "HallA" to listOf("ExitMain" to 1, "HallB" to 10),
            "HallB" to listOf("ExitEmergency" to 1, "HallA" to 10),
            "ExitMain" to listOf("HallA" to 1),
            "ExitEmergency" to listOf("HallB" to 1)
        ))

        // If ExitMain is blocked, should prefer R101 -> HallB -> ExitEmergency 
        // over R101 -> HallA -> HallB -> ExitEmergency
        val path = engine.findNearestSafeExit("R101", listOf("ExitEmergency"), listOf("ExitMain"))
        
        assertEquals(listOf("R101", "HallB", "ExitEmergency"), path)
    }

    @Test
    fun `test path when starting at an exit`() {
        val path = engine.findNearestSafeExit("ExitMain", listOf("ExitMain"), emptyList())
        assertEquals(listOf("ExitMain"), path)
    }
}
