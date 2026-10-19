package com.reelcounter.feature.tracking

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Opens the system Usage access screen, on this app's own entry where the device supports it.
 * Usage access can't be requested with a dialog; the user grants it there.
 */
internal fun Context.openUsageAccessSettings() {
    val forThisApp = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.fromParts("package", packageName, null))
    val candidates = listOf(forThisApp, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), Intent(Settings.ACTION_SETTINGS))
    for (intent in candidates) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: ActivityNotFoundException) {
            // Some OEM builds lack the app-specific page; fall back to the list, then to Settings.
        }
    }
}
