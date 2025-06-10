// In ConnectedFragment.kt
package com.example.visualvivid

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment

class ConnectedFragment : Fragment() {
    // REMOVED: private lateinit var tvBattery: TextView
    private lateinit var tvObstacle: TextView
    private lateinit var cardObstacle: CardView
    private lateinit var layoutRoot: ConstraintLayout
    var onDisconnectClicked: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.layout_connected, container, false)
        // REMOVED: tvBattery = view.findViewById(R.id.tv_battery)
        tvObstacle = view.findViewById(R.id.tv_obstacle)
        cardObstacle = view.findViewById(R.id.card_obstacle)
        layoutRoot = view.findViewById(R.id.connected_layout)
        val circle: ImageView = view.findViewById(R.id.iv_status_indicator)
        val pulseAnimation: Animation = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_effect)
        circle.startAnimation(pulseAnimation)
        circle.setOnClickListener { onDisconnectClicked?.invoke() }
        return view
    }

    // CHANGED: The function no longer accepts a "battery" parameter
    fun updateData(obstacle: String) {
        if (isAdded) {
            // REMOVED: tvBattery.text = "Battery: $battery"
            tvObstacle.text = "Obstacle: $obstacle"
            if (obstacle.contains("Clear")) {
                cardObstacle.setCardBackgroundColor(Color.parseColor("#66BB6A"))
                layoutRoot.setBackgroundColor(Color.parseColor("#E1F8DC"))
                view?.announceForAccessibility("Status is clear.")
            } else {
                cardObstacle.setCardBackgroundColor(Color.parseColor("#FFA726"))
                layoutRoot.setBackgroundColor(Color.parseColor("#FFF3E0"))
                view?.announceForAccessibility("Warning, obstacle ahead!")
            }
        }
    }

    companion object {
        fun newInstance(): ConnectedFragment = ConnectedFragment()
    }
}