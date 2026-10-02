package com.reader.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class ReadingPaletteTest {

    @Test
    fun `all five reading themes are registered`() {
        val allTypes = ReadingThemeType.entries
        assertThat(ReadingPalettes.all).hasSize(5)
        assertThat(allTypes).hasSize(5)

        val registeredTypes = ReadingPalettes.all.map { it.type }
        assertThat(registeredTypes).containsExactlyElementsIn(allTypes)
    }

    @Test
    fun `fromType returns correct palette for each enum`() {
        for (type in ReadingThemeType.entries) {
            val palette = ReadingPalettes.fromType(type)
            assertThat(palette.type).isEqualTo(type)
        }
    }

    @Test
    fun `fromName is case insensitive and handles fallback`() {
        assertThat(ReadingThemeType.fromName("parchment")).isEqualTo(ReadingThemeType.PARCHMENT)
        assertThat(ReadingThemeType.fromName("DEFAULT_LIGHT")).isEqualTo(ReadingThemeType.DEFAULT_LIGHT)
        assertThat(ReadingThemeType.fromName("eye_care_green")).isEqualTo(ReadingThemeType.EYE_CARE_GREEN)
        assertThat(ReadingThemeType.fromName("e_ink")).isEqualTo(ReadingThemeType.E_INK)
        assertThat(ReadingThemeType.fromName("oled_black")).isEqualTo(ReadingThemeType.OLED_BLACK)
        assertThat(ReadingThemeType.fromName("invalid_theme")).isEqualTo(ReadingThemeType.DEFAULT_LIGHT)
    }

    @Test
    fun `oled black theme has pure black background and dark flag`() {
        val oled = ReadingPalettes.OledBlack
        assertThat(oled.isDark).isTrue()
        assertThat(oled.canvasBackground).isEqualTo(Color(0xFF000000))
        assertThat(oled.canvasBackgroundArgb).isEqualTo(0xFF000000.toInt())
    }

    @Test
    fun `e-ink theme is pure black text on white canvas with e-ink flag`() {
        val eink = ReadingPalettes.EInk
        assertThat(eink.isEInk).isTrue()
        assertThat(eink.isDark).isFalse()
        assertThat(eink.canvasBackground).isEqualTo(Color(0xFFFFFFFF))
        assertThat(eink.textColor).isEqualTo(Color(0xFF000000))
    }

    @Test
    fun `all palettes have sufficient contrast ratio between canvas and text`() {
        // WCAG relative contrast ratio: (L1 + 0.05) / (L2 + 0.05)
        // For reading texts, minimum recommended contrast ratio is >= 4.5:1
        for (palette in ReadingPalettes.all) {
            val lumBg = palette.canvasBackground.luminance()
            val lumText = palette.textColor.luminance()

            val lighter = max(lumBg, lumText)
            val darker = min(lumBg, lumText)
            val contrastRatio = (lighter + 0.05f) / (darker + 0.05f)

            // Assert each theme has legible contrast >= 4.5
            assertThat(contrastRatio).isAtLeast(4.5f)
        }
    }

    @Test
    fun `argb integer getters return non-zero valid color values`() {
        val defaultLight = ReadingPalettes.DefaultLight
        assertThat(defaultLight.canvasBackgroundArgb).isNotEqualTo(0)
        assertThat(defaultLight.textColorArgb).isNotEqualTo(0)
        assertThat(defaultLight.secondaryTextColorArgb).isNotEqualTo(0)
        assertThat(defaultLight.headerFooterColorArgb).isNotEqualTo(0)
        assertThat(defaultLight.dividerColorArgb).isNotEqualTo(0)
    }
}
