package com.reader.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Standard Material 3 Shape tokens for Reader App.
 */
val ReaderShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

/**
 * Card and dialog specific corner radii.
 */
object ReaderCornerRadii {
    val BookCover = RoundedCornerShape(6.dp)
    val Chip = RoundedCornerShape(8.dp)
    val Card = RoundedCornerShape(12.dp)
    val BottomSheet = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    val Dialog = RoundedCornerShape(24.dp)
    val Pill = RoundedCornerShape(50)
}
