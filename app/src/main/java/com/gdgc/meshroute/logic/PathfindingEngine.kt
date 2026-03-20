package com.gdgc.meshroute.logic

import java.util.*

class PathfindingEngine {

    /**
     * Represents a basic building floorplan as an adjacency list.
     * Keys are node names (e.g., "Room 101", "Stairwell A"), values are connected nodes.
     */
    private val floorplanGraph = mutableMapOf<String, List<String>>()

    fun setupFloorplan(graph: Map<String, List<String>>) {
        floorplanGraph.clear()
        floorplanGraph.putAll(graph)
    }

    /**
     * Finds the shortest safe path using Breadth-First Search (BFS).
     * @param startNode Starting location.
     * @param exitNode Target exit node.
     * @param blockedNodes Nodes that are currently hazardous and should be avoided.
     * @return A list of node names representing the path, or null if no path exists.
     */
    fun findShortestSafePath(
        startNode: String,
        exitNode: String,
        blockedNodes: List<String>
    ): List<String>? {
        if (startNode == exitNode) return listOf(startNode)
        if (blockedNodes.contains(startNode) || blockedNodes.contains(exitNode)) return null

        val queue: Queue<List<String>> = LinkedList()
        val visited = mutableSetOf<String>()

        queue.add(listOf(startNode))
        visited.add(startNode)

        while (queue.isNotEmpty()) {
            val path = queue.poll() ?: continue
            val lastNode = path.last()

            val neighbors = floorplanGraph[lastNode] ?: emptyList()
            for (neighbor in neighbors) {
                if (!visited.contains(neighbor) && !blockedNodes.contains(neighbor)) {
                    val newPath = path + neighbor
                    if (neighbor == exitNode) {
                        return newPath
                    }
                    visited.add(neighbor)
                    queue.add(newPath)
                }
            }
        }

        return null // No safe path found
    }
}
