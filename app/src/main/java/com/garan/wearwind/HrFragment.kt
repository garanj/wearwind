package com.garan.wearwind

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.garan.wearwind.databinding.FragmentHrBinding


class HrFragment : Fragment() {
    private var _binding: FragmentHrBinding? = null
    private val binding get() = _binding!!
    private val model by activityViewModels<FanControlViewModel>()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHrBinding.inflate(inflater, container, false)
        val view = binding.root

        model.speedFromDevice.observe(viewLifecycleOwner, Observer {
            binding.speed.text = "$it"
        })

        model.hr.observe(viewLifecycleOwner, Observer {
            binding.hr.text = "$it"
        })

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}