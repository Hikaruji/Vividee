package com.example.visualvivid // Replace with your package name

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.media.MediaPlayer
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@SuppressLint("MissingPermission") // Suppress check since we handle permissions at runtime
class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 101
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    // --- Activity Lifecycle ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (hasRequiredPermissions()) {
            showDisconnectedScreen()
        } else {
            requestRequiredPermissions()
        }
    }

    override fun onStop() {
        super.onStop()
        // Clean up resources
        mediaPlayer?.release()
        mediaPlayer = null
        (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).cancel()
        handler.removeCallbacksAndMessages(null) // Stop any pending mock tasks
    }

    // --- Screen State Management ---

    private fun switchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun showDisconnectedScreen() {
        switchFragment(DisconnectedFragment.newInstance {
            // This lambda is called when the connect button is clicked
            showConnectingScreen()
            // MOCK: Simulate a BLE connection process
            handler.postDelayed({ showConnectedScreen() }, 3000)
        })
    }

    private fun showConnectingScreen() {
        switchFragment(ConnectingFragment())
        // Announce for accessibility
        findViewById<View>(android.R.id.content)
            .announceForAccessibility("Searching for and connecting to smart cane.")
    }

// Paste this function inside your MainActivity class

    private fun showConnectedScreen() {
        // Create a new instance of our ConnectedFragment
        val fragment = ConnectedFragment.newInstance()

        // This is where we define what happens when the circle is clicked on the connected screen
        fragment.onDisconnectClicked = {
            // In a real app, you would add your BLE disconnection logic here
            Log.d("AppFlow", "Disconnect clicked. Returning to disconnected screen.")
            Snackbar.make(findViewById(R.id.root_layout), "Cane Disconnected.", Snackbar.LENGTH_SHORT).show()

            // This is the line that switches the screen back to the disconnected state
            showDisconnectedScreen()
        }

        // This command actually puts the fragment on the screen
        switchFragment(fragment)

        // Announce the new state for accessibility
        findViewById<View>(android.R.id.content)
            .announceForAccessibility("Cane connected successfully.")

        // --- MOCK DATA FLOW ---
        // This section simulates receiving data from the smart cane over time.
        // In your final app, this would be replaced by your real Bluetooth data handling.
        handler.postDelayed({
            updateConnectedUI("90%", "Clear")

            // Simulate an obstacle warning after 5 seconds
            handler.postDelayed({
                updateConnectedUI("88%", "Obstacle Ahead!")
                vibrate(longArrayOf(0, 200, 100, 200), -1) // Short vibration for obstacle

                // Simulate an SOS signal after another 8 seconds
                handler.postDelayed({
                    triggerSOS()
                }, 8000)

            }, 5000)

        }, 1000)
    }

    private fun showSosScreen() {
        // CHANGE: We now create the fragment with a listener
        val fragment = SosFragment.newInstance()
        fragment.onCancelSosClicked = {
            exitSosMode()
        }
        switchFragment(fragment)
        findViewById<View>(android.R.id.content)
            .announceForAccessibility("SOS activated. Requesting help and sending location.")
    }

    // --- UI Update Logic ---

    private fun updateConnectedUI(battery: String, obstacle: String) {
        // Find the fragment and update its views
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is ConnectedFragment) {
            fragment.updateData(battery, obstacle)
        }
    }


    // --- Core Functionality (SOS, Alerts) ---

    private fun triggerSOS() {
        vibrate(longArrayOf(0, 1000, 500, 1000, 500), 0)
        // playAlarmSound() // This was commented out before, keeping it that way
        showSosScreen()
        sendLocationToCloud()
    }

    private fun sendLocationToCloud() {
        LocationServices.getFusedLocationProviderClient(this).lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val payload = SosPayload(userId = "VisualVividUser01", latitude = location.latitude, longitude = location.longitude)
                    Log.d("SOS_Flow", "Sending payload to cloud: $payload")
                    // Uncomment the following lines to make a real network call
                    /*
                    ApiClient.apiService.sendSos(payload).enqueue(object : Callback<Void> {
                        override fun onResponse(call: Call<Void>, response: Response<Void>) {
                            val message = if(response.isSuccessful) "Help signal sent successfully." else "Failed to send help signal."
                            Snackbar.make(findViewById(R.id.root_layout), message, Snackbar.LENGTH_LONG).show()
                            Log.d("SOS_Flow", message)
                        }

                        override fun onFailure(call: Call<Void>, t: Throwable) {
                            Snackbar.make(findViewById(R.id.root_layout), "Network error. Could not send help signal.", Snackbar.LENGTH_LONG).show()
                            Log.e("SOS_Flow", "Network failure", t)
                        }
                    })
                    */
                    // Mock success message for demonstration
                    Snackbar.make(findViewById(R.id.root_layout), "Help signal sent to server.", Snackbar.LENGTH_LONG).show()

                } else {
                    Log.w("SOS_Flow", "Could not get location to send SOS.")
                    Snackbar.make(findViewById(R.id.root_layout), "Could not get current location.", Snackbar.LENGTH_LONG).show()
                }
            }
    }

    private fun vibrate(pattern: LongArray, repeat: Int) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, repeat))
            } else {
                vibrator.vibrate(pattern, repeat)
            }
        }
    }

    private fun playAlarmSound() {
        // Stop any previous sound
        mediaPlayer?.stop()
        mediaPlayer?.release()
        // Start new sound
     //   mediaPlayer = MediaPlayer.create(this, R.raw.emergency_alarm)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
    }
    private fun exitSosMode() {
        Log.d("AppFlow", "SOS Cancelled by user.")
        stopAlerts()

        // TODO: In a real app, send a "cancellation" message to your cloud server or emergency contacts here.

        Snackbar.make(findViewById(R.id.root_layout), "SOS Cancelled.", Snackbar.LENGTH_LONG).show()
        // Return to the normal connected screen
        showConnectedScreen()
    }

    // NEW: Helper function to stop all alerts
    private fun stopAlerts() {
        // Stop vibration
        (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).cancel()

        // Stop sound
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.stop()
        }
        mediaPlayer?.release()
        mediaPlayer = null
    }

    // --- Permissions Handling ---

    private fun hasRequiredPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestRequiredPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.VIBRATE
            ),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showDisconnectedScreen()
            } else {
                // Explain to the user why the permissions are needed
                Snackbar.make(findViewById(R.id.root_layout), "Permissions are required to connect to the cane and use SOS features.", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Grant") { requestRequiredPermissions() }
                    .show()
            }
        }
    }
}


// --- Fragment Classes ---

class DisconnectedFragment : Fragment() {
    private var onConnect: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.layout_disconnected, container, false)
        // CHANGE: Find the ImageView instead of the Button
        view.findViewById<ImageView>(R.id.iv_status_indicator).setOnClickListener {
            onConnect?.invoke()
        }
        return view
    }

    companion object {
        fun newInstance(onConnect: () -> Unit): DisconnectedFragment {
            val fragment = DisconnectedFragment()
            fragment.onConnect = onConnect
            return fragment
        }
    }
}

class ConnectingFragment : Fragment(R.layout.layout_connecting)

class SosFragment : Fragment() {
    // NEW: Add a callback for when the cancel button is clicked
    var onCancelSosClicked: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.layout_sos, container, false)

        // NEW: Set the click listener on the cancel button
        view.findViewById<Button>(R.id.btn_cancel_sos).setOnClickListener {
            onCancelSosClicked?.invoke()
        }

        return view
    }

    // NEW: Add a companion object to easily create new instances
    companion object {
        fun newInstance(): SosFragment {
            return SosFragment()
        }
    }
}

class ConnectedFragment : Fragment() {
    private lateinit var tvBattery: TextView
    private lateinit var tvObstacle: TextView
    private lateinit var cardObstacle: CardView
    private lateinit var layoutRoot: ConstraintLayout

    // NEW: Add a callback for when the circle is clicked to disconnect
    var onDisconnectClicked: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.layout_connected, container, false)
        tvBattery = view.findViewById(R.id.tv_battery)
        tvObstacle = view.findViewById(R.id.tv_obstacle)
        cardObstacle = view.findViewById(R.id.card_obstacle)
        layoutRoot = view.findViewById(R.id.connected_layout)

        // NEW: Set the click listener on the connected circle
        view.findViewById<ImageView>(R.id.iv_status_indicator).setOnClickListener {
            onDisconnectClicked?.invoke()
        }

        return view
    }

    fun updateData(battery: String, obstacle: String) {
        tvBattery.text = "Battery: $battery"
        tvObstacle.text = "Obstacle: $obstacle"

        if (obstacle.contains("Clear")) {
            cardObstacle.setCardBackgroundColor(Color.parseColor("#66BB6A"))
            layoutRoot.setBackgroundColor(Color.parseColor("#E1F8DC"))
            view?.announceForAccessibility("Status is clear. Battery is at $battery.")
        } else {
            cardObstacle.setCardBackgroundColor(Color.parseColor("#FFA726"))
            layoutRoot.setBackgroundColor(Color.parseColor("#FFF3E0"))
            view?.announceForAccessibility("Warning, obstacle ahead! Battery is at $battery.")
        }
    }

    // NEW: Add a companion object to easily create new instances
    companion object {
        fun newInstance(): ConnectedFragment {
            return ConnectedFragment()
        }
    }
}

// --- Networking (Retrofit) ---

data class SosPayload(val userId: String, val latitude: Double, val longitude: Double)

interface ApiService {
    @POST("api/sos") // Example endpoint
    fun sendSos(@Body payload: SosPayload): Call<Void>
}

object ApiClient {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://your-cloud-server-url.com/") // <-- IMPORTANT: REPLACE WITH YOUR SERVER URL
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}