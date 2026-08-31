package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val CurbShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),      // Small controls: 12dp-16dp
    medium = RoundedCornerShape(16.dp),     // Inputs: 16dp-20dp
    large = RoundedCornerShape(24.dp),      // Standard cards: 24dp
    extraLarge = RoundedCornerShape(32.dp)  // Major hero cards, sheets: 28dp-32dp
)
