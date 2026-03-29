package pl.jb.nawigacjahotel.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pl.jb.nawigacjahotel.data.dataSources.BeaconDataSource
import pl.jb.nawigacjahotel.data.mappers.toDomain
import pl.jb.nawigacjahotel.domain.model.Beacon
import android.util.Log

class MainViewModel(private val beaconDataSource: BeaconDataSource
) : ViewModel() {

    companion object {
        private const val TAG = "pw.MainViewModel"
    }

    private val _beacons = MutableStateFlow<List<Beacon>>(emptyList())
    val beacons: StateFlow<List<Beacon>> = _beacons

    fun loadBeacons() {
        viewModelScope.launch {
            val result = beaconDataSource.loadBeacons()
            result.onSuccess { beaconsDto ->
                _beacons.value = beaconsDto.map {it.toDomain()}
            }.onFailure { error ->
                Log.e(TAG, "Błąd wczytywania beaconów", error)
            }
        }
    }

}