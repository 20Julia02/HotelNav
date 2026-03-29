package pl.jb.nawigacjahotel.data.dto
import kotlinx.serialization.Serializable

@Serializable
data class BeaconDto(
    val id: Int,
    val uid: String,
    val name: String,
    val longitude: Double,
    val latitude: Double,
    val floorId: Int,
    val roomPlaced: Boolean,
    val nearFloorChange: Boolean,
    val txPowerToSet: Int,
    val tcPower: Int?
)