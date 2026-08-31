package com.example.transportapp.data.transport.dashboard

import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.dao.IdleVehicleRow
import com.example.transportapp.domain.transport.RoleRank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/** One exception-strip entry grouped by reason (§13). */
data class ExceptionReason(val reasonCode: String, val count: Int)

/** One idle own vehicle as the tile prints it (DB types stay internal to the data layer). */
data class VehicleIdle(val vehicleId: String, val number: String, val idleDays: Long)

/** The §13 "This month" tile: freight earned, lorry hire paid, provisional margin. */
data class MonthPosition(
    val freightPaise: Long,
    val hirePaise: Long,
    val costPaise: Long,
    val lastMonthFreightPaise: Long,
) {
    val marginPaise: Long get() = freightPaise - hirePaise - costPaise
}

/** All ten §13 tiles, computed as of one instant. Null tiles are role-hidden. */
data class DashboardData(
    val asOf: Long,
    val runningServices: Int,
    val nearestRunningArrival: Long?,
    val inTransit: Int,
    val inTransitPackages: Long,
    val bookedToday: Int,
    val bookedTodayWeightKg: Long,
    val bookedTodayFreightPaise: Long,
    val topayAwaiting: Int,
    val topayAwaitingPaise: Long,
    val unbilledPaise: Long,
    val unbilledOldestDays: Long?,
    val receivablePaise: Long,
    val receivableOver90Paise: Long,
    val exceptions: List<ExceptionReason>,
    val ageing1to3: Int,
    val ageing4to7: Int,
    val ageing7plus: Int,
    val idleVehicles: List<VehicleIdle>,
    val month: MonthPosition,
)

/**
 * The §13 dashboard, read entirely from local Room (§13: every tile is a projection over
 * already-synced data — never a network call on the critical path). The ten tile queries run
 * in parallel and the result carries the as-of stamp rather than pretending to be live.
 */
interface DashboardRepository {

    suspend fun load(now: Long, companyWideToday: Boolean = false): DashboardData

    /** The tile's visibility rule, exposed so the ViewModel can hide rather than grey out. */
    fun tileVisible(role: String, minRole: String): Boolean = RoleRank.atLeast(role, minRole)
}

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val database: TransportDatabase,
    private val sessionRepository: com.example.transportapp.data.transport.session.SessionRepository,
) : DashboardRepository {

    override suspend fun load(now: Long, companyWideToday: Boolean): DashboardData {
        val session = sessionRepository.session.first()
        val company = session.companyId
        val branch = session.branchId
        val dao = database.dashboardDao()
        return coroutineScope {
            val running = async(Dispatchers.IO) { dao.countRunningTrips(company) to dao.nearestRunningArrival(company) }
            val transit = async(Dispatchers.IO) { dao.inTransit(company) }
            val todayStart = startOfDay(now)
            val today = async(Dispatchers.IO) {
                dao.bookedToday(company, if (companyWideToday) null else branch, todayStart)
            }
            val topay = async(Dispatchers.IO) { dao.topayAwaiting(company, branch) }
            val unbilled = async(Dispatchers.IO) { dao.unbilled(company) }
            val receivable = async(Dispatchers.IO) { dao.receivable(company, now) }
            val exceptions = async(Dispatchers.IO) {
                dao.exceptionReasons(company, branch, now - THIRTY_DAYS)
            }
            val ageing = async(Dispatchers.IO) {
                dao.overdueBuckets(company, branch, now, now - com.example.transportapp.domain.transport.tracking.Ageing.DEFAULT_GRACE_DAYS * DAY_MS)
            }
            val idle = async(Dispatchers.IO) {
                dao.idleVehicles(company, now, IDLE_DAYS, now - 30 * DAY_MS)
            }
            val month = async(Dispatchers.IO) {
                val start = startOfMonth(now)
                val thisMonth = dao.monthMoney(company, start, nextMonth(now))
                val lastMonth = dao.monthMoney(company, previousMonth(now), start)
                thisMonth to lastMonth
            }
            val (runningCount, nearest) = running.await()
            val transitRow = transit.await()
            val todayRow = today.await()
            val topayRow = topay.await()
            val unbilledRow = unbilled.await()
            val receivableRow = receivable.await()
            val exceptionList = exceptions.await().map { ExceptionReason(it.reason_code ?: "OTHER", it.cnt) }
            val ageingRow = ageing.await()
            val idleList = idle.await().map { VehicleIdle(it.vehicle_id, it.number, it.idle_days) }
            val monthPair = month.await()

            DashboardData(
                asOf = now,
                runningServices = runningCount,
                nearestRunningArrival = nearest,
                inTransit = transitRow.cnt,
                inTransitPackages = transitRow.packages,
                bookedToday = todayRow.cnt,
                bookedTodayWeightKg = todayRow.weightG / 1000,
                bookedTodayFreightPaise = todayRow.freightPaise,
                topayAwaiting = topayRow.cnt,
                topayAwaitingPaise = topayRow.amountPaise,
                unbilledPaise = unbilledRow.amountPaise,
                unbilledOldestDays = unbilledRow.oldestBookedAt?.let { (now - it) / DAY_MS },
                receivablePaise = receivableRow.outstandingPaise,
                receivableOver90Paise = receivableRow.over90Paise,
                exceptions = exceptionList,
                ageing1to3 = ageingRow.bucket1to3.toInt(),
                ageing4to7 = ageingRow.bucket4to7.toInt(),
                ageing7plus = ageingRow.bucket7plus.toInt(),
                idleVehicles = idleList,
                month = MonthPosition(
                    freightPaise = monthPair.first.freightPaise,
                    hirePaise = monthPair.first.hirePaise,
                    costPaise = monthPair.first.costPaise,
                    lastMonthFreightPaise = monthPair.second.freightPaise,
                ),
            )
        }
    }

    private fun startOfDay(now: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun startOfMonth(now: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun nextMonth(now: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = startOfMonth(now)
        cal.add(Calendar.MONTH, 1)
        return cal.timeInMillis
    }

    private fun previousMonth(now: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = startOfMonth(now)
        cal.add(Calendar.MONTH, -1)
        return cal.timeInMillis
    }

    companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
        const val THIRTY_DAYS = 30 * DAY_MS
        const val IDLE_DAYS = 7L
    }
}
