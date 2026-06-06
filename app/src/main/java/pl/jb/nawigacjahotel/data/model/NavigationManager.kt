package pl.jb.nawigacjahotel.data.model

import android.content.Context
import kotlinx.serialization.json.Json

class NavigationManager(private val context: Context) {
    private var navigationGraph: NavigationGraph? = null

    fun loadGraph() {
        val jsonString = context.assets.open("graf_nawigacyjny.json").bufferedReader().use { it.readText() }
        val jsonConfig = Json { ignoreUnknownKeys = true }
        navigationGraph = jsonConfig.decodeFromString<NavigationGraph>(jsonString)
    }

    fun getRoute(fromNodeId: Int, toNodeId: Int): List<GraphNode> {
        val graph = navigationGraph ?: return emptyList()
        val nodeIds = findShortestPath(graph, fromNodeId, toNodeId)

        return nodeIds.mapNotNull { id -> graph.nodes.find { it.id == id } }
    }
}