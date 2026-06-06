package pl.jb.nawigacjahotel.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.arcgismaps.Color
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.PolylineBuilder
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.CompositeSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

import pl.jb.nawigacjahotel.R
import pl.jb.nawigacjahotel.common.ResultState

private const val ROOMS_LAYER_URL =
    "https://arcgis.cenagis.edu.pl/server/rest/services/SION2_Topo_MV/sion2_wms_topo_GG_f19/MapServer/5"

fun createMap(lat: Double, lon: Double): ArcGISMap {
    val roomServiceTable = ServiceFeatureTable(ROOMS_LAYER_URL)
    val featureLayerRooms = FeatureLayer.createWithFeatureTable(roomServiceTable)

    return ArcGISMap(BasemapStyle.ArcGISTopographic).apply {
        initialViewpoint = Viewpoint(
            latitude = lat,
            longitude = lon,
            scale = 2000.0
        )
        operationalLayers.add(featureLayerRooms)
    }
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.locationState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isSuggestionsVisible by remember { mutableStateOf(false) }

    // Lista pobierana z kluczy mapy zdefiniowanej w ViewModelu
    val allSuggestions = viewModel.destinationMap.keys.toList()

    val filteredSuggestions = allSuggestions.filter {
        it.contains(searchQuery, true)
    }

    val barcodeLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            viewModel.onQrScanned(result.contents)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val currentState = state) {
            is ResultState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is ResultState.Success -> {
                val navigationData = currentState.data

                // Pamiętamy mapę dla danych współrzędnych
                val map = remember(navigationData.lat, navigationData.lon) {
                    createMap(navigationData.lat, navigationData.lon)
                }

                // Generowanie nakładki graficznej (punkty i linie trasy)
                val graphicsOverlay = remember(navigationData) {
                    GraphicsOverlay().apply {

                        // 1. RYSOWANIE KROPKI LOKALIZACJI UŻYTKOWNIKA (Z KODU QR)
                        if (navigationData.isUserPosition) {
                            val userPoint = Point(navigationData.lon, navigationData.lat, SpatialReference.wgs84())

                            // Efekt poświaty wokół kropki (Google Blue, 20% widoczności)
                            val outerHalo = SimpleMarkerSymbol(
                                style = SimpleMarkerSymbolStyle.Circle,
                                color = Color.fromRgba(26, 115, 232, 51),
                                size = 24f
                            )

                            // Wewnętrzny rdzeń z białą obwódką
                            val innerCore = SimpleMarkerSymbol(
                                style = SimpleMarkerSymbolStyle.Circle,
                                color = Color.fromRgba(26, 115, 232, 255),
                                size = 12f
                            ).apply {
                                outline = SimpleLineSymbol(
                                    style = SimpleLineSymbolStyle.Solid,
                                    color = Color.white,
                                    width = 1.5f
                                )
                            }

                            val compositeSymbol = CompositeSymbol(listOf(outerHalo, innerCore))
                            graphics.add(Graphic(geometry = userPoint, symbol = compositeSymbol))
                        }

                        // 2. RYSOWANIE LINII TRASY (WYNIK DIJKSTRY)
                        if (navigationData.calculatedRoutePoints.isNotEmpty()) {
                            // Konwersja par (Lat, Lon) na punkty ArcGIS
                            val routePoints = navigationData.calculatedRoutePoints.map { (lat, lon) ->
                                Point(lon, lat, SpatialReference.wgs84())
                            }

                            // Budowanie linii łamanej łączącej punkty
                            val polylineBuilder = PolylineBuilder(SpatialReference.wgs84()).apply {
                                routePoints.forEach { addPoint(it) }
                            }
                            val routeGeometry = polylineBuilder.toGeometry()

                            // Styl czerwonej linii nawigacyjnej
                            val lineSymbol = SimpleLineSymbol(
                                style = SimpleLineSymbolStyle.Solid,
                                color = Color.fromRgba(232, 26, 26, 255),
                                width = 4f
                            )
                            graphics.add(Graphic(geometry = routeGeometry, symbol = lineSymbol))

                            // Dodanie małego znacznika (X) na końcu trasy (celu podróży)
                            routePoints.lastOrNull()?.let { endPoint ->
                                val finishMarker = SimpleMarkerSymbol(
                                    style = SimpleMarkerSymbolStyle.X,
                                    color = Color.fromRgba(232, 26, 26, 255),
                                    size = 14f
                                )
                                graphics.add(Graphic(geometry = endPoint, symbol = finishMarker))
                            }
                        }
                    }
                }

                MapView(
                    modifier = Modifier.fillMaxSize(),
                    arcGISMap = map,
                    graphicsOverlays = listOf(graphicsOverlay)
                )
            }

            is ResultState.Error -> {
                Text(
                    text = "Błąd: ${currentState.throwable.message}",
                    modifier = Modifier.align(Alignment.Center),
                    color = ComposeColor.Red,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // PANEL GÓRNY (Wyszukiwarka, Tytuł i Skaner QR)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .align(Alignment.TopCenter)
        ) {
            Text(
                text = "Hotel Navigator",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                color = ComposeColor.Black
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        isSuggestionsVisible = it.isNotEmpty()
                    },
                    placeholder = {
                        Text("Wyszukaj miejsce...", color = ComposeColor.Gray)
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = ComposeColor.Gray
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ComposeColor.White.copy(alpha = 0.85f),
                        unfocusedContainerColor = ComposeColor.White.copy(alpha = 0.75f),
                        focusedBorderColor = ComposeColor.Transparent,
                        unfocusedBorderColor = ComposeColor.Transparent
                    )
                )

                Card(
                    modifier = Modifier.size(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ComposeColor.White.copy(alpha = 0.75f)
                    )
                ) {
                    IconButton(
                        onClick = {
                            val options = ScanOptions().apply {
                                setCaptureActivity(ScannerActivity::class.java)
                                setPrompt("Zeskanuj kod QR")
                                setBeepEnabled(true)
                                setOrientationLocked(true)
                            }
                            barcodeLauncher.launch(options)
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.qrcodescanicon),
                                contentDescription = null,
                                tint = ComposeColor.Black
                            )
                            Text(
                                text = "QR",
                                fontSize = 10.sp,
                                color = ComposeColor.Black
                            )
                        }
                    }
                }
            }

            // LISTA PODPOWIEDZI WYSZUKIWANIA
            if (isSuggestionsVisible && filteredSuggestions.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ComposeColor.White.copy(alpha = 0.95f)
                    )
                ) {
                    LazyColumn {
                        items(filteredSuggestions) { suggestion ->
                            Text(
                                text = suggestion,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        searchQuery = suggestion
                                        isSuggestionsVisible = false
                                        // Wywołanie przeliczenia ścieżki w ViewModelu
                                        viewModel.onDestinationSelected(suggestion)
                                    }
                                    .padding(16.dp),
                                color = ComposeColor.Black
                            )
                            HorizontalDivider(color = ComposeColor.LightGray.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}