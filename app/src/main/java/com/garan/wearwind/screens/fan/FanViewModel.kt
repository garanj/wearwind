package com.garan.wearwind.screens.fan

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garan.wearwind.FanControlService
import com.garan.wearwind.SettingsRepository
import com.garan.wearwind.TAG
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class FanViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val preferences: SettingsRepository
) : ViewModel() {
    private var fanControlService: FanControlService? = null

    val serviceState: MutableState<ServiceState> = mutableStateOf(ServiceState.Disconnected)
    var bound = mutableStateOf(false)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as FanControlService.LocalBinder
            binder.getService().let {
                fanControlService = it
                serviceState.value = ServiceState.Connected(
                    fanConnectionStatus = it.fanConnectionStatus,
                    hr = it.hr,
                    speed = it.speedToDevice
                )
            }
            Log.i(TAG, "onServiceConnected")
            bound.value = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bound.value = false
            fanControlService = null
            serviceState.value = ServiceState.Disconnected
            Log.i(TAG, "onServiceDisconnected")
        }
    }

    init {
        if (!bound.value) {
            createService()
        }
    }

    val hrEnabled = preferences.hrEnabled

    val toastCount = preferences.toastCount

    fun connectToFan() = fanControlService?.connectOrDisconnect()

    fun setFanSpeed(speed: Int) = fanControlService?.changeSpeed(speed)

    fun disconnectFromFan() = fanControlService?.connectOrDisconnect()

    fun setHrEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            preferences.setHrEnabled(isEnabled)
        }
    }

    fun incrementToastCount() {
        viewModelScope.launch {
            preferences.incrementShowToast()
        }
    }

    private fun createService() {
        Intent(applicationContext, FanControlService::class.java).also { intent ->
            applicationContext.startService(intent)
            applicationContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (bound.value) {
            applicationContext.unbindService(connection)
        }
    }
}

sealed class ServiceState {
    object Disconnected : ServiceState()
    data class Connected(
        val fanConnectionStatus: State<FanControlService.FanConnectionStatus>,
        val hr: State<Int>,
        val speed: StateFlow<Int>
    ) : ServiceState()
}