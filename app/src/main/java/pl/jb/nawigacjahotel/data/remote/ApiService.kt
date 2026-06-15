package pl.jb.nawigacjahotel.data.remote

import pl.jb.nawigacjahotel.data.model.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("query")
    suspend fun getLocation(
        @Query("where") where: String,
        @Query("outFields") outFields: String = "*",
        @Query("returnGeometry") returnGeometry: Boolean = true,
        @Query("f") f: String = "pjson"
    ): ApiResponse

}
