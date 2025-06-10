package com.example.visualvivid

import android.content.*
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MainActivity : AppCompatActivity() {

    private lateinit var caneStatusReceiver: BroadcastReceiver

    companion object {
        const val ACTION_CANE_CONNECTED = "com.example.visualvivid.ACTION_CANE_CONNECTED"
        const val ACTION_UPDATE_UI = "com.example.visualvivid.ACTION_UPDATE_UI"
        const val ACTION_SOS_TRIGGERED = "com.example.visualvivid.ACTION_SOS_TRIGGERED"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // The receiver MUST be created before it can be registered.
        setupBroadcastReceiver()

        // This ensures that when the app starts, it shows the disconnected screen
        // only if it's not already showing a screen (e.g., on rotation).
        if (savedInstanceState == null) {
            showDisconnectedScreen()
        }
    }

    override fun onStart() {
        super.onStart()
        // Register the receiver in onStart for better lifecycle handling
        val intentFilter = IntentFilter().apply {
            addAction(ACTION_CANE_CONNECTED)
            addAction(ACTION_UPDATE_UI)
            addAction(ACTION_SOS_TRIGGERED)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(caneStatusReceiver, intentFilter)
    }

    override fun onStop() {
        super.onStop()
        // Unregister in onStop to match onStart
        LocalBroadcastManager.getInstance(this).unregisterReceiver(caneStatusReceiver)
    }

    private fun setupBroadcastReceiver() {
        caneStatusReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d("MainActivity", "Broadcast received with action: ${intent.action}")
                when (intent.action) {
                    ACTION_CANE_CONNECTED -> {
                        Log.d("MainActivity", "ACTION_CANE_CONNECTED received. Showing connected screen.")
                        showConnectedScreen()
                    }
                    ACTION_UPDATE_UI -> {
                        val obstacle = intent.getStringExtra("obstacle") ?: "N/A"
                        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                        if (fragment is ConnectedFragment) {
                            fragment.updateData(obstacle)
                        }
                    }
                    ACTION_SOS_TRIGGERED -> {
                        Log.d("MainActivity", "SOS Broadcast Received. Showing SOS screen.")
                        showSosScreen()
                    }
                }
            }
        }
    }

    private fun switchFragment(fragment: Fragment) {
        // Check if activity is still active before committing
        if (!isFinishing && !isDestroyed) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit()
        }
    }

    private fun showDisconnectedScreen() {
        val fragment = DisconnectedFragment.newInstance()
        fragment.onConnectClicked = {
            Log.d("MainActivity", "Connect circle clicked. Starting CaneService.")
            startService(Intent(this, CaneService::class.java))
            showConnectingScreen()
        }
        switchFragment(fragment)
    }

    private fun showConnectingScreen() {
        switchFragment(ConnectingFragment())
    }

    private fun showConnectedScreen() {
        val fragment = ConnectedFragment.newInstance()
        fragment.onDisconnectClicked = {
            stopService(Intent(this, CaneService::class.java))
            showDisconnectedScreen()
        }
        switchFragment(fragment)
    }

    private fun showSosScreen() {
        val fragment = SosFragment.newInstance()
        fragment.onCancelSosClicked = {
            stopService(Intent(this, CaneService::class.java))
            showDisconnectedScreen()
        }
        switchFragment(fragment)
    }
}