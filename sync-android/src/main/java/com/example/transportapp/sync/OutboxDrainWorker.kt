package com.example.transportapp.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.transportapp.core.database.outbox.OutboxDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * The outbox drain (TransportApp.md §16.2). **Phase 2: drain is a no-op** (Phase2.md §1,
 * decision 2) — the worker runs, counts drainable rows, and logs. It exists now so the
 * dependency-aware readiness query, the constraints and the retry policy are exercised
 * before the sync phase swaps the body for real push; nothing above this seam changes then.
 */
@HiltWorker
class OutboxDrainWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val outboxDao: OutboxDao,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pending = outboxDao.getPendingCount()
        val ready = outboxDao.getReady(now = System.currentTimeMillis(), limit = 100)
        Log.i(
            TAG,
            "Outbox drain (no-op until Phase 3): $pending pending, ${ready.size} ready — nothing sent.",
        )
        // Never consume rows in Phase 2: the local mirror is the only truth there is.
        return Result.success()
    }

    companion object {
        const val TAG = "OutboxDrain"
        const val UNIQUE_NAME = "outbox-drain"

        fun periodicRequest() = PeriodicWorkRequestBuilder<OutboxDrainWorker>(6, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        fun enqueue(workManager: WorkManager) {
            workManager.enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, periodicRequest())
        }
    }
}
