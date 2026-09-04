package com.example.transportapp.core.network

import com.example.transportapp.core.common.ErrorCode
import com.example.transportapp.core.common.Result
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * S24 — the masters read endpoints (§14: one HTTP boundary). Every list answers the same
 * shape: a JSON array of docs carrying `_id` + the master's fields. The refresher in
 * `:data:transport` upserts these into Room keyed by `server_id`; Room stays the only
 * source the UI reads.
 */
class MastersApi(private val client: ApiClient) {

    suspend fun parties(): Result<List<RemoteMaster>> = client.getRaw("api/parties").toList()
    suspend fun stations(): Result<List<RemoteMaster>> = client.getRaw("api/stations").toList()
    suspend fun routes(): Result<List<RemoteMaster>> = client.getRaw("api/routes").toList()
    suspend fun goods(): Result<List<RemoteMaster>> = client.getRaw("api/goods").toList()
    suspend fun vehicles(): Result<List<RemoteMaster>> = client.getRaw("api/vehicles").toList()
    suspend fun drivers(): Result<List<RemoteMaster>> = client.getRaw("api/drivers").toList()

    private fun Result<String>.toList(): Result<List<RemoteMaster>> = when (this) {
        is Result.Success -> {
            val parsed = runCatching {
                Json.parseToJsonElement(value).jsonArray.map { el ->
                    val o = el.jsonObject
                    RemoteMaster(
                        serverId = o["_id"]?.jsonPrimitive?.content ?: "",
                        name = o["name"]?.jsonPrimitive?.content ?: "",
                        extra = o,
                    )
                }
            }.getOrElse { return Result.failure(ErrorCode.OFFLINE_UNAVAILABLE, "The server answered with an unexpected body") }
            Result.success(parsed)
        }
        is Result.Failure -> this
    }
}

/** One remote master doc: the id Room will key `server_id` on, plus the raw fields. */
data class RemoteMaster(
    val serverId: String,
    val name: String,
    val extra: JsonObject,
)

/**
 * S24 — the numbering endpoints (§9). `lease` consumes atomically server-side;
 * `peek` reads without consuming. Both answer prefixed numbers.
 */
class NumberingApi(private val client: ApiClient) {

    suspend fun lease(type: String): Result<String> = client.post("api/numbering/$type/lease", emptyBody())
        .mapString("number")

    suspend fun peek(type: String): Result<String> = client.post("api/numbering/$type/next", emptyBody())
        .mapString("next")

    private fun Result<JsonObject>.mapString(field: String): Result<String> = when (this) {
        is Result.Success -> {
            val parsed = runCatching { value[field]?.jsonPrimitive?.content ?: "" }
                .getOrElse { return Result.failure(ErrorCode.OFFLINE_UNAVAILABLE, "The server answered with an unexpected body") }
            Result.success(parsed)
        }
        is Result.Failure -> this
    }

    private fun emptyBody(): JsonObject = Json.parseToJsonElement("{}").jsonObject
}
