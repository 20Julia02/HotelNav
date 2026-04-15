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


class MainViewModel : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val _locationState =
        MutableStateFlow<ResultState<String>>(ResultState.Loading)

    val locationState: StateFlow<ResultState<String>> = _locationState

    fun onQrScanned(result: String) {
        val lastPart = result.substringAfterLast("/")
        getLocationFromQr(lastPart)
    }

    private fun getLocationFromQr(qr: String) {
        viewModelScope.launch {
            _locationState.value = ResultState.Loading


            try {
                val response = RetrofitInstance.api.getLocation(
                    where = "qr_text='$qr'"
                )

                val feature = response.features.firstOrNull()
                val geometry = feature?.geometry

                val result = if (geometry != null) {
                    "X: ${geometry.x}\nY: ${geometry.y}"
                } else {
                    "Brak danych"
                }

                _locationState.value = ResultState.Success(result)

            } catch (e: Exception) {
                Log.e(TAG, "Błąd API", e)
                _locationState.value = ResultState.Error(e)
            }
        }
    }
}