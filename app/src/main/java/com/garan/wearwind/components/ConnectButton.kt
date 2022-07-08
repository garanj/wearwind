package com.garan.wearwind.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import com.garan.wearwind.FanControlService
import com.garan.wearwind.R

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

