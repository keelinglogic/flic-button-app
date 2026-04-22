package uk.co.sixpillar.flicbridge.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import io.flic.flic2libandroid.Flic2Button
import io.flic.flic2libandroid.Flic2ButtonListener
import io.flic.flic2libandroid.Flic2Manager
import uk.co.sixpillar.flicbridge.FlicBridgeApp
import uk.co.sixpillar.flicbridge.MainActivity
import uk.co.sixpillar.flicbridge.R

class FlicBridgeService : Service() {

    companion object {
        const val TAG = "FlicBridgeService"
        const val NOTIFICATION_ID = 1001

        // Default intents (Zello)
        const val DEFAULT_DOWN_INTENT = "com.zello.ptt.down"
        const val DEFAULT_UP_INTENT = "com.zello.ptt.up"

        // Preference keys
        const val PREF_DOWN_INTENT = "pref_down_intent"
        const val PREF_UP_INTENT = "pref_up_intent"
        const val PREF_SERVICE_ENABLED = "pref_service_enabled"
    }

    private val buttonListeners = mutableMapOf<String, Flic2ButtonListener>()
    private var downIntent: String = DEFAULT_DOWN_INTENT
    private var upIntent: String = DEFAULT_UP_INTENT

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "FlicBridgeService created")
        loadIntentConfig()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "FlicBridgeService starting")

        startForeground(NOTIFICATION_ID, createNotification())
        connectToAllButtons()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "FlicBridgeService destroyed")
        disconnectAllButtons()
    }

    private fun loadIntentConfig() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        downIntent = prefs.getString(PREF_DOWN_INTENT, DEFAULT_DOWN_INTENT) ?: DEFAULT_DOWN_INTENT
        upIntent = prefs.getString(PREF_UP_INTENT, DEFAULT_UP_INTENT) ?: DEFAULT_UP_INTENT
        Log.i(TAG, "Loaded intents - Down: $downIntent, Up: $upIntent")
    }

    fun reloadConfig() {
        loadIntentConfig()
    }

    private fun connectToAllButtons() {
        try {
            val manager = Flic2Manager.getInstance()
            val buttons = manager.buttons

            Log.i(TAG, "Found ${buttons.size} paired Flic buttons")

            for (button in buttons) {
                connectButton(button)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to buttons", e)
        }
    }

    fun connectButton(button: Flic2Button) {
        val bdAddr = button.bdAddr

        if (buttonListeners.containsKey(bdAddr)) {
            Log.d(TAG, "Button $bdAddr already has a listener")
            return
        }

        val listener = createButtonListener(bdAddr)
        buttonListeners[bdAddr] = listener

        button.addListener(listener)
        button.connect()

        Log.i(TAG, "Connected to button: $bdAddr (${button.name ?: "unnamed"})")
    }

    private fun createButtonListener(bdAddr: String): Flic2ButtonListener {
        return object : Flic2ButtonListener() {

            override fun onButtonUpOrDown(
                button: Flic2Button,
                wasQueued: Boolean,
                lastQueued: Boolean,
                timestamp: Long,
                isUp: Boolean,
                isDown: Boolean
            ) {
                // Ignore queued events (from when button was disconnected)
                if (wasQueued) {
                    Log.d(TAG, "Ignoring queued event from $bdAddr")
                    return
                }

                if (isDown) {
                    Log.i(TAG, "Button DOWN: $bdAddr -> Broadcasting: $downIntent")
                    broadcastIntent(downIntent)
                }

                if (isUp) {
                    Log.i(TAG, "Button UP: $bdAddr -> Broadcasting: $upIntent")
                    broadcastIntent(upIntent)
                }
            }

            override fun onConnect(button: Flic2Button) {
                Log.i(TAG, "Button connected: $bdAddr")
                updateNotification("Connected to ${button.name ?: bdAddr}")
            }

            override fun onReady(button: Flic2Button, timestamp: Long) {
                Log.i(TAG, "Button ready: $bdAddr")
            }

            override fun onDisconnect(button: Flic2Button) {
                Log.w(TAG, "Button disconnected: $bdAddr")
                updateNotification("Button disconnected - reconnecting...")

                // Auto-reconnect
                button.connect()
            }

            override fun onFailure(button: Flic2Button, errorCode: Int, subCode: Int) {
                Log.e(TAG, "Button failure: $bdAddr, error: $errorCode, subCode: $subCode")
            }

            override fun onBatteryLevelUpdated(button: Flic2Button, level: io.flic.flic2libandroid.BatteryLevel) {
                Log.d(TAG, "Button $bdAddr battery: ${level.estimatedPercentage}%")
            }
        }
    }

    private fun broadcastIntent(action: String) {
        try {
            val intent = Intent(action)
            sendBroadcast(intent)
            Log.d(TAG, "Broadcast sent: $action")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to broadcast intent: $action", e)
        }
    }

    private fun disconnectAllButtons() {
        try {
            val manager = Flic2Manager.getInstance()
            for (button in manager.buttons) {
                buttonListeners[button.bdAddr]?.let { listener ->
                    button.removeListener(listener)
                }
                button.disconnectOrAbortPendingConnection()
            }
            buttonListeners.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting buttons", e)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, FlicBridgeApp.CHANNEL_ID)
            .setContentTitle("Flic Bridge Active")
            .setContentText("Listening for button presses")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = NotificationCompat.Builder(this, FlicBridgeApp.CHANNEL_ID)
            .setContentTitle("Flic Bridge Active")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setOngoing(true)
            .setSilent(true)
            .build()

        val notificationManager = getSystemService(android.app.NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
