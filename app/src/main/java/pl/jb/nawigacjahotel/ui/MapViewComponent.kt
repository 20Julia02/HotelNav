package pl.jb.nawigacjahotel.ui

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.api.IMapController
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.util.GeoPoint

@Composable
fun OsmMapView(
    lat: Double,
    lon: Double,
    isUserPosition: Boolean,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val mapView = MapView(context)

            mapView.setTileSource(TileSourceFactory.MAPNIK)
            mapView.setMultiTouchControls(true)

            mapView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

            mapView.isTilesScaledToDpi = true

            mapView.tileProvider.tileCache.ensureCapacity(64)

            mapView.setHasTransientState(true)

            mapView
        },
        // ... (reszta kodu factory bez zmian)
        update = { mapView ->
            val point = GeoPoint(lat, lon)
            val mapController: IMapController = mapView.controller

            // Centrowanie i zoom działają zawsze, żeby użytkownik widział hotel na starcie
            val currentCenter = mapView.mapCenter
            if (currentCenter.latitude != point.latitude || currentCenter.longitude != point.longitude) {
                mapController.setZoom(17)
                mapController.animateTo(point)
            }

            // Czyszczenie starych nakładek (overlayów)
            mapView.overlays.clear()

            // WARUNEK: Rysuj pinezkę TYLKO wtedy, gdy to faktyczna pozycja użytkownika (po QR)
            if (isUserPosition) {
                val marker = Marker(mapView)
                marker.position = point
                // marker.icon = ContextCompat.getDrawable(mapView.context, R.drawable.my_blue_dot)
                marker.title = "Twoja pozycja"

                mapView.overlays.add(marker)
            }

            // Odświeżenie widoku mapy
            mapView.invalidate()
        }
    )
}