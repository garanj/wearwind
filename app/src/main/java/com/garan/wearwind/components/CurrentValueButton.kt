package com.garan.wearwind.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.garan.wearwind.ui.theme.Colors

@Composable
fun CurrentValueButton(
    currentValue: Int,
    metricLabel: String,
    isSelected: Boolean,
    buttonSize: Dp,
    onClick: () -> Unit
) {
    val color = if (isSelected) Colors.primary else Color.Black
    Button(
        modifier = Modifier
            .size(buttonSize)
            .padding(8.dp)
            .aspectRatio(1f)
            .border(2.dp, color, CircleShape),
        colors = ButtonDefaults.secondaryButtonColors(),
        onClick = onClick
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(fontStyle = MaterialTheme.typography.display3.fontStyle)) {
                    append("$currentValue\n")
                }
                withStyle(style = SpanStyle(fontStyle = MaterialTheme.typography.body2.fontStyle)) {
                    append(metricLabel)
                }
            }
        )
    }
}
