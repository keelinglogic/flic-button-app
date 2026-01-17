package uk.co.sixpillar.flicbridge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import uk.co.sixpillar.flicbridge.service.FlicBridgeService

class BootReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            Log.i(TAG, "Boot completed - checking if service should start")

            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val serviceEnabled = prefs.getBoolean(FlicBridgeService.PREF_SERVICE_ENABLED, true)

            if (serviceEnabled) {
                Log.i(TAG, "Starting FlicBridgeService on boot")
                val serviceIntent = Intent(context, FlicBridgeService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                Log.i(TAG, "Service disabled - not starting on boot")
            }
        }
    }
}
