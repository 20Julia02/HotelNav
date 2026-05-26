package pl.jb.nawigacjahotel

import android.app.Application
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        ArcGISEnvironment.apiKey =
            ApiKey.create(
                getString(R.string.maps_api_key)
            )
    }
}