package pl.jb.nawigacjahotel.data.model

import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateReferenceSystem
import org.locationtech.proj4j.CoordinateTransformFactory
import org.locationtech.proj4j.ProjCoordinate

class CoordinateConverter {
    private val ctFactory = CoordinateTransformFactory()
    private val crsFactory = CRSFactory()

    private val wgs84: CoordinateReferenceSystem by lazy {
        crsFactory.createFromParameters(
            "EPSG:4326",
            "+proj=longlat +datum=WGS84 +no_defs"
        )
    }

    private val epsg2180: CoordinateReferenceSystem by lazy {
        val params =
            "+proj=tmerc +lat_0=0 +lon_0=19 +k=0.9993 +x_0=500000 +y_0=-5300000 +ellps=GRS80 +units=m +no_defs"
        crsFactory.createFromParameters("EPSG:2180", params)
    }

    private val transform by lazy {
        ctFactory.createTransform(epsg2180, wgs84)
    }

    /**
     * Konwersja z PUWG 1992 / EPSG:2180 do WGS84.
     *
     * Wejście: x, y w metrach, np. okolice Warszawy ~ x=637000, y=486000.
     * Wyjście: Pair(lat, lon), np. 52.22, 21.01.
     */
    fun toWgs84(x: Double, y: Double): Pair<Double, Double> {
        val result = ProjCoordinate()
        transform.transform(ProjCoordinate(x, y), result)
        return Pair(result.y, result.x) // lat, lon
    }
}
