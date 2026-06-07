package pl.jb.nawigacjahotel.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import pl.jb.nawigacjahotel.common.ResultState
import pl.jb.nawigacjahotel.data.model.NavigationGraph
import pl.jb.nawigacjahotel.data.model.findShortestPath
import pl.jb.nawigacjahotel.data.remote.RetrofitInstance
import java.io.IOException
import pl.jb.nawigacjahotel.data.model.GraphNode
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private var navigationGraph: NavigationGraph? = null

    // Zaktualizowane mapowanie na rzeczywiste ID (Int) z pliku grafu
    private val _destinationMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val destinationMap: StateFlow<Map<String, Int>> = _destinationMap

    companion object {
        private const val DEFAULT_LAT = 52.2207
        private const val DEFAULT_LON = 21.0100
    }

    data class NavigationState(
        val lat: Double,
        val lon: Double,
        val isUserPosition: Boolean = false,
        val currentStartNodeId: Int? = null,
        val calculatedRoutePoints: List<Pair<Double, Double>> = emptyList()
    )

    private val _locationState = MutableStateFlow<ResultState<NavigationState>>(
        ResultState.Success(NavigationState(DEFAULT_LAT, DEFAULT_LON, false))
    )
    val locationState: StateFlow<ResultState<NavigationState>> = _locationState

    init {
        loadGraphFromAssets()
    }

    private fun loadGraphFromAssets() {
        viewModelScope.launch {
            try {
                val jsonString = getApplication<Application>().assets
                    .open("graf_nawigacyjny.json")
                    .bufferedReader().use { it.readText() }

                val jsonConfig = Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }

                val graph = jsonConfig.decodeFromString<NavigationGraph>(jsonString)
                navigationGraph = graph

                _destinationMap.value = graph.nodes
                    .filter { node ->
                        node.poziom == 1 &&
                                !node.nazwa.isNullOrBlank() &&
                                node.coordinates.size >= 2
                    }
                    .distinctBy { it.nazwa }
                    .sortedBy { it.nazwa }
                    .associate { node ->
                        node.nazwa!! to node.id
                    }

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun getNodeById(id: Int): GraphNode? {
        return navigationGraph?.nodes?.firstOrNull { it.id == id }
    }

    fun onQrScanned(result: String) {
        val lastPart = result.substringAfterLast("/")
        getLocationFromQr(lastPart)
    }

    private fun getLocationFromQr(qr: String) {
        viewModelScope.launch {
            _locationState.value = ResultState.Loading
            try {
                // Szukamy w naszym lokalnym grafie węzła o takim samym kodzie QR
                val localNode = navigationGraph?.nodes?.find { it.qr_text == qr }

                if (localNode != null && localNode.coordinates.size >= 2) {
                    val lon = localNode.coordinates[0]
                    val lat = localNode.coordinates[1]

                    _locationState.value = ResultState.Success(
                        NavigationState(
                            lat = lat,
                            lon = lon,
                            isUserPosition = true,
                            currentStartNodeId = localNode.id,
                            calculatedRoutePoints = emptyList() // Reset trasy przy nowym skanie
                        )
                    )
                } else {
                    // Jeśli nie ma w lokalnym, odpytujemy API (stara logika awaryjna)
                    val response = RetrofitInstance.api.getLocation(where = "qr_text='$qr'")
                    val geometry = response.features.firstOrNull()?.geometry

                    if (geometry != null) {
                        // Współrzędne z serwera (zakładamy, że podaje lat/lon bezpośrednio, bez konwertera)
                        val lat = geometry.y
                        val lon = geometry.x

                        _locationState.value = ResultState.Success(
                            NavigationState(
                                lat = lat,
                                lon = lon,
                                isUserPosition = true,
                                currentStartNodeId = null,
                                calculatedRoutePoints = emptyList()
                            )
                        )
                    } else {
                        _locationState.value = ResultState.Error(Exception("Nie znaleziono pozycji dla QR: $qr"))
                    }
                }
            } catch (e: Exception) {
                _locationState.value = ResultState.Error(e)
            }
        }
    }

    fun onDestinationSelected(destinationName: String) {
        val graph = navigationGraph ?: return
        val currentState = (_locationState.value as? ResultState.Success)?.data ?: return
        val startNodeId = currentState.currentStartNodeId

        if (startNodeId == null) {
            // Brak zeskanowanego punktu startowego z grafu
            return
        }

        val endNodeId = destinationMap.value[destinationName] ?: return

        viewModelScope.launch {
            val pathNodeIds = findShortestPath(graph, startNodeId, endNodeId)

            if (pathNodeIds.isNotEmpty()) {
                // Mapujemy listę ID węzłów na pary współrzędnych (Lat, Lon) z grafu
                val geoPoints = pathNodeIds.mapNotNull { id ->
                    val node = graph.nodes.find { it.id == id }
                    if (node != null && node.coordinates.size >= 2) {
                        // Zwracamy Pair(lat, lon) -> w JSON kolejność to [lon, lat]
                        Pair(node.coordinates[1], node.coordinates[0])
                    } else null
                }

                _locationState.value = ResultState.Success(
                    currentState.copy(calculatedRoutePoints = geoPoints)
                )
            }
        }
    }
}