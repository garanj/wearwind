package com.garan.wearwind.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
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
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 10.dp,
            end = 20.dp,
            bottom = 30.dp
        )
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
