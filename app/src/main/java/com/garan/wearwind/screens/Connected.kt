package com.garan.wearwind.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.lifecycle.Lifecycle
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.garan.wearwind.FanControlService
import com.garan.wearwind.R
import com.garan.wearwind.Screen
import com.garan.wearwind.UiState
import com.garan.wearwind.rememberUiState
import com.garan.wearwind.ui.theme.Colors
import com.garan.wearwind.ui.theme.MetricTypography
import com.garan.wearwind.ui.theme.WearwindTheme
import kotlin.math.abs


/**
 * Composable functions for use when connected to the fan, either when in HR-guided or non-HR mode.
 */

@Composable
fun ConnectedScreen(
    connectionStatus: FanControlService.FanConnectionStatus,
    hrEnabled: Boolean,
    service: FanControlService,
    uiState: UiState,
    screenStarted: Boolean = uiState.navHostController
        .getBackStackEntry(Screen.CONNECTED.route)
        .lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED),
    metricDisplayTypography: MetricTypography = MetricTypography(),
    onSwipeBack: () -> Unit,
    onSetSpeed: (Int) -> Unit = {}
) {
    // Hold state reflecting how this screen is being closed.
    val closedBySwipe = remember { mutableStateOf(true) }

    // Handle changes in connectionStatus from the Service. i.e. If the fan disconnects by itself
    // ensure that the app automatically navigates back to the Connect screen.
    LaunchedEffect(screenStarted, connectionStatus) {
        if (screenStarted && connectionStatus == FanControlService.FanConnectionStatus.DISCONNECTED) {
            closedBySwipe.value = false
            uiState.navHostController.popBackStack(Screen.CONNECT.route, false)
        }
    }

    val hr by service.metrics.hr.collectAsState()
    val speed by service.metrics.speedToDevice.collectAsState()
    if (hrEnabled) {
        SpeedAndHrBox(
            uiState = uiState,
            screenStarted = screenStarted,
            hr = hr,
            speed = speed)
    } else {
        SpeedLabel(
            uiState = uiState,
            screenStarted = screenStarted,
            speed = speed,
            onSetSpeed = onSetSpeed)
    }
    // Side effect used to disconnect from the fan when dismissing the connected screen.
    DisposableEffect(Unit) {
        onDispose {
            // Only call the action associated with swiping back (i.e. instructing Service to
            // disconnect) if this clean up is happening from a swipe.
            if (closedBySwipe.value) {
                onSwipeBack.invoke()
            }
        }
    }
}

@Composable
fun SpeedAndHrBox(
    uiState: UiState,
    screenStarted: Boolean,
    metricDisplayTypography: MetricTypography = MetricTypography(),
    hr: Int,
    speed: Int) {
    LaunchedEffect(screenStarted) {
        if (screenStarted) {
            uiState.isShowTime.value = true
            uiState.isShowVignette.value = false
        }
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

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun SpeedAndHrLabel(
    metricDisplayTypography: MetricTypography = MetricTypography(),
    hr: Int,
    speed: Int) {
    val constraintSet = ConstraintSet {
        val speedLabel = createRefFor("speedLabel")
        val hrLabel = createRefFor("hrLabel")

        constrain(speedLabel) {
            top.linkTo(parent.top)
            bottom.linkTo(hrLabel.top)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
        }
        constrain(hrLabel) {
            top.linkTo(speedLabel.bottom)
            bottom.linkTo(parent.bottom)
            end.linkTo(speedLabel.end)
        }
        createVerticalChain(
            speedLabel, hrLabel, chainStyle = ChainStyle.Packed
        )
    }
    ConstraintLayout(constraintSet = constraintSet, modifier = Modifier.fillMaxSize()) {
        Text(
            modifier = Modifier.layoutId("speedLabel"),
            text = "$speed",
            color = Colors.primary,
            style = metricDisplayTypography.mediumDisplayMetric,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            modifier = Modifier.layoutId("hrLabel"),
            text = "$hr",
            color = Colors.secondary,
            style = metricDisplayTypography.smallDisplayMetric,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun SpeedAndHrPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = LocalContext.current.getString(R.string.waiting_for_hr),
            color = Colors.primary,
            style = MaterialTheme.typography.title1,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun SpeedLabel(
    uiState: UiState,
    screenStarted: Boolean,
    metricDisplayTypography: MetricTypography = MetricTypography(),
    speed: Int,
    onSetSpeed: (Int) -> Unit = {}
) {
    LaunchedEffect(screenStarted) {
        if (screenStarted) {
            uiState.isShowVignette.value = true
            uiState.isShowTime.value = true
        }
    }
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        reverseLayout = true,
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(20 + 1) { item ->
            Text(
                "${item * 5}",
                color = Colors.primary,
                style = metricDisplayTypography.largeDisplayMetric,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
    // Sets the initial value when the screen is first created
    LaunchedEffect(Unit) {
        listState.scrollToItem(speed / 5)
    }
    /**
     * When a scroll has taken place, trigger setting the speed of the connected fan. Speed selected
     * is that of the closest item on the screen to the vertical midpoint.
     */
    LaunchedEffect(listState.isScrollInProgress) {
        val startOffset = listState.layoutInfo.viewportStartOffset
        val endOffset = listState.layoutInfo.viewportEndOffset

        val midPoint = (endOffset - startOffset) / 2.0

        var closestIndex = 0
        var closestDist = (endOffset - startOffset) * 1.0

        listState.layoutInfo.visibleItemsInfo.forEach {
            val itemMidPoint = it.offset + it.size / 2
            val dist = abs(midPoint - itemMidPoint)
            if (dist < closestDist) {
                closestDist = dist
                closestIndex = it.index
            }
        }
        onSetSpeed(closestIndex * 5)
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
fun ManualModePreview() {
    WearwindTheme {
        val uiState = rememberUiState()
        SpeedLabel(
            uiState = uiState,
            screenStarted = true,
            speed = 73)
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
fun HrModePreview() {
    WearwindTheme {
        val uiState = rememberUiState()
        SpeedAndHrLabel(
            hr = 124,
            speed = 73)
    }
}