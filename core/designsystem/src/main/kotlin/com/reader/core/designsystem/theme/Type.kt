package com.reader.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Standard Material 3 Typography configuration for Reader App.
 * Covers Display, Headline, Title, Body, and Label scales.
 */
val ReaderTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * Reading font scale and line spacing specification ladder.
 * Used for reader content sizing, settings sliders, and typography measurements.
 */
object ReadingFontScale {
    const val FONT_SIZE_MIN: Int = 12
    const val FONT_SIZE_MAX: Int = 42
    const val FONT_SIZE_DEFAULT: Int = 18
    const val FONT_SIZE_STEP: Int = 2

    /** Standard discreet font size presets (in sp) */
    val PRESETS: List<Int> = listOf(12, 14, 16, 18, 20, 22, 24, 28, 32, 36, 40)

    /** Clamps font size within allowed range */
    fun clampFontSize(sizeSp: Int): Int {
        return sizeSp.coerceIn(FONT_SIZE_MIN, FONT_SIZE_MAX)
    }

    /**
     * Line spacing multiplier options.
     */
    val LINE_SPACING_PRESETS: List<Float> = listOf(1.2f, 1.4f, 1.6f, 1.8f, 2.0f)
    const val LINE_SPACING_DEFAULT: Float = 1.5f

    /**
     * Paragraph spacing multiplier options (relative to line spacing).
     */
    val PARAGRAPH_SPACING_PRESETS: List<Float> = listOf(0.5f, 1.0f, 1.5f, 2.0f)
    const val PARAGRAPH_SPACING_DEFAULT: Float = 1.0f

    /**
     * Letter spacing options (in sp).
     */
    val LETTER_SPACING_PRESETS: List<Float> = listOf(0f, 0.5f, 1.0f, 1.5f, 2.0f)
    const val LETTER_SPACING_DEFAULT: Float = 0.5f
}

/**
 * Immutable specifications for reader text rendering.
 */
@Immutable
data class ReadingTypographySpecs(
    val fontSizeSp: Int = ReadingFontScale.FONT_SIZE_DEFAULT,
    val lineSpacingMultiplier: Float = ReadingFontScale.LINE_SPACING_DEFAULT,
    val paragraphSpacingMultiplier: Float = ReadingFontScale.PARAGRAPH_SPACING_DEFAULT,
    val letterSpacingSp: Float = ReadingFontScale.LETTER_SPACING_DEFAULT,
    val firstLineIndentChars: Int = 2
) {
    /** Computes effective line height in SP */
    val lineHeightSp: TextUnit
        get() = (fontSizeSp * lineSpacingMultiplier).sp

    /** Computes effective paragraph extra spacing in SP */
    val paragraphSpacingSp: TextUnit
        get() = (fontSizeSp * paragraphSpacingMultiplier).sp

    /** Letter spacing as TextUnit */
    val letterSpacing: TextUnit
        get() = letterSpacingSp.sp

    /** Compose TextStyle representation for reader previews or chapter headers */
    fun toTextStyle(color: androidx.compose.ui.graphics.Color): TextStyle {
        return TextStyle(
            fontSize = fontSizeSp.sp,
            lineHeight = lineHeightSp,
            letterSpacing = letterSpacing,
            color = color,
            fontWeight = FontWeight.Normal
        )
    }
}
