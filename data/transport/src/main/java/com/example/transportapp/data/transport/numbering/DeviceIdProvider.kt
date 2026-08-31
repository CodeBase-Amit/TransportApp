package com.example.transportapp.data.transport.numbering

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** The device short id stamped into provisional numbers (§9: "PROV- prefix + device short id"). */
fun interface DeviceIdProvider {
    fun shortId(): String
}

@Singleton
class AndroidDeviceIdProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : DeviceIdProvider {

    override fun shortId(): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        return androidId.take(4).uppercase().ifEmpty { "DEV1" }
    }
}
