package com.garan.wearwind.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import com.garan.wearwind.FanControlService
import com.garan.wearwind.R
import com.garan.wearwind.Screen
import com.garan.wearwind.UiState
import com.garan.wearwind.rememberUiState
import com.garan.wearwind.ui.theme.Colors
import com.garan.wearwind.ui.theme.WearwindTheme

/**
 * Composable functions used on the Connect screen, for initiating a connection to the fan.
 */
@Composable
fun ConnectScreen(
    hrEnabled: Boolean = false,
    connectionStatus: FanControlService.FanConnectionStatus,
    uiState: UiState,
    screenStarted: Boolean = uiState.navHostController
        .getBackStackEntry(Screen.CONNECT.route)
        .lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED),
    onConnectClick: () -> Unit,
    onHrClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    LaunchedEffect(screenStarted) {
        if (screenStarted) {
            uiState.isShowTime.value = true
        }
    }
    LaunchedEffect(screenStarted, connectionStatus) {
        if (screenStarted && connectionStatus == FanControlService.FanConnectionStatus.CONNECTED) {
            uiState.navHostController.navigate(Screen.CONNECTED.route)
        }
    }

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
                    modifier = Modifier
                        .padding(16.dp),
                    connectionStatus,
                    onClick = onConnectClick
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HrButton(
                    connectionStatus,
                    hrEnabled = hrEnabled,
                    onClick = onHrClick
                )
                SettingsButton(
                    onClick = onSettingsClick,
                    enabled = connectionStatus == FanControlService.FanConnectionStatus.DISCONNECTED
                )
            }
        }
    }
}

@Composable
fun ConnectButton(
    modifier: Modifier = Modifier,
    connectionStatus: FanControlService.FanConnectionStatus,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier,
        onClick = onClick
    ) {
        val buttonTextId = when (connectionStatus) {
            FanControlService.FanConnectionStatus.CONNECTED -> R.string.disconnect
            FanControlService.FanConnectionStatus.DISCONNECTED -> R.string.connect
            else -> R.string.cancel
        }
        Text(
            stringResource(id = buttonTextId).uppercase(),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ConnectionIndicator(connectionStatus: FanControlService.FanConnectionStatus) {
    if (connectionStatus == FanControlService.FanConnectionStatus.CONNECTING ||
        connectionStatus == FanControlService.FanConnectionStatus.SCANNING
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .layoutId("progress")
                .fillMaxSize(),
            color = Colors.primary
        )
    }
}

@Composable
fun SettingsButton(
    onClick: () -> Unit,
    enabled: Boolean
) {
    Button(
        onClick = onClick,
        modifier = Modifier.layoutId("settingsButton"),
        enabled = enabled
    ) {
        Icon(
            imageVector = Icons.Rounded.Settings,
            stringResource(id = R.string.settings)
        )
    }
}

@Composable
fun HrButton(
    connectionStatus: FanControlService.FanConnectionStatus,
    hrEnabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val iconId = if (hrEnabled) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder
    Button(
        onClick = onClick,
        modifier = Modifier.layoutId("hrButton"),
        enabled = connectionStatus == FanControlService.FanConnectionStatus.DISCONNECTED
    ) {
        Icon(
            imageVector = iconId,
            stringResource(id = R.string.hr)
        )
    }
}

@Preview(
    widthDp = WEAR_PREVIEW_DEVICE_WIDTH_DP,
    heightDp = WEAR_PREVIEW_DEVICE_HEIGHT_DP,
    apiLevel = WEAR_PREVIEW_API_LEVEL,
    uiMode = WEAR_PREVIEW_UI_MODE,
    backgroundColor = WEAR_PREVIEW_BACKGROUND_COLOR_BLACK,
    showBackground = WEAR_PREVIEW_SHOW_BACKGROUND
)
@Composable
fun ConnectScreenPreview() {
    WearwindTheme {

        val uiState = rememberUiState()
        ConnectScreen(
            connectionStatus = FanControlService.FanConnectionStatus.DISCONNECTED,
            uiState = uiState,
            screenStarted = true,
            onConnectClick = { },
            onHrClick = { },
            onSettingsClick = { }
        )
    }
}
