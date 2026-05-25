package pl.jb.nawigacjahotel.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pl.jb.nawigacjahotel.common.ResultState
import android.util.Log
import pl.jb.nawigacjahotel.data.remote.RetrofitInstance
import pl.jb.nawigacjahotel.data.model.CoordinateConverter

class MainViewModel : ViewModel() {
    private val converter = CoordinateConverter()

    companion object {
        private const val TAG = "MainViewModel"
        private const val DEFAULT_LAT = 52.2207
        private const val DEFAULT_LON = 21.0100
    }

    // Dodajemy flagę isUserPosition, żeby odróżnić widok hotelu od pozycji usera
    data class LocationCoords(
        val x: Double,
        val y: Double,
        val isUserPosition: Boolean = false
    )

    // STARTUJEMY OD RAZU Z SUKCESEM (Domyślny widok hotelu)
    private val _locationState = MutableStateFlow<ResultState<LocationCoords>>(
        ResultState.Success(LocationCoords(DEFAULT_LAT, DEFAULT_LON, isUserPosition = false))
    )
    val locationState: StateFlow<ResultState<LocationCoords>> = _locationState

    fun onQrScanned(result: String) {
        val lastPart = result.substringAfterLast("/")
        getLocationFromQr(lastPart)
    }

    // W MainViewModel
    private fun getLocationFromQr(qr: String) {
        viewModelScope.launch {
            _locationState.value = ResultState.Loading
            try {
                val response = RetrofitInstance.api.getLocation(where = "qr_text='$qr'")
                val geometry = response.features.firstOrNull()?.geometry

                if (geometry != null) {
                    val (lat, lon) = converter.toWgs84(geometry.y, geometry.x)
                    // Zwracamy pozycję z flagą isUserPosition = true
                    _locationState.value = ResultState.Success(LocationCoords(lat, lon, isUserPosition = true))
                } else {
                    _locationState.value = ResultState.Error(Exception("Nie znaleziono pozycji dla tego kodu QR"))
                }
            } catch (e: Exception) {
                _locationState.value = ResultState.Error(e)
            }
        }
    }
}