package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Standardized Corner Radii across the whole UI hierarchy
val RadiusHero = 28.dp        // Hierarchy Level 1: Hero cards, bottom sheets, viewfinders
val RadiusCard = 24.dp        // Hierarchy Level 2: Bento tiles, standard cards, dialogs
val RadiusNested = 16.dp      // Hierarchy Level 3: Nested items, inputs, list tiles, status boxes
val RadiusChip = 14.dp        // Hierarchy Level 4: Badges, preset pills, chips
val RadiusSmall = 8.dp        // Hierarchy Level 5: Micro tags, indicators

val CurbShapes = Shapes(
    extraSmall = RoundedCornerShape(RadiusSmall),
    small = RoundedCornerShape(RadiusChip),
    medium = RoundedCornerShape(RadiusNested),
    large = RoundedCornerShape(RadiusCard),
    extraLarge = RoundedCornerShape(RadiusHero)
)
