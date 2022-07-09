package com.garan.wearwind

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val DEFAULT_MIN_MAX = mapOf(
        HR_MIN_MAX_KEY to MinMaxHolder(0.0f, 220.0f, 80.0f, 160.0f, 5),
        SPEED_MIN_MAX_KEY to MinMaxHolder(0.0f, 100.0f, 25.0f, 60.0f, 5)
    )

    suspend fun setThreshold(type: SettingType, level: SettingLevel, value: Float) {
        when (type) {
            SettingType.HR -> {
                val settings = getHrMinMax().first()
                when (level) {
                    SettingLevel.MIN -> settings.currentMin = value
                    SettingLevel.MAX -> settings.currentMax = value
                }
                setHrMinMax(settings)
            }
            SettingType.SPEED -> {
                val settings = getSpeedMinMax().first()
                when (level) {
                    SettingLevel.MIN -> settings.currentMin = value
                    SettingLevel.MAX -> settings.currentMax = value
                }
                setSpeedMinMax(settings)
            }
        }
    }

    fun getHrMinMax() = getMinMax(HR_MIN_MAX_KEY)
    fun getSpeedMinMax() = getMinMax(SPEED_MIN_MAX_KEY)

    private fun getMinMax(key: String): Flow<MinMaxHolder> = dataStore.data.map { prefs ->
        val json = prefs[stringPreferencesKey(key)]
        if (json == null) {
            DEFAULT_MIN_MAX[key]!!
        } else {
            Gson().fromJson(json, MinMaxHolder::class.java)
        }
    }

    private suspend fun setHrMinMax(hrMinMax: MinMaxHolder) = setMinMax(HR_MIN_MAX_KEY, hrMinMax)
    private suspend fun setSpeedMinMax(hrMinMax: MinMaxHolder) =
        setMinMax(SPEED_MIN_MAX_KEY, hrMinMax)

    private suspend fun setMinMax(key: String, minMax: MinMaxHolder) {
        val json = Gson().toJson(minMax)
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey(key)] = json
        }
    }

    val hrEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[booleanPreferencesKey(HR_KEY)] ?: false
    }

    suspend fun setHrEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[booleanPreferencesKey(HR_KEY)] = enabled
        }
    }

    val toastCount: Flow<Int> = dataStore.data.map { prefs ->
        prefs[intPreferencesKey(TOAST_COUNT_KEY)] ?: 0
    }

    suspend fun incrementShowToast() {
        dataStore.edit { prefs ->
            prefs[intPreferencesKey(TOAST_COUNT_KEY)] =
                (prefs[intPreferencesKey(TOAST_COUNT_KEY)] ?: 0) + 1
        }
    }

    companion object {
        const val PREFERENCES_FILENAME = "wearwind_prefs"
        private const val HR_MIN_MAX_KEY = "hr_min_max_key"
        private const val SPEED_MIN_MAX_KEY = "speed_min_max_key"
        private const val HR_KEY = "hr"
        private const val TOAST_COUNT_KEY = "toast_count"
        const val TOAST_MAX = 2
    }
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
        set(value) {
            _currentMin = value
        }

    var currentMax: Float
        get() = _currentMax
        set(value) {
            _currentMax = value
        }

    fun minRange(): ClosedFloatingPointRange<Float> {
        return absMin..(_currentMax - minInterval)
    }

    fun maxRange(): ClosedFloatingPointRange<Float> {
        return (_currentMin + minInterval)..absMax
    }
}
