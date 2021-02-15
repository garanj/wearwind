package com.garan.wearwind

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import com.garan.wearwind.databinding.ActivityMinMaxBinding

class MinMaxActivity : FragmentActivity() {
    private lateinit var binding: ActivityMinMaxBinding
    private lateinit var sharedPref: SharedPreferences

    companion object {
        const val HR_MAX_KEY = "hr_max"
        const val HR_MIN_KEY = "hr_min"
        const val SPEED_MAX_KEY = "speed_max"
        const val SPEED_MIN_KEY = "speed_min"

        const val HR_MAX_DEFAULT = 160
        const val HR_MIN_DEFAULT = 80
        const val SPEED_MAX_DEFAULT = 50
        const val SPEED_MIN_DEFAULT = 25

        const val FAN_SPEED_MAX = 100
        const val FAN_SPEED_MIN = 5

        const val HR_MAX = 220
        const val HR_MIN = 60
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMinMaxBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val boundaryName =
            intent.getStringExtra(ConnectActivity.BOUNDARY) ?: getString(R.string.fan_hr_max)
        binding.heading.text = boundaryName

        sharedPref = getPreferences(Context.MODE_PRIVATE)
        val hrMaxBound = sharedPref.getInt(HR_MAX_KEY, HR_MAX_DEFAULT)
        val hrMinBound = sharedPref.getInt(HR_MIN_KEY, HR_MIN_DEFAULT)
        val speedMaxBound = sharedPref.getInt(SPEED_MAX_KEY, SPEED_MAX_DEFAULT)
        val speedMinBound = sharedPref.getInt(SPEED_MIN_KEY, SPEED_MIN_DEFAULT)

        val speedValues = when (boundaryName) {
            getString(R.string.fan_hr_max) -> createArrayList(speedMinBound, FAN_SPEED_MAX)
            else -> createArrayList(FAN_SPEED_MIN, speedMaxBound)
        }
        val speedIndex = when (boundaryName) {
            getString(R.string.fan_hr_max) -> speedValues.indexOf(speedMaxBound)
            else -> speedValues.indexOf(speedMinBound)
        }
        binding.speedSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, speedValues)
        binding.speedSpinner.setSelection(speedIndex)
        binding.speedSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                saveSpeedBoundary(boundaryName, speedValues[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val hrValues = when (boundaryName) {
            getString(R.string.fan_hr_max) -> createArrayList(hrMinBound, HR_MAX)
            else -> createArrayList(HR_MIN, hrMaxBound)
        }
        val hrIndex = when (boundaryName) {
            getString(R.string.fan_hr_max) -> hrValues.indexOf(hrMaxBound)
            else -> hrValues.indexOf(hrMinBound)
        }
        binding.hrSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, hrValues)
        binding.hrSpinner.setSelection(hrIndex)
        binding.hrSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                saveHrBoundary(boundaryName, hrValues[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun createArrayList(start: Int, end: Int) = (start..end step 5).toList().reversed()

    private fun saveHrBoundary(boundaryName: String, value: Int) {
        val key = when (boundaryName) {
            getString(R.string.fan_hr_max) -> HR_MAX_KEY
            else -> HR_MIN_KEY
        }
        sharedPref.edit(commit = true) { putInt(key, value) }
        Log.i(TAG, "Saving $key $value")
    }

    private fun saveSpeedBoundary(boundaryName: String, value: Int) {
        val key = when (boundaryName) {
            getString(R.string.fan_hr_max) -> SPEED_MAX_KEY
            else -> SPEED_MIN_KEY
        }
        sharedPref.edit(commit = true) { putInt(key, value) }
        Log.i(TAG, "Saving $key $value")
    }
}