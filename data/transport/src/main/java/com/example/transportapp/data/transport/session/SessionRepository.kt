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
 * The signed-in membership, read-only for Phase 2 (mocked session; sign-out wipes the store).
 * Real auth replaces the backing store behind the seam — this interface is the only surface
 * ViewModels may see (Spec.md §6.1).
 */
interface SessionRepository {
    val session: Flow<UserSession>
    suspend fun signOut()
}

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val store: SessionStore,
) : SessionRepository {

    override val session: Flow<UserSession> = store.session.map { it.toUserSession() }

    override suspend fun signOut() {
        // Offline phase: only the local mirror is cleared; company data is untouched (§17.4).
        store.clear()
    }
}
