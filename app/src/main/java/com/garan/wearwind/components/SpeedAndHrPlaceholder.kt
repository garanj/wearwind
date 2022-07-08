package com.garan.wearwind.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.garan.wearwind.R

@Composable
fun SpeedAndHrPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.waiting_for_hr),
            color = MaterialTheme.colors.primary,
            style = MaterialTheme.typography.title1,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.ExtraBold
        )
    }
}