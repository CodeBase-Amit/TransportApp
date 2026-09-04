package com.example.transportapp.core.network

import com.example.transportapp.core.common.ErrorCode
import com.example.transportapp.core.common.Result
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.put
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * S23 — the HTTP boundary's contract: JSON in, typed `Result` out. The error mapping
 * is the whole point — 401/403/404/409 land on their §18.3 codes, and a dead network
 * becomes OFFLINE_UNAVAILABLE, the state every caller already renders.
 */
class ApiClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: ApiClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = ApiClient(baseUrl = server.url("/").toString(), tokenProvider = { "test-token" })
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `post sends the body and the bearer header`() = runTest {
        server.enqueue(MockResponse().setBody("""{"ok":true}"""))

        client.post("api/auth/login", loginBody())

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("Bearer test-token", recorded.getHeader("Authorization"))
        assertTrue(recorded.body.readUtf8().contains("\"email\":\"dev@transportapp.test\""))
    }

    @Test
    fun `success parses json`() = runTest {
        server.enqueue(MockResponse().setBody("""{"token":"t-1"}"""))

        val result = client.get("api/health")

        assertTrue(result.isSuccess())
        assertEquals("t-1", (result as Result.Success).value["token"]?.toString()?.trim('"'))
    }

    @Test
    fun `401 maps to AUTH_EXPIRED with the backend copy`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"Invalid email or password"}"""))

        val result = client.post("api/auth/login", loginBody())

        val failure = result as Result.Failure
        assertEquals(ErrorCode.AUTH_EXPIRED, failure.code)
        assertEquals("Invalid email or password", failure.message)
    }

    @Test
    fun `403 maps to AUTH_NO_ACCESS`() = runTest {
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"error":"Requires OWNER"}"""))

        val result = client.get("api/parties")

        assertEquals(ErrorCode.AUTH_NO_ACCESS, (result as Result.Failure).code)
    }

    @Test
    fun `404 maps to MASTER_IN_USE`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"error":"No such thing"}"""))

        val result = client.get("api/parties/nope")

        assertEquals(ErrorCode.MASTER_IN_USE, (result as Result.Failure).code)
    }

    @Test
    fun `409 maps to DUP_CLIENT_OP`() = runTest {
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"error":"Already synced"}"""))

        val result = client.post("api/consignments", loginBody())

        assertEquals(ErrorCode.DUP_CLIENT_OP, (result as Result.Failure).code)
    }

    private fun loginBody() = kotlinx.serialization.json.buildJsonObject {
        put("email", "dev@transportapp.test")
        put("password", "devpassword")
    }
}
