package com.example.transportapp.data.transport.session

import com.example.transportapp.core.datastore.session.SessionStore
import com.example.transportapp.core.datastore.session.SessionSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The signed-in membership as ViewModels may see it (§6: role + branch scope). This is the
 * public read model — `:core:datastore`'s primitive [SessionSnapshot] is mapped here so no
 * feature module needs a datastore dependency (Spec.md §2).
 */
data class UserSession(
    val userId: String,
    val name: String,
    val email: String,
    val role: String,
    val companyId: String,
    val companyName: String,
    val branchId: String,
    val branchName: String,
) {
    val isSignedIn: Boolean get() = userId.isNotEmpty()
}

fun SessionSnapshot.toUserSession() = UserSession(
    userId = userId,
    name = name,
    email = email,
    role = role,
    companyId = companyId,
    companyName = companyName,
    branchId = branchId,
    branchName = branchName,
)

/**
 * The signed-in membership (§6: role + branch scope). Sign-in is the mock identity write;
 * Phase 3.3's Credential Manager replaces the body of [signIn] behind this seam.
 */
interface SessionRepository {
    val session: Flow<UserSession>
    suspend fun signIn()
    /** S23: real auth — email/password against the backend; offline failure keeps the mock. */
    suspend fun signInWithPassword(email: String, password: String): com.example.transportapp.core.common.Result<Unit>
    suspend fun signOut()
    /** S21: the profile's Save — updates the stored display name. */
    suspend fun updateDisplayName(name: String)
}

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val store: SessionStore,
    private val authApi: com.example.transportapp.core.network.AuthApi,
) : SessionRepository {

    override val session: Flow<UserSession> = store.session.map { it.toUserSession() }

    override suspend fun signIn() {
        // Offline-first (D62): no backend is required to run. The mock resolves to the demo
        // identity; a device that has never reached the server still books and prints.
        store.signIn(SessionSnapshot.DEMO)
    }

    /**
     * S23: real sign-in. On success the JWT + identity persist; on `OFFLINE_UNAVAILABLE`
     * the caller falls back to the mock identity so the app still opens — the sync will
     * reconcile when there's signal (Room is the truth, the server mirrors it).
     */
    override suspend fun signInWithPassword(email: String, password: String): com.example.transportapp.core.common.Result<Unit> =
        when (val result = authApi.login(email, password)) {
            is com.example.transportapp.core.common.Result.Success -> {
                val auth = result.value
                store.signIn(
                    SessionSnapshot(
                        userId = auth.userId,
                        name = auth.name,
                        email = auth.email,
                        role = auth.role,
                        companyId = auth.companyId,
                        companyName = auth.companyName,
                        branchId = "", // resolved from memberships on first sync
                        branchName = "",
                    )
                )
                store.saveToken(auth.token)
                com.example.transportapp.core.common.Result.success(Unit)
            }
            is com.example.transportapp.core.common.Result.Failure -> {
                // Offline-first: a network failure degrades to the mock identity.
                if (result.code == com.example.transportapp.core.common.ErrorCode.OFFLINE_UNAVAILABLE) {
                    store.signIn(SessionSnapshot.DEMO)
                    com.example.transportapp.core.common.Result.success(Unit)
                } else {
                    result
                }
            }
        }

    override suspend fun signOut() {
        store.clear()
    }

    override suspend fun updateDisplayName(name: String) {
        store.updateDisplayName(name)
    }
}
