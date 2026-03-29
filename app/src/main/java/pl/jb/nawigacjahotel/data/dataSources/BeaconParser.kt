package pl.jb.nawigacjahotel.data.dataSources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import pl.jb.nawigacjahotel.data.dto.BeaconDto
import java.io.InputStream

class BeaconParser(
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    suspend fun parseFromStream(
        input: InputStream
    ): List<BeaconDto> =
        withContext(Dispatchers.Default) {
            val text = input.bufferedReader().use { it.readText() }
            json.decodeFromString<List<BeaconDto>>(text)
        }
}
