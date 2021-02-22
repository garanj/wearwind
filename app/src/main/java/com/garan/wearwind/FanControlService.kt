package com.garan.wearwind

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.punchthrough.ble.ConnectionEventListener
import com.punchthrough.ble.ConnectionManager
import java.util.UUID


class FanControlService : LifecycleService() {
    private val binder = LocalBinder()
    private lateinit var device: BluetoothDevice
    private var useHeartRate = false
    private var fanCharacteristic: BluetoothGattCharacteristic? = null
    private var poweredOn = false

    private var hrMax: Int = 0
    private var hrMin: Int = 0
    private var speedMax: Int = 0
    private var speedMin: Int = 0

    private val characteristics by lazy {
        ConnectionManager.servicesOnDevice(device)?.flatMap { service ->
            service.characteristics ?: listOf()
        } ?: listOf()
    }

    private val sensorManager by lazy { getSystemService(SENSOR_SERVICE) as SensorManager }
    private val sensor by lazy { sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) }
    private var listener: SensorEventListener? = null

    val metrics = FanMetrics()

    override fun onCreate() {
        super.onCreate()
        ConnectionManager.registerListener(connectionEventListener)
        loadHrPrefs()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (!poweredOn) {
            device = intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    ?: error("Missing BluetoothDevice from ConnectActivity!")
            useHeartRate = intent.getBooleanExtra(ConnectActivity.USE_HEART_RATE, false)

            fanCharacteristic = characteristics.find { it.uuid == CHARACTERISTIC_UUID }

            fanCharacteristic?.let {
                ConnectionManager.enableNotifications(device, it)
                ConnectionManager.writeCharacteristic(device, it, POWER_ON)
            }

            if (useHeartRate) {
                initializeHeartRateSensor()
            }

            metrics.speedToDevice.observe(this, {
                setSpeed(it)
            })

            poweredOn = true
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        stopForeground(true)
        return binder
    }

    override fun onRebind(intent: Intent?) {
        stopForeground(true)
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        enableForegroundService()
        return true
    }

    private fun enableForegroundService() {
        createNotificationChannel()
        val notificationIntent = Intent(this, FanControlActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
                this,
                0, notificationIntent, 0
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(TITLE)
                .setContentText(TEXT)
                .setSmallIcon(R.drawable.fan_white_24)
                .setContentIntent(pendingIntent)
                .build()
        startForeground(1, notification)
    }

    fun stopService() {
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

        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(NotificationManager::class.java)
            manager!!.createNotificationChannel(serviceChannel)
        }
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
                val heartRate = event.values.last().toInt()
                if (heartRate > 0) {
                    val speed = getFanSpeedForHeartRate(heartRate)
                    metrics.speedToDevice.postValue(speed)
                    metrics.hr.postValue(heartRate)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        listener?.let {
            sensorManager.registerListener(listener, sensor, 5_000_000)
        }
    }

    /**
     * Packages up the desired fan speed (0-100) in a byte array formatted as required for setting
     * the GATT characteristic.
     */
    private fun fanValue(value: Int) = byteArrayOf(2, value.toByte())

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

    private fun setSpeed(value: Int) {
        fanCharacteristic?.let { characteristic ->
            if (value != metrics.speedFromDevice.value) {
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
                // TODO: Something if disconnect
            }

            onCharacteristicChanged = { _, characteristic ->
                if (characteristic.uuid == CHARACTERISTIC_UUID) {
                    if (isFanSpeedResponse(characteristic.value)) {
                        metrics.speedFromDevice.postValue(characteristic.value[2].toInt())
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

    inner class LocalBinder : Binder() {
        fun getService(): FanControlService = this@FanControlService
    }

    companion object {
        const val CHANNEL_ID = "com.garan.wearwind.FanControlService"
        const val TITLE = "Wearwind"
        const val TEXT = "Fan service running"
        private val CHARACTERISTIC_UUID = UUID.fromString("a026e038-0a7d-4ab3-97fa-f1500f9feb8b")
        private val POWER_ON = byteArrayOf(4, 4, 1)
        private val POWER_OFF = byteArrayOf(2, 0)
    }
}

data class FanMetrics(val speedToDevice: MutableLiveData<Int> = MutableLiveData(0),
                      val speedFromDevice: MutableLiveData<Int> = MutableLiveData(0),
                      val hr: MutableLiveData<Int> = MutableLiveData(0))