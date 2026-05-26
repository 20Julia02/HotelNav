package pl.jb.nawigacjahotel.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import org.osmdroid.config.Configuration
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

        Configuration.getInstance().userAgentValue = packageName

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