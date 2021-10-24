package com.garan.wearwind

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.wear.ambient.AmbientModeSupport
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.garan.wearwind.FanControlService.FanConnectionStatus.CONNECTED
import com.garan.wearwind.FanControlService.FanConnectionStatus.DISCONNECTED
import com.garan.wearwind.screens.ConnectScreen
import com.garan.wearwind.screens.ConnectedScreen
import com.garan.wearwind.screens.SettingsDetailScreen
import com.garan.wearwind.screens.SettingsItem
import com.garan.wearwind.screens.SettingsScreen
import com.garan.wearwind.screens.SpeedAndHrBox
import com.garan.wearwind.screens.SpeedLabel
import com.garan.wearwind.ui.theme.WearwindTheme
import kotlinx.coroutines.launch

const val TAG = "Wearwind"

/**
 * Activity for controlling searching for the Headwind fan, and launching the fan control activity
 * on successfully locating and connecting to it.
 */
class FanActivity : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider {
    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BODY_SENSORS
    )

    private var fanService: FanControlService? = null
    private var bound: Boolean = false

    private enum class Screen(val route: String) {
        CONNECT("connect"),
        CONNECTED("connected"),
        SETTINGS("settings"),
        SETTINGS_HR("settings_hr"),
        SETTINGS_SPEED("settings_speed")
    }

    // TODO try removing all
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.all { it.value }) {
            Log.i(TAG, "All required permissions granted")
            createService()
        } else {
            Log.i(TAG, "Not all required permissions granted")
            // TODO permissions Composable
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as FanControlService.LocalBinder
            binder.getService().let { fanControlService ->
                setContent {
                    WearwindTheme {
                        WearwindNavigation(fanControlService = fanControlService)
                    }
                }
            }
            Log.i(TAG, "onServiceConnected")
            bound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
            fanService = null
            Log.i(TAG, "onServiceDisconnected")
        }
    }

    private fun createService() {
        Intent(this, FanControlService::class.java).also { intent ->
            startForegroundService(intent)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    @OptIn(ExperimentalWearMaterialApi::class)
    @Composable
    fun WearwindNavigation(fanControlService: FanControlService) {
        val navController = rememberSwipeDismissableNavController()
        val connectionStatus = fanControlService.fanConnectionStatus.observeAsState()
        val hrEnabled = fanControlService.hrEnabled.observeAsState()

        LaunchedEffect(connectionStatus.value) {
            if (connectionStatus.value == CONNECTED && navController.currentDestination?.route != Screen.CONNECTED.route) {
                navController.navigate(Screen.CONNECTED.route)
            } else if (connectionStatus.value == DISCONNECTED && navController.currentDestination?.route == Screen.CONNECTED.route) {
                navController.popBackStack()
            }
        }
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = Screen.CONNECT.route
        ) {
            composable(Screen.CONNECT.route) {
                ConnectScreen(
                    connectionStatus = connectionStatus.value!!,
                    hrEnabled = hrEnabled.value!!,
                    onConnectClick = {
                        fanControlService.connectOrDisconnect()
                    },
                    onHrClick = {
                        fanControlService.toggleHrState()
                    },
                    onSettingsClick = {
                        navController.navigate(Screen.SETTINGS.route)
                    }
                )
            }
            composable(Screen.CONNECTED.route) {
                ConnectedScreen(
                    hrEnabled = hrEnabled.value!!,
                    service = fanControlService,
                    onSwipeBack = {
                        fanControlService.connectOrDisconnect()
                    })
            }
            composable(Screen.SETTINGS.route) {
                val composableScope = rememberCoroutineScope()
                val context = LocalContext.current
                SettingsScreen(listOf(
                    SettingsItem.SettingsHeading(R.string.settings),
                    SettingsItem.SettingsButton(
                        labelId = R.string.hr,
                        imageVector = Icons.Rounded.FavoriteBorder,
                        onClick = {
                            navController.navigate(Screen.SETTINGS_HR.route)
                        }
                    ),
                    SettingsItem.SettingsButton(
                        labelId = R.string.speed,
                        imageVector = Icons.Rounded.BrightnessLow,
                        onClick = {
                            navController.navigate(Screen.SETTINGS_SPEED.route)
                        }
                    ),
                    SettingsItem.SettingsButton(
                        labelId = R.string.about,
                        imageVector = Icons.Rounded.QuestionAnswer,
                        onClick = {
                            Toast.makeText(
                                context,
                                context.getString(R.string.about_toast_msg),
                                Toast.LENGTH_SHORT
                            ).show()
                            composableScope.launch {
                                launchAbout(this@FanActivity)
                            }
                        }
                    ),
                ))
            }
            composable(Screen.SETTINGS_HR.route) {
                SettingsDetailScreen(
                    fanControlService,
                    SettingType.HR,
                    fanControlService.hrSettings
                )
            }
            composable(Screen.SETTINGS_SPEED.route) {
                SettingsDetailScreen(
                    fanControlService,
                    SettingType.SPEED,
                    fanControlService.speedSettings
                )
            }
        }
    }

//    fun checkPermissionsThenLaunch() {
//        if (requiredPermissions.any { permission ->
//            ContextCompat.checkSelfPermission(this, permission) == PERMISSION_DENIED
//            }) {
//            if (shouldShowRequestPermissionRationale())
//        }
//    }

    // TODO
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AmbientModeSupport.attach(this)

        permissionLauncher.launch(requiredPermissions)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (bound) {
            unbindService(connection)
        }
    }

    override fun getAmbientCallback() = object : AmbientModeSupport.AmbientCallback() {}
}