package com.gdgc.meshroute.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gdgc.meshroute.logic.PathfindingEngine
import com.gdgc.meshroute.models.FloorMap
import com.gdgc.meshroute.models.HazardReport
import com.gdgc.meshroute.models.Node
import com.gdgc.meshroute.network.MeshNetworkManager
import com.gdgc.meshroute.sync.FirestoreSyncManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val meshManager: MeshNetworkManager,
    private val pathfindingEngine: PathfindingEngine,
    private val firestoreSyncManager: FirestoreSyncManager
) : ViewModel() {

    private val _hazards = MutableStateFlow<List<HazardReport>>(emptyList())
    val hazards: StateFlow<List<HazardReport>> = _hazards.asStateFlow()

    private val _safePath = MutableStateFlow<List<String>>(emptyList())
    val safePath: StateFlow<List<String>> = _safePath.asStateFlow()

    private val floorMap = createMockFloorplan()

    init {
        // Convert to weighted format for Dijkstra
        val weightedAdjacencies = floorMap.adjacencies.mapValues { entry ->
            entry.value.map { neighborId -> neighborId to 1 } // Using weight 1 for all edges for now
        }
        pathfindingEngine.setupFloorplan(weightedAdjacencies)
        observeNetworkHazards()
        updatePath()
    }

    private fun observeNetworkHazards() {
        viewModelScope.launch {
            meshManager.receivedHazards.collect { payload ->
                payload?.let {
                    HazardReport.fromPayload(it)?.let { report ->
                        if (!_hazards.value.any { h -> h.id == report.id }) {
                            _hazards.value = _hazards.value + report
                            updatePath()
                        }
                    }
                }
            }
        }
    }

    fun reportHazard(nodeName: String, description: String) {
        val newHazard = HazardReport(nodeName = nodeName, description = description)
        _hazards.value = _hazards.value + newHazard
        meshManager.sendHazardReport("ALL", newHazard.toPayload())
        viewModelScope.launch { firestoreSyncManager.uploadHazardReport(nodeName, description) }
        updatePath()
    }

    private fun updatePath() {
        val blockedNodes = _hazards.value.map { it.nodeName }
        val startNode = "Room 101"
        val exitNodes = floorMap.getExitNodes()
        
        // Now find the nearest exit dynamically using Dijkstra
        val path = pathfindingEngine.findNearestSafeExit(startNode, exitNodes, blockedNodes)
        _safePath.value = path ?: emptyList()
    }

    private fun createMockFloorplan(): FloorMap {
        val nodes = mapOf(
            "Room 101" to Node("Room 101", "Room 101"),
            "Hallway A" to Node("Hallway A", "Hallway A"),
            "Hallway B" to Node("Hallway B", "Hallway B"),
            "Exit Main" to Node("Exit Main", "Main Exit", isExit = true),
            "Exit Emergency" to Node("Exit Emergency", "Fire Exit", isExit = true)
        )
        // Adjacency list: Room 101 is connected to both Hallways.
        // Each Hallway leads to a different Exit.
        val adjacencies = mapOf(
            "Room 101" to listOf("Hallway A", "Hallway B"),
            "Hallway A" to listOf("Room 101", "Exit Main"),
            "Hallway B" to listOf("Room 101", "Exit Emergency"),
            "Exit Main" to listOf("Hallway A"),
            "Exit Emergency" to listOf("Hallway B")
        )
        return FloorMap(nodes, adjacencies)
    }
}
