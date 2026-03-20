package com.gdgc.meshroute.models

/**
 * Represents a single node in the building graph.
 * @property id Unique identifier for the node.
 * @property label Human-readable name.
 * @property isExit Whether this node is an evacuation exit.
 */
data class Node(
    val id: String,
    val label: String,
    val isExit: Boolean = false
)

/**
 * Represents the entire building floorplan.
 * @property nodes All nodes in the building.
 * @property adjacencies Adjacency list: node id -> list of neighbor ids.
 */
data class FloorMap(
    val nodes: Map<String, Node>,
    val adjacencies: Map<String, List<String>>
) {
    // Helper to get only the exit node IDs
    fun getExitNodes(): List<String> = nodes.values.filter { it.isExit }.map { it.id }
}
