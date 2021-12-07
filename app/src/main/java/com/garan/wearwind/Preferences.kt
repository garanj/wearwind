package com.garan.wearwind

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Preferences @Inject constructor(@ApplicationContext context: Context) {
    private val HR_MIN_MAX_KEY = "hr_min_max_key"
    private val SPEED_MIN_MAX_KEY = "speed_min_max_key"
    private val HR_KEY = "hr"
    private val PREFERENCES_KEY = "preferences"

    private val prefs = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE)

    private val DEFAULT_MIN_MAX = mapOf(
        HR_MIN_MAX_KEY to MinMaxHolder(0.0f, 220.0f, 80.0f, 160.0f, 5),
        SPEED_MIN_MAX_KEY to MinMaxHolder(0.0f, 100.0f, 25.0f, 60.0f, 5)
    )

    private fun setMinMax(key: String, minMax: MinMaxHolder) = prefs.edit(commit = true) {
        val json = Gson().toJson(minMax)
        putString(key, json)
    }

    fun setThreshold(type: SettingType, level: SettingLevel, value: Float) {
        when (type) {
            SettingType.HR -> {
                val settings = getHrMinMax()
                when (level) {
                    SettingLevel.MIN -> settings.currentMin = value
                    SettingLevel.MAX -> settings.currentMax = value
                }
                setHrMinMax(settings)
            }
            SettingType.SPEED -> {
                val settings = getSpeedMinMax()
                when (level) {
                    SettingLevel.MIN -> settings.currentMin = value
                    SettingLevel.MAX -> settings.currentMax = value
                }
                setSpeedMinMax(settings)
            }
        }
    }

    private fun setHrMinMax(hrMinMax: MinMaxHolder) = setMinMax(HR_MIN_MAX_KEY, hrMinMax)
    private fun setSpeedMinMax(hrMinMax: MinMaxHolder) = setMinMax(SPEED_MIN_MAX_KEY, hrMinMax)

    private fun getMinMax(key: String): MinMaxHolder {
        val json = prefs.getString(key, null)
        json?.let {
            return Gson().fromJson(json, MinMaxHolder::class.java)
        }
        return DEFAULT_MIN_MAX[key]!!
    }

    fun getHrMinMax() = getMinMax(HR_MIN_MAX_KEY)
    fun getSpeedMinMax() = getMinMax(SPEED_MIN_MAX_KEY)

    fun getHrEnabled() = prefs.getBoolean(HR_KEY, false)
    fun setHrEnabled(isEnabled: Boolean) =
        prefs.edit(commit = true) { putBoolean(HR_KEY, isEnabled) }
}

class MinMaxHolder(
    private val absMin: Float = 0.0f,
    private val absMax: Float = 100.0f,
    initialMin: Float = 0.0f,
    initialMax: Float = 100.0f,
    val step: Int = 5,
    private val minInterval: Int = 20
) {
    private var _currentMin = initialMin
    private var _currentMax = initialMax

    var currentMin: Float
        get() = _currentMin
        set(value) { _currentMin = value }

    var currentMax: Float
        get() = _currentMax
        set(value) { _currentMax = value }

    fun minRange(): ClosedFloatingPointRange<Float> {
        return absMin..(_currentMax - minInterval)
    }

    fun maxRange(): ClosedFloatingPointRange<Float> {
        return (_currentMin + minInterval)..absMax
    }
}
