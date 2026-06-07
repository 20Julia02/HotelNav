package pl.jb.nawigacjahotel.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalFocusManager
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
import com.arcgismaps.mapping.symbology.HorizontalAlignment
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.TextSymbol
import com.arcgismaps.mapping.symbology.VerticalAlignment
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

import pl.jb.nawigacjahotel.R
import pl.jb.nawigacjahotel.common.ResultState
import android.graphics.BitmapFactory
import androidx.core.graphics.drawable.toDrawable
private const val ROOMS_LAYER_URL =
    "https://arcgis.cenagis.edu.pl/server/rest/services/SION2_Topo_MV/sion2_wms_topo_GG_f19/MapServer/5"

fun createMap(lat: Double, lon: Double): ArcGISMap {
    val roomServiceTable = ServiceFeatureTable(ROOMS_LAYER_URL)

    val featureLayerRooms = FeatureLayer.createWithFeatureTable(roomServiceTable).apply {
        labelsEnabled = false
    }

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

    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }
    var isSuggestionsVisible by remember { mutableStateOf(false) }

    val destinationMap by viewModel.destinationMap.collectAsState()

    val appContext = androidx.compose.ui.platform.LocalContext.current

    val destinationPinSymbol = remember {
        val bitmap = BitmapFactory.decodeResource(
            appContext.resources,
            R.drawable.pin_destination
        )

        PictureMarkerSymbol.createWithImage(
            bitmap.toDrawable(appContext.resources)
        ).apply {
            width = 40f
            height = 40f

            // Czubek pinezki ma wskazywać dokładny punkt docelowy.
            offsetY = 16f
        }
    }

    val allSuggestions = destinationMap.keys.toList()

    val filteredSuggestions = if (searchQuery.isBlank()) {
        allSuggestions
    } else {
        allSuggestions.filter {
            it.contains(searchQuery, ignoreCase = true)
        }
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

                val map = remember(navigationData.lat, navigationData.lon) {
                    createMap(navigationData.lat, navigationData.lon)
                }

                val roomLabelsOverlay = remember(destinationMap) {
                    GraphicsOverlay().apply {
                        // Etykiety pojawiają się dopiero po odpowiednim przybliżeniu.
                        // Większa wartość = pokażą się wcześniej.
                        // Mniejsza wartość = trzeba mocniej przybliżyć.
                        minScale = 1000.0

                        destinationMap.forEach { (placeName, placeId) ->
                            val placeNode = viewModel.getNodeById(placeId)

                            if (placeNode != null && placeNode.coordinates.size >= 2) {
                                val placePoint = Point(
                                    placeNode.coordinates[0],
                                    placeNode.coordinates[1],
                                    SpatialReference.wgs84()
                                )

                                val labelText = if (placeName.startsWith("Pokój", ignoreCase = true)) {
                                    placeName.removePrefix("Pokój").trim()
                                } else {
                                    placeName
                                }

                                val textSymbol = TextSymbol(
                                    labelText,
                                    Color.white,
                                    15f,
                                    HorizontalAlignment.Center,
                                    VerticalAlignment.Middle
                                ).apply {
                                    haloColor = Color.fromRgba(0, 0, 0, 190)
                                    haloWidth = 2.5f

                                    // Jeśli Twoja wersja SDK tego nie obsługuje,
                                    // usuń te dwie linie.
                                    fontWeight = com.arcgismaps.mapping.symbology.FontWeight.Bold
                                    fontFamily = "Sans Serif"
                                }

                                graphics.add(
                                    Graphic(
                                        geometry = placePoint,
                                        symbol = textSymbol
                                    )
                                )
                            }
                        }
                    }
                }

                val graphicsOverlay = remember(navigationData) {
                    GraphicsOverlay().apply {

                        // 1. Punkt lokalizacji użytkownika po zeskanowaniu QR
                        if (navigationData.isUserPosition) {
                            val userPoint = Point(
                                navigationData.lon,
                                navigationData.lat,
                                SpatialReference.wgs84()
                            )

                            val outerHalo = SimpleMarkerSymbol(
                                style = SimpleMarkerSymbolStyle.Circle,
                                color = Color.fromRgba(26, 115, 232, 51),
                                size = 24f
                            )

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

                            val compositeSymbol = CompositeSymbol(
                                listOf(
                                    outerHalo,
                                    innerCore
                                )
                            )

                            graphics.add(
                                Graphic(
                                    geometry = userPoint,
                                    symbol = compositeSymbol
                                )
                            )
                        }

                        // 2. Trasa i punkt docelowy
                        if (navigationData.calculatedRoutePoints.isNotEmpty()) {
                            val routePoints = navigationData.calculatedRoutePoints.map { (lat, lon) ->
                                Point(lon, lat, SpatialReference.wgs84())
                            }

                            val polylineBuilder = PolylineBuilder(SpatialReference.wgs84()).apply {
                                routePoints.forEach { addPoint(it) }
                            }

                            val routeGeometry = polylineBuilder.toGeometry()

                            // Biała obwódka pod trasą — dzięki temu linia jest czytelna na podkładzie
                            val routeOutlineSymbol = SimpleLineSymbol(
                                style = SimpleLineSymbolStyle.Solid,
                                color = Color.fromRgba(255, 255, 255, 235),
                                width = 9f
                            )

                            // Główna linia trasy
                            val routeMainSymbol = SimpleLineSymbol(
                                style = SimpleLineSymbolStyle.Solid,
                                color = Color.fromRgba(0, 122, 255, 255),
                                width = 5.5f
                            )

                            graphics.add(
                                Graphic(
                                    geometry = routeGeometry,
                                    symbol = routeOutlineSymbol
                                )
                            )

                            graphics.add(
                                Graphic(
                                    geometry = routeGeometry,
                                    symbol = routeMainSymbol
                                )
                            )

                            routePoints.lastOrNull()?.let { endPoint ->
                                graphics.add(
                                    Graphic(
                                        geometry = endPoint,
                                        symbol = destinationPinSymbol
                                    )
                                )
                            }
                        }
                    }
                }

                MapView(
                    modifier = Modifier.fillMaxSize(),
                    arcGISMap = map,
                    graphicsOverlays = listOf(
                        graphicsOverlay,
                        roomLabelsOverlay
                    )
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

        // Przezroczysta warstwa do zamykania listy po kliknięciu poza panelem
        if (isSuggestionsVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        isSuggestionsVisible = false
                        focusManager.clearFocus()
                    }
            )
        }

        // Panel górny
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
                        isSuggestionsVisible = true
                    },
                    placeholder = {
                        Text(
                            text = "Wyszukaj miejsce...",
                            color = ComposeColor.Gray
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = ComposeColor.Gray
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                isSuggestionsVisible = true
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ComposeColor.White.copy(alpha = 0.85f),
                        unfocusedContainerColor = ComposeColor.White.copy(alpha = 0.75f),

                        focusedTextColor = ComposeColor.Black,
                        unfocusedTextColor = ComposeColor.Black,
                        cursorColor = ComposeColor.Black,

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

            // Lista podpowiedzi wyszukiwania
            if (isSuggestionsVisible && filteredSuggestions.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .heightIn(max = 260.dp),
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
                                        focusManager.clearFocus()
                                        viewModel.onDestinationSelected(suggestion)
                                    }
                                    .padding(16.dp),
                                color = ComposeColor.Black
                            )

                            HorizontalDivider(
                                color = ComposeColor.LightGray.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}