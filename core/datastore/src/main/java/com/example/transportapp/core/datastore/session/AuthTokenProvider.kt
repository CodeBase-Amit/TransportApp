package com.example.transportapp.core.datastore.session

import javax.inject.Inject
import javax.inject.Singleton

/**
 * The identity seam (Phase2.md D5). Phase 2 is offline: the only implementation is the mock.
 * The real Credential Manager + Firebase implementation is a later phase and lands *behind
 * this interface* — no caller above it may change.
 */
interface AuthTokenProvider {
    suspend fun accessToken(): String?
    suspend fun refresh(): Boolean
}

/** Offline-phase stand-in. Serves a stable fake token; never a network call. */
@Singleton
class MockAuthTokenProvider @Inject constructor() : AuthTokenProvider {
    override suspend fun accessToken(): String? = "mock.offline.token"
    override suspend fun refresh(): Boolean = true
}
