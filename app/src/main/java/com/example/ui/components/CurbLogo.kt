package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CurbBlack

@Composable
fun CurbIconSymbol(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    tint: Color = CurbBlack
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = w * 0.16f

        // Draw Curb's abstract bold geometric symbol
        // Outer stylized C / parking curve
        val path = Path().apply {
            moveTo(w * 0.85f, h * 0.22f)
            cubicTo(
                w * 0.65f, h * 0.08f,
                w * 0.28f, h * 0.08f,
                w * 0.18f, h * 0.32f
            )
            cubicTo(
                w * 0.08f, h * 0.52f,
                w * 0.08f, h * 0.68f,
                w * 0.22f, h * 0.82f
            )
            cubicTo(
                w * 0.36f, h * 0.94f,
                w * 0.68f, h * 0.94f,
                w * 0.85f, h * 0.78f
            )
        }

        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Center parking beam indicator
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.40f, h * 0.44f),
            size = Size(w * 0.45f, strokeWidth * 0.85f),
            cornerRadius = CornerRadius(strokeWidth * 0.4f, strokeWidth * 0.4f)
        )
    }
}

@Composable
fun CurbLogo(
    modifier: Modifier = Modifier,
    symbolSize: Dp = 28.dp,
    fontSize: Int = 22,
    showWordmark: Boolean = true,
    tint: Color = CurbBlack
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CurbIconSymbol(size = symbolSize, tint = tint)
        if (showWordmark) {
            Text(
                text = "CURB",
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.SansSerif,
                color = tint
            )
        }
    }
}
