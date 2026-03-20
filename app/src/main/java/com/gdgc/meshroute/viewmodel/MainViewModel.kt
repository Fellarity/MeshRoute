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

    // Track received hazard reports
    private val _hazards = MutableStateFlow<List<HazardReport>>(emptyList())
    val hazards: StateFlow<List<HazardReport>> = _hazards.asStateFlow()

    // Track the current safe path
    private val _safePath = MutableStateFlow<List<String>>(emptyList())
    val safePath: StateFlow<List<String>> = _safePath.asStateFlow()

    private val floorMap = createMockFloorplan()

    init {
        pathfindingEngine.setupFloorplan(floorMap.adjacencies)
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
        
        // Broadcast via Mesh
        meshManager.sendHazardReport("ALL", newHazard.toPayload())
        
        // Upload to Firestore (Offline Persistence)
        viewModelScope.launch {
            firestoreSyncManager.uploadHazardReport(nodeName, description)
        }
        
        updatePath()
    }

    private fun updatePath() {
        val blockedNodes = _hazards.value.map { it.nodeName }
        val startNode = "Room 101" // Assume current location for demo
        val exitNode = "Exit Main"
        
        val path = pathfindingEngine.findShortestSafePath(startNode, exitNode, blockedNodes)
        _safePath.value = path ?: emptyList()
    }

    private fun createMockFloorplan(): FloorMap {
        val nodes = mapOf(
            "Room 101" to Node("Room 101", "Room 101"),
            "Hallway A" to Node("Hallway A", "Hallway A"),
            "Stairwell B" to Node("Stairwell B", "Stairwell B"),
            "Exit Main" to Node("Exit Main", "Main Exit", isExit = true)
        )
        val adjacencies = mapOf(
            "Room 101" to listOf("Hallway A"),
            "Hallway A" to listOf("Room 101", "Stairwell B", "Exit Main"),
            "Stairwell B" to listOf("Hallway A"),
            "Exit Main" to listOf("Hallway A")
        )
        return FloorMap(nodes, adjacencies)
    }
}
