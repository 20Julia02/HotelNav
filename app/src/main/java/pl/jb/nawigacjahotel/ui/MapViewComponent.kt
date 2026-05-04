package pl.jb.nawigacjahotel.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.api.IMapController
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.util.GeoPoint

@Composable
fun OsmMapView(lat: Double, lon: Double, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
            }
        },
        update = { mapView ->
            val point = GeoPoint(lat, lon)
            val mapController: IMapController = mapView.controller
            mapController.setZoom(18.0)
            mapController.setCenter(point)

            // Dodanie markera
            mapView.overlays.clear()
            val marker = Marker(mapView)
            marker.position = point
            mapView.overlays.add(marker)
            mapView.invalidate()
        }
    )
}