package pl.jb.nawigacjahotel.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.toolkit.geoviewcompose.MapView

import pl.jb.nawigacjahotel.R
import pl.jb.nawigacjahotel.common.ResultState

private const val ROOMS_LAYER_URL =
    "https://arcgis.cenagis.edu.pl/server/rest/services/SION2_Topo_MV/sion2_wms_topo_GG_f19/MapServer/5"

fun createMap(lat: Double, lon: Double): ArcGISMap {

    val roomServiceTable =
        ServiceFeatureTable(ROOMS_LAYER_URL)

    val featureLayerRooms =
        FeatureLayer.createWithFeatureTable(roomServiceTable)

    return ArcGISMap(BasemapStyle.ArcGISTopographic).apply {

        initialViewpoint = Viewpoint(
            latitude = lat,
            longitude = lon,
            scale = 5000.0
        )

        operationalLayers.add(featureLayerRooms)
    }
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val state by viewModel.locationState.collectAsState()

    var searchQuery by remember {
        mutableStateOf("")
    }

    var isSuggestionsVisible by remember {
        mutableStateOf(false)
    }

    val allSuggestions = listOf(
        "Restauracja \"Vivaldi\"",
        "Basen i Spa",
        "Recepcja",
        "Konferencja Apollo",
        "Winda Główna"
    )

    val filteredSuggestions =
        allSuggestions.filter {
            it.contains(searchQuery, true)
        }

    Box(
        modifier = modifier.fillMaxSize()
    ) {

        when (val currentState = state) {

            is ResultState.Loading -> {

                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is ResultState.Success -> {

                val coords = currentState.data

                val map = remember(coords.lat, coords.lon) {
                    createMap(coords.lat, coords.lon)
                }

                MapView(
                    modifier = Modifier.fillMaxSize(),
                    arcGISMap = map
                )
            }

            is ResultState.Error -> {

                Text(
                    text = "Błąd: ${currentState.throwable.message}",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

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
                color = Color.Black
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        isSuggestionsVisible = it.isNotEmpty()
                    },
                    placeholder = {
                        Text("Wyszukaj miejsce...")
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Card(
                    modifier = Modifier.size(54.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {

                    IconButton(
                        onClick = onScanClick,
                        modifier = Modifier.fillMaxSize()
                    ) {

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {

                            Icon(
                                painter = painterResource(
                                    id = R.drawable.qrcodescanicon
                                ),
                                contentDescription = null,
                                tint = Color.Unspecified
                            )

                            Text(
                                text = "QR",
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            if (
                isSuggestionsVisible &&
                filteredSuggestions.isNotEmpty()
            ) {

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp)
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
                                    }
                                    .padding(16.dp)
                            )

                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}