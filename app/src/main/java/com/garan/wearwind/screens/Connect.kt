package com.garan.wearwind.screens

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.lifecycle.Lifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import com.garan.wearwind.FanControlService
import com.garan.wearwind.R
import com.garan.wearwind.Screen
import com.garan.wearwind.UiState
import com.garan.wearwind.ui.theme.Colors

/**
 * Composable functions used on the Connect screen, for initiating a connection to the fan.
 */
@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun ConnectScreen(
    hrEnabled: Boolean = false,
    connectionStatus: FanControlService.FanConnectionStatus,
    uiState: UiState,
    screenStarted: Boolean = uiState.navHostController
        .getBackStackEntry(Screen.CONNECT.route)
        .lifecycle.currentState == Lifecycle.State.STARTED,
    onConnectClick: () -> Unit,
    onHrClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    if (screenStarted) {
        uiState.isShowTime.value = true
    }
    LaunchedEffect(connectionStatus) {
        if (screenStarted && connectionStatus == FanControlService.FanConnectionStatus.CONNECTED) {
            uiState.navHostController.navigate(Screen.CONNECTED.route)
        }
    }

    val constraintSet = ConstraintSet {
        val connectButton = createRefFor("connectButton")
        val hrButton = createRefFor("hrButton")
        val settingsButton = createRefFor("settingsButton")
        val progressIndicator = createRefFor("progress")

        constrain(progressIndicator) {
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
        }
        constrain(connectButton) {
            top.linkTo(parent.top)
            bottom.linkTo(settingsButton.top)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
        }
        constrain(hrButton) {
            top.linkTo(settingsButton.top)
            bottom.linkTo(settingsButton.bottom)
            start.linkTo(parent.start)
            end.linkTo(settingsButton.start)
        }
        constrain(settingsButton) {
            top.linkTo(connectButton.bottom)
            bottom.linkTo(parent.bottom)
            start.linkTo(hrButton.end)
            end.linkTo(parent.end)
        }
        createVerticalChain(
            connectButton,
            settingsButton,
            chainStyle = ChainStyle.Packed
        )
    }

    ConstraintLayout(constraintSet = constraintSet, modifier = Modifier.fillMaxSize()) {
        ConnectionIndicator(connectionStatus)
        ConnectButton(
            modifier = Modifier.layoutId("connectButton"),
            connectionStatus,
            onClick = onConnectClick
        )
        Button(
            onClick = onSettingsClick,
            modifier = Modifier.layoutId("settingsButton"),
            enabled = connectionStatus == FanControlService.FanConnectionStatus.DISCONNECTED
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                stringResource(id = R.string.settings)
            )
        }
        HrButton(
            connectionStatus,
            hrEnabled = hrEnabled,
            onClick = onHrClick
        )
    }
}

@Composable
fun ConnectButton(
    modifier: Modifier = Modifier,
    connectionStatus: FanControlService.FanConnectionStatus,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 16.dp),
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