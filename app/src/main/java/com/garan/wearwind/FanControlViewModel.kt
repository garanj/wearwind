package com.garan.wearwind

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class FanControlViewModel : ViewModel() {
    val speedToDevice = MutableLiveData(0)
    val speedFromDevice = MutableLiveData(0)
    val hr = MutableLiveData(0)
}