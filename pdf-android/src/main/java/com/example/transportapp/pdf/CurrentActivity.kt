package com.example.transportapp.pdf

import android.app.Activity

/**
 * The headless WebView must attach to a real window (checklist item 3), but the repository
 * layer only carries the Application context, which no amount of ContextWrapper walking
 * turns into an Activity. The resumed activity registers itself here; the renderer falls
 * back to it.
 */
object CurrentActivity {

    @Volatile
    var current: Activity? = null

    fun register(activity: Activity) {
        current = activity
    }

    fun unregister(activity: Activity) {
        if (current === activity) current = null
    }
}
