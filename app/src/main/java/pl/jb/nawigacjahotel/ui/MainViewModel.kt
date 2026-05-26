package pl.jb.nawigacjahotel.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pl.jb.nawigacjahotel.common.ResultState
import pl.jb.nawigacjahotel.data.model.CoordinateConverter
import pl.jb.nawigacjahotel.data.remote.RetrofitInstance

class MainViewModel : ViewModel() {

    private val converter = CoordinateConverter()

    companion object {
        private const val DEFAULT_LAT = 52.2207
        private const val DEFAULT_LON = 21.0100
    }

    data class LocationCoords(
        val lat: Double,
        val lon: Double,
        val isUserPosition: Boolean = false
    )

    private val _locationState =
        MutableStateFlow<ResultState<LocationCoords>>(
            ResultState.Success(
                LocationCoords(
                    DEFAULT_LAT,
                    DEFAULT_LON,
                    false
                )
            )
        )

    val locationState: StateFlow<ResultState<LocationCoords>> =
        _locationState

    fun onQrScanned(result: String) {

        val lastPart = result.substringAfterLast("/")

        getLocationFromQr(lastPart)
    }

    private fun getLocationFromQr(qr: String) {

        viewModelScope.launch {

            _locationState.value = ResultState.Loading

            try {

                val response =
                    RetrofitInstance.api.getLocation(
                        where = "qr_text='$qr'"
                    )

                val geometry =
                    response.features.firstOrNull()?.geometry

                if (geometry != null) {

                    val (lat, lon) =
                        converter.toWgs84(
                            geometry.y,
                            geometry.x
                        )

                    _locationState.value =
                        ResultState.Success(
                            LocationCoords(
                                lat,
                                lon,
                                true
                            )
                        )

                } else {

                    _locationState.value =
                        ResultState.Error(
                            Exception("Nie znaleziono pozycji dla QR")
                        )
                }

            } catch (e: Exception) {

                _locationState.value =
                    ResultState.Error(e)
            }
        }
    }
}