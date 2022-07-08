package com.garan.wearwind.screens.fan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.garan.wearwind.FanControlService
import com.garan.wearwind.Screen
import com.garan.wearwind.UiState
import com.garan.wearwind.components.ConnectButton
import com.garan.wearwind.components.ConnectionIndicator
import com.garan.wearwind.components.HrButton
import com.garan.wearwind.components.SettingsButton

@Composable
fun FanNotActiveScreen(
    uiState: UiState,
    connectionStatus: FanControlService.FanConnectionStatus,
    onConnectClick: () -> Unit,
    onHrClick: (Boolean) -> Unit,
    hrEnabled: Boolean
) {
    ConnectionIndicator(connectionStatus)
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                ConnectButton(
                    modifier = Modifier.padding(16.dp),
                    connectionStatus = connectionStatus,
                    onClick = onConnectClick
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HrButton(
                    connectionStatus = connectionStatus,
                    hrEnabled = hrEnabled,
                    onClick = { onHrClick(!hrEnabled) }
                )
                SettingsButton(
                    onClick = {
                        uiState.navHostController.navigate(Screen.SETTINGS.route)
                    },
                    enabled = connectionStatus == FanControlService.FanConnectionStatus.DISCONNECTED
                )
            }
        }
    }
}