package com.garan.wearwind.screens.fan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.garan.wearwind.FanControlService
import com.garan.wearwind.UiState
import com.garan.wearwind.screens.WearwindLoadingMessage

/**
 * Composable functions for use when connected to the fan, either when in HR-guided or non-HR mode.
 */
@Composable
fun FanScreen(
    uiState: UiState,
    serviceState: ServiceState,
    onConnectClick: () -> Unit,
    onSetSpeed: (Int) -> Unit,
    onDisconnect: () -> Unit,
    onHrClick: (Boolean) -> Unit,
    hrEnabled: Boolean,
    shouldShowToast: Boolean,
    onIncrementToastCount: () -> Unit
) {
    // ServiceState is a container that represents whether the ForgroundService has been connected
    // to in the viewModel or not.
    when (serviceState) {
        is ServiceState.Connected -> {
            val fanConnectionStatus by serviceState.fanConnectionStatus
            when (fanConnectionStatus) {
                FanControlService.FanConnectionStatus.CONNECTED -> {
                    val hr by serviceState.hr
                    val speed by serviceState.speed.collectAsState()
                    FanActiveScreen(
                        uiState = uiState,
                        speed = speed,
                        hr = hr,
                        hrEnabled = hrEnabled,
                        onSetSpeed = onSetSpeed,
                        onDisconnect = onDisconnect,
                        shouldShowToast = shouldShowToast,
                        incrementToastCount = onIncrementToastCount
                    )
                }
                FanControlService.FanConnectionStatus.SCANNING,
                FanControlService.FanConnectionStatus.CONNECTING,
                FanControlService.FanConnectionStatus.DISCONNECTED -> FanNotActiveScreen(
                    uiState = uiState,
                    connectionStatus = fanConnectionStatus,
                    onConnectClick = onConnectClick,
                    onHrClick = onHrClick,
                    hrEnabled = hrEnabled
                )
            }
        }
        else -> WearwindLoadingMessage()
    }
}