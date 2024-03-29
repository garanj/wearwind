package com.garan.wearwind.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.wear.compose.material.AutoCenteringParams
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.ScalingLazyListAnchorType
import androidx.wear.compose.material.Text
import com.garan.wearwind.Screen
import com.garan.wearwind.UiState

@Composable
fun SettingsScreen(
    uiState: UiState,
    screenStarted: Boolean = uiState.navHostController
        .getBackStackEntry(Screen.SETTINGS.route)
        .lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED),
    settingsItemList: List<SettingsItem>
) {
    LaunchedEffect(screenStarted) {
        if (screenStarted) {
            uiState.isShowTime.value = false
            uiState.isShowVignette.value = true
        }
    }
    ScalingLazyColumn(
        autoCentering = AutoCenteringParams(2),
        anchorType = ScalingLazyListAnchorType.ItemStart
    ) {
        items(settingsItemList.size) {
            SettingsEntry(settingsItem = settingsItemList[it])
        }
    }
}

@Composable
fun SettingsEntry(settingsItem: SettingsItem) = when (settingsItem) {
    is SettingsItem.SettingsHeading -> Text(
        modifier = Modifier
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
        text = stringResource(settingsItem.labelId),
        style = MaterialTheme.typography.title2
    )
    is SettingsItem.SettingsButton -> Chip(
        modifier = Modifier.fillMaxWidth(),
        onClick = settingsItem.onClick,
        label = { Text(stringResource(settingsItem.labelId)) },
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
