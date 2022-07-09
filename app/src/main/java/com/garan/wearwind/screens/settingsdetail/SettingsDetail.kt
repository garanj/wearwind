package com.garan.wearwind.screens.settingsdetail

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Stepper
import com.garan.wearwind.MinMaxHolder
import com.garan.wearwind.R
import com.garan.wearwind.SettingLevel
import com.garan.wearwind.SettingType
import com.garan.wearwind.UiState
import com.garan.wearwind.components.CurrentValueButton
import com.garan.wearwind.rememberUiState
import com.garan.wearwind.screens.WEAR_PREVIEW_API_LEVEL
import com.garan.wearwind.screens.WEAR_PREVIEW_BACKGROUND_COLOR_BLACK
import com.garan.wearwind.screens.WEAR_PREVIEW_DEVICE_HEIGHT_DP
import com.garan.wearwind.screens.WEAR_PREVIEW_DEVICE_WIDTH_DP
import com.garan.wearwind.screens.WEAR_PREVIEW_SHOW_BACKGROUND
import com.garan.wearwind.screens.WEAR_PREVIEW_UI_MODE
import com.garan.wearwind.ui.theme.WearwindTheme
import kotlin.math.roundToInt

@Composable
fun SettingsDetailScreen(
    uiState: UiState,
    settingType: SettingType,
    minMaxHolder: MinMaxHolder,
    onClick: (SettingType, SettingLevel, Float) -> Unit = { _, _, _ -> }
) {
    val selected = remember { mutableStateOf(SettingLevel.MAX) }

    LaunchedEffect(Unit) {
        uiState.isShowTime.value = false
        uiState.isShowVignette.value = false
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
        onValueChange = { value: Float ->
            onClick(settingType, selected.value, value)
        },
        valueRange = range,
        steps = ((range.endInclusive - range.start) / minMaxHolder.step).toInt() - 1,
        increaseIcon = {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(id = R.string.add)
            )
        },
        decreaseIcon = {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = stringResource(id = R.string.minus)
            )
        }
    ) {
        Row(
            modifier = Modifier,
            verticalAlignment = Alignment.Bottom
        ) {
            CurrentValueButton(
                currentValue = minMaxHolder.currentMin.roundToInt(),
                metricLabel = stringResource(id = R.string.min_small),
                isSelected = selected.value == SettingLevel.MIN,
                buttonSize = 96.dp,
                onClick = {
                    selected.value = SettingLevel.MIN
                }
            )
            CurrentValueButton(
                currentValue = minMaxHolder.currentMax.roundToInt(),
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
            SettingType.SPEED,
            MinMaxHolder()
        )
    }
}
