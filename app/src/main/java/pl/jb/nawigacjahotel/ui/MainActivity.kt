package pl.jb.nawigacjahotel.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import org.osmdroid.config.Configuration
import pl.jb.nawigacjahotel.ui.theme.NawigacjaHotelTheme

class MainActivity : ComponentActivity() {

    // Używamy delegata 'by viewModels()', który automatycznie przekaże
    // instancję Application do konstruktora klasy MainViewModel (AndroidViewModel)
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Konfiguracja osmdroid (wymagana przez bibliotekę kafelków mapowych)
        Configuration.getInstance().userAgentValue = packageName

        // Włączenie rysowania od krawędzi do krawędzi ekranu (Edge-to-Edge)
        enableEdgeToEdge()

        setContent {
            NawigacjaHotelTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    // Wywołanie głównego ekranu i przekazanie zainicjalizowanego ViewModelu
                    MainScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}