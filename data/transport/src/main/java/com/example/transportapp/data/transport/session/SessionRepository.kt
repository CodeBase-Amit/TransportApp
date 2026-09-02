package com.example.transportapp.data.transport.session

import com.example.transportapp.core.datastore.session.SessionStore
import com.example.transportapp.core.datastore.session.SessionSnapshot
import kotlinx.coroutines.flow.Flow
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
    suspend fun signOut()
}

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val store: SessionStore,
) : SessionRepository {

    override val session: Flow<UserSession> = store.session.map { it.toUserSession() }

    override suspend fun signIn() {
        // Offline phase: the mock resolves instantly to the demo identity (S18 — the sign-in
        // screen now actually writes it, so a release install is genuinely signed out first).
        store.signIn(SessionSnapshot.DEMO)
    }

    override suspend fun signOut() {
        // Offline phase: only the local mirror is cleared; company data is untouched (§17.4).
        store.clear()
    }
}
