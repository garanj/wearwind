package com.garan.wearwind.screens.settingsdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garan.wearwind.SettingLevel
import com.garan.wearwind.SettingType
import com.garan.wearwind.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class SettingsDetailViewModel @Inject constructor(
    private val preferences: SettingsRepository
) : ViewModel() {
    fun getHrMinMax() = preferences.getHrMinMax()

    fun getSpeedMinMax() = preferences.getSpeedMinMax()

    fun setThreshold(type: SettingType, level: SettingLevel, value: Float) {
        viewModelScope.launch {
            preferences.setThreshold(type, level, value)
        }
    }
}
