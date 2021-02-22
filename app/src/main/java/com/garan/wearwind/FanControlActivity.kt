package com.garan.wearwind

import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.wear.ambient.AmbientModeSupport
import com.garan.wearwind.databinding.ActivityFanControlBinding

const val FAST_SWIPE_UP = -5000
const val SLOW_SWIPE_UP = -1000
const val FAST_SWIPE_DOWN = 5000
const val SLOW_SWIPE_DOWN = 1000


/**
 * Activity for fan speed control, either manually or linked to heart rate.
 */
class FanControlActivity : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider {
    private lateinit var binding: ActivityFanControlBinding
    private lateinit var fanService: FanControlService
    private lateinit var fragment: FanFragment
    private var bound: Boolean = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as FanControlService.LocalBinder
            fanService = binder.getService()
            fragment.setModel(fanService.metrics)
            bound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AmbientModeSupport.attach(this)

        binding = ActivityFanControlBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Headwind device, as connected to in {@code ConnectActivity}
        val device: BluetoothDevice = intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                ?: error("Missing BluetoothDevice from ConnectActivity!")
        val useHeartRate = intent.getBooleanExtra(ConnectActivity.USE_HEART_RATE, false)

        fragment = when (useHeartRate) {
            true -> HrFragment()
            else -> ManualFragment()
        }

        Intent(this, FanControlService::class.java).also { intent ->
            intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device)
            intent.putExtra(ConnectActivity.USE_HEART_RATE, useHeartRate)
            startService(intent)
        }

        supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainerView.id, fragment)
                .commit()
    }

    override fun onStart() {
        super.onStart()
        Log.i(TAG, "onStart")
        Intent(this, FanControlService::class.java).also { intent ->
            //startService(intent)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        Log.i(TAG, "onStop")

        unbindService(connection)
    }

    override fun onDestroy() {
        fanService.stopService()
        super.onDestroy()
    }

    private val callback = object : AmbientModeSupport.AmbientCallback() {
        override fun onExitAmbient() {
            super.onExitAmbient()
            Log.i(TAG, "Exit ambient")
        }

        override fun onEnterAmbient(ambientDetails: Bundle?) {
            super.onEnterAmbient(ambientDetails)
            Log.i(TAG, "Enter ambient")
        }
    }

    override fun getAmbientCallback() = callback
}

abstract class FanFragment : Fragment() {
    abstract fun setModel(model: FanMetrics)
}