package com.example.transportapp.data.transport.rate

import com.example.transportapp.core.database.dao.MastersDao
import com.example.transportapp.core.database.dao.OrgDao
import com.example.transportapp.core.database.dao.SettingsDao
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
 * Company configuration the booking calculator needs (§10.1/§10.5). The GST rate and the
 * volumetric divisor come from the dated COMPANY_SETTING_E table (Phase 3 S14): the newest
 * row effective at "now" governs a *new* booking, and a setting change never alters an
 * already-booked consignment — its charge lines were frozen at booking time (§12.1's
 * freeze principle applied to configuration, the audit's D1 closed).
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

/** Rate-card reads that feed the S4 calculation engine (Phase2.md S4). */
interface RateCardRepository {

    /** The §3 five-step resolution over the company's rate rows. */
    suspend fun resolveBookingRate(companyId: String, partyId: String?, routeId: String?, goodsId: String?): ResolvedRate?

    /** The §3 charge templates that attach themselves to a booking, in head order. */
    suspend fun autoApplyHeads(companyId: String): List<ChargeHeadDef>

    suspend fun bookingSettings(companyId: String, routeId: String?): BookingCalcSettings

    /** The T5 route picker's options (S14): every company route with station names. */
    suspend fun routeOptions(companyId: String): List<RouteChoice>

    /** The T5 goods picker's options (S14). */
    suspend fun goodsOptions(companyId: String): List<GoodsChoice>
}

/** One route row of the booking form's picker (id = ROUTE_E local id). */
data class RouteChoice(val id: String, val label: String, val distanceKm: Long, val transitDays: Long)

/** One goods row of the booking form's picker (id = GOODS_E local id). */
data class GoodsChoice(val id: String, val name: String)

@Singleton
class RateCardRepositoryImpl @Inject constructor(
    private val mastersDao: MastersDao,
    private val orgDao: OrgDao,
    private val settingsDao: SettingsDao,
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
        // The dated setting governs: newest effective_from at or before now. With no row at
        // all (a pre-S14 database), the §10 demo defaults apply — forward charge, 5%.
        val setting = settingsDao.governingSetting(companyId, System.currentTimeMillis())
        return BookingCalcSettings(
            weightStepG = setting?.weight_step_g ?: 1000L,
            volumetricDivisor = setting?.volumetric_divisor_g,
            gstTreatment = runCatching { GstTreatment.valueOf(company?.gst_treatment ?: "") }.getOrDefault(GstTreatment.FORWARD),
            gstRateBp = setting?.gst_rate_bp ?: 500,
            rounding = setting?.rounding?.let { runCatching { RoundingRule.valueOf(it) }.getOrNull() } ?: RoundingRule.NEAREST_RUPEE,
            companyRegisteredState = company?.gstin?.let { GstinStateCodes.stateOf(it) },
            defaultPlaceOfSupplyState = destinationState,
        )
    }

    override suspend fun routeOptions(companyId: String): List<RouteChoice> =
        settingsDao.routeOptions(companyId).map {
            RouteChoice(
                id = it.local_id,
                label = "${it.from_name} → ${it.to_name} · ${it.distance_km} km · usually ${it.transit_days} day${if (it.transit_days == 1L) "" else "s"}",
                distanceKm = it.distance_km,
                transitDays = it.transit_days,
            )
        }

    override suspend fun goodsOptions(companyId: String): List<GoodsChoice> =
        settingsDao.goodsOptions(companyId).map { GoodsChoice(it.local_id, it.name) }

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
