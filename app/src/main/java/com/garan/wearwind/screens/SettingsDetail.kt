package com.garan.wearwind.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Stepper
import androidx.wear.compose.material.Text
import com.garan.wearwind.MinMaxHolder
import com.garan.wearwind.R
import com.garan.wearwind.SettingLevel
import com.garan.wearwind.SettingType
import com.garan.wearwind.UiState
import com.garan.wearwind.rememberUiState
import com.garan.wearwind.ui.theme.Colors
import com.garan.wearwind.ui.theme.WearwindTheme

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun SettingsDetailScreen(
    uiState: UiState,
    screenStarted: Boolean,
    settingType: SettingType,
    minMaxHolder: MinMaxHolder,
    onClick: (SettingType, SettingLevel, Float) -> Unit = { _, _, _ -> }
) {
    val selected = remember { mutableStateOf(SettingLevel.MAX) }

    LaunchedEffect(screenStarted) {
        if (screenStarted) {
            uiState.isShowTime.value = false
            uiState.isShowVignette.value = false
        }
    }
    val currentValue = if (selected.value == SettingLevel.MIN) {
        minMaxHolder.currentMin
    } else {
        minMaxHolder.currentMax
    }.toFloat()

    val range = if (selected.value == SettingLevel.MIN) {
        minMaxHolder.minRange()
    } else {
        minMaxHolder.maxRange()
    }

    Stepper(
        value = currentValue,
        onValueChange = { value ->
            onClick(settingType, selected.value, value)
        },
        valueRange = range,
        steps = ((range.endInclusive - range.start) / minMaxHolder.step).toInt() - 1
    ) {
        Row(
            modifier = Modifier,
            verticalAlignment = Alignment.Bottom
        ) {
            CurrentValueButton(
                currentValue = minMaxHolder.currentMin.toInt(),
                metricLabel = stringResource(id = R.string.min_small),
                isSelected = selected.value == SettingLevel.MIN,
                buttonSize = 96.dp,
                onClick = {
                    selected.value = SettingLevel.MIN
                }
            )
            CurrentValueButton(
                currentValue = minMaxHolder.currentMax.toInt(),
                metricLabel = stringResource(id = R.string.max_small),
                isSelected = selected.value == SettingLevel.MAX,
                buttonSize = 96.dp,
                onClick = {
                    selected.value = SettingLevel.MAX
                }
            )
        }
    }
}

@Composable
fun CurrentValueButton(
    currentValue: Int,
    metricLabel: String,
    isSelected: Boolean,
    buttonSize: Dp,
    onClick: () -> Unit
) {
    val color = if (isSelected) Colors.primary else Color.Black
    Button(
        modifier = Modifier
            .size(buttonSize)
            .padding(8.dp)
            .aspectRatio(1f)
            .border(2.dp, color, CircleShape),
        colors = ButtonDefaults.secondaryButtonColors(),
        onClick = onClick
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(fontStyle = MaterialTheme.typography.display3.fontStyle)) {
                    append("$currentValue\n")
                }
                withStyle(style = SpanStyle(fontStyle = MaterialTheme.typography.body2.fontStyle)) {
                    append(metricLabel)
                }
            }
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
fun SettingDetailPreview() {
    val uiState = rememberUiState()
    WearwindTheme {
        SettingsDetailScreen(
            uiState,
            true,
            SettingType.SPEED,
            MinMaxHolder()
        )
    }
}
