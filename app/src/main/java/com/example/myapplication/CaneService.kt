package com.example.visualvivid

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.LocationServices

class CaneService : Service() {

    private val CHANNEL_ID = "CaneServiceChannel"
    private val NOTIFICATION_ID = 1
    private val handler = Handler(Looper.getMainLooper())

    // --- RE-ADD THIS PROPERTY ---
    private var bubbleMetadata: NotificationCompat.BubbleMetadata? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("CaneService", "Service Created.")
        createNotificationChannel()
        // --- RE-ADD THIS FUNCTION CALL ---
        prepareBubbleMetadata()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("CaneService", "Service Started.")
        val notification = createNotification("Cane Connecting...")
        startForeground(NOTIFICATION_ID, notification)

        handler.postDelayed({
            Log.d("CaneService", "Mock connection successful. Announcing to UI.")
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(MainActivity.ACTION_CANE_CONNECTED))
            startMockDataFlow()
        }, 1500)

        return START_STICKY
    }

    // --- RE-ADD THIS ENTIRE FUNCTION ---
    private fun prepareBubbleMetadata() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Create an intent that launches the BubbleActivity when the bubble is tapped.
            val bubbleIntent = Intent(this, BubbleActivity::class.java)
            val bubblePendingIntent = PendingIntent.getActivity(
                this, 0, bubbleIntent,
                PendingIntent.FLAG_MUTABLE
            )

            // Create the Bubble's metadata and store it in our class property
            bubbleMetadata = NotificationCompat.BubbleMetadata.Builder(bubblePendingIntent,
                IconCompat.createWithResource(this, R.drawable.ic_stat_name))
                .setDesiredHeight(600)
                // This allows the bubble to launch automatically without the user expanding the notification shade first.
                .setAutoExpandBubble(true)
                .setSuppressNotification(true) // Hides the notification in the shade once it's a bubble
                .build()
        }
    }

    private fun createNotification(text: String): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Smart Cane Connected")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        // --- RE-ADD THIS LOGIC ---
        // Only add the bubble metadata if it was successfully created.
        bubbleMetadata?.let {
            builder.setBubbleMetadata(it)
        }

        return builder.build()
    }

    // --- The rest of the file is the same ---

    // In CaneService.kt
    private fun startMockDataFlow() {
        handler.postDelayed({
            updateCaneData("Clear") // REMOVED battery
            handler.postDelayed({
                updateCaneData("Obstacle Ahead!") // REMOVED battery
                vibrate(longArrayOf(0, 200, 100, 200), -1)
                handler.postDelayed({ triggerSOS() }, 8000)
            }, 5000)
        }, 1000)
    }

    private fun updateCaneData(obstacle: String) { // REMOVED battery parameter
        Log.d("CaneService", "Updating data: Obstacle=$obstacle")
        updateNotification("Status: $obstacle") // REMOVED battery from notification

        // Send a broadcast to update the MainActivity UI
        val intent = Intent(MainActivity.ACTION_UPDATE_UI).apply {
            // REMOVED: intent.putExtra("battery", battery)
            putExtra("obstacle", obstacle)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    @SuppressLint("MissingPermission")
    private fun triggerSOS() {
        Log.d("CaneService", "SOS Triggered!")
        vibrate(longArrayOf(0, 1000, 500, 1000, 500), 0)
        LocationServices.getFusedLocationProviderClient(this).lastLocation
            .addOnSuccessListener { location ->
                val intent = Intent(MainActivity.ACTION_SOS_TRIGGERED)
                location?.let {
                    intent.putExtra("latitude", it.latitude)
                    intent.putExtra("longitude", it.longitude)
                }
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
            }
    }

    private fun vibrate(pattern: LongArray, repeat: Int) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, repeat))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, repeat)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Smart Cane Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(serviceChannel)
        }
    }

    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CaneService", "Service Destroyed.")
        handler.removeCallbacksAndMessages(null)
        (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}