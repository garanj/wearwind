package com.garan.wearwind.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CircularProgressIndicator
import com.garan.wearwind.FanControlService
import com.garan.wearwind.ui.theme.Colors

@Composable
fun ConnectionIndicator(connectionStatus: FanControlService.FanConnectionStatus) {
    if (connectionStatus == FanControlService.FanConnectionStatus.CONNECTING ||
        connectionStatus == FanControlService.FanConnectionStatus.SCANNING
    ) {
        CircularProgressIndicator(
            modifier = Modifier.fillMaxSize(),
            indicatorColor = Colors.primary,
            trackColor = Color.Black,
            strokeWidth = 10.dp
        )
    }
}