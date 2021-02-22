package com.garan.wearwind

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.garan.wearwind.databinding.FragmentHrBinding


class HrFragment : FanFragment() {
    private var _binding: FragmentHrBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHrBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun setModel(fanMetrics: FanMetrics) {
        fanMetrics.speedFromDevice.observe(viewLifecycleOwner, {
            binding.speed.text = "$it"
        })

        fanMetrics.hr.observe(viewLifecycleOwner, {
            binding.hr.text = "$it"
        })
    }
}