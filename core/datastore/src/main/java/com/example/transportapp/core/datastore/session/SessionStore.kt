package com.example.transportapp.core.datastore.session

import android.content.Context
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
 * The mocked offline session store (Phase2.md D5). Emits [SessionSnapshot.DEMO] until a real
 * sign-in writes different values; persists across process death.
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
    }

    val session: Flow<SessionSnapshot> = context.sessionStore.data.map { prefs ->
        SessionSnapshot(
            userId = prefs[Keys.USER_ID] ?: SessionSnapshot.DEMO.userId,
            name = prefs[Keys.NAME] ?: SessionSnapshot.DEMO.name,
            email = prefs[Keys.EMAIL] ?: SessionSnapshot.DEMO.email,
            role = prefs[Keys.ROLE] ?: SessionSnapshot.DEMO.role,
            companyId = prefs[Keys.COMPANY_ID] ?: SessionSnapshot.DEMO.companyId,
            companyName = prefs[Keys.COMPANY_NAME] ?: SessionSnapshot.DEMO.companyName,
            branchId = prefs[Keys.BRANCH_ID] ?: SessionSnapshot.DEMO.branchId,
            branchName = prefs[Keys.BRANCH_NAME] ?: SessionSnapshot.DEMO.branchName,
        )
    }

    suspend fun setActiveContext(companyId: String, companyName: String, branchId: String, branchName: String) {
        context.sessionStore.edit { prefs ->
            prefs[Keys.COMPANY_ID] = companyId
            prefs[Keys.COMPANY_NAME] = companyName
            prefs[Keys.BRANCH_ID] = branchId
            prefs[Keys.BRANCH_NAME] = branchName
        }
    }

    suspend fun clear() {
        context.sessionStore.edit { it.clear() }
    }
}
