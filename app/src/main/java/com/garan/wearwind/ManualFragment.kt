package com.garan.wearwind

import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.garan.wearwind.databinding.FragmentManualBinding


class ManualFragment : Fragment() {
    private var _binding: FragmentManualBinding? = null
    private val binding get() = _binding!!
    private val model by activityViewModels<FanControlViewModel>()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentManualBinding.inflate(inflater, container, false)
        val view = binding.root

        model.speedFromDevice.observe(viewLifecycleOwner, Observer {
            binding.speed.text = "$it"
        })

        initializeSwipe()
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Allows fan speed to be adjusted by swiping up or down on the screen.
     */
    private fun initializeSwipe() {
        val gesture = GestureDetector(requireActivity(),
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDown(e: MotionEvent): Boolean {
                        return true
                    }

                    override fun onFling(
                            e1: MotionEvent, e2: MotionEvent, velocityX: Float,
                            velocityY: Float
                    ): Boolean {
                        model.speedFromDevice.value?.let {
                            when {
                                velocityY < FAST_SWIPE_UP -> incrementSpeed(10)
                                velocityY < SLOW_SWIPE_UP -> incrementSpeed(1)
                                velocityY > FAST_SWIPE_DOWN -> decrementSpeed(10)
                                velocityY > SLOW_SWIPE_DOWN -> decrementSpeed(1)
                            }
                        }
                        return true
                    }
                })

        binding.speed.setOnTouchListener { v, event -> gesture.onTouchEvent(event) }
    }

    private fun incrementSpeed(delta: Int) {
        model.speedFromDevice.value?.let {
            if ((it != -1) &&
                    (it + delta <= 100) &&
                    (model.speedToDevice.value != it + delta)) {
                model.speedToDevice.value = it + delta
            }
        }
    }

    private fun decrementSpeed(delta: Int) {
        model.speedFromDevice.value?.let {
            if ((it != -1) &&
                    (it - delta >= 0) &&
                    (model.speedToDevice.value != it - delta)) {
                model.speedToDevice.value = it - delta
            }
        }
    }
}