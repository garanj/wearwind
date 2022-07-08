package com.garan.wearwind.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Icon
import com.garan.wearwind.FanControlService
import com.garan.wearwind.R

@Composable
fun HrButton(
    connectionStatus: FanControlService.FanConnectionStatus,
    hrEnabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val iconId = if (hrEnabled) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder
    Button(
        onClick = onClick,
        enabled = connectionStatus == FanControlService.FanConnectionStatus.DISCONNECTED
    ) {
        Icon(
            imageVector = iconId,
            stringResource(id = R.string.hr)
        )
    }
}