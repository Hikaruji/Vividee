package com.example.visualvivid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment

class SosFragment : Fragment() {
    var onCancelSosClicked: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.layout_sos, container, false)
        val sosCircle: ImageView = view.findViewById(R.id.iv_sos_background)
        val pulseAnimation: Animation = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_effect)
        sosCircle.startAnimation(pulseAnimation)
        view.findViewById<Button>(R.id.btn_cancel_sos).setOnClickListener {
            onCancelSosClicked?.invoke()
        }
        return view
    }

    companion object {
        fun newInstance(): SosFragment = SosFragment()
    }
}

