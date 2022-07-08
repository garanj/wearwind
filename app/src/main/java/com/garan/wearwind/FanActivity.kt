package com.garan.wearwind

import android.Manifest
import android.os.Bundle
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.navDeepLink
import androidx.wear.ambient.AmbientModeSupport
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.garan.wearwind.screens.SettingsItem
import com.garan.wearwind.screens.SettingsScreen
import com.garan.wearwind.screens.fan.FanScreen
import com.garan.wearwind.screens.fan.FanViewModel
import com.garan.wearwind.screens.settingsdetail.SettingsDetailScreen
import com.garan.wearwind.screens.settingsdetail.SettingsDetailViewModel
import com.garan.wearwind.ui.theme.WearwindTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

const val TAG = "Wearwind"

enum class Screen(val route: String) {
    FAN("fan"),
    SETTINGS("settings"),
    SETTINGS_HR("settings_hr"),
    SETTINGS_SPEED("settings_speed")
}

class UiState(
    var isShowTime: MutableState<Boolean>,
    var isShowVignette: MutableState<Boolean>,
    val navHostController: NavHostController
)

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
@AndroidEntryPoint
class FanActivity : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider {
    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BODY_SENSORS
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.all { it.value }) {
            Log.i(TAG, "All required permissions granted")
        } else {
            Log.i(TAG, "Not all required permissions granted")
            // TODO permissions Composable
        }
    }

    @Composable
    fun WearwindScreen() {
        val appState = rememberUiState()
        WearwindTheme {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                timeText = { if (appState.isShowTime.value) TimeText() },
                vignette = {
                    if (appState.isShowVignette.value) Vignette(vignettePosition = VignettePosition.TopAndBottom)
                }
            ) {
                WearwindNavigation(appState)
            }
        }
    }

    @Composable
    fun WearwindNavigation(uiState: UiState) {
        SwipeDismissableNavHost(
            navController = uiState.navHostController,
            startDestination = Screen.FAN.route
        ) {
            composable(Screen.FAN.route) {
                val viewModel = hiltViewModel<FanViewModel>()
                val serviceState by viewModel.serviceState
                val hrEnabled by viewModel.hrEnabled.collectAsState(initial = false)
                FanScreen(
                    uiState = uiState,
                    serviceState = serviceState,
                    onConnectClick = {
                        viewModel.connectToFan()
                    },
                    onSetSpeed = { speed ->
                        viewModel.setFanSpeed(speed)
                    },
                    onDisconnectSwipe = {
                        viewModel.disconnectFromFan()
                    },
                    onHrClick = { enabled ->
                        viewModel.setHrEnabled(enabled)
                    },
                    hrEnabled = hrEnabled
                )
            }
            composable(
                Screen.SETTINGS.route,
                deepLinks = listOf(navDeepLink { uriPattern = "app://wearwind/settings" })
            ) {
                val composableScope = rememberCoroutineScope()
                val context = LocalContext.current

                SettingsScreen(
                    uiState = uiState,
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
                    )
                )
            }
            composable(Screen.SETTINGS_HR.route) {
                val viewModel = hiltViewModel<SettingsDetailViewModel>()
                val hrSettings by viewModel.getHrMinMax().collectAsState(MinMaxHolder())
                SettingsDetailScreen(
                    uiState = uiState,
                    settingType = SettingType.HR,
                    minMaxHolder = hrSettings,
                    onClick = { type, level, value ->
                        viewModel.setThreshold(type, level, value)
                    }
                )
            }
            composable(Screen.SETTINGS_SPEED.route) {
                val viewModel = hiltViewModel<SettingsDetailViewModel>()
                val speedSettings by viewModel.getSpeedMinMax().collectAsState(MinMaxHolder())
                SettingsDetailScreen(
                    uiState = uiState,
                    settingType = SettingType.SPEED,
                    minMaxHolder = speedSettings,
                    onClick = { type, level, value ->
                        viewModel.setThreshold(type, level, value)
                    }
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AmbientModeSupport.attach(this)

        permissionLauncher.launch(requiredPermissions)

        setContent {
            WearwindScreen()
        }
    }

    override fun getAmbientCallback() = object : AmbientModeSupport.AmbientCallback() {}
}
