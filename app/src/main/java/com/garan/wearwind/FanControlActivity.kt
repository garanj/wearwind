package com.garan.wearwind

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.wear.ambient.AmbientModeSupport
import com.garan.wearwind.databinding.ActivityFanControlBinding
import com.punchthrough.ble.ConnectionEventListener
import com.punchthrough.ble.ConnectionManager
import java.util.UUID

const val FAST_SWIPE_UP = -5000
const val SLOW_SWIPE_UP = -1000
const val FAST_SWIPE_DOWN = 5000
const val SLOW_SWIPE_DOWN = 1000


/**
 * Activity for fan speed control, either manually or linked to heart rate.
 */
class FanControlActivity : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider {
    private lateinit var device: BluetoothDevice
    private lateinit var binding: ActivityFanControlBinding

    private var useHeartRate: Boolean = false

    private val sensorManager by lazy { getSystemService(SENSOR_SERVICE) as SensorManager }
    private val sensor by lazy { sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) }
    private var listener: SensorEventListener? = null

    private val model: FanControlViewModel by viewModels()

    private var hrMax: Int = 0
    private var hrMin: Int = 0
    private var speedMax: Int = 0
    private var speedMin: Int = 0

    private val characteristics by lazy {
        ConnectionManager.servicesOnDevice(device)?.flatMap { service ->
            service.characteristics ?: listOf()
        } ?: listOf()
    }

    private var fanCharacteristic: BluetoothGattCharacteristic? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ConnectionManager.registerListener(connectionEventListener)
        super.onCreate(savedInstanceState)

        AmbientModeSupport.attach(this)

        binding = ActivityFanControlBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Headwind device, as connected to in {@code ConnectActivity}
        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                ?: error("Missing BluetoothDevice from ConnectActivity!")

        useHeartRate = intent.getBooleanExtra(ConnectActivity.USE_HEART_RATE, false)

        val fragment = when (useHeartRate) {
            true -> HrFragment()
            else -> ManualFragment()
        }
        supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainerView.id, fragment)
                .commit()

        loadHrPrefs()

        fanCharacteristic = characteristics.find { it.uuid == CHARACTERISTIC_UUID }

        // Power on the Headwind. First notifications require enabling.
        fanCharacteristic?.let {
            ConnectionManager.enableNotifications(device, it)
            ConnectionManager.writeCharacteristic(device, it, POWER_ON)
        }

        if (useHeartRate) {
            initializeHeartRateSensor()
        }

        model.speedToDevice.observe(this, Observer {
            setSpeed(it)
        })
    }

    override fun onDestroy() {
        if (useHeartRate) {
            listener?.let {
                sensorManager.unregisterListener(it)
            }
        }
        fanCharacteristic?.let {
            ConnectionManager.writeCharacteristic(device, it, POWER_OFF)
        }
        ConnectionManager.unregisterListener(connectionEventListener)
        ConnectionManager.teardownConnection(device)
        super.onDestroy()
    }

    private fun loadHrPrefs() {
        val sharedPref = getSharedPreferences(MinMaxActivity.HR_PREFERENCES_KEY, Context.MODE_PRIVATE)
        hrMax = sharedPref.getInt(MinMaxActivity.HR_MAX_KEY, MinMaxActivity.HR_MAX_DEFAULT)
        hrMin = sharedPref.getInt(MinMaxActivity.HR_MIN_KEY, MinMaxActivity.HR_MIN_DEFAULT)
        speedMax = sharedPref.getInt(MinMaxActivity.SPEED_MAX_KEY, MinMaxActivity.SPEED_MAX_DEFAULT)
        speedMin = sharedPref.getInt(MinMaxActivity.SPEED_MIN_KEY, MinMaxActivity.SPEED_MIN_DEFAULT)
    }

    private fun initializeHeartRateSensor() {
        listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val hr = event.values.last().toInt()
                if (hr > 0) {
                    val speed = getFanSpeedForHeartRate(hr)
                    model.speedToDevice.postValue(speed)
                    model.hr.postValue(hr)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        listener?.let {
            sensorManager.registerListener(listener, sensor, 5_000_000)
        }
    }

    private fun getFanSpeedForHeartRate(heartRate: Int): Int {
        return when {
            heartRate < hrMin -> speedMin
            heartRate > hrMax -> speedMax
            else -> {
                val hrPc =
                        (heartRate - hrMin).toFloat() / (hrMax - hrMin)
                val fanRange = speedMax - speedMin
                (speedMin + hrPc * fanRange).toInt()
            }
        }
    }

    /**
     * Packages up the desired fan speed (0-100) in a byte array formatted as required for setting
     * the GATT characteristic.
     */
    private fun fanValue(value: Int) = byteArrayOf(2, value.toByte())

    private fun setSpeed(value: Int) {
        fanCharacteristic?.let { characteristic ->
            if (value != model.speedFromDevice.value) {
                ConnectionManager.writeCharacteristic(
                        device,
                        characteristic,
                        fanValue(value)
                )
            }
        }
    }

    private val connectionEventListener by lazy {
        ConnectionEventListener().apply {
            onDisconnect = {
                //
            }

            onCharacteristicChanged = { _, characteristic ->
                if (characteristic.uuid == CHARACTERISTIC_UUID) {
                    if (isFanSpeedResponse(characteristic.value)) {
                        model.speedFromDevice.postValue(characteristic.value[2].toInt())
                    }
                }
            }
        }
    }

    /**
     * Determines if the Characteristic value represents the current fan speed: The Characteristic
     * value can represent other things.
     */
    private fun isFanSpeedResponse(byteArray: ByteArray): Boolean {
        with(byteArray) {
            return size == 4 && get(0) == 0xFD.b
                    && get(1) == 0x01.b && get(3) == 0x04.b
        }
    }

    private val Int.b
        get() =
            this.toByte()

    companion object {
        private val CHARACTERISTIC_UUID = UUID.fromString("a026e038-0a7d-4ab3-97fa-f1500f9feb8b")
        private val POWER_ON = byteArrayOf(4, 4, 1)
        private val POWER_OFF = byteArrayOf(2, 0)
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
