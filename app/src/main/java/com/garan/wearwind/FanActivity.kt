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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.navDeepLink
import androidx.wear.ambient.AmbientModeSupport
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.garan.wearwind.screens.ConnectScreen
import com.garan.wearwind.screens.ConnectedScreen
import com.garan.wearwind.screens.SettingsDetailScreen
import com.garan.wearwind.screens.SettingsItem
import com.garan.wearwind.screens.SettingsScreen
import com.garan.wearwind.screens.WearwindLoadingMessage
import com.garan.wearwind.ui.theme.WearwindTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

const val TAG = "Wearwind"

enum class Screen(val route: String) {
    LOADING("loading"),
    CONNECT("connect"),
    CONNECTED("connected"),
    SETTINGS("settings"),
    SETTINGS_HR("settings_hr"),
    SETTINGS_SPEED("settings_speed")
}

class UiState(
    var isShowTime: MutableState<Boolean>,
    var isShowVignette: MutableState<Boolean>,
    val navHostController: NavHostController
)

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun rememberUiState(
    isShowTime: MutableState<Boolean> = mutableStateOf(true),
    isShowVignette: MutableState<Boolean> = mutableStateOf(false),
    navHostController: NavHostController = rememberSwipeDismissableNavController()
) = remember(isShowTime, isShowVignette, navHostController) {
    UiState(isShowTime, isShowVignette, navHostController)
}

/**
 * Activity for controlling searching for the Headwind fan, and launching the fan control activity
 * on successfully locating and connecting to it.
 */
class FanActivity : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider {
    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BODY_SENSORS
    )

    private var fanService: MutableStateFlow<FanControlService?> = MutableStateFlow(null)
    private var bound: Boolean = false

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
            binder.getService().let {
                fanService.value = it
            }
            Log.i(TAG, "onServiceConnected")
            bound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
            fanService.value = null
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
    fun WearwindScreen() {
        val appState = rememberUiState()

        WearwindTheme {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize(),
                timeText = { if (appState.isShowTime.value) TimeText() },
                vignette = {
                    if (appState.isShowVignette.value) Vignette(vignettePosition = VignettePosition.TopAndBottom)
                }
            ) {
                WearwindNavigation(appState)
            }
        }
    }

    @OptIn(ExperimentalWearMaterialApi::class)
    @Composable
    fun WearwindNavigation(uiState: UiState) {
        val service by fanService.collectAsState()

        SwipeDismissableNavHost(
            navController = uiState.navHostController,
            startDestination = Screen.LOADING.route
        ) {
            composable(Screen.LOADING.route) {
                WearwindLoadingMessage(
                    uiState = uiState,
                    service = service
                )
            }
            composable(Screen.CONNECT.route) {
                val connectionStatus by service!!.fanConnectionStatus.collectAsState()
                val hrEnabled by service!!.hrEnabled.collectAsState()
                ConnectScreen(
                    connectionStatus = connectionStatus,
                    hrEnabled = hrEnabled,
                    uiState = uiState,
                    onConnectClick = {
                        service?.connectOrDisconnect()
                    },
                    onHrClick = {
                        service?.toggleHrState()
                    },
                    onSettingsClick = {
                        uiState.navHostController.navigate(Screen.SETTINGS.route)
                    }
                )
            }
            composable(Screen.CONNECTED.route) {
                val connectionStatus by service!!.fanConnectionStatus.collectAsState()
                val hrEnabled by service!!.hrEnabled.collectAsState()
                ConnectedScreen(
                    connectionStatus = connectionStatus,
                    hrEnabled = hrEnabled,
                    service = service!!,
                    uiState = uiState,
                    onSwipeBack = {
                        service?.connectOrDisconnect()
                    },
                    onSetSpeed = { speed : Int ->
                        service?.changeSpeed(speed)
                    })
            }
            composable(
                Screen.SETTINGS.route,
                deepLinks = listOf(navDeepLink { uriPattern = "app://wearwind/settings" })
            ) {
                val composableScope = rememberCoroutineScope()
                val context = LocalContext.current

                SettingsScreen(uiState = uiState,
                    settingsItemList = listOf(
                        SettingsItem.SettingsHeading(R.string.settings),
                        SettingsItem.SettingsButton(
                            labelId = R.string.hr,
                            imageVector = Icons.Rounded.FavoriteBorder,
                            onClick = {
                                uiState.navHostController.navigate(Screen.SETTINGS_HR.route)
                            }
                        ),
                        SettingsItem.SettingsButton(
                            labelId = R.string.speed,
                            imageVector = Icons.Rounded.BrightnessLow,
                            onClick = {
                                uiState.navHostController.navigate(Screen.SETTINGS_SPEED.route)
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
                val hrSettings by service!!.hrSettings.collectAsState()
                SettingsDetailScreen(
                    uiState = uiState,
                    screenStarted = uiState.navHostController
                        .getBackStackEntry(Screen.SETTINGS_HR.route)
                        .lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED),
                    settingType = SettingType.HR,
                    minMaxHolder = hrSettings,
                    onIncrementClick = { type, level ->
                        service?.incrementSetting(type, level)
                    },
                    onDecrementClick = { type, level ->
                        service?.decrementSetting(type, level)
                    }
                )
            }
            composable(Screen.SETTINGS_SPEED.route) {
                val speedSettings by service!!.speedSettings.collectAsState()
                SettingsDetailScreen(
                    uiState = uiState,
                    screenStarted = uiState.navHostController
                        .getBackStackEntry(Screen.SETTINGS_SPEED.route)
                        .lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED),
                    settingType = SettingType.SPEED,
                    minMaxHolder = speedSettings,
                    onIncrementClick = { type, level ->
                        service?.incrementSetting(type, level)
                    },
                    onDecrementClick = { type, level ->
                        service?.decrementSetting(type, level)
                    }
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

    @OptIn(ExperimentalWearMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AmbientModeSupport.attach(this)

        permissionLauncher.launch(requiredPermissions)

        setContent {
            WearwindScreen()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (bound) {
            unbindService(connection)
        }
    }

    override fun getAmbientCallback() = object : AmbientModeSupport.AmbientCallback() {}
}