package com.garan.wearwind.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.lifecycle.LiveData
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.garan.wearwind.FanControlService
import com.garan.wearwind.MinMaxHolder
import com.garan.wearwind.R
import com.garan.wearwind.SettingLevel
import com.garan.wearwind.SettingType
import com.garan.wearwind.ui.theme.Colors

@Composable
fun SettingsScreen(settingsItemList: List<SettingsItem>) {
    Scaffold(
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        ScalingLazyColumn(
            contentPadding = PaddingValues(20.dp, 10.dp, 20.dp, 30.dp)
        ) {
            items(settingsItemList.size) {
                SettingsEntry(settingsItem = settingsItemList[it])
            }
        }
    }
}

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun SettingsDetailScreen(
    fanControlService: FanControlService,
    settingType: SettingType,
    minMaxHolder: LiveData<MinMaxHolder>
) {
    val context = LocalContext.current
    val minMax = minMaxHolder.observeAsState()
    Scaffold(timeText = { TimeText() }) {
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
                            append("${minMax.value?.currentMin}\n")
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
                            append("${minMax.value?.currentMax}\n")
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
                onClick = {
                    fanControlService.decrementSetting(settingType, selected.value)
                }
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
                onClick = {
                    fanControlService.incrementSetting(settingType, selected.value)
                }
            ) {
                Text(
                    text = context.getString(R.string.plus),
                    style = MaterialTheme.typography.display3
                )
            }
        }

    }
}

@Composable
fun SettingsEntry(settingsItem: SettingsItem) = when (settingsItem) {
    is SettingsItem.SettingsHeading -> Text(
        modifier = Modifier
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        text = LocalContext.current.getString(settingsItem.labelId),
        style = MaterialTheme.typography.title2
    )
    is SettingsItem.SettingsButton -> Chip(
        onClick = settingsItem.onClick,
        label = { Text(LocalContext.current.getString(settingsItem.labelId)) },
        icon = {
            Icon(
                imageVector = settingsItem.imageVector,
                stringResource(id = settingsItem.labelId)
            )
        }
    )
}

sealed class SettingsItem {
    data class SettingsButton(
        val labelId: Int,
        val imageVector: ImageVector,
        val onClick: () -> Unit
    ) :
        SettingsItem()

    data class SettingsHeading(val labelId: Int) : SettingsItem()
}

