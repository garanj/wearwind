package com.garan.wearwind.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.garan.wearwind.UiState

@Composable
fun SpeedAndHrBox(
    uiState: UiState,
    hr: Int,
    speed: Int
) {
    LaunchedEffect(Unit) {
        uiState.isShowTime.value = true
        uiState.isShowVignette.value = false
    }
    if (hr == 0) {
        SpeedAndHrPlaceholder()
    } else {
        SpeedAndHrLabel(
            hr = hr,
            speed = speed
        )
    }
}