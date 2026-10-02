package com.reader.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReadingTypographyTest {

    @Test
    fun `clampFontSize constrains within minimum and maximum bounds`() {
        assertThat(ReadingFontScale.clampFontSize(5)).isEqualTo(ReadingFontScale.FONT_SIZE_MIN)
        assertThat(ReadingFontScale.clampFontSize(18)).isEqualTo(18)
        assertThat(ReadingFontScale.clampFontSize(50)).isEqualTo(ReadingFontScale.FONT_SIZE_MAX)
    }

    @Test
    fun `font size presets are strictly ascending and valid`() {
        val presets = ReadingFontScale.PRESETS
        assertThat(presets).isNotEmpty()
        for (i in 0 until presets.size - 1) {
            assertThat(presets[i]).isLessThan(presets[i + 1])
            assertThat(presets[i]).isAtLeast(ReadingFontScale.FONT_SIZE_MIN)
            assertThat(presets[i + 1]).isAtMost(ReadingFontScale.FONT_SIZE_MAX)
        }
    }

    @Test
    fun `typography specs calculates lineHeight and paragraphSpacing correctly`() {
        val specs = ReadingTypographySpecs(
            fontSizeSp = 20,
            lineSpacingMultiplier = 1.5f,
            paragraphSpacingMultiplier = 1.0f,
            letterSpacingSp = 0.5f
        )

        assertThat(specs.lineHeightSp).isEqualTo(30.sp)
        assertThat(specs.paragraphSpacingSp).isEqualTo(20.sp)
        assertThat(specs.letterSpacing).isEqualTo(0.5.sp)
    }

    @Test
    fun `toTextStyle builds valid TextStyle with matching properties`() {
        val specs = ReadingTypographySpecs(fontSizeSp = 18, lineSpacingMultiplier = 1.6f)
        val style = specs.toTextStyle(Color.Black)

        assertThat(style.fontSize).isEqualTo(18.sp)
        assertThat(style.color).isEqualTo(Color.Black)
        assertThat(style.lineHeight).isEqualTo((18 * 1.6f).sp)
    }
}
