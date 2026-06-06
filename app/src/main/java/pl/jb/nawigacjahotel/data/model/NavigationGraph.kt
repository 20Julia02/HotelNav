package pl.jb.nawigacjahotel.data.model

import kotlinx.serialization.Serializable

@Serializable
data class NavigationGraph(
    val nodes: List<GraphNode>
)

@Serializable
data class GraphNode(
    val id: Int,
    val nazwa: String?,
    val poziom: Int?,
    val qr_text: String?,
    val coordinates: List<Double>, // [lon, lat]
    val edges: List<GraphEdge>
)

@Serializable
data class GraphEdge(
    val to: Int,
    val weight: Double
)