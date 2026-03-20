package com.gdgc.meshroute.logic

import java.util.*

/**
 * Advanced Pathfinding Engine using Dijkstra's Algorithm.
 * Supports weighted edges (e.g., distance, hazard levels) and multiple exits.
 */
class PathfindingEngine {

    // Node ID -> List of (Neighbor ID, Weight)
    private val floorplanGraph = mutableMapOf<String, MutableList<Pair<String, Int>>>()

    /**
     * Set up the floorplan with weights.
     * @param adjacencies Map of Node ID to list of Pair(Neighbor ID, Distance/Weight)
     */
    fun setupFloorplan(adjacencies: Map<String, List<Pair<String, Int>>>) {
        floorplanGraph.clear()
        adjacencies.forEach { (node, neighbors) ->
            floorplanGraph[node] = neighbors.toMutableList()
        }
    }

    /**
     * Finds the optimal safe path to the nearest available exit.
     * @param startNode Current user location.
     * @param exitNodes List of all possible exit nodes in the building.
     * @param blockedNodes Nodes to avoid entirely.
     * @return The shortest safe path as a list of IDs, or null if trapped.
     */
    fun findNearestSafeExit(
        startNode: String,
        exitNodes: List<String>,
        blockedNodes: List<String>
    ): List<String>? {
        if (blockedNodes.contains(startNode)) return null

        val distances = mutableMapOf<String, Int>().withDefault { Int.MAX_VALUE }
        val previousNodes = mutableMapOf<String, String?>()
        val priorityQueue = PriorityQueue<Pair<String, Int>>(compareBy { it.second })

        distances[startNode] = 0
        priorityQueue.add(startNode to 0)

        val visited = mutableSetOf<String>()

        while (priorityQueue.isNotEmpty()) {
            val (currentNode, currentDist) = priorityQueue.poll()!!

            if (currentNode in visited) continue
            visited.add(currentNode)

            // If we reached an exit, we can stop early (Dijkstra guarantees it's the shortest)
            if (exitNodes.contains(currentNode)) {
                return reconstructPath(currentNode, previousNodes)
            }

            floorplanGraph[currentNode]?.forEach { (neighbor, weight) ->
                if (!blockedNodes.contains(neighbor) && !visited.contains(neighbor)) {
                    val newDist = currentDist + weight
                    if (newDist < distances.getValue(neighbor)) {
                        distances[neighbor] = newDist
                        previousNodes[neighbor] = currentNode
                        priorityQueue.add(neighbor to newDist)
                    }
                }
            }
        }

        return null
    }

    private fun reconstructPath(targetNode: String, previousNodes: Map<String, String?>): List<String> {
        val path = mutableListOf<String>()
        var current: String? = targetNode
        while (current != null) {
            path.add(0, current)
            current = previousNodes[current]
        }
        return path
    }
}
