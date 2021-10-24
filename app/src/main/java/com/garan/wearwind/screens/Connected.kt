package com.garan.wearwind.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.garan.wearwind.FanControlService
import com.garan.wearwind.R
import com.garan.wearwind.ui.theme.Colors
import kotlin.math.abs

/**
 * Composable functions for use when connected to the fan, either when in HR-guided or non-HR mode.
 */

@Composable
fun ConnectedScreen(
    hrEnabled: Boolean,
    service: FanControlService,
    onSwipeBack: () -> Unit
) {
    val hr = service.metrics.hr.observeAsState()
    val speed = service.metrics.speedToDevice.observeAsState()
    if (hrEnabled) {
        SpeedAndHrBox(hr, speed)
    } else {
        SpeedLabel(speed, service)
    }
    // Side effect used to disconnect from the fan when dismissing the connected screen.
    DisposableEffect(Unit) {
        onDispose(onSwipeBack)
    }
}

@Composable
fun SpeedAndHrBox(hr: State<Int?>, speed: State<Int?>) {
    if (hr.value == 0) {
        SpeedAndHrPlaceholder()
    } else {
        SpeedAndHrLabel(hr, speed)
    }
}

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun SpeedAndHrLabel(hr: State<Int?>, speed: State<Int?>) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
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
                text = "${speed.value}",
                color = Colors.primary,
                style = LocalTextStyle.current.copy(fontSize = 64.sp),
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                modifier = Modifier.layoutId("hrLabel"),
                text = "${hr.value}",
                color = Colors.secondary,
                style = LocalTextStyle.current.copy(fontSize = 48.sp),
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun SpeedAndHrPlaceholder() {
    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = LocalContext.current.getString(R.string.waiting_for_hr),
                color = Colors.primary,
                style = LocalTextStyle.current.copy(fontSize = 18.sp),
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun SpeedLabel(speed: State<Int?>, fanControlService: FanControlService) {
    val listState = rememberLazyListState()
    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
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
                    style = LocalTextStyle.current.copy(fontSize = 108.sp),
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
    // Sets the initial value when the screen is first created
    LaunchedEffect(Unit) {
        speed.value?.let {
            listState.scrollToItem(it / 5)
        }
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
        fanControlService.metrics.speedToDevice.postValue(closestIndex * 5)
    }
}