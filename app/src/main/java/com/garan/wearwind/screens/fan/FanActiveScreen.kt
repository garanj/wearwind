package com.garan.wearwind.screens.fan

import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.garan.wearwind.R
import com.garan.wearwind.UiState
import com.garan.wearwind.components.EndRing
import com.garan.wearwind.components.SpeedAndHrBox
import com.garan.wearwind.components.SpeedLabel

@Composable
fun FanActiveScreen(
    uiState: UiState,
    speed: Int,
    hr: Int,
    hrEnabled: Boolean,
    onSetSpeed: (Int) -> Unit,
    onDisconnect: () -> Unit,
    shouldShowToast: Boolean,
    incrementToastCount: () -> Unit
) {
    val context = LocalContext.current
    val longPressMessage = stringResource(id = R.string.long_press)
    if (shouldShowToast) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, longPressMessage, Toast.LENGTH_LONG).show()
            incrementToastCount()
        }
    }
    var showTimer by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        showTimer = true
                        tryAwaitRelease()
                        showTimer = false
                    },
                    onDoubleTap = { },
                    onLongPress = { },
                    onTap = { }
                )
            },
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
        if (showTimer) {
            EndRing(onFinishTap = onDisconnect)
        }
    }
}