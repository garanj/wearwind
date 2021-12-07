package com.garan.wearwind.ui.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme

@Composable
fun WearwindTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = Colors,
        content = content
    )
}
