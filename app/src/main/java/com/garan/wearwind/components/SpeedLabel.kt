package com.garan.wearwind.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.garan.wearwind.UiState
import com.garan.wearwind.rememberUiState
import com.garan.wearwind.screens.WEAR_PREVIEW_API_LEVEL
import com.garan.wearwind.screens.WEAR_PREVIEW_BACKGROUND_COLOR_BLACK
import com.garan.wearwind.screens.WEAR_PREVIEW_DEVICE_HEIGHT_DP
import com.garan.wearwind.screens.WEAR_PREVIEW_DEVICE_WIDTH_DP
import com.garan.wearwind.screens.WEAR_PREVIEW_SHOW_BACKGROUND
import com.garan.wearwind.screens.WEAR_PREVIEW_UI_MODE
import com.garan.wearwind.ui.theme.MetricTypography
import com.garan.wearwind.ui.theme.WearwindTheme

@Composable
fun SpeedLabel(
    uiState: UiState,
    metricDisplayTypography: MetricTypography = MetricTypography(),
    speed: Int,
    onSetSpeed: (Int) -> Unit = {}
) {
    LaunchedEffect(Unit) {
        uiState.isShowVignette.value = true
        uiState.isShowTime.value = true
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
                color = MaterialTheme.colors.primary,
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
            val dist = kotlin.math.abs(midPoint - itemMidPoint)
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
            speed = 73
        )
    }
}