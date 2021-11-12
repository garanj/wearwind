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
        HR_MIN_MAX_KEY to MinMaxHolder(0, 220, 80, 160, 20),
        SPEED_MIN_MAX_KEY to MinMaxHolder(0, 100, 25, 60, 10)
    )

    private fun setMinMax(key: String, minMax: MinMaxHolder) = prefs.edit(commit = true) {
        val json = Gson().toJson(minMax)
        putString(key, json)
    }

    fun incrementSetting(type: SettingType, level: SettingLevel) {
        when (type) {
            SettingType.HR -> {
                val settings = getHrMinMax()
                when (level) {
                    SettingLevel.MIN -> settings.incrementMin()
                    SettingLevel.MAX -> settings.incrementMax()
                }
                setHrMinMax(settings)
            }
            SettingType.SPEED -> {
                val settings = getSpeedMinMax()
                when (level) {
                    SettingLevel.MIN -> settings.incrementMin()
                    SettingLevel.MAX -> settings.incrementMax()
                }
                setSpeedMinMax(settings)
            }
        }
    }

    fun decrementSetting(type: SettingType, level: SettingLevel) {
        when (type) {
            SettingType.HR -> {
                val settings = getHrMinMax()
                when (level) {
                    SettingLevel.MIN -> settings.decrementMin()
                    SettingLevel.MAX -> settings.decrementMax()
                }
                setHrMinMax(settings)
            }
            SettingType.SPEED -> {
                val settings = getSpeedMinMax()
                when (level) {
                    SettingLevel.MIN -> settings.decrementMin()
                    SettingLevel.MAX -> settings.decrementMax()
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
    private val absMin: Int = 0,
    private val absMax: Int = 100,
    initialMin: Int = 0,
    initialMax: Int = 100,
    private val minInterval: Int = 20
) {
    private var _currentMin = initialMin
    private var _currentMax = initialMax

    val currentMin: Int
        get() = _currentMin

    val currentMax: Int
        get() = _currentMax

    fun incrementMin(step: Int = 5) {
        val cmp = _currentMin + step + minInterval
        if (cmp <= _currentMax && cmp <= absMax) {
            _currentMin += step
        }
    }

    fun decrementMax(step: Int = 5) {
        val cmp = _currentMax - step - minInterval
        if (cmp >= _currentMin && cmp >= absMin) {
            _currentMax -= step
        }
    }

    fun incrementMax(step: Int = 5) {
        if (_currentMax + step <= absMax) {
            _currentMax += step
        }
    }

    fun decrementMin(step: Int = 5) {
        if (_currentMin - step >= absMin) {
            _currentMin -= step
        }
    }
}