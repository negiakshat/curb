package com.example.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CurbOnSurfaceVariant

@Composable
fun CurbSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 4.dp
) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = CurbOnSurfaceVariant,
        letterSpacing = 1.sp,
        modifier = modifier.padding(horizontal = horizontalPadding)
    )
}
