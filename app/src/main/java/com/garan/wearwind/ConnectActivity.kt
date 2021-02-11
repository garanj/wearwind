/*
 * Copyright 2019 Punch Through Design LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.garan.wearwind

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import com.garan.wearwind.databinding.ActivityConnectBinding
import com.punchthrough.ble.ConnectionEventListener
import com.punchthrough.ble.ConnectionManager

const val TAG = "WearWind"

/**
 * Activity for controlling searching for the Headwind fan, and launching the fan control activity
 * on successfully locating and connecting to it.
 */
class ConnectActivity : FragmentActivity() {
    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BODY_SENSORS
    )
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private lateinit var binding: ActivityConnectBinding

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private var isConnected = false
        set(value) {
            field = value
            runOnUiThread { binding.scanButton.text = if (value) "Disconnect" else "Connect" }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConnectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.scanButton.setOnClickListener { if (isConnected) stopBleScan() else startBleScan() }

        val launcher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                result.entries.find { !it.value }?.let {
                    Log.i(TAG, "Permission not granted!")
                }
            }

        launcher.launch(requiredPermissions)
    }

    override fun onResume() {
        super.onResume()
        ConnectionManager.registerListener(connectionEventListener)
    }

    private fun startBleScan() {
        bleScanner.startScan(null, scanSettings, scanCallback)
        isConnected = true
    }

    private fun stopBleScan() {
        bleScanner.stopScan(scanCallback)
        isConnected = false
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            with(result.device) {
                if (name?.contains("HEADWIND", true) == true) {
                    stopBleScan()
                    ConnectionManager.connect(this, this@ConnectActivity)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            //
        }
    }

    private val connectionEventListener by lazy {
        ConnectionEventListener().apply {
            onConnectionSetupComplete = { gatt ->
                isConnected = true
                Intent(this@ConnectActivity, FanControlActivity::class.java).also {
                    it.putExtra(BluetoothDevice.EXTRA_DEVICE, gatt.device)
                    it.putExtra(USE_HEART_RATE, binding.hrSwitch.isChecked)
                    startActivity(it)
                }
            }
            onDisconnect = {
                isConnected = false
            }
        }
    }

    companion object {
        const val USE_HEART_RATE = "use_heart_rate"
    }
}
