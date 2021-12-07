package com.garan.wearwind.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import com.garan.wearwind.FanControlService
import com.garan.wearwind.R
import com.garan.wearwind.Screen
import com.garan.wearwind.UiState
import com.garan.wearwind.rememberUiState

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun WearwindLoadingMessage(
    uiState: UiState,
    service: FanControlService?
) {
    LaunchedEffect(service) {
        service?.let {
            uiState.navHostController.popBackStack(Screen.LOADING.route, true)
            uiState.navHostController.navigate(Screen.CONNECT.route)
        }
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val context = LocalContext.current
        Image(
            painter = painterResource(
                id = R.drawable.ic_logo
            ),
            contentDescription = context.getString(R.string.loading_message)
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
fun LoadingScreenPreview() {
    val uiState = rememberUiState()
    WearwindLoadingMessage(
        uiState = uiState,
        service = null
    )
}
