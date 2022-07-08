package com.garan.wearwind.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.garan.wearwind.screens.WEAR_PREVIEW_API_LEVEL
import com.garan.wearwind.screens.WEAR_PREVIEW_BACKGROUND_COLOR_BLACK
import com.garan.wearwind.screens.WEAR_PREVIEW_DEVICE_HEIGHT_DP
import com.garan.wearwind.screens.WEAR_PREVIEW_DEVICE_WIDTH_DP
import com.garan.wearwind.screens.WEAR_PREVIEW_SHOW_BACKGROUND
import com.garan.wearwind.screens.WEAR_PREVIEW_UI_MODE
import com.garan.wearwind.ui.theme.MetricTypography
import com.garan.wearwind.ui.theme.WearwindTheme

@Composable
fun SpeedAndHrLabel(
    metricDisplayTypography: MetricTypography = MetricTypography(),
    hr: Int,
    speed: Int
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "$speed",
                color = MaterialTheme.colors.primary,
                style = metricDisplayTypography.mediumDisplayMetric,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "$hr",
                color = MaterialTheme.colors.secondary,
                style = metricDisplayTypography.smallDisplayMetric,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.ExtraBold
            )
        }
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
        SpeedAndHrLabel(
            hr = 124,
            speed = 73
        )
    }
}