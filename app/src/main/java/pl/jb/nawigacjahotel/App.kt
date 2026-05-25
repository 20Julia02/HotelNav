package pl.jb.nawigacjahotel

import android.app.Application
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        ArcGISRuntimeEnvironment.setApiKey(
            getString(R.string.maps_api_key)
        )

        ArcGISRuntimeEnvironment.setLicense(
            getString(R.string.arc_gis_license)
        )
    }
}