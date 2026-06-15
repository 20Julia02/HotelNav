package pl.jb.nawigacjahotel.data.model

import com.google.gson.annotations.SerializedName

data class Feature(
    val geometry: Geometry? = null,
    val attributes: QrAttributes? = null
)

data class QrAttributes(
    @SerializedName("qr_text")
    val qrText: String? = null,

    val poziom: Int? = null,

    val pietro: String? = null,

    @SerializedName("floor_id")
    val floorId: Int? = null
)
