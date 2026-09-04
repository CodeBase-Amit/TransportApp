package com.example.transportapp.core.network

import com.example.transportapp.core.common.ErrorCode
import com.example.transportapp.core.common.Result
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonPrimitive

/**
 * S23 — the auth endpoints (§17: real auth replaces the mock seam). `login` posts
 * email/password; `devLogin` is the test-backend convenience the debug build uses.
 * Both return the parsed [AuthResponse] the session store consumes.
 */
class AuthApi(private val client: ApiClient) {

    suspend fun login(email: String, password: String): Result<AuthResponse> =
        client.post("api/auth/login", buildJsonObject {
            put("email", email)
            put("password", password)
        }).toAuth()

    /** Test-backend convenience; replaced by Credential Manager with the real backend. */
    suspend fun devLogin(email: String? = null): Result<AuthResponse> =
        client.post("api/auth/dev-login", buildJsonObject {
            if (email != null) put("email", email)
        }).toAuth()

    private fun Result<JsonObject>.toAuth(): Result<AuthResponse> = when (this) {
        is Result.Success -> {
            val obj = runCatching { value.jsonObject }.getOrElse { return Result.failure(ErrorCode.OFFLINE_UNAVAILABLE, "The server answered with an unexpected body") }
            val user = obj["user"]?.jsonObject
            Result.success(
                AuthResponse(
                    token = obj["token"]?.jsonPrimitive?.content ?: "",
                    userId = user?.get("id")?.jsonPrimitive?.content ?: "",
                    name = user?.get("name")?.jsonPrimitive?.content ?: "",
                    email = user?.get("email")?.jsonPrimitive?.content ?: "",
                    companyId = obj["companyId"]?.jsonPrimitive?.content ?: "",
                    companyName = obj["company"]?.jsonObject?.get("name")?.jsonPrimitive?.content ?: "",
                    role = obj["role"]?.jsonPrimitive?.content ?: "OWNER",
                )
            )
        }
        is Result.Failure -> this
    }
}

/** The parsed auth response (§17.1: token + identity + tenant context). */
data class AuthResponse(
    val token: String,
    val userId: String,
    val name: String,
    val email: String,
    val companyId: String,
    val companyName: String,
    val role: String,
)
