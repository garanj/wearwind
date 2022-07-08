package com.garan.wearwind

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.punchthrough.ble.ConnectionEventListener
import com.punchthrough.ble.ConnectionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class FanControlService : LifecycleService() {
    enum class FanConnectionStatus {
        DISCONNECTED,
        SCANNING,
        CONNECTING,
        CONNECTED
    }

    @Inject
    lateinit var preferences: SettingsRepository

    private val binder = LocalBinder()
    private var device: BluetoothDevice? = null
    private var fanCharacteristic: BluetoothGattCharacteristic? = null

    private val characteristics by lazy {
        device?.let {
            ConnectionManager.servicesOnDevice(it)?.flatMap { service ->
                service.characteristics ?: listOf()
            }
        } ?: listOf()
    }

    private var started = false

    private val sensorManager by lazy { getSystemService(SENSOR_SERVICE) as SensorManager }
    private val sensor by lazy { sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) }
    private var listener: SensorEventListener? = null

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            with(result.device) {
                if (name?.contains(BLE_DEVICE_NAME, true) == true) {
                    stopBleScan()
                    // TODO pause 500 ms?
                    this@FanControlService.device = this
                    fanConnectionStatus.value = FanConnectionStatus.CONNECTING
                    ConnectionManager.connect(this, this@FanControlService)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            fanConnectionStatus.value = FanConnectionStatus.DISCONNECTED
        }
    }

    private val connectionEventListener by lazy {
        ConnectionEventListener().apply {
            onConnectionSetupComplete = { gatt ->
                if (fanConnectionStatus.value != FanConnectionStatus.CONNECTED) {
                    fanConnectionStatus.value = FanConnectionStatus.CONNECTED

                    device = gatt.device
                    fanCharacteristic = characteristics.find { it.uuid == CHARACTERISTIC_UUID }

                    fanCharacteristic?.let {
                        ConnectionManager.enableNotifications(gatt.device, it)
                        ConnectionManager.writeCharacteristic(gatt.device, it, POWER_ON)

                        setSpeed(INITIAL_SPEED)
                    }

                    if (hrEnabled.value) {
                        initializeHeartRateSensor()
                    }
                }
            }
            onDisconnect = {
                teardownHeartRateSensor()
                fanConnectionStatus.value = FanConnectionStatus.DISCONNECTED
            }
            onCharacteristicChanged = { _, characteristic ->
                if (characteristic.uuid == CHARACTERISTIC_UUID) {
                    if (isFanSpeedResponse(characteristic.value)) {
                        speedFromDevice.value = characteristic.value[2].toInt()
                    }
                }
            }
        }
    }

    // Current status
    val fanConnectionStatus = mutableStateOf(FanConnectionStatus.DISCONNECTED)
    val speedToDevice: MutableStateFlow<Int> = MutableStateFlow(0)
    private val speedFromDevice: MutableState<Int> = mutableStateOf(0)
    val hr: MutableState<Int> = mutableStateOf(0)

    // Current settings
    private var speedSettings = MinMaxHolder()
    private var hrSettings = MinMaxHolder()
    private val hrEnabled: MutableState<Boolean> = mutableStateOf(false)


    override fun onCreate() {
        super.onCreate()
        ConnectionManager.registerListener(connectionEventListener)
        lifecycleScope.launch {
            preferences.hrEnabled.collect {
                hrEnabled.value = it
            }
        }
        lifecycleScope.launch {
            preferences.getSpeedMinMax().collect {
                speedSettings = it
            }
        }
        lifecycleScope.launch {
            preferences.getHrMinMax().collect {
                hrSettings = it
            }
        }
        lifecycleScope.launch {
            speedToDevice.collect {
                setSpeed(it)
            }
        }

        Log.i(TAG, "Service onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (!started) {
            enableForegroundService()

            started = true
        }
        Log.i(TAG, "service onStartCommand")
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        Log.i(TAG, "service onBind")
        return binder
    }

    override fun onRebind(intent: Intent?) {
        Log.i(TAG, "service onRebind")
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "service onUnbind")
        maybeStopService()
        return true
    }

    fun changeSpeed(speed: Int) {
        speedToDevice.value = speed
    }

    private fun maybeStopService() {
        if (fanConnectionStatus.value == FanConnectionStatus.DISCONNECTED) {
            Log.i(TAG, "Stopping service")
            ConnectionManager.unregisterListener(connectionEventListener)
            stopSelf()
        }
    }

    private fun enableForegroundService() {
        createNotificationChannel()
        startForeground(1, buildNotification())
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            NOTIFICATION_CHANNEL, "com.garan.wearwind.ONGOING",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager!!.createNotificationChannel(serviceChannel)
    }

    private fun buildNotification(): Notification {
        val notificationIntent = Intent(this, FanActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        // Build the notification.
        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(NOTIFICATION_TEXT)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val ongoingActivityStatus = Status.Builder()
            .addTemplate(STATUS_TEMPLATE)
            .addPart("speed", Status.TextPart("${speedToDevice.value}"))
            .build()
        val ongoingActivity =
            OngoingActivity.Builder(applicationContext, NOTIFICATION_ID, notificationBuilder)
                .setAnimatedIcon(R.drawable.ic_logo)
                .setStaticIcon(R.drawable.ic_logo)
                .setTouchIntent(pendingIntent)
                .setStatus(ongoingActivityStatus)
                .build()
        ongoingActivity.apply(applicationContext)

        return notificationBuilder.build()
    }

    fun connectOrDisconnect() {
        when (fanConnectionStatus.value) {
            FanConnectionStatus.DISCONNECTED -> {
                resetMetrics()
                bleScanner.startScan(null, scanSettings, scanCallback)
                fanConnectionStatus.value = FanConnectionStatus.SCANNING
            }
            FanConnectionStatus.CONNECTED -> {
                device?.let {
                    fanCharacteristic?.let { characteristic ->
                        ConnectionManager.writeCharacteristic(it, characteristic, POWER_OFF)
                    }
                    ConnectionManager.teardownConnection(device = it)
                }
            }
            FanConnectionStatus.CONNECTING -> {
                fanConnectionStatus.value = FanConnectionStatus.DISCONNECTED
            }
            FanConnectionStatus.SCANNING -> {
                stopBleScan()
                fanConnectionStatus.value = FanConnectionStatus.DISCONNECTED
            }
        }
    }

    private fun stopBleScan() {
        bleScanner.stopScan(scanCallback)
    }

    private fun initializeHeartRateSensor() {
        listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val heartRate = event.values.last().toInt()
                if (heartRate > 0) {
                    val speed = getFanSpeedForHeartRate(heartRate)
                    speedToDevice.value = speed
                    hr.value = heartRate
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        listener?.let {
            sensorManager.registerListener(listener, sensor, 5_000_000)
        }
    }

    private fun teardownHeartRateSensor() {
        listener?.let {
            sensorManager.unregisterListener(listener)
        }
        listener = null
    }

    /**
     * Packages up the desired fan speed (0-100) in a byte array formatted as required for setting
     * the GATT characteristic.
     */
    private fun fanValue(value: Int) = byteArrayOf(2, value.toByte())

    private fun getFanSpeedForHeartRate(heartRate: Int): Int {
        val hr = hrSettings
        val speed = speedSettings
        return when {
            heartRate < hr.currentMin -> speed.currentMin.toInt()
            heartRate > hr.currentMax -> speed.currentMax.toInt()
            else -> {
                val hrPc =
                    (heartRate - hr.currentMin) / (hr.currentMax - hr.currentMin)
                val fanRange = speed.currentMax - speed.currentMin
                (speed.currentMin + hrPc * fanRange).toInt()
            }
        }
    }

    /**
     * Determines if the Characteristic value represents the current fan speed: The Characteristic
     * value can represent other things.
     */
    private fun isFanSpeedResponse(byteArray: ByteArray): Boolean {
        with(byteArray) {
            return size == 4 && get(0) == 0xFD.toByte() &&
                    get(1) == 0x01.toByte() && get(3) == 0x04.toByte()
        }
    }

    private fun setSpeed(value: Int) {
        fanCharacteristic?.let { char ->
            if (value != speedFromDevice.value) {
                device?.let { dev ->
                    ConnectionManager.writeCharacteristic(dev, char, fanValue(value))
                }
            }
        }
    }

    private fun resetMetrics() {
        speedToDevice.value = 0
        speedFromDevice.value = 0
        hr.value = 0
    }

    inner class LocalBinder : Binder() {
        fun getService(): FanControlService = this@FanControlService
    }

    companion object {
        const val BLE_DEVICE_NAME = "HEADWIND"
        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_CHANNEL = "com.garan.wearwind.FanControlService"
        const val NOTIFICATION_TITLE = "Wearwind"
        const val NOTIFICATION_TEXT = "Fan service running"
        const val STATUS_TEMPLATE = "Speed #speed#"
        const val INITIAL_SPEED = 10
        private val CHARACTERISTIC_UUID = UUID.fromString("a026e038-0a7d-4ab3-97fa-f1500f9feb8b")
        private val POWER_ON = byteArrayOf(4, 4, 1)
        private val POWER_OFF = byteArrayOf(2, 0)
    }
}

enum class SettingType {
    HR,
    SPEED
}

enum class SettingLevel {
    MIN,
    MAX
}
