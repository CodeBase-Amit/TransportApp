package com.example.transportapp.core.datastore.session

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionStore by preferencesDataStore(name = "session")

/**
 * The session store (Phase2.md D5, S18 revision). Three states:
 * - Explicitly signed out (SIGNED_OUT flag) → nobody is signed in, in any build.
 * - Fresh store (no user_id): debug builds fall back to the demo identity so the seeded
 *   dataset opens straight away; release builds start signed out (FLAG_DEBUGGABLE gate).
 * - Stored identity: whatever the last sign-in wrote.
 * The mock sign-in writes the demo identity; Phase 3.3's Credential Manager replaces it
 * behind the same [signIn] seam.
 */
@Singleton
class SessionStore @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val NAME = stringPreferencesKey("name")
        val EMAIL = stringPreferencesKey("email")
        val ROLE = stringPreferencesKey("role")
        val COMPANY_ID = stringPreferencesKey("company_id")
        val COMPANY_NAME = stringPreferencesKey("company_name")
        val BRANCH_ID = stringPreferencesKey("branch_id")
        val BRANCH_NAME = stringPreferencesKey("branch_name")
        val SIGNED_OUT = stringPreferencesKey("signed_out")
    }

    val session: Flow<SessionSnapshot> = context.sessionStore.data.map { prefs ->
        when {
            prefs[Keys.SIGNED_OUT] == "true" -> SessionSnapshot.SIGNED_OUT
            // An explicit identity (mock sign-in) or a company context (company picker /
            // wizard registration) both count as a session; either may exist without the other.
            prefs[Keys.USER_ID] != null || prefs[Keys.COMPANY_ID] != null -> SessionSnapshot(
                userId = prefs[Keys.USER_ID].orEmpty(),
                name = prefs[Keys.NAME].orEmpty(),
                email = prefs[Keys.EMAIL].orEmpty(),
                role = prefs[Keys.ROLE].orEmpty(),
                companyId = prefs[Keys.COMPANY_ID].orEmpty(),
                companyName = prefs[Keys.COMPANY_NAME].orEmpty(),
                branchId = prefs[Keys.BRANCH_ID].orEmpty(),
                branchName = prefs[Keys.BRANCH_NAME].orEmpty(),
            )
            // Fresh store: debug falls back to the demo identity so the seeded dataset opens
            // straight away; release starts signed out (FLAG_DEBUGGABLE gate).
            else -> if (isDebuggable()) SessionSnapshot.DEMO else SessionSnapshot.SIGNED_OUT
        }
    }

    suspend fun signIn(snapshot: SessionSnapshot) {
        context.sessionStore.edit { prefs ->
            prefs[Keys.SIGNED_OUT] = "false"
            prefs[Keys.USER_ID] = snapshot.userId
            prefs[Keys.NAME] = snapshot.name
            prefs[Keys.EMAIL] = snapshot.email
            prefs[Keys.ROLE] = snapshot.role
            prefs[Keys.COMPANY_ID] = snapshot.companyId
            prefs[Keys.COMPANY_NAME] = snapshot.companyName
            prefs[Keys.BRANCH_ID] = snapshot.branchId
            prefs[Keys.BRANCH_NAME] = snapshot.branchName
        }
    }

    suspend fun setActiveContext(companyId: String, companyName: String, branchId: String, branchName: String) {
        context.sessionStore.edit { prefs ->
            prefs[Keys.COMPANY_ID] = companyId
            prefs[Keys.COMPANY_NAME] = companyName
            prefs[Keys.BRANCH_ID] = branchId
            prefs[Keys.BRANCH_NAME] = branchName
        }
    }

    /** S21: the profile's Save — the display name rides the stored identity. */
    suspend fun updateDisplayName(name: String) {
        context.sessionStore.edit { prefs ->
            if (prefs[Keys.USER_ID] != null || prefs[Keys.COMPANY_ID] != null) {
                prefs[Keys.NAME] = name
            }
        }
    }

    suspend fun clear() {
        context.sessionStore.edit { prefs ->
            prefs.clear()
            prefs[Keys.SIGNED_OUT] = "true"
        }
    }

    private fun isDebuggable(): Boolean =
        context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
}
