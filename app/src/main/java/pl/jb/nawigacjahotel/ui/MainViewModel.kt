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
    }
    data class LocationCoords(val x: Double, val y: Double)
    private val _locationState = MutableStateFlow<ResultState<LocationCoords>>(ResultState.Loading)
    val locationState: StateFlow<ResultState<LocationCoords>> = _locationState

    fun onQrScanned(result: String) {
        val lastPart = result.substringAfterLast("/")
        getLocationFromQr(lastPart)
    }

    private fun getLocationFromQr(qr: String) {
        viewModelScope.launch {
            _locationState.value = ResultState.Loading
            try {
                val response = RetrofitInstance.api.getLocation(where = "qr_text='$qr'")
                val geometry = response.features.firstOrNull()?.geometry

                if (geometry != null) {
                    val (lat, lon) = converter.toWgs84(geometry.y, geometry.x)

                    _locationState.value =
                        ResultState.Success(
                            LocationCoords(
                                x = lon,
                                y = lat
                            )
                        )
                } else {
                    _locationState.value = ResultState.Error(Exception("Brak danych"))
                }
            } catch (e: Exception) {
                _locationState.value = ResultState.Error(e)
            }
        }
    }
}