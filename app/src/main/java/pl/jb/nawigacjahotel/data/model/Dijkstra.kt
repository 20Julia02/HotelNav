package pl.jb.nawigacjahotel.data.model

import java.util.PriorityQueue

fun findShortestPath(
    graphData: NavigationGraph,
    startNodeId: Int,
    endNodeId: Int
): List<Int> {
    // Mapa sąsiedztwa na podstawie gotowych wag z pliku JSON
    val adjacencyMap = graphData.nodes.associate { node ->
        node.id to node.edges.associate { edge -> edge.to to edge.weight }
    }

    val distances = mutableMapOf<Int, Double>().withDefault { Double.MAX_VALUE }
    val previous = mutableMapOf<Int, Int?>()
    val queue = PriorityQueue<Pair<Int, Double>>(compareBy { it.second })

    distances[startNodeId] = 0.0
    queue.add(Pair(startNodeId, 0.0))

    while (queue.isNotEmpty()) {
        val (currentNode, currentDist) = queue.poll()

        if (currentNode == endNodeId) break
        if (currentDist > (distances[currentNode] ?: Double.MAX_VALUE)) continue

        adjacencyMap[currentNode]?.forEach { (neighbor, weight) ->
            val distanceThroughCurrent = currentDist + weight
            if (distanceThroughCurrent < (distances[neighbor] ?: Double.MAX_VALUE)) {
                distances[neighbor] = distanceThroughCurrent
                previous[neighbor] = currentNode
                queue.add(Pair(neighbor, distanceThroughCurrent))
            }
        }
    }

    val path = mutableListOf<Int>()
    var current: Int? = endNodeId
    while (current != null) {
        path.add(0, current)
        current = previous[current]
    }

    return if (path.isNotEmpty() && path.first() == startNodeId) path else emptyList()
}