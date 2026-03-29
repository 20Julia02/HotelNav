package pl.jb.nawigacjahotel.data.mappers

import pl.jb.nawigacjahotel.data.dto.BeaconDto
import pl.jb.nawigacjahotel.domain.model.Beacon

fun BeaconDto.toDomain() = Beacon(
    uid = uid,
    name = name,
    longitude = longitude,
    latitude = latitude,
    floorId = floorId
)
