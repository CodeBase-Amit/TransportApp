package com.example.transportapp

import android.app.Application
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
 * factory, seeds the demo dataset before the first frame (Phase2.md §3.5), and schedules the
 * outbox drain. The drain itself is a no-op until the sync phase.
 */
@HiltAndroidApp
class TransportApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var demoSeeder: DemoSeeder

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Org dataset is tiny (<50 rows): seed synchronously so T2 renders rows at first paint.
        runBlocking { demoSeeder.seedIfNeeded() }
        com.example.transportapp.debug.DebugProvReceiver.registerIfDebuggable(this)
        WorkManager.getInstance(this@TransportApp).enqueueUniquePeriodicWork(
            OutboxDrainWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            OutboxDrainWorker.periodicRequest(),
        )
    }
}
