package com.example.transportapp.core.datastore.context

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.activeContextStore by preferencesDataStore(name = "active_context")

/** Per-branch sticky defaults for the booking form (TransportApp.md §3). */
data class StickyDefaults(
    val vehicleNumber: String = "",
    val risk: String = "",
    val deliveryType: String = "",
    val gstTreatment: String = "",
)

/**
 * Active company/branch selection and the §3 sticky defaults, stored per branch —
 * "sticky defaults stored per branch, not per user, so a relief clerk gets the same behaviour".
 */
@Singleton
class ActiveContextStore @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val COMPANY_ID = stringPreferencesKey("company_id")
        val BRANCH_ID = stringPreferencesKey("branch_id")
        fun sticky(branchId: String, field: String) = stringPreferencesKey("sticky.$branchId.$field")
    }

    val activeCompanyId: Flow<String> = context.activeContextStore.data.map { it[Keys.COMPANY_ID].orEmpty() }
    val activeBranchId: Flow<String> = context.activeContextStore.data.map { it[Keys.BRANCH_ID].orEmpty() }

    suspend fun setActive(companyId: String, branchId: String) {
        context.activeContextStore.edit { prefs ->
            prefs[Keys.COMPANY_ID] = companyId
            prefs[Keys.BRANCH_ID] = branchId
        }
    }

    suspend fun stickyDefaults(branchId: String): StickyDefaults {
        val prefs = context.activeContextStore.data.first()
        return StickyDefaults(
            vehicleNumber = prefs[Keys.sticky(branchId, "vehicle")].orEmpty(),
            risk = prefs[Keys.sticky(branchId, "risk")].orEmpty(),
            deliveryType = prefs[Keys.sticky(branchId, "delivery")].orEmpty(),
            gstTreatment = prefs[Keys.sticky(branchId, "gst")].orEmpty(),
        )
    }

    suspend fun writeStickyDefaults(branchId: String, defaults: StickyDefaults) {
        context.activeContextStore.edit { prefs ->
            prefs[Keys.sticky(branchId, "vehicle")] = defaults.vehicleNumber
            prefs[Keys.sticky(branchId, "risk")] = defaults.risk
            prefs[Keys.sticky(branchId, "delivery")] = defaults.deliveryType
            prefs[Keys.sticky(branchId, "gst")] = defaults.gstTreatment
        }
    }
}
