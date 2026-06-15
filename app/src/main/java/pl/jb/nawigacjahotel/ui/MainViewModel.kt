package pl.jb.nawigacjahotel.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import pl.jb.nawigacjahotel.common.ResultState
import pl.jb.nawigacjahotel.data.model.CoordinateConverter
import pl.jb.nawigacjahotel.data.model.Feature
import pl.jb.nawigacjahotel.data.model.GraphNode
import pl.jb.nawigacjahotel.data.model.NavigationGraph
import pl.jb.nawigacjahotel.data.model.QrAttributes
import pl.jb.nawigacjahotel.data.model.findShortestPath
import pl.jb.nawigacjahotel.data.remote.RetrofitInstance
import java.io.IOException
import java.net.URLDecoder
import kotlin.math.pow

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private var navigationGraph: NavigationGraph? = null
    private val coordinateConverter = CoordinateConverter()

    private val _destinationMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val destinationMap: StateFlow<Map<String, Int>> = _destinationMap

    companion object {
        private const val DEFAULT_LAT = 52.2207
        private const val DEFAULT_LON = 21.0100
        private const val DEFAULT_FLOOR = 1
        private const val TAG = "MainViewModel"
    }

    data class NavigationState(
        val lat: Double,
        val lon: Double,
        val currentFloor: Int = DEFAULT_FLOOR,
        val isUserPosition: Boolean = false,
        val currentStartNodeId: Int? = null,
        val calculatedRoutePoints: List<Pair<Double, Double>> = emptyList()
    )

    private val _locationState = MutableStateFlow<ResultState<NavigationState>>(
        ResultState.Success(
            NavigationState(
                lat = DEFAULT_LAT,
                lon = DEFAULT_LON,
                currentFloor = DEFAULT_FLOOR,
                isUserPosition = false
            )
        )
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
                    .bufferedReader()
                    .use { it.readText() }

                val jsonConfig = Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }

                val graph = jsonConfig.decodeFromString<NavigationGraph>(jsonString)
                navigationGraph = graph

                Log.d(TAG, "Załadowano graf: nodes=${graph.nodes.size}")
                updateDestinationMapForFloor(DEFAULT_FLOOR)
            } catch (e: IOException) {
                Log.e(TAG, "Nie udało się wczytać graf_nawigacyjny.json", e)
            } catch (e: Exception) {
                Log.e(TAG, "Błąd parsowania graf_nawigacyjny.json", e)
            }
        }
    }

    private fun updateDestinationMapForFloor(floor: Int) {
        val graph = navigationGraph ?: return

        _destinationMap.value = graph.nodes
            .filter { node ->
                node.poziom == floor &&
                        !node.nazwa.isNullOrBlank() &&
                        node.coordinates.size >= 2
            }
            .distinctBy { it.nazwa }
            .sortedBy { it.nazwa }
            .associate { node -> node.nazwa!! to node.id }

        Log.d(TAG, "Zaktualizowano listę miejsc dla piętra=$floor, liczba=${_destinationMap.value.size}")
    }

    fun getNodeById(id: Int): GraphNode? {
        return navigationGraph?.nodes?.firstOrNull { it.id == id }
    }

    fun getNodeWgs84PointById(id: Int): Pair<Double, Double>? {
        val node = getNodeById(id) ?: return null
        return nodeCoordinatesToWgs84(node)
    }

    fun onQrScanned(result: String) {
        val qr = extractQrCode(result)
        Log.d(TAG, "QR raw='$result', extracted='$qr'")
        getLocationFromQr(qrRaw = result, qr = qr)
    }

    private fun getLocationFromQr(qrRaw: String, qr: String) {
        viewModelScope.launch {
            _locationState.value = ResultState.Loading

            try {
                val localNode = findLocalNodeByQr(qrRaw, qr)
                val apiFeature = runCatching { getApiFeatureForQr(qrRaw, qr) }
                    .onFailure { Log.w(TAG, "Nie udało się pobrać QR z API — używam danych lokalnych, jeśli są", it) }
                    .getOrNull()

                val floorFromQrText = extractFloorFromText(qrRaw) ?: extractFloorFromText(qr)
                val floorFromApi = apiFeature?.attributes?.toFloor()
                val floorFromNode = localNode?.poziom
                val finalFloor = floorFromQrText ?: floorFromApi ?: floorFromNode ?: DEFAULT_FLOOR

                Log.d(
                    TAG,
                    "QR floor: fromText=$floorFromQrText, fromApi=$floorFromApi, fromNode=$floorFromNode, FINAL=$finalFloor"
                )

                if (localNode != null && localNode.coordinates.size >= 2) {
                    val point = nodeCoordinatesToWgs84(localNode)
                        ?: throw IllegalStateException("Niepoprawne współrzędne dla QR: $qr")

                    updateDestinationMapForFloor(finalFloor)

                    _locationState.value = ResultState.Success(
                        NavigationState(
                            lat = point.first,
                            lon = point.second,
                            currentFloor = finalFloor,
                            isUserPosition = true,
                            currentStartNodeId = localNode.id,
                            calculatedRoutePoints = emptyList()
                        )
                    )
                    return@launch
                }

                val geometry = apiFeature?.geometry
                if (geometry != null) {
                    val point = rawCoordinatesToWgs84(geometry.x, geometry.y)
                        ?: throw IllegalStateException("Niepoprawne współrzędne z API dla QR: $qr")

                    updateDestinationMapForFloor(finalFloor)

                    _locationState.value = ResultState.Success(
                        NavigationState(
                            lat = point.first,
                            lon = point.second,
                            currentFloor = finalFloor,
                            isUserPosition = true,
                            currentStartNodeId = null,
                            calculatedRoutePoints = emptyList()
                        )
                    )
                    return@launch
                }

                _locationState.value = ResultState.Error(Exception("Nie znaleziono pozycji dla QR: $qr"))
            } catch (e: Exception) {
                Log.e(TAG, "Błąd obsługi QR", e)
                _locationState.value = ResultState.Error(e)
            }
        }
    }

    private fun findLocalNodeByQr(qrRaw: String, qr: String): GraphNode? {
        val graph = navigationGraph ?: return null
        val normalizedQr = normalizeQr(qr)
        val normalizedRaw = normalizeQr(qrRaw)

        val node = graph.nodes.firstOrNull { node ->
            val nodeQr = node.qr_text ?: return@firstOrNull false
            val normalizedNodeQr = normalizeQr(nodeQr)

            normalizedNodeQr == normalizedQr ||
                    normalizedNodeQr == normalizedRaw ||
                    (nodeQr.length >= 3 && qrRaw.contains(nodeQr, ignoreCase = true)) ||
                    (normalizedNodeQr.length >= 3 && normalizedRaw.contains(normalizedNodeQr))
        }

        Log.d(
            TAG,
            if (node != null) {
                "Znaleziono lokalny QR: id=${node.id}, poziom=${node.poziom}, qr_text=${node.qr_text}"
            } else {
                "Nie znaleziono lokalnego QR dla extracted='$qr'"
            }
        )

        return node
    }

    private suspend fun getApiFeatureForQr(qrRaw: String, qr: String): Feature? {
        val candidates = listOf(qr, extractQrCode(qrRaw), qrRaw)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        for (candidate in candidates) {
            val safeCandidate = candidate.replace("'", "''")
            val response = RetrofitInstance.api.getLocation(
                where = "qr_text='$safeCandidate'",
                outFields = "*",
                returnGeometry = true
            )

            val feature = response.features.firstOrNull()
            if (feature != null) {
                Log.d(TAG, "API QR match dla '$candidate': attributes=${feature.attributes}")
                return feature
            }
        }

        Log.d(TAG, "API nie zwróciło obiektu dla QR candidates=$candidates")
        return null
    }

    fun onDestinationSelected(destinationName: String) {
        val graph = navigationGraph ?: return
        val currentState = (_locationState.value as? ResultState.Success)?.data ?: return
        val startNodeId = currentState.currentStartNodeId ?: return
        val endNodeId = destinationMap.value[destinationName] ?: return

        viewModelScope.launch {
            val pathNodeIds = findShortestPath(graph, startNodeId, endNodeId)

            if (pathNodeIds.isNotEmpty()) {
                val geoPoints = pathNodeIds.mapNotNull { id ->
                    val node = graph.nodes.find { it.id == id }
                    node?.let { nodeCoordinatesToWgs84(it) }
                }

                _locationState.value = ResultState.Success(
                    currentState.copy(calculatedRoutePoints = geoPoints)
                )
            }
        }
    }

    private fun nodeCoordinatesToWgs84(node: GraphNode): Pair<Double, Double>? {
        if (node.coordinates.size < 2) return null

        val first = node.coordinates[0]
        val second = node.coordinates[1]
        val point = rawCoordinatesToWgs84(first, second)

        Log.d(
            TAG,
            "node=${node.id}, poziom=${node.poziom}, raw=[$first,$second], wgs84=$point"
        )

        return point
    }

    /**
     * Zwraca Pair(lat, lon).
     * Obsługuje zarówno WGS84, jak i PUWG 1992 / EPSG:2180 jako XY w metrach.
     */
    private fun rawCoordinatesToWgs84(first: Double, second: Double): Pair<Double, Double>? {
        val candidates = mutableListOf<Pair<Double, Double>>()

        // WGS84 zapisane jako [lon, lat], np. [21.01, 52.22]
        if (first in -180.0..180.0 && second in -90.0..90.0) {
            candidates.add(Pair(second, first))
        }

        // WGS84 zapisane jako [lat, lon], np. [52.22, 21.01]
        if (first in -90.0..90.0 && second in -180.0..180.0) {
            candidates.add(Pair(first, second))
        }

        // Dane metrowe, np. EPSG:2180. Próbujemy obu kolejności osi.
        runCatching { coordinateConverter.toWgs84(first, second) }
            .getOrNull()
            ?.let { candidates.add(it) }

        runCatching { coordinateConverter.toWgs84(second, first) }
            .getOrNull()
            ?.let { candidates.add(it) }

        return candidates
            .filter { it.first in 48.0..56.0 && it.second in 13.0..25.0 }
            .minByOrNull { point ->
                (point.first - DEFAULT_LAT).pow(2) + (point.second - DEFAULT_LON).pow(2)
            }
    }

    private fun extractQrCode(raw: String): String {
        val decoded = runCatching { URLDecoder.decode(raw.trim(), "UTF-8") }
            .getOrElse { raw.trim() }

        val patterns = listOf(
            Regex("""(?i)qr_text\s*=\s*['\"]?([^'\"&\s]+)"""),
            Regex("""(?i)(?:qr|code|kod|id)\s*=\s*['\"]?([^'\"&\s]+)""")
        )

        for (pattern in patterns) {
            val value = pattern.find(decoded)?.groupValues?.getOrNull(1)
            if (!value.isNullOrBlank()) return cleanToken(value)
        }

        return cleanToken(
            decoded
                .substringBefore("?")
                .substringAfterLast("/")
        )
    }

    private fun cleanToken(value: String): String {
        return value
            .trim()
            .trim('"', '\'', '`')
            .substringBefore("&")
            .substringBefore("#")
            .trim()
    }

    private fun normalizeQr(value: String?): String {
        if (value.isNullOrBlank()) return ""

        val extracted = extractQrCode(value)
        return extracted
            .lowercase()
            .replace(" ", "")
            .replace("-", "")
            .replace("_", "")
    }

    private fun extractFloorFromText(value: String?): Int? {
        if (value.isNullOrBlank()) return null

        val decoded = runCatching { URLDecoder.decode(value, "UTF-8") }
            .getOrElse { value }

        val lower = decoded.lowercase()
        if ("parter" in lower || "ground" in lower) return 1

        val patterns = listOf(
            Regex("""(?i)(?:poziom|pietro|pi[eę]tro|floor|level|floor_id)\s*[=:]\s*['\"]?(-?\d+)"""),
            Regex("""(?i)(?:poziom|pietro|pi[eę]tro|floor|level)[_/ -]+(-?\d+)""")
        )

        for (pattern in patterns) {
            val number = pattern.find(decoded)?.groupValues?.getOrNull(1)?.toIntOrNull()
            if (number != null) return number
        }

        return null
    }

    private fun QrAttributes.toFloor(): Int? {
        return poziom
            ?: floorId
            ?: extractFloorFromText(pietro)
            ?: extractFloorFromText(qrText)
    }
}
