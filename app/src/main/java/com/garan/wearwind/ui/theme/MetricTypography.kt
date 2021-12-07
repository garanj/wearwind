package com.garan.wearwind.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Holds definitions of text styling for the main Wearwind displays when connected. As these labels
 * don't fit into the existing [Typography] semantic definitions, these additional names are defined
 * as opposed to trying to somehow reuse/misuse Title1 etc.
 */
data class MetricTypography(
    val largeDisplayMetric: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontStyle = FontStyle.Italic,
        fontSize = 108.sp
    ),
    val mediumDisplayMetric: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontStyle = FontStyle.Italic,
        fontSize = 64.sp
    ),
    val smallDisplayMetric: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontStyle = FontStyle.Italic,
        fontSize = 48.sp
    )
)
