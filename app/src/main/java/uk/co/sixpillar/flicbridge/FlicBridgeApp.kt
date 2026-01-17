package uk.co.sixpillar.flicbridge

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.flic.flic2libandroid.Flic2Manager

class FlicBridgeApp : Application() {

    companion object {
        const val TAG = "FlicBridge"
        const val CHANNEL_ID = "flic_bridge_service"
        const val CHANNEL_NAME = "Flic Bridge Service"

        lateinit var instance: FlicBridgeApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        createNotificationChannel()
        initFlicManager()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps Flic Bridge running in the background"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun initFlicManager() {
        try {
            Flic2Manager.initAndGetInstance(this, Handler(Looper.getMainLooper()))
            Log.i(TAG, "Flic2Manager initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Flic2Manager", e)
        }
    }
}
