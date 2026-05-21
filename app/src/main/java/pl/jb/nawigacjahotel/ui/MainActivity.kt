package pl.jb.nawigacjahotel.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

import pl.jb.nawigacjahotel.common.ResultState
import pl.jb.nawigacjahotel.ui.theme.NawigacjaHotelTheme

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

        enableEdgeToEdge()

        setContent {

            NawigacjaHotelTheme {

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),

        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Button(
            onClick = onScanClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Skanuj kod QR")
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (val currentState = state) {

            is ResultState.Loading -> {

                CircularProgressIndicator()
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
    }
}