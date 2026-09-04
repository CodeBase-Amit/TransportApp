package com.example.transportapp.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.transportapp.core.database.outbox.OutboxDao
import com.example.transportapp.data.transport.sync.OutboxPush
import com.example.transportapp.core.common.Result
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * The outbox drain (TransportApp.md §16.2). **S25 activates the push**: ready rows
 * (dependency-ordered by the prereq table) map onto the backend's REST surface via
 * `OutboxPush`; 2xx marks rows DONE, failures back off exponentially with the typed
 * code recorded for T31's queue. A dead network leaves rows PENDING — the D62
 * offline-first contract means a drain failure is never an app failure.
 */
@HiltWorker
class OutboxDrainWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val outboxDao: OutboxDao,
    private val outboxPush: OutboxPush,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pending = outboxDao.getPendingCount()
        when (val report = outboxPush.drain(limit = BATCH)) {
            is com.example.transportapp.core.common.Result.Success -> {
                val r = report.value
                if (r.pending > 0 || pending > 0) {
                    android.util.Log.i(TAG, "Drain: ${r.pushed} pushed, ${r.failed} failed, $pending pending")
                }
                // Drain results are advisory; WorkManager's own retry policy governs scheduling.
                return Result.success()
            }
            is com.example.transportapp.core.common.Result.Failure -> {
                android.util.Log.w(TAG, "Drain blocked: ${report.message}")
                return Result.success() // the periodic schedule retries; never surface to the user here
            }
        }
    }

    companion object {
        const val TAG = "OutboxDrain"
        const val UNIQUE_NAME = "outbox-drain"
        const val BATCH = 100

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
