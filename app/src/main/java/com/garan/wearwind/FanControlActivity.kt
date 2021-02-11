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

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.wear.ambient.AmbientModeSupport
import com.garan.wearwind.databinding.ActivityFanControlBinding
import com.punchthrough.ble.ConnectionEventListener
import com.punchthrough.ble.ConnectionManager
import java.util.UUID

const val FAST_SWIPE_UP = -5000
const val SLOW_SWIPE_UP = -1000
const val FAST_SWIPE_DOWN = 5000
const val SLOW_SWIPE_DOWN = 1000

// HR fan control is very basic: When HR is at or below the minimum threshold, the fan speed will be
// set to FAN_SPEED_MIN. When the HR is at or above the maximum threshold, the fan speed will be set
// to FAN_SPEED_MAX. Between HR_MIN_THRESHOLD and HR_MAX_THRESHOLD, the fan speed will be
// interpolated between FAN_SPEED_MIN and FAN_SPEED_MAX.
const val HR_MIN_THRESHOLD = 80
const val HR_MAX_THRESHOLD = 160

const val FAN_SPEED_MIN = 20
const val FAN_SPEED_MAX = 50

/**
 * Activity for fan speed control, either manually or linked to heart rate.
 */
class FanControlActivity : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider {
    private lateinit var device: BluetoothDevice
    private lateinit var binding: ActivityFanControlBinding
    private val metric = MutableLiveData(0)
    private var useHeartRate: Boolean = false

    private val sensorManager by lazy { getSystemService(SENSOR_SERVICE) as SensorManager }
    private val sensor by lazy { sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) }
    private var listener: SensorEventListener? = null

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

        fanCharacteristic = characteristics.find { it.uuid == CHARACTERISTIC_UUID }

        // Power on the Headwind. First notifications require enabling.
        fanCharacteristic?.let {
            ConnectionManager.enableNotifications(device, it)
            ConnectionManager.writeCharacteristic(device, it, POWER_ON)
        }

        metric.observe(this, {
            binding.metric.text = "$it"
        })

        if (useHeartRate) {
            initializeHeartRateSensor()
            setHeartRateColors()
        } else {
            // Swipe control only available in manual mode.
            initializeSwipe()
        }
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

    private fun setHeartRateColors() {
        binding.metric.setTextColor(Color.RED)
    }

    private fun initializeHeartRateSensor() {
        listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val hr = event.values.last().toInt()
                if (hr > 0) {
                    val speed = getFanSpeedForHeartRate(hr)
                    setSpeed(speed)
                    metric.postValue(hr)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        listener?.let {
            sensorManager.registerListener(listener, sensor, 5_000_000)
        }
    }

    /**
     * Allows fan speed to be adjusted by swiping up or down on the screen.
     */
    private fun initializeSwipe() {
        val gesture = GestureDetector(this,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean {
                    return true
                }

                override fun onFling(
                    e1: MotionEvent, e2: MotionEvent, velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    when {
                        velocityY < FAST_SWIPE_UP -> incrementSpeed(10)
                        velocityY < SLOW_SWIPE_UP -> incrementSpeed(1)
                        velocityY > FAST_SWIPE_DOWN -> decrementSpeed(1)
                        velocityY > SLOW_SWIPE_DOWN -> decrementSpeed(10)
                    }
                    return true
                }
            })

        binding.metric.setOnTouchListener { v, event -> gesture.onTouchEvent(event) }
    }

    private fun getFanSpeedForHeartRate(heartRate: Int): Int {
        return when {
            heartRate < HR_MIN_THRESHOLD -> FAN_SPEED_MIN
            heartRate > HR_MAX_THRESHOLD -> FAN_SPEED_MAX
            else -> {
                val hrPc =
                    (heartRate - HR_MIN_THRESHOLD).toFloat() / (HR_MAX_THRESHOLD - HR_MIN_THRESHOLD)
                val fanRange = FAN_SPEED_MAX - FAN_SPEED_MIN
                (FAN_SPEED_MIN + hrPc * fanRange).toInt()
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
            metric.value = value
            Log.i(TAG, "Speed: $metric")
            ConnectionManager.writeCharacteristic(
                device,
                characteristic,
                fanValue(value)
            )
        }
    }

    private fun incrementSpeed(delta: Int) {
        fanCharacteristic?.let { characteristic ->
            metric.value?.let {
                if (it != -1 && it + delta <= 100) {
                    metric.value = it + delta
                    Log.i(TAG, "Speed: $metric")
                    ConnectionManager.writeCharacteristic(
                        device,
                        characteristic,
                        fanValue(it + delta)
                    )
                }
            }
        }
    }

    private fun decrementSpeed(delta: Int) {
        fanCharacteristic?.let { characteristic ->
            metric.value?.let {
                if (it != -1 && it - delta >= 0) {
                    metric.value = it - delta
                    Log.i(TAG, "Speed: $metric")
                    ConnectionManager.writeCharacteristic(
                        device,
                        characteristic,
                        fanValue(it - delta)
                    )
                }
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
                    // Update the displayed fan speed, based up value from fan notifications.
                    if (isFanSpeedResponse(characteristic.value) && !useHeartRate) {
                        metric.postValue(characteristic.value[2].toInt())
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
