package pl.jb.nawigacjahotel.ui

import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.dp


import androidx.compose.ui.unit.sp
import pl.jb.nawigacjahotel.ui.theme.NawigacjaHotelTheme
import pl.jb.nawigacjahotel.common.ResultState
import pl.jb.nawigacjahotel.ui.theme.NawigacjaHotelTheme
import pl.jb.nawigacjahotel.data.model.CoordinateConverter
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import pl.jb.nawigacjahotel.R

class MainActivity : ComponentActivity() {

    private val viewModel = MainViewModel()

    private val barcodeLauncher =
        registerForActivityResult(ScanContract()) { result ->

            if (result.contents != null) {
                viewModel.onQrScanned(result.contents)
            }
        }

    private fun startScan() {

        val options = ScanOptions().apply {
            setPrompt("Zeskanuj kod QR")
            setBeepEnabled(true)
            setOrientationLocked(true)
        }

        barcodeLauncher.launch(options)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        enableEdgeToEdge()

        setContent {
            NawigacjaHotelTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        viewModel = viewModel,
                        onScanClick = { startScan() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.locationState.collectAsState()

    // Stany dla wyszukiwarki
    var searchQuery by remember { mutableStateOf("") }
    var isSuggestionsVisible by remember { mutableStateOf(false) }

    // Dane do sugestii
    val allSuggestions = listOf(
        "Restauracja \"Vivaldi\" (Parter)",
        "Basen i Spa (Piętro -1)",
        "Recepcja (Parter)",
        "Konferencja \"Apollo\" (Piętro 3)",
        "Winda Główna"
    )
    val filteredSuggestions = allSuggestions.filter { it.contains(searchQuery, ignoreCase = true) }

    // Główny kontener jako Box – wszystko nakłada się na siebie
    Box(
        modifier = modifier.fillMaxSize()
    ) {

        // WARSTWA 1: MAPA (Jako tło całego ekranu)
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when (val currentState = state) {
                is ResultState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is ResultState.Success -> {
                    val coords = currentState.data

                    EsriMapView(
                        lat = coords.y,
                        lon = coords.x,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    )
                }

                is ResultState.Error -> {

                    Text(
                        text = "Błąd: ${currentState.throwable.message}"
                    )
                }
            }

            // WARSTWA 2: INTERFEJS WISZĄCY NAD MAPĄ
            // Używamy Column z wyrównaniem do góry (TopCenter)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding() // Zapobiega wchodzeniu na pasek stanu telefonu (baterię, zegarek)
                    .align(Alignment.TopCenter)
            ) {
                // Tytuł aplikacji
                Text(
                    text = "Hotel Navigator",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = Color.Black // Warto wymusić kolor, by był widoczny na mapie
                )

                // Wyszukiwarka i QR kod obok siebie
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pasek wyszukiwania
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            isSuggestionsVisible = it.isNotEmpty()
                        },
                        placeholder = {
                            Text(
                                "Wyszukaj miejsce (np. Restauracja, Basen)...",
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color.LightGray,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        singleLine = true
                    )

                    // Przycisk QR (Biała karta z cieniem)
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), // Zwiększony cień, by bardziej "wisiał"
                        modifier = Modifier.size(54.dp)
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
                                    painter = painterResource(id = R.drawable.qrcodescanicon),
                                    contentDescription = "Skanuj QR",
                                    modifier = Modifier.size(24.dp),
                                    tint = Color.Unspecified
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "SKANUJ QR",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }

                // Rozwijana lista sugestii
                if (isSuggestionsVisible && filteredSuggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp) // Większy cień pod listą
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(filteredSuggestions) { suggestion ->
                                Text(
                                    text = suggestion,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            searchQuery = suggestion
                                            isSuggestionsVisible = false
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    color = Color.Black,
                                    fontSize = 14.sp
                                )
                                HorizontalDivider(color = Color(0xFFF0F0F0))
                            }
                        }
                    }
                }
            }
        }
    }}