package com.example.transportapp

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.example.transportapp.core.database.seed.DemoSeeder
import com.example.transportapp.sync.OutboxDrainWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Hilt root (Phase2.md S1). Owns the WorkManager configuration backed by the Hilt worker
 * factory, seeds the demo dataset before the first frame in debug builds only (Phase2.md §3.5),
 * and schedules the outbox drain. The drain itself is a no-op until the sync phase.
 *
 * Seeding is gated on [ApplicationInfo.FLAG_DEBUGGABLE] (the BuildConfig build feature is off
 * in AGP 9, so the debug receiver's idiom is reused): a production install must never write
 * the demo companies, parties or money rows into a real database, and the release cold-start
 * path skips the seed-check read entirely.
 */
@HiltAndroidApp
class TransportApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var demoSeeder: DemoSeeder

    private val isDebuggable: Boolean
        get() = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Org dataset is tiny (<50 rows): seed synchronously so T2 renders rows at first paint.
        // Debug builds only — see the class KDoc for why release must never seed.
        if (isDebuggable) {
            runBlocking { demoSeeder.seedIfNeeded() }
        }
        com.example.transportapp.debug.DebugProvReceiver.registerIfDebuggable(this)
        WorkManager.getInstance(this@TransportApp).enqueueUniquePeriodicWork(
            OutboxDrainWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            OutboxDrainWorker.periodicRequest(),
        )
    }
}
