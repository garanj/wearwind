package com.garan.wearwind.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.MaterialTheme
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
    onIncrementClick: (SettingType, SettingLevel) -> Unit = { _, _ -> },
    onDecrementClick: (SettingType, SettingLevel) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current

    LaunchedEffect(screenStarted) {
        if (screenStarted) {
            uiState.isShowTime.value = false
            uiState.isShowVignette.value = false
        }
    }
    val constraintSet = ConstraintSet {
        val minButton = createRefFor("minButton")
        val maxButton = createRefFor("maxButton")
        val minusButton = createRefFor("minusButton")
        val plusButton = createRefFor("plusButton")

        constrain(minButton) {
            top.linkTo(parent.top)
            bottom.linkTo(minusButton.top)
            start.linkTo(parent.start)
            end.linkTo(maxButton.start)
        }
        constrain(maxButton) {
            top.linkTo(parent.top)
            bottom.linkTo(plusButton.top)
            start.linkTo(minButton.end)
            end.linkTo(parent.end)
        }
        constrain(minusButton) {
            top.linkTo(minButton.bottom)
            bottom.linkTo(parent.bottom)
            start.linkTo(parent.start)
            end.linkTo(plusButton.start)
        }
        constrain(plusButton) {
            top.linkTo(maxButton.bottom)
            bottom.linkTo(parent.bottom)
            start.linkTo(minusButton.end)
            end.linkTo(parent.end)
        }
        createVerticalChain(
            minButton, minusButton, chainStyle = ChainStyle.Packed
        )
        createVerticalChain(
            maxButton, plusButton, chainStyle = ChainStyle.Packed
        )
        createHorizontalChain(
            minButton, maxButton, chainStyle = ChainStyle.Packed
        )
        createHorizontalChain(
            minusButton, plusButton, chainStyle = ChainStyle.Packed
        )
    }

    ConstraintLayout(constraintSet = constraintSet, modifier = Modifier.fillMaxSize()) {
        val selected = remember { mutableStateOf(SettingLevel.MIN) }
        Button(
            modifier = Modifier
                .layoutId("minButton")
                .fillMaxSize(0.4f)
                .padding(8.dp)
                .then(
                    if (selected.value == SettingLevel.MIN) {
                        Modifier.border(2.dp, Colors.primary, CircleShape)
                    } else Modifier
                ),
            colors = ButtonDefaults.secondaryButtonColors(),
            onClick = {
                selected.value = SettingLevel.MIN
            }
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontStyle = MaterialTheme.typography.display3.fontStyle)) {
                        append("${minMaxHolder.currentMin}\n")
                    }
                    withStyle(style = SpanStyle(fontStyle = MaterialTheme.typography.body2.fontStyle)) {
                        append(context.getString(R.string.min_small))
                    }
                }
            )
        }
        Button(
            modifier = Modifier
                .layoutId("maxButton")
                .fillMaxSize(0.4f)
                .padding(8.dp)
                .then(
                    if (selected.value == SettingLevel.MAX) {
                        Modifier.border(2.dp, Colors.primary, CircleShape)
                    } else Modifier
                ),
            colors = ButtonDefaults.secondaryButtonColors(),
            onClick = {
                selected.value = SettingLevel.MAX
            }
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontStyle = MaterialTheme.typography.display3.fontStyle)) {
                        append("${minMaxHolder.currentMax}\n")
                    }
                    withStyle(style = SpanStyle(fontStyle = MaterialTheme.typography.body2.fontStyle)) {
                        append(context.getString(R.string.max_small))
                    }
                }
            )
        }
        Button(
            modifier = Modifier
                .layoutId("minusButton")
                .fillMaxSize(0.4f)
                .padding(8.dp),
            onClick = { onDecrementClick(settingType, selected.value) }
        ) {
            Text(
                text = context.getString(R.string.minus),
                style = MaterialTheme.typography.display3
            )
        }
        Button(
            modifier = Modifier
                .layoutId("plusButton")
                .fillMaxSize(0.4f)
                .padding(8.dp),
              onClick = { onIncrementClick(settingType, selected.value) }
        ) {
            Text(
                text = context.getString(R.string.plus),
                style = MaterialTheme.typography.display3
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
            true,
            SettingType.SPEED,
            MinMaxHolder()
        )
    }
}