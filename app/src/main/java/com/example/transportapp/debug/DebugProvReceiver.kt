package com.example.transportapp.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.transportapp.data.transport.numbering.NumberingRepository
import com.example.transportapp.data.transport.session.SessionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Debug-only hook for the §9 provisional-numbering demo: adb broadcasts
 * `am broadcast -a com.example.transportapp.DEBUG_PROV_MODE` to exhaust the active bilty
 * lease and simulate a server that cannot grant right now, so the next booking falls
 * through to the PROV- series and the T5 banner appears. Registered only in debuggable
 * builds (TransportApp.onCreate).
 */
@AndroidEntryPoint
class DebugProvReceiver : BroadcastReceiver() {

    @Inject lateinit var numberingRepository: NumberingRepository
    @Inject lateinit var sessionRepository: SessionRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val session = sessionRepository.session.first()
                numberingRepository.debugShrinkActiveLease(session.companyId, session.branchId, "BILTY")
                numberingRepository.debugSetGrantsEnabled(false)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION = "com.example.transportapp.DEBUG_PROV_MODE"

        fun registerIfDebuggable(context: Context) {
            val debuggable = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            if (!debuggable) return
            ContextCompat.registerReceiver(
                context,
                DebugProvReceiver(),
                android.content.IntentFilter(ACTION),
                ContextCompat.RECEIVER_EXPORTED,
            )
        }
    }
}
