package com.garan.wearwind.screens.fan

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.SwipeToDismissBox
import com.garan.wearwind.UiState
import com.garan.wearwind.components.SpeedAndHrBox
import com.garan.wearwind.components.SpeedLabel

@Composable
fun FanActiveScreen(
    uiState: UiState,
    speed: Int,
    hr: Int,
    hrEnabled: Boolean,
    onSetSpeed: (Int) -> Unit,
    onDisconnectSwipe: () -> Unit
) {
    SwipeToDismissBox(
        modifier = Modifier.fillMaxSize(),
        onDismissed = onDisconnectSwipe
    ) {
        if (hrEnabled) {
            SpeedAndHrBox(
                uiState = uiState,
                hr = hr,
                speed = speed
            )
        } else {
            SpeedLabel(
                uiState = uiState,
                speed = speed,
                onSetSpeed = onSetSpeed
            )
        }
    }
}