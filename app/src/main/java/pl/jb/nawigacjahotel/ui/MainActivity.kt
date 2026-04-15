package pl.jb.nawigacjahotel.ui

import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.jb.nawigacjahotel.ui.theme.NawigacjaHotelTheme
import pl.jb.nawigacjahotel.common.ResultState

class MainActivity : ComponentActivity() {

    private val viewModel = MainViewModel()

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            viewModel.onQrScanned(result.contents)
        }
    }
    private fun startScan() {
        val options = ScanOptions()
        options.setPrompt("Zeskanuj kod QR")
        options.setBeepEnabled(true)
        options.setOrientationLocked(true)

        barcodeLauncher.launch(options)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    modifier: Modifier
) {
    val state by viewModel.locationState.collectAsState()

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Button(onClick = { onScanClick() }) {
            Text("Skanuj QR")
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (val currentState = state) {

            is ResultState.Loading -> {
                CircularProgressIndicator()
            }

            is ResultState.Success -> {
                Text("Współrzędne:")
                Text(text = currentState.data)
            }

            is ResultState.Error -> {
                Text("Błąd: ${currentState.throwable.message}")
            }
        }
    }
}
