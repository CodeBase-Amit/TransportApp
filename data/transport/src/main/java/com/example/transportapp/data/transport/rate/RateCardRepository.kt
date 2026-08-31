package com.example.transportapp.data.transport.rate

import com.example.transportapp.core.database.dao.MastersDao
import com.example.transportapp.core.database.dao.OrgDao
import com.example.transportapp.domain.transport.calc.ChargeHeadBasis
import com.example.transportapp.domain.transport.calc.ChargeHeadDef
import com.example.transportapp.domain.transport.calc.GstTreatment
import com.example.transportapp.domain.transport.calc.RateBasis
import com.example.transportapp.domain.transport.calc.RateCandidate
import com.example.transportapp.domain.transport.calc.RateResolver
import com.example.transportapp.domain.transport.calc.ResolvedRate
import com.example.transportapp.domain.transport.calc.RoundingRule
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Company configuration the booking calculator needs (§10.1/§10.5). The GST rate lives
 * here as demo data with a named seam — §10.5 forbids hardcoding it in the engine and
 * mandates company settings with a dated history, which is S9+ (settings table + UI).
 */
data class BookingCalcSettings(
    val weightStepG: Long,
    val volumetricDivisor: Long?,
    val gstTreatment: GstTreatment,
    val gstRateBp: Int,
    val rounding: RoundingRule,
    val companyRegisteredState: String?,
    /** Consignment's stored place-of-supply default (§10.5): the destination station's state. */
    val defaultPlaceOfSupplyState: String?,
)

object DemoBookingSettings {
    const val WEIGHT_STEP_G = 1000L

    /** Full-load house: volumetric weight off, so clerks never see dimension boxes (§10.1). */
    val VOLUMETRIC_DIVISOR: Long? = null

    const val GST_RATE_BP = 500 // 5% — replace with COMPANY_SETTINGS (dated) in S9.
    val ROUNDING = RoundingRule.NEAREST_RUPEE
}

/** Rate-card reads that feed the S4 calculation engine (Phase2.md S4). */
interface RateCardRepository {

    /** The §3 five-step resolution over the company's rate rows. */
    suspend fun resolveBookingRate(companyId: String, partyId: String?, routeId: String?, goodsId: String?): ResolvedRate?

    /** The §3 charge templates that attach themselves to a booking, in head order. */
    suspend fun autoApplyHeads(companyId: String): List<ChargeHeadDef>

    suspend fun bookingSettings(companyId: String, routeId: String?): BookingCalcSettings
}

@Singleton
class RateCardRepositoryImpl @Inject constructor(
    private val mastersDao: MastersDao,
    private val orgDao: OrgDao,
) : RateCardRepository {

    override suspend fun resolveBookingRate(companyId: String, partyId: String?, routeId: String?, goodsId: String?): ResolvedRate? {
        val rows = mastersDao.getRateCandidates(companyId, partyId, routeId, goodsId)
        val candidates = rows.mapNotNull { it.toCandidate() }
        return RateResolver.resolve(candidates, partyId, routeId, goodsId)
    }

    override suspend fun autoApplyHeads(companyId: String): List<ChargeHeadDef> =
        mastersDao.getAutoChargeHeads(companyId).mapNotNull { head ->
            val basis = runCatching { ChargeHeadBasis.valueOf(head.basis) }.getOrNull() ?: return@mapNotNull null
            ChargeHeadDef(
                localId = head.local_id,
                code = head.code,
                label = head.label,
                basis = basis,
                defaultValue = head.default_value_paise,
                taxable = head.taxable,
                sortOrder = head.sort_order,
            )
        }

    override suspend fun bookingSettings(companyId: String, routeId: String?): BookingCalcSettings {
        val company = orgDao.getCompany(companyId)
        val destinationState = routeId?.let { routeId ->
            mastersDao.getRoute(routeId)?.let { mastersDao.getStation(it.dest_station_id)?.state }
        }
        return BookingCalcSettings(
            weightStepG = DemoBookingSettings.WEIGHT_STEP_G,
            volumetricDivisor = DemoBookingSettings.VOLUMETRIC_DIVISOR,
            gstTreatment = runCatching { GstTreatment.valueOf(company?.gst_treatment ?: "") }.getOrDefault(GstTreatment.FORWARD),
            gstRateBp = DemoBookingSettings.GST_RATE_BP,
            rounding = DemoBookingSettings.ROUNDING,
            companyRegisteredState = company?.gstin?.let { GstinStateCodes.stateOf(it) },
            defaultPlaceOfSupplyState = destinationState,
        )
    }

    private fun com.example.transportapp.core.database.entity.RateCardEntity.toCandidate(): RateCandidate? {
        val basis = runCatching { RateBasis.valueOf(basis) }.getOrNull() ?: return null
        return RateCandidate(
            localId = local_id,
            partyId = party_id,
            routeId = route_id,
            goodsId = goods_id,
            basis = basis,
            ratePaise = rate_paise,
            minFreightPaise = min_freight_paise,
            maxFreightPaise = max_freight_paise,
            minQtyLabel = min_qty_label,
            sortOrder = sort_order,
        )
    }
}
