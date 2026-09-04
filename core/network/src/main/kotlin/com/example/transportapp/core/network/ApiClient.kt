package com.example.transportapp.core.network

import com.example.transportapp.core.common.ErrorCode
import com.example.transportapp.core.common.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

/**
 * S23 — the app's single HTTP surface (TransportApp.md §14: one typed boundary).
 *
 * Offline-first contract: every call answers [Result]. A network failure is *never* an
 * exception past this layer — it becomes `OFFLINE_UNAVAILABLE`, the same typed state the
 * UI already renders as an explanation, so callers simply keep working from Room.
 *
 * Error mapping (D47 amendment: kotlinx-serialization lives here and in :doc-engine):
 *   401 → AUTH_EXPIRED · 403 → AUTH_NO_ACCESS · 404 → MASTER_IN_USE ·
 *   409 → DUP_CLIENT_OP · other 4xx/5xx → the body's message on a generic code ·
 *   IOException/timeout → OFFLINE_UNAVAILABLE.
 *
 * The auth token is supplied by [TokenProvider]; the interceptor attaches it when present
 * and the caller never sees the header machinery.
 */
class ApiClient(
    private val baseUrl: String,
    private val tokenProvider: TokenProvider,
    client: OkHttpClient = OkHttpClient.Builder().build(),
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()
    private val client = client.newBuilder()
        .addInterceptor { chain ->
            val token = tokenProvider.token()
            val request = if (token.isNullOrEmpty()) chain.request()
            else chain.request().newBuilder().header("Authorization", "Bearer $token").build()
            chain.proceed(request)
        }
        .build()

    suspend fun get(path: String): Result<JsonObject> = call("GET", path, null)
    suspend fun post(path: String, body: JsonObject): Result<JsonObject> = call("POST", path, body)
    suspend fun put(path: String, body: JsonObject): Result<JsonObject> = call("PUT", path, body)
    suspend fun patch(path: String, body: JsonObject): Result<JsonObject> = call("PATCH", path, body)
    suspend fun delete(path: String): Result<JsonObject> = call("DELETE", path, null)

    suspend fun getRaw(path: String): Result<String> = callRaw("GET", path, null)
    suspend fun postRaw(path: String, body: JsonObject): Result<String> = callRaw("POST", path, body)
    suspend fun putRaw(path: String, body: JsonObject): Result<String> = callRaw("PUT", path, body)
    suspend fun patchRaw(path: String, body: JsonObject): Result<String> = callRaw("PATCH", path, body)
    suspend fun deleteRaw(path: String): Result<String> = callRaw("DELETE", path, null)

    suspend fun postJson(path: String, build: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit): Result<JsonObject> =
        post(path, buildJsonObject { build() })

    suspend fun putJson(path: String, build: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit): Result<JsonObject> =
        put(path, buildJsonObject { build() })

    suspend fun patchJson(path: String, build: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit): Result<JsonObject> =
        patch(path, buildJsonObject { build() })

    private suspend fun call(method: String, path: String, body: JsonObject?): Result<JsonObject> =
        when (val raw = callRaw(method, path, body)) {
            is Result.Success -> Result.success(runCatching { json.parseToJsonElement(raw.value).jsonObject }.getOrDefault(JsonObject(emptyMap())))
            is Result.Failure -> raw
        }

    private suspend fun callRaw(method: String, path: String, body: JsonObject?): Result<String> =
        withContext(Dispatchers.IO) {
            val url = if (path.startsWith("http")) path else baseUrl.trimEnd('/') + (if (path.startsWith("/")) path else "/$path")
            val request = Request.Builder()
                .url(url)
                .method(method, body?.toString()?.toRequestBody(jsonMedia))
                .build()
            try {
                client.newCall(request).execute().use { response -> response.toResult() }
            } catch (e: Exception) {
                // Offline-first: no network is a typed, expected state — never a crash.
                Result.failure(ErrorCode.OFFLINE_UNAVAILABLE, OFFLINE_MESSAGE)
            }
        }

    /** The backend's `error` field is the user-facing copy; fall back to a short raw body. */
    private fun bodyMessage(body: String): String =
        runCatching {
            (json.parseToJsonElement(body).jsonObject)["error"]?.toString()?.trim('"')
        }.getOrNull() ?: ""

    private fun Response.toResult(): Result<String> {
        val text = body?.string().orEmpty()
        val message = bodyMessage(text).ifEmpty { "The server answered $code" }
        return when {
            isSuccessful -> Result.success(text)
            code == 401 -> Result.failure(ErrorCode.AUTH_EXPIRED, message)
            code == 403 -> Result.failure(ErrorCode.AUTH_NO_ACCESS, message)
            code == 404 -> Result.failure(ErrorCode.MASTER_IN_USE, message)
            code == 409 -> Result.failure(ErrorCode.DUP_CLIENT_OP, message)
            else -> Result.failure(ErrorCode.OFFLINE_UNAVAILABLE, message)
        }
    }
}

/** Supplies the JWT for the auth interceptor; the session store implements it. */
fun interface TokenProvider {
    fun token(): String?
}

/** S23: the offline copy every network failure carries. */
const val OFFLINE_MESSAGE = "You're offline - this will sync when there's signal"
