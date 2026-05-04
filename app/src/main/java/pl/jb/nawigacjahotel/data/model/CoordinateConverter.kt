package pl.jb.nawigacjahotel.data.model

import android.util.Log
import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateReferenceSystem
import org.locationtech.proj4j.CoordinateTransformFactory
import org.locationtech.proj4j.ProjCoordinate

class CoordinateConverter {
    private val ctFactory = CoordinateTransformFactory()
    private val crsFactory = CRSFactory()

    // 1. Definicja WGS84 (EPSG:4326) jako string
    private val wgs84: CoordinateReferenceSystem by lazy {
        crsFactory.createFromParameters("EPSG:4326", "+proj=longlat +datum=WGS84 +no_defs")
    }

    // 2. Definicja EPSG:2180 (PUWG 1992)
    private val epsg2180: CoordinateReferenceSystem by lazy {
        val params = "+proj=tmerc +lat_0=0 +lon_0=19 +k=0.9993 +x_0=500000 +y_0=-5300000 +ellps=GRS80 +units=m +no_defs"
        crsFactory.createFromParameters("EPSG:2180", params)
    }

    private val transform by lazy { ctFactory.createTransform(epsg2180, wgs84) }

    fun toWgs84(x: Double, y: Double): Pair<Double, Double> {
        val result = ProjCoordinate()
        // Proj4j transformuje (x, y) gdzie x to często Easting (Y w geodezji),
        // a y to Northing (X w geodezji). Jeśli punkt będzie w morzu, zamień (x, y) na (y, x).
        transform.transform(ProjCoordinate(y, x), result)
        return Pair(result.y, result.x)
    }
}