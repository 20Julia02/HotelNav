package pl.jb.nawigacjahotel.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.geometry.SpatialReferences
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.BasemapStyle
import com.esri.arcgisruntime.mapping.Viewpoint
import com.esri.arcgisruntime.mapping.view.Graphic
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay
import com.esri.arcgisruntime.mapping.view.MapView
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol
import androidx.compose.material3.MaterialTheme

@Composable
fun EsriMapView(
    lat: Double,
    lon: Double,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Pobieramy kolor z motywu Compose (np. Primary), żeby znacznik pasował do wyglądu aplikacji
    val markerColor = MaterialTheme.colorScheme.primary.toArgb()

    // Tworzymy nakładkę na grafiki, która będzie trzymać nasz punkt
    val graphicsOverlay = remember { GraphicsOverlay() }

    val mapView = remember {
        MapView(context).apply {
            val map = ArcGISMap(BasemapStyle.ARCGIS_STREETS)
            this.map = map
            // Dodajemy nakładkę do widoku mapy
            this.graphicsOverlays.add(graphicsOverlay)
        }
    }

    // Reagujemy na zmianę lat/lon (np. po ponownym skanowaniu)
    LaunchedEffect(lat, lon) {
        // Czyszczenie poprzedniego punktu (jeśli był)
        graphicsOverlay.graphics.clear()

        // Tworzenie punktu geograficznego
        val point = Point(lon, lat, SpatialReferences.getWgs84())

        // Definiowanie wyglądu znacznika (Czerwona/Primary kropka o rozmiarze 14dp)
        val symbol = SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, markerColor, 14f)

        // Tworzenie obiektu graficznego łączącego geometrię i wygląd
        val pinGraphic = Graphic(point, symbol)

        // Dodanie punktu na mapę
        graphicsOverlay.graphics.add(pinGraphic)

        // Centrowanie mapy na nowej pozycji
        mapView.setViewpointAsync(Viewpoint(point, 5000.0), 1f)
    }

    DisposableEffect(Unit) {
        mapView.resume()
        onDispose {
            mapView.pause()
            mapView.dispose()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}