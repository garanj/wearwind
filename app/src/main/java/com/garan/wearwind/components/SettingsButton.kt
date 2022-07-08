package com.garan.wearwind.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Icon
import com.garan.wearwind.R

@Composable
fun SettingsButton(
    onClick: () -> Unit,
    enabled: Boolean
) {
    Button(
        onClick = onClick,
        enabled = enabled
    ) {
        Icon(
            imageVector = Icons.Rounded.Settings,
            stringResource(id = R.string.settings)
        )
    }
}