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
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import com.garan.wearwind.databinding.ActivityConnectBinding
import com.punchthrough.ble.ConnectionEventListener
import com.punchthrough.ble.ConnectionManager

const val TAG = "Bardy"

/**
 * Activity for controlling searching for the Headwind fan, and launching the fan control activity
 * on successfully locating and connecting to it.
 */
class ConnectActivity : FragmentActivity() {
    private var device: BluetoothDevice? = null
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

    private var connectionStatus = ConnectionStatus.DISCONNECTED
        set(value) {
            field = value
            runOnUiThread {
                binding.scanButton.text = when(value) {
                    ConnectionStatus.CONNECTED -> "Disconnect"
                    ConnectionStatus.SCANNING -> "Cancel scan"
                    ConnectionStatus.CONNECTING -> "Cancel connect"
                    ConnectionStatus.DISCONNECTED -> "Connect"
                }
            }
        }

    private val menuItemClickListener = MenuItem.OnMenuItemClickListener { item ->
        if (item.title == getString(R.string.license)) {
            Intent(this@ConnectActivity, AboutActivity::class.java).also {
                startActivity(it)
            }
        } else {
            Intent(this@ConnectActivity, MinMaxActivity::class.java).also {
                it.putExtra(BOUNDARY, item.title)
                startActivity(it)
            }
        }
        true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConnectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.scanButton.setOnClickListener {
            when (connectionStatus) {
                ConnectionStatus.DISCONNECTED -> startBleScan()
                ConnectionStatus.SCANNING -> {
                    stopBleScan()
                    connectionStatus = ConnectionStatus.DISCONNECTED
                }
                ConnectionStatus.CONNECTING, ConnectionStatus.CONNECTED -> {
                    device?.let {
                        ConnectionManager.teardownConnection(it)
                    }
                    connectionStatus = ConnectionStatus.DISCONNECTED
                }
            }
        }

        val launcher =
                registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                    result.entries.find { !it.value }?.let {
                        Log.i(TAG, "Permission not granted!")
                    }
                }

        launcher.launch(requiredPermissions)

        with(binding.bottomActionDrawer) {
            setOnMenuItemClickListener(menuItemClickListener)
            controller.peekDrawer()
        }
    }

    override fun onResume() {
        super.onResume()
        ConnectionManager.registerListener(connectionEventListener)
    }

    private fun startBleScan() {
        bleScanner.startScan(null, scanSettings, scanCallback)
        connectionStatus = ConnectionStatus.SCANNING
    }

    private fun stopBleScan() {
        bleScanner.stopScan(scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            with(result.device) {
                if (name?.contains("HEADWIND", true) == true) {
                    stopBleScan()
                    connectionStatus = ConnectionStatus.CONNECTING
                    ConnectionManager.connect(this, this@ConnectActivity)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            connectionStatus = ConnectionStatus.DISCONNECTED
        }
    }

    private val connectionEventListener by lazy {
        ConnectionEventListener().apply {
            onConnectionSetupComplete = { gatt ->
                if (connectionStatus != ConnectionStatus.CONNECTED) {
                    Intent(this@ConnectActivity, FanControlActivity::class.java).also {
                        it.putExtra(BluetoothDevice.EXTRA_DEVICE, gatt.device)
                        it.putExtra(USE_HEART_RATE, binding.hrSwitch.isChecked)
                        startActivity(it)
                    }
                    connectionStatus = ConnectionStatus.CONNECTED
                }
            }
            onDisconnect = {
                connectionStatus = ConnectionStatus.DISCONNECTED
            }
        }
    }

    companion object {
        const val USE_HEART_RATE = "use_heart_rate"
        const val BOUNDARY = "boundary"
    }
}

enum class ConnectionStatus {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED
}