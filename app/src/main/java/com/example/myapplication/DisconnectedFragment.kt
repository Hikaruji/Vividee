package com.example.visualvivid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.fragment.app.Fragment

class DisconnectedFragment : Fragment() {
    var onConnectClicked: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.layout_disconnected, container, false)
        val circle: ImageView = view.findViewById(R.id.iv_status_indicator)
        val pulseAnimation: Animation = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_effect)
        circle.startAnimation(pulseAnimation)
        circle.setOnClickListener { onConnectClicked?.invoke() }
        return view
    }

    companion object {
        fun newInstance(): DisconnectedFragment = DisconnectedFragment()
    }
}

